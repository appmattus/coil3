import org.gradle.api.DefaultTask
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

/** Extracts Skiko's JS/Wasm runtime (`*.mjs` and `skiko.wasm`) from the `skiko-js-wasm-runtime` jar. */
abstract class UnpackSkikoWasmRuntime : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val runtimeJars: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Inject
    protected abstract val archives: ArchiveOperations

    @get:Inject
    protected abstract val fileSystem: FileSystemOperations

    @TaskAction
    fun unpack() {
        fileSystem.sync {
            runtimeJars.forEach { from(archives.zipTree(it)) }
            include("*.mjs", "*.wasm")
            into(outputDirectory)
        }
    }
}
