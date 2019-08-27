package matcher

import arrow.meta.extensions.ExtensionPhase
import arrow.meta.extensions.MetaComponentRegistrar
import arrow.meta.qq.Func
import arrow.meta.qq.func

private const val SUPPORTED_ANNOTATION = "@When"

typealias FunctionName = String
typealias Scope = String
typealias Parameter = String

val MetaComponentRegistrar.patternMatching get() = with(ExtensionMatcher()) { patternMatching() }

class ExtensionMatcher {

    private val functionOccurrences = mutableMapOf<String, PhaseFunctionOccurrence>()

    fun MetaComponentRegistrar.patternMatching(): List<ExtensionPhase> = with(this) {
        meta(
            func(
                match = {
                    valueParameters.firstOrNull { it.text.contains(SUPPORTED_ANNOTATION) } != null
                },
                map = { func ->
                    val nameAsString = name.toString()
                    val occurrence = functionOccurrences[nameAsString] ?: nameAsString.createFunctionOccurrence(func.parent.parent.text)
                    occurrence.info.add(FunctionInfo(valueParameters.asString().getType(), body.toString()))
                    occurrence.occurrence ++
                    functionOccurrences[nameAsString] = occurrence
                    buildFunction(occurrence, nameAsString)
                }
            )
        )
    }

    private fun Func.FuncScope.buildFunction(occurrence: PhaseFunctionOccurrence, name: String): List<String> {
        return if (occurrence.occurrence >= occurrence.max) {
            listOf(
                """
              |$modality $visibility fun $name(e: kotlin.Exception): Unit {
              |  ${occurrence.info.buildBody()}
              |}
              |"""
            )
        } else {
            listOf(
                """
              |$modality $visibility fun $name($valueParameters): Unit {
              |  $body
              |}
              |"""
            )
        }
    }

    private fun List<FunctionInfo>.buildBody(): String {
        var updatedBody = ""
        forEach {
            updatedBody += " \nif (e is ${it.type}) { ${it.body.replace("{", "").replace("}", "")} }"
        }
        return updatedBody
    }

    private fun FunctionName.createFunctionOccurrence(scope: String) = PhaseFunctionOccurrence(
        max = scope.replace(" ", "").findOccurrencesOf(this)
    )

    private fun Scope.findOccurrencesOf(functionName: FunctionName): Int =
        if(!contains("fun$functionName")) 0
        else replaceFirst("fun$functionName", "").findOccurrencesOf(functionName) + 1

    private fun Parameter.getType() = trim().substring(
        indexOf(":") + 1, if (contains(",")) indexOf(",") else lastIndex + 1
    )
}