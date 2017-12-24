import tinka.*;

fun main(args: Array<String>) {
    if(args.size > 0) {
        val outFileOptionIndex = args.indexOf("-o")
        if(outFileOptionIndex == -1) {
            val transpiler = object : TinkaTranscompiler(args) {
                protected override fun write(outFile: String) {
                }
            }

            transpiler.run("a.out")
        }
        else if(outFileOptionIndex == args.size - 1){
            println("No set output file name")
            println("java -jar tinka.jar [inFileNames] (-o [outFileName])")
        }
        else {
            val outFileIndex = outFileOptionIndex + 1
            val inFiles = args.filterIndexed { index, _ -> (index != outFileIndex && index != outFileOptionIndex) }
            val transpiler = object : TinkaTranscompiler(inFiles) {
                protected override fun write(outFile: String) {
                }
            }

            transpiler.run(args[outFileIndex])
        }
    }
    else {
        println("java -jar tinka.jar [inFileNames] (-o [outFileName])")
    }
}
