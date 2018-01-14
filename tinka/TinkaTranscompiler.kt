package tinka

import java.io.*

abstract class TinkaTranscompiler(val inFiles: Array<String>) {
	companion object {
		private val monoOperatorList = listOf("nac")

		private val biOperatorList = listOf(
				"krz", "kRz", "ata", "nta",
				"kak", "ada",
				"ekc", "nac", "dal", "dto",
				"dro", "dRo", "dtosna")

		private val triOperatorList = listOf("lat", "latsna")

		private val compareMap = mapOf(
				Pair("xtlo", "llo"), Pair("xylo", "xolo"),
				Pair("clo", "niv"), Pair("niv", "clo"),
				Pair("llo", "xtlo"), Pair("xolo", "xylo"),
				Pair("xtlonys", "llonys"), Pair("xylonys", "xolonys"),
				Pair("llonys", "xtlonys"), Pair("xolonys", "xylonys"))

		private val retCharCodes = arrayOf('\n'.toInt(), '\r'.toInt())
	}

	protected val expressions: MutableList<TinkaExpression>
	protected val kuexok: MutableMap<String, Boolean>

	protected var useVarStack: Long
	protected val varDictionary: MutableMap<String, Long>
	protected val countMap: MutableMap<String, Int>
	protected val rinyvStack: MutableList<String>
	var hasMain: Boolean

	init {
		expressions = mutableListOf()
		kuexok = mutableMapOf()

		useVarStack = 0
		varDictionary = mutableMapOf()
		countMap = mutableMapOf(
				Pair("fi", 0), Pair("fal-rinyv", 0),
				Pair("fal", 0), Pair("dosnud", 0))
		rinyvStack = mutableListOf()

		hasMain = false
	}

	constructor(inFiles: List<String>) : this(inFiles.toTypedArray()) {}

	fun run(outFile: String) {
		if(!inFiles.all { x -> x.endsWith(".tinka") }) {
			throw RuntimeException("Included to be not tinka files");
		}

		for(file in inFiles) {
			val wordList = read(file)

			// デバッグ用
			println(wordList.fold(StringBuilder("["), { buffer, x -> buffer.append("\"").append(x).append("\",")}).append("]").toString())

			analyze(wordList)
		}

		println(this.expressions.fold(StringBuilder("\nTinkaExpressionList: [\n"), { buffer, x -> buffer.append("  ").append(x).append("\n")}).append("]").toString())

		write(outFile)
	}

	private fun read(inFile: String): List<String> {
		val wordList = mutableListOf<String>()
		val buffer = StringBuffer()

		try {
			File(inFile).bufferedReader().use { reader ->
				var c = reader.read()
				while(c != -1) {
					val ch = c.toChar()

					if(ch == '-') {
						c = reader.read()
						if(c == -1) { break }
						else if(c.toChar() == '-') {
							if(buffer.length > 0) {
								wordList.add(buffer.toString())
								buffer.delete(0, buffer.length)
							}

							do {
								c = reader.read()
								if(c == -1) {
									break
								}
							} while(c !in retCharCodes)
						}
						else {
							buffer.append('-').append(c.toChar())
						}
					}
					else if(ch.isWhitespace()) {
						if(buffer.length > 0) {
							wordList.add(buffer.toString())
							buffer.delete(0, buffer.length)
						}
					}
					else {
						buffer.append(ch)
					}

					c = reader.read()
				}
			}
		}
		catch(e: IOException) {
			throw RuntimeException(e)
		}

		return wordList.toList()
	}

	private fun analyze(wordList: List<String>) {
		var rinyvCount = 0
		var isFuncMode = false
		var i = 0

		fun compareOperation(word: String): String = compareMap[word]
			?: throw IllegalArgumentException("unknown 'fi ${wordList[i - 1]} ${wordList[i]} ${wordList[i + 1]}'")

		while(i < wordList.size) {
			val str = wordList[i]

			when(str) {
				"kue" -> {
					if(isFuncMode) {
						throw RuntimeException("Can't use in cersva: ${str}")
					}

					val label = wordList[++i]
					this.expressions.add(Kue(label))

					this.kuexok[label] = true
				}
				"xok" -> {
					if(isFuncMode) {
						throw RuntimeException("Can't use in cersva: ${str}");
					}

					val label = wordList[++i]
					this.expressions.add(Xok(label))

					if(!this.kuexok.containsKey(label)) {
						this.kuexok[label] = false
					}
				}
				"anax" -> {
					if(!isFuncMode) {
						throw RuntimeException("Can't use out of cersva: ${str}");
					}

					val label = wordList[++i]
					val anaxVar = if(label.indexOf("@") != -1) {
						val split = label.split("@")
						Anax(split[0], true, split[1].toLong())
					} else {
						Anax(label)
					}

					this.expressions.add(anaxVar)
				}
				"fi" -> {
					if(!isFuncMode) {
						throw RuntimeException("Can't use out of cersva: ${str}");
					}

					this.expressions.add(Fi(toOperand(wordList[++i]), compareOperation(wordList[++i]), toOperand(wordList[++i])))
				}
				"fal" -> {
					if(!isFuncMode) {
						throw RuntimeException("Can't use out of cersva: ${str}");
					}

					this.expressions.add(Fal(toOperand(wordList[++i]), compareOperation(wordList[++i]), toOperand(wordList[++i])))
				}
				"rinyv" -> {
					this.expressions.add(Rinyv(++rinyvCount))
				}
				"situv" -> {
					if(rinyvCount == 1) {
						isFuncMode = false
					}
					this.expressions.add(Situv(rinyvCount--))
				}
				in monoOperatorList -> {
					if(!isFuncMode) {
						throw RuntimeException("Can't use out of cersva: ${str}");
					}

					this.expressions.add(Operation(str, listOf(toOperand(wordList[++i]))))
				}
				in biOperatorList -> {
					if(!isFuncMode) {
						throw RuntimeException("Can't use out of cersva: ${str}");
					}

					this.expressions.add(Operation(str, listOf(toOperand(wordList[++i]), toOperand(wordList[++i]))))
				}
				in triOperatorList -> {
					if(!isFuncMode) {
						throw RuntimeException("Can't use out of cersva: ${str}");
					}

					this.expressions.add(Operation(str, listOf(toOperand(wordList[++i]), toOperand(wordList[++i]), toOperand(wordList[++i]))))
				}
				"cersva" -> {
					if(isFuncMode) {
						throw RuntimeException("Can't use in cersva: ${str}")
					}

					fun toAnax(word: String): Anax {
						return if(word.indexOf("@") != -1) {
							Anax(word.dropLast(1), true)
						}
						else {
							Anax(word)
						}
					}

					val label = wordList[++i]
					if(label[0] in '0'..'9') {
						throw RuntimeException("Bad cersva-akrapt '${label}'")
					}
					else if(label == "'3126834864") {
						throw RuntimeException("Already define '3126834864")
					}

					if(label == "_fasal") {
						this.hasMain = true
					}

					val subList = wordList.drop(i + 1)
					val rinyvIndex = subList.indexOf("rinyv")

					if(rinyvIndex == -1) {
						throw RuntimeException("Not found 'rinyv'")
					}

					this.expressions.add(Cersva(label, subList.take(rinyvIndex).map{ x -> toAnax(x) }.toList()))
					i += rinyvIndex
					isFuncMode = true
				}
				"dosnud" -> {
					if(!isFuncMode) {
						throw RuntimeException("Can't use out of cersva: ${str}");
					}

					this.expressions.add(Dosnud(toOperand(wordList[++i])))
				}
				"fenxeo" -> {
					if(!isFuncMode) {
						throw RuntimeException("Can't use in cersva: ${str}")
					}

					var label = wordList[++i]
					if(label[0] in '0'..'9') {
						throw RuntimeException("Bad cersva name: '${label}'")
					}
					if(label == "'3126834864") {
						label = "3126834864"
					}

					val subList = wordList.drop(i + 1)
					val elIndex = subList.indexOf("el")

					if(elIndex == -1) {
						throw RuntimeException("Not found 'el'")
					}
					else {
						i += elIndex + 1
						var operandList = subList.take(elIndex).map { x -> toOperand(x) }.toList()

						val setVarName = wordList[++i]
						if(setVarName == "niv") {
							this.expressions.add(Fenxeo(label, operandList, Anakswa))
						}
						else {
							val setVar = toOperand(setVarName)
							if(setVar is AnaxName) {
								this.expressions.add(Fenxeo(label, operandList, setVar))
							}
							else {
								throw RuntimeException("Not variable")
							}
						}
					}
				}
			}

			i++;
		}

		if(rinyvCount != 0) {
			throw RuntimeException("'rinyv' count do not equals 'situv' count.")
		}
	}

	private fun toOperand(word: String): TinkaOperand {
		return if(word.all { x -> x.isDigit() }) {
			Constant(word)
		} else if(word.indexOf("@") != -1) {
			val split = word.split("@", ignoreCase = false, limit = 2)
			if(split[1] == "") {
				throw RuntimeException("Invalid operand '${word}'")
			}
			else {
				AnaxName(split[0], toOperand(split[1]))
			}
		} else {
			AnaxName(word)
		}
	}

	abstract protected fun write(outFile: String)
}

