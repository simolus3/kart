import eu.simonbinder.kart.kernel.binary.reader.KernelReader
import java.io.File

fun main() {
    val file = File("output.dill")
    val bytes = file.readBytes()

    val components = KernelReader(bytes).readComponent()
}