import arrow.meta.extensions.ExtensionPhase
import arrow.meta.extensions.MetaComponentRegistrar
import com.google.auto.service.AutoService
import matcher.patternMatching
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar

@AutoService(ComponentRegistrar::class)
class JotaComponentRegistrar : MetaComponentRegistrar {

    override fun intercept(): List<ExtensionPhase> = patternMatching
}