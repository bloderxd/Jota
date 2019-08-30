package arrow.meta.autofold

import arrow.meta.extensions.ExtensionPhase
import arrow.meta.extensions.MetaComponentRegistrar
import arrow.meta.higherkind.arity
import arrow.meta.higherkind.invariant
import arrow.meta.qq.classOrObject
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.findFunctionByName
import org.jetbrains.kotlin.psi.psiUtil.getSuperNames
import org.jetbrains.kotlin.utils.addToStdlib.safeAs


val MetaComponentRegistrar.autoFold: List<ExtensionPhase>
  get() =
    meta(
      classOrObject(::isAutoFoldable) { c ->
        println("Processing Sealed class: ${c.name}")
        sealedVariants.forEach(::println)
        val sealedExtraTypes = sealedVariants.map { it.typeVariables }.filter(String::isNotEmpty)
        val typeInfo = (c.renderTypeParameters().split(", ") + sealedExtraTypes).distinct().sorted()
        val typeInfoString = typeInfo.joinToString(separator = ", ")
        val returnType = typeInfo.returnType
        println(returnType)
        listOfNotNull(
          if (sealedVariants.any { it.typeVariables.split(',').size > c.arity })
            c.text
          else
            """
              |$visibility $modality $kind $name<$typeParameters>($valueParameters)${supertypes.identifier.doIf(String::isNotEmpty) { " : $it" }} {
              |  ${body.asString().trimMargin()}
              |  @Suppress("UNCHECKED_CAST", "USELESS_CAST", "NO_ELSE_IN_WHEN")
              |  fun <${typeInfoString.doIf(String::isNotEmpty) { "$it, " }}$returnType> ${c.name}${c.renderTypeParameters().doIf(String::isNotEmpty) { "<$it>" }}.fold(
              |  ${sealedVariants.params(returnType)}
              |  ): $returnType = when (val x = this) {
              |  ${sealedVariants.patternMatch()}
              |  }
              |}
            """.trimMargin()
        )
      }
    )

private fun KtClass.hasFoldFunction(): Boolean =
  findFunctionByName("fold").safeAs<KtNamedFunction>()?.typeParameters?.size == 1

private fun isAutoFoldable(ktClass: KtClass): Boolean =
  ktClass.isSealed() && !ktClass.isAnnotation() &&
    ktClass.isKinded() &&
    !ktClass.hasFoldFunction() && ktClass.sealedSubclasses().isNotEmpty()

private fun KtClass.isKinded() =
  fqName?.asString()?.startsWith("arrow.Kind") != true

data class SealedSubclass(val simpleName: Name, val fqName: FqName?, val typeVariables: String) // add typeVariable with <>

fun KtClass.sealedSubclasses(): List<SealedSubclass> =
  innerSealedSubclasses() + outerSealedSubclasses()

val List<String>.returnType
  get() = sorted().run {
    last().dropLast(1) + with(last().last()) { if (inc().isLetterOrDigit()) inc().toUpperCase() else plus("A") }
  }

fun List<KtDeclaration>.sealedVariants(superKt: KtClass): List<SealedSubclass> =
  filter {
    (it is KtClassOrObject) && it.getSuperNames().contains(superKt.nameAsSafeName.identifier)
  }.map { it as KtClassOrObject }.map {
    SealedSubclass(
      simpleName = it.nameAsSafeName,
      fqName = it.fqName,
      typeVariables = if (it is KtClass) it.renderTypeParameters() else ""
    )
  }

fun KtClass.innerSealedSubclasses(): List<SealedSubclass> =
  declarations.sealedVariants(this)

fun KtClass.outerSealedSubclasses(): List<SealedSubclass> =
  containingKtFile.declarations.sealedVariants(this)

private fun KtClass.renderTypeParameters(): String =
  Name.identifier(typeParameters.joinToString(separator = ", ") { it.text }).invariant

inline fun <T> T.doIf(cond: T.() -> Boolean, doIt: (T) -> T): T =
  if (cond(this)) doIt(this) else this

private fun List<SealedSubclass>.patternMatch(): String = // works
  joinToString(
    transform = { s ->
      "  is ${s.simpleName.identifier} -> ${s.simpleName.identifier.decapitalize()}(x${s.typeVariables.doIf(String::isNotEmpty) { " as ${s.simpleName.identifier}<$it>" }})"
    },
    separator = "\n  ")

private fun List<SealedSubclass>.params(returns: String): String =
  joinToString(
    transform = { "  ${it.simpleName.identifier.decapitalize()}: (${it.simpleName.identifier}${it.typeVariables.doIf(String::isNotEmpty) { "<$it>" }}) -> $returns" }
    , separator = ",\n  ")