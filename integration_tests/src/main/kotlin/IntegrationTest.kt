import eu.simonbinder.kart.kernel.ast.members.Component
import eu.simonbinder.kart.kernel.binary.serializer.KernelSerializer
import java.io.ByteArrayOutputStream
import java.io.PrintWriter
import java.nio.file.*
import java.util.stream.Stream

fun main(args: Array<String>) {
    when (args.first()) {
        "generate-outputs" -> generateOutputs(args[1])
    }
}

private val glob = FileSystems.getDefault().getPathMatcher("glob:**.kt")

private val inputFiles: Stream<Path> get() =  Files.walk(Paths.get("cases")).filter {
    Files.isRegularFile(it) && glob.matches(it)
}

private fun componentToText(component: Component): String {
    val output = ByteArrayOutputStream()
    KernelSerializer(output).writeComponent(component)

    val outputArray = output.toByteArray()
    output.close()

    val process = ProcessBuilder("dart", "tool/kernel_to_text.dart", outputArray.size.toString())
        .directory(Paths.get("../kart_support").toFile())
        .start()

    process.outputStream.write(outputArray)
    process.outputStream.close()

    val textInput = ByteArrayOutputStream()
    process.inputStream.copyTo(textInput)

    process.waitFor()

    return String(textInput.toByteArray())
}

private fun generateOutputs(stdlibJar: String) {
    for (input in inputFiles) {
        println("Now running on $input")
        val outputFile = input.resolveSibling("compiled.txt")

        val component = compileDirectoryToDart(stdlibJar, input.parent.normalize().toString())
        val componentString = componentToText(component)

        val outputWriter = PrintWriter(outputFile.toFile())
        outputWriter.print(componentString)
        outputWriter.close()
    }
}