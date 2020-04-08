import eu.simonbinder.kart.kernel.ast.members.Component
import eu.simonbinder.kart.kernel.binary.serializer.KernelSerializer
import eu.simonbinder.kart.kotlin.Frontend
import eu.simonbinder.kart.transformer.Kotlin2KernelTransformer
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.GroupingMessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream
import java.nio.file.Paths

fun main(args: Array<String>) {
    if (args.size != 1) {
        println("Usage: kotlin2kernel \$KOTLINC_HOME/lib/kotlin-stdlib.jar")
        return
    }

    val stream = PrintStream(ByteArrayOutputStream())
    val currentDir = Paths.get("example").toAbsolutePath().toString()
    val component = compileDirectoryToDart(args[0], currentDir, stream)

    val output = FileOutputStream("output.dill")
    KernelSerializer(output).writeComponent(component)
}

fun compileDirectoryToDart(stdlibJar: String, directory: String, logOutputs: PrintStream = System.out): Component {
    val collector = GroupingMessageCollector(
        PrintingMessageCollector(logOutputs, MessageRenderer.PLAIN_RELATIVE_PATHS, true), false)
    val frontend = Frontend(collector)

    frontend.config.run {
        put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, collector)
        put(CommonConfigurationKeys.MODULE_NAME, "example")
        add(CLIConfigurationKeys.CONTENT_ROOTS, JvmClasspathRoot(File(stdlibJar)))
    }

    frontend.addSourceRoot(File(directory))

    val fragment = frontend.obtainIr()

    val transformer = Kotlin2KernelTransformer(frontend.dartBackendContext!!)
    transformer.addModule(fragment)

    return transformer.component
}