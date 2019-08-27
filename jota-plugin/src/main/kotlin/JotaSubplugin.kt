import com.google.auto.service.AutoService
import org.gradle.api.Project
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.plugin.*

private const val GROUP_ID = "com.bloder"
private const val ARTIFACT = "jota-kotlin-plugin"
private const val VERSION = "0.1"

@AutoService(KotlinGradleSubplugin::class)
class JotaSubplugin : KotlinGradleSubplugin<AbstractCompile> {

    override fun apply(
        project: Project,
        kotlinCompile: AbstractCompile,
        javaCompile: AbstractCompile?,
        variantData: Any?,
        androidProjectHandler: Any?,
        kotlinCompilation: KotlinCompilation<KotlinCommonOptions>?
    ): List<SubpluginOption> = listOf()

    override fun getCompilerPluginId(): String = "jota"

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = GROUP_ID,
        artifactId = ARTIFACT,
        version = VERSION
    )

    override fun isApplicable(project: Project, task: AbstractCompile): Boolean = project.plugins.hasPlugin(JotaPlugin::class.java)
}