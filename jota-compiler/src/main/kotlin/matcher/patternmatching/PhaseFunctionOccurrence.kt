package matcher.patternmatching

data class PhaseFunctionOccurrence(
    val max: Int,
    var occurrence: Int = 0,
    val info: MutableList<FunctionInfo> = mutableListOf()
)