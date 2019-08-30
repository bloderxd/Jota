package matcher.patternmatching

import arrow.meta.extensions.MetaComponentRegistrar
import arrow.meta.qq.Func
import arrow.meta.qq.func
import matcher.DeclarativePatternMatchingPlugin

private const val SUPPORTED_ANNOTATION = "@When"

private typealias FunctionName = String
private typealias Scope = String
private typealias Parameter = String
private typealias ParameterType = String

val MetaComponentRegistrar.patternMatching get() = with(DeclarativePatternMatchingPlugin) {
    meta(
        func(
            match = {
                valueParameters.firstOrNull { it.text.contains(SUPPORTED_ANNOTATION) } != null
            },
            map = { func ->
                val nameAsString = name.toString()
                val occurrence = functionOccurrences[nameAsString] ?: nameAsString.createFunctionOccurrence(func.parent.parent.text)
                val type = valueParameters.asString().getType()
                occurrence.info.add(FunctionInfo(type, body.toString()))
                occurrence.occurrence ++
                functionOccurrences[nameAsString] = occurrence
                buildFunction(type, occurrence, nameAsString)
            }
        )
    )
}

private fun Func.FuncScope.buildFunction(type: String, occurrence: PhaseFunctionOccurrence, name: String): List<String> {
    return if (occurrence.occurrence >= occurrence.max) {
        listOf(
            """
              |$modality $visibility fun $name(arg: ${type.buildComposedType()}): Unit {
              |  ${occurrence.info.buildBody()}
              |}
              |"""
        )
    } else {
        listOf()
    }
}

private fun List<FunctionInfo>.buildBody(): String {
    var updatedBody = ""
    forEach {
        updatedBody += " \nif (arg is ${it.type}) { ${it.body.replace("{", "").replace("}", "")} }"
    }
    return updatedBody
}

private fun ParameterType.buildComposedType(): String {
    return DeclarativePatternMatchingPlugin.adt.firstOrNull {
        it.fqName?.asString()?.trim()?.toLowerCase() == this.trim().toLowerCase()
    }?.fqName?.asString()?.substring(0, indexOf(".") - 1) ?: ""
}

private fun FunctionName.createFunctionOccurrence(scope: String) = matcher.patternmatching.PhaseFunctionOccurrence(
    max = scope.replace(" ", "").findOccurrencesOf(this)
)

private fun Scope.findOccurrencesOf(functionName: FunctionName): Int =
    if(!contains("fun$functionName")) 0
    else replaceFirst("fun$functionName", "").findOccurrencesOf(functionName) + 1

private fun Parameter.getType() = trim().substring(
    indexOf(":") + 1, if (contains(",")) indexOf(",") else lastIndex + 1
)