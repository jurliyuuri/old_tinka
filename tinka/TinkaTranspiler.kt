package tinka;

import java.io.*

class TinkaTranspiler(val inFiles: Array<String>) {
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
    }

    val expressions: MutableList<TinkaExpression>
    val funcExpressions: MutableList<TinkaExpression>
    val kuexok: MutableMap<String, Boolean>

    var varStack: Int
    var fiCount: Int
    var falRinyvCount: Int
    var falCount: Int
    var dosnudCount: Int
    val varDictionary: MutableMap<String, Int>
    val rinyvStack: MutableList<String>

    init {
        kuexok = mutableMapOf()
        expressions = mutableListOf()
        funcExpressions = mutableListOf()

        varDictionary = mutableMapOf()
        rinyvStack = mutableListOf()

        varStack = 0
        fiCount = 0
        falRinyvCount = 0
        falCount = 0
        dosnudCount = 0
    }

    constructor(inFiles: List<String>) : this(inFiles.toTypedArray()) {}

    fun run(outFile: String) {
        if(!inFiles.all { x -> x.endsWith(".tinka") }) {
            throw RuntimeException("Include to be not tinka files");
        }

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

    private fun analyze(wordList: List<String>) {
        val list = mutableListOf<TinkaExpression>()
        val funcList = mutableListOf<TinkaExpression>()
        var isFuncMode = false
        var isMain = true
        var i = 0
        var rinyvCount = 0

        fun compareOperation(word: String): String = compareMap[word]
            ?: throw IllegalArgumentException("unknown 'fi ${wordList[i - 1]} ${wordList[i]} ${wordList[i + 1]}'")

        fun toAnax(word: String): Anax = if(word.endsWith("@")) {
                Anax(word.dropLast(1), true)
            } else {
                Anax(word, false)
            }

        while (i < wordList.size) {
            val str = wordList[i]
            var label: String?
            val rinyvIndex: Int
            val subList: List<String>

            when(str) {
                "kue" -> {
                    if(isFuncMode) {
                        throw RuntimeException("Can't defined cersva in cersva");
                    }

                    label = wordList[++i]
                    list.add(Kue(label))

                    this.kuexok[label] = true
                    isMain = false
                }
                "xok" -> {
                    if(isFuncMode) {
                        throw RuntimeException("Can't defined cersva in cersva");
                    }

                    label = wordList[++i]
                    list.add(Xok(label))

                    this.kuexok[label] = true
                }
                "anax" -> {
                    label = wordList[++i]
                    val anaxVar = if(label.indexOf("@") != -1) {
                        val split = label.split("@")
                        Anax(split[0], true, split[1].toInt())
                    } else {
                        Anax(label)
                    }

                    if(isFuncMode) {
                        funcList.add(anaxVar)
                    }
                    else {
                        list.add(anaxVar)
                    }
                }
                "cersva" -> {
                    if(isFuncMode) {
                        throw RuntimeException("Can't defined cersva in cersva");
                    }

                    label = wordList[++i]
                    if(label[0] in '0'..'9') {
                        throw RuntimeException("Bad cersva-akrapt '${label}'")
                    }
                    if(label == "'3126834864") {
                        throw RuntimeException("Already define '3126834864")
                    }
                    subList = wordList.drop(i + 1)
                    rinyvIndex = subList.indexOf("rinyv")

                    if(rinyvIndex == -1) {
                        throw RuntimeException("Not found 'rinyv'")
                    }

                    funcList.add(Cersva(label, subList.take(rinyvIndex).map{ x -> toAnax(x) }.toList()))
                    i += rinyvIndex
                    isFuncMode = true
                }
                "fi" -> {
                    if(isFuncMode) {
                        funcList.add(Fi(toOperand(wordList[++i]), compareOperation(wordList[++i]), toOperand(wordList[++i])))
                    }
                    else {
                        list.add(Fi(toOperand(wordList[++i]), compareOperation(wordList[++i]), toOperand(wordList[++i])))
                    }
                }
                "fal" -> {
                    if(isFuncMode) {
                        funcList.add(Fal(toOperand(wordList[++i]), compareOperation(wordList[++i]), toOperand(wordList[++i])))
                    }
                    else {
                        list.add(Fal(toOperand(wordList[++i]), compareOperation(wordList[++i]), toOperand(wordList[++i])))
                    }
                }
                "rinyv" -> {
                    rinyvCount++

                    if(isFuncMode) {
                        funcList.add(Rinyv(rinyvCount))
                    }
                    else {
                        list.add(Rinyv(rinyvCount))
                    }
                }
                "situv" -> {
                    if(isFuncMode) {
                        funcList.add(Situv(rinyvCount))
                        if(rinyvCount == 1) {
                            isFuncMode = false
                        }
                    }
                    else {
                        list.add(Situv(rinyvCount))
                    }

                    rinyvCount--
                }
                in monoOperatorList -> {
                    if(isFuncMode) {
                        funcList.add(Operation(str, listOf(toOperand(wordList[++i]))))
                    }
                    else {
                        list.add(Operation(str, listOf(toOperand(wordList[++i]))))
                    }
                }
                in biOperatorList -> {
                    if(isFuncMode) {
                        funcList.add(Operation(str, listOf(toOperand(wordList[++i]), toOperand(wordList[++i]))))
                    }
                    else {
                        list.add(Operation(str, listOf(toOperand(wordList[++i]), toOperand(wordList[++i]))))
                    }
                }
                in triOperatorList -> {
                    if(isFuncMode) {
                        funcList.add(Operation(str, listOf(toOperand(wordList[++i]), toOperand(wordList[++i]), toOperand(wordList[++i]))))
                    }
                    else {
                        list.add(Operation(str, listOf(toOperand(wordList[++i]), toOperand(wordList[++i]), toOperand(wordList[++i]))))
                    }
                }
                "dosnud" -> {
                    if(isFuncMode) {
                        funcList.add(Dosnud(toOperand(wordList[++i])))
                    }
                    else {
                        list.add(Dosnud(toOperand(wordList[++i])))
                    }
                }
                "fenxeo" -> {
                    label = wordList[++i]
                    if(label[0] in '0'..'9') {
                        throw RuntimeException("Bad cersva-akrapt: '${label}'")
                    }
                    if(label == "'3126834864") {
                        label = "3126834864"
                    }

                    subList = wordList.drop(i + 1)
                    rinyvIndex = subList.indexOf("el")

                    if(rinyvIndex == -1) {
                        throw Exception("Not found 'el'")
                    }
                    else {
                        i += rinyvIndex + 1
                        var operandList = subList.take(rinyvIndex).map { x -> toOperand(x) }.toList()
                        val setVar = toOperand(wordList[++i])

                        if(setVar is AnaxName) {
                            if(isFuncMode) {
                                funcList.add(Fenxeo(label, operandList, setVar))
                            }
                            else {
                                list.add(Fenxeo(label, operandList, setVar))
                            }
                        }
                        else {
                            throw RuntimeException("Not variable")
                        }
                    }
                }
            }

            i++
        }

        if(rinyvCount != 0) {
            throw Exception("'rinyv' count do not equals 'situv' count.")
        }

        println(list.fold(StringBuilder("\nTinkaExpressionList: [\n"), { buffer, x -> buffer.append("  ").append(x).append("\n")}).append("]").toString())
        println(funcList.fold(StringBuilder("TinkaExpressionFuncList: [\n"), { buffer, x -> buffer.append("  ").append(x).append("\n")}).append("]").toString())

        if(isMain) {
            expressions.addAll(0, list)
        }
        else {
            expressions.addAll(list)
        }

        funcExpressions.addAll(funcList)
    }

    private fun toOperand(word: String): TinkaOperand {
        if(word.all { x -> x.isDigit() }) {
            return Constant(word)
        }
        else {
            return if(word.indexOf("@") != -1) {
                val split = word.split("@", ignoreCase = false, limit = 2)
                AnaxName(split[0], toOperand(split[1]))
            } else {
                AnaxName(word)
            }
        }
    }

    private fun write(outFile: String) {
        try {
            File(outFile).printWriter().use { writer ->
                varStack = 0
                varDictionary.clear()
                rinyvStack.clear()

                fiCount = 0
                falRinyvCount = 0
                falCount = 0

                writeExpression(expressions, writer)
                if(expressions.any{ x -> !(x is Kue || x is Xok)}) {
                    writer.println("ata ${(varStack * 4)} f5 l' dosnud0");
                    writer.println("krz f5@ xx");
                }

                varStack = 0
                writeExpression(funcExpressions, writer)

                writer.close()
            }
        }
        catch(e: IOException) {
            throw RuntimeException(e)
        }
    }

    private fun writeExpression(expressions: List<TinkaExpression>, writer: PrintWriter) {
        val isFunc = expressions.any { x -> x is Cersva }
        var cersvaArgCount = 0

        for(opd in expressions) {
            when(opd) {
                is Kue -> writer.println("kue ${opd.labelName}")
                is Xok -> writer.println("xok ${opd.labelName}")
                is Fi -> {
                    val lval = convert(writer, opd.left)
                    val rval = convert(writer, opd.right)
                    writer.println("fi ${lval} ${rval} ${opd.compare}")
                    rinyvStack.add(0, "fi${(fiCount++)}")
                }
                is Fal -> {
                    val lval = convert(writer, opd.left)
                    val rval = convert(writer, opd.right)
                    writer.println("fi ${lval} ${rval} ${opd.compare} l' fal-rinyv${(falRinyvCount)}")
                    rinyvStack.add(0, "fal-rinyv${(falRinyvCount++)}");
                    rinyvStack.add(0, "fal${(falCount++)}");
                }
                is Anax -> {
                    varStack += opd.length
                    writer.println("nta ${(opd.length * 4)} f5")
                    varDictionary[opd.varName] = varStack
                }
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
                        opd.count == 1 && isFunc -> {
                            if(varStack > cersvaArgCount) {
                                writer.println("ata ${((varStack - cersvaArgCount) * 4)} f5 l' dosnud${dosnudCount}")
                                writer.println("krz f5@ xx")
                            }
                            else {
                                writer.println("krz f5@ xx l' dosnud${dosnudCount}")
                            }
                        }
                    }
                }
                is Fenxeo -> {
                    opd.arguments.forEachIndexed { i, x ->
                        //if(x.pointer) {
                        //}
                        //else {
                            writer.println("nta 4 f5 krz ${convert(writer, x, i + 1)} f5@")
                        //}
                    }
                    writer.println("nta 4 f5 inj ${opd.funcName} xx f5@ ata ${((opd.arguments.size + 1) * 4)} f5 krz f0 ${convert(writer, opd.setVar)}")
                }
                is Operation -> {
                    val args = opd.arguments.fold(StringBuilder()) {
                        buffer, x -> buffer.append(" ").append(convert(writer, x))
                    }.toString()
                    writer.println("${opd.mnemonic}${args}")
                }
                is Cersva -> {
                    varStack = 0
                    for(argOpd in opd.arguments) {
                        varDictionary[argOpd.varName] = varStack
                        varStack += argOpd.length
                    }
                    cersvaArgCount = varStack

                    writer.println("\nnll ${opd.funcName}")
                    rinyvStack.add(0, "${opd.funcName}")
                    dosnudCount++
                }
                is Dosnud -> if(isFunc) {
                    writer.println("krz ${convert(writer, opd.retVal)} f0")
                    writer.println("malkrz dosnud${dosnudCount} xx")
                }
                else {
                    writer.println("malkrz dosnud0 xx")
                }
            }
        }
    }

    private fun convert(writer: PrintWriter, opd: TinkaOperand, count: Int = 0): String {
        return when(opd) {
            is Constant -> opd.value
            is AnaxName -> {
                val pos = convert(writer, opd.pos, count)
                if(opd.pos is Constant) {
                    return "f5+${(varStack - (varDictionary[opd.varName] as Int) + pos.toInt() + count) * 4}@"
                }
                else {
                    writer.println("krz ${pos} f0")
                    writer.println("ata ${varStack - (varDictionary[opd.varName] as Int) + count} f0")
                    writer.println("dro 2 f0")
                    return "f5+f0@"
                }
            }
            else -> throw RuntimeException("Invalid arguments: '${opd}'")
        }
    }
}

