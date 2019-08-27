package arrow.meta.higherkind

import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.getAbbreviation
import org.jetbrains.kotlin.types.typeUtil.getImmediateSuperclassNotAny

class KindAwareTypeChecker(val typeChecker: KotlinTypeChecker) : KotlinTypeChecker by typeChecker {
  override fun isSubtypeOf(p0: KotlinType, p1: KotlinType): Boolean {
    //println("KindAwareTypeChecker.isSubtypeOf: $p0 <-> $p1")
    val subType = p0
    val superType = p1
    val isKind: Boolean =
      (subType.isKind() || superType.isKind()) && (subType.typeAliasMatch(superType) || superType.typeAliasMatch(subType))
    return isKind || typeChecker.isSubtypeOf(p0, p1)
  }

  override fun equalTypes(p0: KotlinType, p1: KotlinType): Boolean {
    //println("KindAwareTypeChecker.equalTypes: $p0 <-> $p1")
    return typeChecker.equalTypes(p0, p1)
  }

  private fun KotlinType.isKind(): Boolean =
    constructor.declarationDescriptor?.fqNameSafe == kindName

  private fun KotlinType.typeAliasMatch(other: KotlinType): Boolean {
    val a = getAbbreviation()?.constructor?.declarationDescriptor?.fqNameSafe?.shortName()
    val b = other.constructor.declarationDescriptor?.fqNameSafe?.shortName() ?: other.getImmediateSuperclassNotAny()?.constructor?.declarationDescriptor?.fqNameSafe?.kindTypeAliasName
    return a == b || a == other.getImmediateSuperclassNotAny()?.constructor?.declarationDescriptor?.fqNameSafe?.kindTypeAliasName
  }

}