package tinka;

import java.io.*

class TinkaTranspiler(val inFiles: Array<String>) {
    val expressions: MutableList<TinkaExpression>
    val kuexok: MutableMap<String, Boolean>

    init {
        kuexok = mutableMapOf()
        expressions = mutableListOf()
    }

    constructor(inFiles: List<String>) : this(inFiles.toTypedArray()) {}

    fun run(outFile: String) {
        for(file in inFiles) {
            val wordList = read(file)

            println(wordList.fold(StringBuilder("["), { buffer, x -> buffer.append("\"").append(x).append("\",")}).append("]").toString())

            analyze(wordList)
        }

        write(outFile)
    }

    private fun read(inFile: String): List<String> {
        val wordList = mutableListOf<String>()
        val buffer = StringBuffer()
        val retCharCodes = arrayOf('\n'.toInt(), '\r'.toInt())

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

    private fun analyze(wordList: List<String>) {
        val list = mutableListOf<TinkaExpression>()
        val funcList = mutableListOf<TinkaExpression>()
        var isMain = true
        var i = 0
        var rinyvCount = 0

        fun compareOperation(word: String): String = compareMap[word]
            ?: throw IllegalArgumentException("unknown 'fi ${wordList[i - 1]} ${wordList[i]} ${wordList[i + 1]}'")

        while (i < wordList.size) {
            val str = wordList[i]
            var label: String?
            val rinyvIndex: Int
            val subList: List<String>

            when(str) {
                "kue" -> {
                    label = wordList[++i]
                    list.add(Kue(label))

                    this.kuexok[label] = true
                    isMain = false
                }
                "xok" -> {
                    label = wordList[++i]
                    list.add(Xok(label))

                    this.kuexok[label] = true
                }
                "anax" -> {
                    label = wordList[++i]

                    list.add(if(label.indexOf("@") != -1) {
                        val split = label.split("@")
                        Anax(split[0], true, split[1].toInt())
                    } else {
                        Anax(label)
                    })
                }
                "cersva" -> {
                    label = wordList[++i]
                    subList = wordList.drop(i + 1)
                    rinyvIndex = subList.indexOf("rinyv")

                    if(rinyvIndex == -1) {
                        throw Exception("Not found 'rinyv'")
                    }
                    else {
                        funcList.add(Cersva(label, subList.take(rinyvIndex).map{ x -> AnaxName(x) }.toList()))
                        i += rinyvIndex
                    }
                }
                "fi" -> {
                    list.add(Fi(toOperand(wordList[++i]), compareOperation(wordList[++i]), toOperand(wordList[++i])))
                }
                "fal" -> {
                    list.add(Fal(toOperand(wordList[++i]), compareOperation(wordList[++i]), toOperand(wordList[++i])))
                }
                "rinyv" -> {
                    rinyvCount++
                    list.add(Rinyv(rinyvCount))
                }
                "situv" -> {
                    list.add(Situv(rinyvCount))
                    rinyvCount--
                }
                in monoOperatorList -> {
                    list.add(Operation(str, listOf(toOperand(wordList[++i]))))
                }
                in biOperatorList -> {
                    list.add(Operation(str, listOf(toOperand(wordList[++i]), toOperand(wordList[++i]))))
                }
                in triOperatorList -> {
                    list.add(Operation(str, listOf(toOperand(wordList[++i]), toOperand(wordList[++i]), toOperand(wordList[++i]))))
                }
                "dosnud" -> {
                    list.add(Dosnud(toOperand(wordList[++i])))
                }
                "fenxeo" -> {
                    label = wordList[++i]
                    subList = wordList.drop(i + 1)
                    rinyvIndex = subList.indexOf("el")

                    if(rinyvIndex == -1) {
                        throw Exception("Not found 'el'")
                    }
                    else {
                        i += rinyvIndex + 1
                        var operandList = subList.take(rinyvIndex).map { x -> toOperand(x) }.toList()
                        val setVar = toOperand(wordList[++i])
                        list.add(Fenxeo(label, operandList, when(setVar) {
                            is AnaxName -> setVar
                            else -> throw RuntimeException("Not variable")
                        } ))
                    }
                }
            }

            i++
        }

        list.addAll(funcList)
        if(rinyvCount != 0) {
            throw Exception("'rinyv' count do not equals 'situv' count.")
        }

        println(list.fold(StringBuilder("\nTinkaExpressionList: [\n"), { buffer, x -> buffer.append("  ").append(x).append("\n")}).append("]").toString())

        if(isMain) {
            expressions.addAll(0, list)
        }
        else {
            expressions.addAll(list)
        }
    }

    private fun toOperand(word: String): TinkaOperand {
        if(word.all { x -> x.isDigit() }) {
            return Constant(word)
        }
        else {
            return if(word.indexOf("@") != -1) {
                val split = word.split("@")
                AnaxName(split[0], split[1].toInt())
            } else {
                AnaxName(word)
            }
        }
    }

    private fun write(outFile: String) {
        try {
            File(outFile).printWriter().use { writer ->
                var varStack = 0
                var varDictionary: MutableMap<String, Int> = mutableMapOf()
                var rinyvStack: MutableList<String> = mutableListOf();

                var fiCount = 0
                var falRinyvCount = 0
                var falCount = 0

                fun convert(opd: TinkaOperand, count: Int = 0): String {
                    return when(opd) {
                        is Constant -> opd.value
                        is AnaxName -> "f5+${((varStack - (varDictionary[opd.varName] as Int) + opd.pos + count) * 4)}@"
                        else -> throw RuntimeException("unknown")
                    }
                }

                for(opd in expressions) {
                    when(opd) {
                        is Kue -> writer.println("kue ${opd.labelName}")
                        is Xok -> writer.println("xok ${opd.labelName}")
                        is Fi -> {
                            val lval = convert(opd.left)
                            val rval = convert(opd.right)
                            writer.println("fi ${lval} ${rval} ${opd.compare}")
                            rinyvStack.add(0, "fi${(fiCount++)}")
                        }
                        is Fal -> {
                            val lval = convert(opd.left)
                            val rval = convert(opd.right)
                            writer.println("fi ${lval} ${rval} ${opd.compare} l' fal-rinyv${(falRinyvCount)}")
                            rinyvStack.add(0, "fal-rinyv${(falRinyvCount++)}");
                            rinyvStack.add(0, "fal${(falCount++)}");
                        }
                        is Anax -> {
                            varStack += opd.length
                            writer.println("nta ${(opd.length * 4)} f5")
                            varDictionary[opd.varName] = varStack
                        }
                        //is Cersva -> {
                        //    writer.println("nll ${opd.funcName}")
                        //    isFunc = true
                        //}
                        is Rinyv -> {
                            val label = rinyvStack[0]
                            when {
                                label.startsWith("fi") -> writer.println("malkrz ${label} xx")
                                label.startsWith("fal") -> writer.println("malkrz ${label} xx")
                            }
                        }
                        is Situv -> {
                            val label = rinyvStack.removeAt(0)
                            when {
                                label.startsWith("fi") -> writer.println("nll ${label}")
                                label.startsWith("fal") -> {
                                    val loop = rinyvStack.removeAt(0)
                                    writer.println("krz ${loop} xx")
                                    writer.println("nll ${label}")
                                }
                            }
                        }
                        is Fenxeo -> {
                            opd.arguments.forEachIndexed { i, x ->
                                writer.println("nta 4 f5 krz ${convert(x, i + 1)} f5@")
                            }
                            writer.println("nta 4 f5 inj ${opd.funcName} xx f5@ ata ${((opd.arguments.size + 1) * 4)} f5 krz f0 ${convert(opd.setVar)}")
                        }
                        //is Dosnud -> writer.println("krz ${opd.retVal} f0 krz f5@ xx")
                        is Operation -> {
                            val args = opd.arguments.fold(StringBuilder(), {
                                buffer, x -> buffer.append(" ").append(convert(x))
                            }).toString()
                            writer.println("${opd.mnemonic}${args}")
                        }
                        else -> {}
                    }
                }

                writer.println("ata ${(varStack * 4)} f5");
                writer.println("krz f5@ xx");

                writer.close()
            }
        }
        catch(e: IOException) {
            throw RuntimeException(e)
        }
    }
}

