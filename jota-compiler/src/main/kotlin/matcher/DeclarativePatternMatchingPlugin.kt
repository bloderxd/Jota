package matcher

import arrow.meta.autofold.SealedSubclass
import arrow.meta.extensions.ExtensionPhase
import arrow.meta.extensions.MESSAGE
import arrow.meta.extensions.MetaComponentRegistrar
import arrow.meta.qq.Func
import arrow.meta.qq.classOrObject
import arrow.meta.qq.func
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity

private const val SUPPORTED_ANNOTATION = "@When"

private typealias FunctionName = String
private typealias Scope = String
private typealias Parameter = String
private typealias ParameterType = String

internal object DeclarativePatternMatchingPlugin {

    private val adt: MutableList<SealedSubclass> = mutableListOf()
    private val functionOccurrences = mutableMapOf<String, PhaseFunctionOccurrence>()

    fun MetaComponentRegistrar.adt() = meta(
        classOrObject(
            match = { true },
            map = { c ->
                if (sealedVariants.isNotEmpty()) {
                    adt.addAll(sealedVariants)
                }
                listOf(c.text)
            }
        )
    )

    fun MetaComponentRegistrar.patternMatching(): List<ExtensionPhase> = meta(
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
                buildFunction(func.text, type, occurrence, nameAsString)
            }
        )
    )

    private fun Func.FuncScope.buildFunction(original: String, type: String, occurrence: PhaseFunctionOccurrence, name: String): List<String> {
        return if (occurrence.occurrence >= occurrence.max) {
            MESSAGE.testMessageCollector?.report(CompilerMessageSeverity.WARNING, "Composed Type -> ${type.buildComposedType()}")
            MESSAGE.testMessageCollector?.report(CompilerMessageSeverity.WARNING, "Composed Type -> ${adt}")
            MESSAGE.testMessageCollector?.report(CompilerMessageSeverity.WARNING, "New Body -> ${occurrence.info.buildBody()}")
            MESSAGE.testMessageCollector?.report(CompilerMessageSeverity.WARNING, "Original Body -> ${"""
              |$modality $visibility fun $name(arg: ${type.buildComposedType()}): Unit {
              |  ${occurrence.info.buildBody()}
              |}
              |"""}")
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
        return adt.firstOrNull {
            it.fqName?.asString()?.trim()?.toLowerCase() == this.trim().toLowerCase()
        }?.fqName?.asString()?.substring(0, indexOf(".") - 1) ?: ""
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