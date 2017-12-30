package tinka

import java.io.*

class TinkaTo2003lk : TinkaTranscompiler {
    constructor(inFiles: Array<String>) : super(inFiles) {}
    constructor(inFiles: List<String>) : super(inFiles.toTypedArray()) {}

    protected override fun write(outFile: String) {
        try {
            File(outFile).printWriter().use { writer ->
                if(this.hasMain) {
                    writer.println("krz _fasal xx")
                }
                writeExpression(expressions, writer)
                writer.close()
            }
        }
        catch(e: IOException) {
            throw RuntimeException(e)
        }
    }

    private fun writeExpression(expressions: List<TinkaExpression>, writer: PrintWriter) {
        val isFunc = expressions.any { x -> x is Cersva }
        var cersvaArgCount: Long = 0

        for(opd in expressions) {
            when(opd) {
                is Kue -> writer.println("kue ${opd.labelName}")
                is Xok -> writer.println("xok ${opd.labelName}")
                is Fi -> {
                    val (lval, lpre) = convert(opd.left)
                    val (rval, rpre) = convert(opd.right)

                    writer.print(lpre)
                    writer.print("krz ${lval} f0 ")
                    writer.print(rpre)

                    writer.println("fi f0 ${rval} ${opd.compare}")

                    rinyvStack.add(0, "fi${countMap["fi"]}")
                    countMap["fi"] = countMap["fi"]!! + 1
                }
                is Fal -> {
                    val (lval, lpre) = convert(opd.left)
                    val (rval, rpre) = convert(opd.right)

                    writer.print(lpre)
                    writer.print("krz ${lval} f0 l' fal-rinyv${(countMap["fal-rinyv"])} ")
                    writer.print(rpre)

                    writer.println("fi f0 ${rval} ${opd.compare}")
                    rinyvStack.add(0, "fal-rinyv${(countMap["fal-rinyv"])}");
                    rinyvStack.add(0, "fal${countMap["fal"]}");

                    countMap["fal-rinyv"] = countMap["fal-rinyv"]!! + 1
                    countMap["fal"] = countMap["fal"]!! + 1
                }
                is Anax -> {
                    useVarStack += opd.length
                    writer.println("nta ${(opd.length * 4)} f5")
                    varDictionary[opd.varName] = useVarStack
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
                            if(useVarStack > cersvaArgCount + 1) {
                                writer.println("ata ${((useVarStack - cersvaArgCount - 1) * 4)} f5 l' dosnud${countMap["dosnud"]}")
                                writer.println("krz f5@ f1 ata 4 f5 krz f5@ xx")
                            }
                            else {
                                writer.println("ata 4 f5 l' dosnud${countMap["dosnud"]} krz f5@ f1 krz f5@ xx")
                            }
                        }
                    }
                }
                is Fenxeo -> {
                    opd.arguments.forEachIndexed { i, x ->
                        val (pos, preprocess) = convert(x, i + 1)

                        writer.print(preprocess)
                        writer.println("nta 4 f5 krz ${pos} f5@")
                    }
                    if(opd.setVar is Anakswa) {
                        writer.println("nta 4 f5 inj ${opd.funcName} xx f5@ ata ${((opd.arguments.size + 1) * 4)} f5")
                    }
                    else {
                        val (pos, preprocess) = convert(opd.setVar)

                        writer.print("nta 4 f5 inj ${opd.funcName} xx f5@ ata ${((opd.arguments.size + 1) * 4)} f5 ")
                        writer.print(preprocess)
                        writer.println("krz f0 ${pos}")
                    }
                }
                is Operation -> {
                    var subAddr: Long = 0
                    val buffer = StringBuilder()
                    for(x in opd.arguments) {
                        val (pos, preprocess) = convert(x)

                        if(preprocess != "" && opd.arguments.last() != x) {
                            writer.print(preprocess)

                            val addr = ((++subAddr) * -4) and 0x00000000FFFFFFFF
                            writer.print("krz ${pos} f5+${addr}@ ")
                            buffer.append(" ").append("f5+${addr}@")
                        }
                        else {
                            writer.print(preprocess)
                            buffer.append(" ").append(pos)
                        }
                    }

                    writer.println("${opd.mnemonic}${buffer.toString()}")
                }
                is Cersva -> {
                    useVarStack = 0
                    for(argOpd in opd.arguments) {
                        varDictionary[argOpd.varName] = useVarStack
                        useVarStack += argOpd.length
                    }
                    cersvaArgCount = useVarStack

                    writer.println("\nnll ${opd.funcName} nta 4 f5 krz f1 f5@")
                    useVarStack++
                    rinyvStack.add(0, "${opd.funcName}")
                    countMap["dosnud"] = countMap["dosnud"]!! + 1
                }
                is Dosnud -> {
                    val (pos, preprocess) = convert(opd.retVal)

                    writer.print(preprocess)
                    writer.println("krz ${pos} f0")
                    writer.println("krz dosnud${countMap["dosnud"]} xx")
                }
            }
        }
    }

    private fun convert(opd: TinkaOperand, count: Int = 0): Pair<String, String> {
        return when(opd) {
            is Constant -> Pair(opd.value, "")
            is AnaxName -> {
                val (pos, preprocess) = convert(opd.pos, count)
                val varData = varDictionary[opd.varName]!!

                if(opd.pos is Constant) {
                    return Pair("f5+${(useVarStack - varData + pos.toLong() + count) * 4}@", preprocess)
                }
                else {
                    return Pair("f5+f1@", preprocess + "krz ${pos} f1 ata ${useVarStack - varData + count} f1 dro 2 f1 ")
                }
            }
            else -> throw RuntimeException("Invalid arguments: '${opd}'")
        }
    }
}
