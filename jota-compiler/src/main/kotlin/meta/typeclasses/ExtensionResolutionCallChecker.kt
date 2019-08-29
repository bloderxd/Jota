package arrow.meta.typeclasses

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.util.slicedMap.Slices
import org.jetbrains.kotlin.util.slicedMap.WritableSlice


var UNABLE_TO_RESOLVE_EXTENSION: DiagnosticFactory1<PsiElement, String> = DiagnosticFactory1.create(Severity.ERROR)

var EXTENSION_RESOLUTION_INFO: WritableSlice<String, ExtensionCandidate> = Slices.createCollectiveSlice()

class ExtensionResolutionCallChecker : CallChecker {
  override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
    resolvedCall.resultingDescriptor.valueParameters.forEach { descriptor: ValueParameterDescriptor ->
      if (descriptor.isWithAnnotated && resolvedCall.valueArguments[descriptor] == null) {
        val valueParameters = functionParameters(context)
        val resolution = ExtensionResolutionStrategy.resolve(
          descriptor,
          valueParameters,
          descriptor.original,
          emptyList(),
          false
        )
        println("ExtensionResolutionCallChecker: \ncandidate: ${resolvedCall.candidateDescriptor}, \nresulting: ${resolvedCall.resultingDescriptor} \nexpression: [${reportOn.text}], \nresolution: $resolution")

        when (resolution) {
          is ExtensionCandidateResolution.Unresolved -> {
            val (message) = resolution
            context.trace.report(
              UNABLE_TO_RESOLVE_EXTENSION.on(
                reportOn,
                message
              )
            )
          }
          is ExtensionCandidateResolution.Resolved -> {
            val key = descriptor.returnType.toString()
            if (context.trace.get(EXTENSION_RESOLUTION_INFO, key) == null) {
              context.trace.record(
                EXTENSION_RESOLUTION_INFO,
                key,
                resolution.candidate
              )
//              descriptor.javaClass.getDeclaredField("declaresDefaultValue").apply {
//                isAccessible = true
//                set(descriptor, true)
//              }
              //context.suppressDiagnostic { diagnostic -> diagnostic.factory.name == Errors.NO_VALUE_FOR_PARAMETER.name }
            }
          }
        }
      }
    }
  }

  private fun functionParameters(context: CallCheckerContext): List<ValueParameterDescriptor> {
    var descriptor: DeclarationDescriptor? = context.scope.ownerDescriptor
    var valueParameters: List<ValueParameterDescriptor> = ArrayList()
    while (descriptor != null) {
      if (descriptor is CallableDescriptor) {
        valueParameters += descriptor.valueParameters
      }
      descriptor = descriptor.containingDeclaration
    }
    return valueParameters
  }
}
