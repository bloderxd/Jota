import com.google.auto.service.AutoService
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor

@AutoService(CommandLineProcessor::class)
class JotaCommandLineProcessor : CommandLineProcessor {
    override val pluginId: String get() = "jota"
    override val pluginOptions: Collection<AbstractCliOption> = listOf()
}