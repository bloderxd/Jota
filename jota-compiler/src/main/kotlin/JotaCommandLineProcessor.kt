import com.google.auto.service.AutoService
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor

private const val PLUGIN_ID = "jota"

@AutoService(CommandLineProcessor::class)
class JotaCommandLineProcessor : CommandLineProcessor {
    override val pluginId: String get() = PLUGIN_ID
    override val pluginOptions: Collection<AbstractCliOption> = listOf()
}