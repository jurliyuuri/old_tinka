package tinka;

sealed class TinkaExpression()
data class Kue(val labelName: String) : TinkaExpression()
data class Xok(val labelName: String) : TinkaExpression()
data class Anax(val varName: String, val pointer: Boolean = false, val length: Int = 1) : TinkaExpression()
data class Fi(val left: TinkaOperand, val compare: String, val right: TinkaOperand): TinkaExpression()
data class Fal(val left: TinkaOperand, val compare: String, val right: TinkaOperand): TinkaExpression()
data class Cersva(val funcName: String, val arguments: List<Anax>) : TinkaExpression()
data class Dosnud(val retVal: TinkaOperand) : TinkaExpression()
data class Fenxeo(val funcName: String, val arguments: List<TinkaOperand>, val setVar: AnaxName) : TinkaExpression()
data class Rinyv(val count: Int) : TinkaExpression()
data class Situv(val count: Int) : TinkaExpression()
data class Operation(val mnemonic: String, val arguments: List<TinkaOperand>) : TinkaExpression()

sealed class TinkaOperand()
data class Constant(val value: String) : TinkaOperand()
data class AnaxName(val varName: String, val pos: TinkaOperand = Constant("0")) : TinkaOperand()
data class CersvaName(val funcName: String) : TinkaOperand()

