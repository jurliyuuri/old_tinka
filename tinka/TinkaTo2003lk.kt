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
        var cersvaArgCount = 0

        for(opd in expressions) {
            when(opd) {
                is Kue -> writer.println("kue ${opd.labelName}")
                is Xok -> writer.println("xok ${opd.labelName}")
                is Fi -> {
                    val lval = convert(writer, opd.left)
                    val rval = convert(writer, opd.right)
                    writer.println("fi ${lval} ${rval} ${opd.compare}")
                    rinyvStack.add(0, "fi${countMap["fi"]}")
                    countMap["fi"] = (countMap["fi"] as Int) + 1
                }
                is Fal -> {
                    val lval = convert(writer, opd.left)
                    val rval = convert(writer, opd.right)
                    writer.println("fi ${lval} ${rval} ${opd.compare} l' fal-rinyv${(countMap["fal-rinyv"])}")
                    rinyvStack.add(0, "fal-rinyv${(countMap["fal-rinyv"])}");
                    rinyvStack.add(0, "fal${countMap["fal"]}");

                    countMap["fal-rinyv"] = (countMap["fal-rinyv"] as Int) + 1
                    countMap["fal"] = (countMap["fal"] as Int)+ 1
                }
                is Anax -> {
                    useVarStack += opd.length
                    writer.println("nta ${(opd.length * 4)} f5")
                    varDictionary[opd.varName] = VarData(useVarStack.toLong())
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
                            if(useVarStack > cersvaArgCount) {
                                writer.println("ata ${((useVarStack - cersvaArgCount) * 4)} f5 l' dosnud${countMap["dosnud"]}")
                                writer.println("krz f5@ xx")
                            }
                            else {
                                writer.println("krz f5@ xx l' dosnud${countMap["dosnud"]}")
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
                    if(opd.setVar is Anakswa) {
                        writer.println("nta 4 f5 inj ${opd.funcName} xx f5@ ata ${((opd.arguments.size + 1) * 4)} f5")
                    }
                    else {
                        writer.println("nta 4 f5 inj ${opd.funcName} xx f5@ ata ${((opd.arguments.size + 1) * 4)} f5 krz f0 ${convert(writer, opd.setVar)}")
                    }
                }
                is Operation -> {
                    val args = opd.arguments.fold(StringBuilder()) {
                        buffer, x -> buffer.append(" ").append(convert(writer, x))
                    }.toString()
                    writer.println("${opd.mnemonic}${args}")
                }
                is Cersva -> {
                    useVarStack = 0
                    for(argOpd in opd.arguments) {
                        varDictionary[argOpd.varName] = VarData(useVarStack.toLong())
                        useVarStack += argOpd.length
                    }
                    cersvaArgCount = useVarStack

                    writer.println("\nnll ${opd.funcName}")
                    rinyvStack.add(0, "${opd.funcName}")
                    countMap["dosnud"] = (countMap["dosnud"] as Int) + 1
                }
                is Dosnud -> {
                    if(isFunc) {
                        writer.println("krz ${convert(writer, opd.retVal)} f0")
                        writer.println("malkrz dosnud${countMap["dosnud"]} xx")
                    }
                    else {
                        writer.println("malkrz dosnud0 xx")
                    }
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
                    return "f5+${(useVarStack - (varDictionary[opd.varName] as VarData).value.toInt() + pos.toInt() + count) * 4}@"
                }
                else {
                    writer.println("krz ${pos} f0")
                    writer.println("ata ${useVarStack - (varDictionary[opd.varName] as VarData).value.toInt() + count} f0")
                    writer.println("dro 2 f0")
                    return "f5+f0@"
                }
            }
            else -> throw RuntimeException("Invalid arguments: '${opd}'")
        }
    }
}
