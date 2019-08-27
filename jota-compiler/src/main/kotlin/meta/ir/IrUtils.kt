package arrow.meta.ir

import arrow.meta.extensions.CompilerContext
import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.referenceFunction
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtModifierList
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtPureClassOrObject
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.jetbrains.kotlin.psi.KtSuperTypeListEntry
import org.jetbrains.kotlin.psi.synthetics.SyntheticClassOrObjectDescriptor
import org.jetbrains.kotlin.psi2ir.Psi2IrConfiguration
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator
import org.jetbrains.kotlin.psi2ir.generators.BodyGenerator
import org.jetbrains.kotlin.psi2ir.generators.ClassGenerator
import org.jetbrains.kotlin.psi2ir.generators.DeclarationGenerator
import org.jetbrains.kotlin.psi2ir.generators.FunctionGenerator
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import org.jetbrains.kotlin.psi2ir.generators.createBodyGenerator
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BodyResolver
import org.jetbrains.kotlin.resolve.FunctionDescriptorResolver
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.descriptorUtil.parents
import org.jetbrains.kotlin.resolve.lazy.LazyClassContext
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class IrUtils(
  val backendContext: BackendContext,
  val compilerContext: CompilerContext
) {

  private val psi2IrTranslator: Psi2IrTranslator = Psi2IrTranslator(LanguageVersionSettingsImpl.DEFAULT, Psi2IrConfiguration(ignoreErrors = true))
  private val generatorContext: GeneratorContext =
    psi2IrTranslator.createGeneratorContext(compilerContext.module, compilerContext.componentProvider.get<LazyClassContext>().trace.bindingContext)
  private val declarationGenerator: DeclarationGenerator =
    DeclarationGenerator(generatorContext)

  fun irBody(ownerSymbol: IrSymbol, code: String): IrBody =
    declarationGenerator.createBodyGenerator(ownerSymbol).generateExpressionBody(
      compilerContext.ktPsiElementFactory.createExpression(code)
    )

  fun irFunction(pckg: FqName, code: String): IrFunction =
    FunctionGenerator(declarationGenerator)
      .generateFunctionDeclaration(compilerContext.ktPsiElementFactory.createFunction(code).also { ktNamedFunction ->
        val packageDescriptor: PackageViewDescriptor = compilerContext.module.getPackage(pckg)
        val lazyClassContext: LazyClassContext = compilerContext.componentProvider.get()
        val functionDescriptorResolver: FunctionDescriptorResolver = compilerContext.componentProvider.get()
        val descriptor = functionDescriptorResolver.resolveFunctionDescriptor(
          packageDescriptor,
          lazyClassContext.declarationScopeProvider.getResolutionScopeForDeclaration(ktNamedFunction),
          ktNamedFunction,
          lazyClassContext.trace,
          DataFlowInfo.EMPTY
        )
      })

  fun irClass(pckg: FqName, code: String): IrClass =
    ClassGenerator(declarationGenerator)
      .generateClass(compilerContext.ktPsiElementFactory.createClass(code).also {
        val packageDescriptor: PackageViewDescriptor = compilerContext.module.getPackage(pckg)
        val lazyClassContext: LazyClassContext = compilerContext.componentProvider.get()
        //internally registers itself in the binding trace
        val descriptor = SyntheticClassOrObjectDescriptor(
          c = lazyClassContext,
          parentClassOrObject = packageSyntheticDeclaration(packageDescriptor),
          containingDeclaration = packageDescriptor,
          name = it.nameAsSafeName,
          source = SourceElement.NO_SOURCE,
          outerScope = lazyClassContext.declarationScopeProvider.getResolutionScopeForDeclaration(it),
          modality = Modality.FINAL,
          visibility = Visibilities.PUBLIC,
          annotations = Annotations.EMPTY,
          constructorVisibility = Visibilities.PUBLIC,
          kind = ClassKind.CLASS,
          isCompanionObject = false
        ).also { it.initialize() }
        lazyClassContext.trace.record(BindingContext.CLASS, it, descriptor)
//        MetaReplacedClassDescriptor(compilerContext, compilerContext.componentProvider.get(), packageDescriptor ,it)
      })

  private fun packageSyntheticDeclaration(
    packageDescriptor: PackageViewDescriptor
  ): KtPureClassOrObject =
    object : KtPureClassOrObject {
      override fun getName(): String? = packageDescriptor.name.asString()
      override fun isLocal(): Boolean = false

      override fun getDeclarations(): List<KtDeclaration> = emptyList()
      override fun getSuperTypeListEntries(): List<KtSuperTypeListEntry> = emptyList()
      override fun getCompanionObjects(): List<KtObjectDeclaration> = emptyList()

      override fun hasExplicitPrimaryConstructor(): Boolean = false
      override fun hasPrimaryConstructor(): Boolean = false
      override fun getPrimaryConstructor(): KtPrimaryConstructor? = null
      override fun getPrimaryConstructorModifierList(): KtModifierList? = null
      override fun getPrimaryConstructorParameters(): List<KtParameter> = emptyList()
      override fun getSecondaryConstructors(): List<KtSecondaryConstructor> = emptyList()

      override fun getPsiOrParent(): KtElement = (packageDescriptor.findPsi() ?: parent!!) as KtElement
      override fun getParent(): PsiElement? = packageDescriptor.parents.first().findPsi()
      @Suppress("USELESS_ELVIS")
      override fun getContainingKtFile(): KtFile =
        // in theory `containingKtFile` is `@NotNull` but in practice EA-114080
        psiOrParent.containingKtFile
          ?: throw IllegalStateException("containingKtFile was null for $parent of ${parent?.javaClass}")

      override fun getBody(): KtClassBody? = null
    }

  fun IrFunctionAccessExpression.defaultValues(): List<String> =
    symbol.descriptor.valueParameters
      .mapNotNull { it.findPsi() as? KtParameter }
      .mapNotNull { it.defaultValue?.text }

  fun FunctionDescriptor.irCall(): IrCall {
    val irFunctionSymbol = backendContext.ir.symbols.externalSymbolTable.referenceFunction(this)
    return IrCallImpl(
      startOffset = UNDEFINED_OFFSET,
      endOffset = UNDEFINED_OFFSET,
      type = irFunctionSymbol.owner.returnType,
      symbol = irFunctionSymbol,
      descriptor = irFunctionSymbol.owner.descriptor,
      typeArgumentsCount = irFunctionSymbol.owner.descriptor.typeParameters.size,
      valueArgumentsCount = irFunctionSymbol.owner.descriptor.valueParameters.size
    )
  }

  fun PropertyDescriptor.irGetterCall(): IrCall? {
    val irField = backendContext.ir.symbols.externalSymbolTable.referenceField(this)
    return irField.owner.correspondingPropertySymbol?.owner?.getter?.symbol?.let { irSimpleFunctionSymbol ->
      IrCallImpl(
        startOffset = UNDEFINED_OFFSET,
        endOffset = UNDEFINED_OFFSET,
        type = irSimpleFunctionSymbol.owner.returnType,
        symbol = irSimpleFunctionSymbol,
        descriptor = irSimpleFunctionSymbol.owner.descriptor,
        typeArgumentsCount = irSimpleFunctionSymbol.owner.descriptor.typeParameters.size,
        valueArgumentsCount = irSimpleFunctionSymbol.owner.descriptor.valueParameters.size
      )
    }
  }

  fun ClassDescriptor.irConstructorCall(): IrConstructorCall? {
    val irClass = backendContext.ir.symbols.externalSymbolTable.referenceClass(this)
    return irClass.constructors.firstOrNull()?.let { irConstructorSymbol ->
      IrConstructorCallImpl(
        startOffset = UNDEFINED_OFFSET,
        endOffset = UNDEFINED_OFFSET,
        type = irConstructorSymbol.owner.returnType,
        symbol = irConstructorSymbol,
        descriptor = irConstructorSymbol.owner.descriptor,
        typeArgumentsCount = irConstructorSymbol.owner.descriptor.typeParameters.size,
        valueArgumentsCount = irConstructorSymbol.owner.descriptor.valueParameters.size,
        constructorTypeArgumentsCount = declaredTypeParameters.size
      )
    }
  }

}