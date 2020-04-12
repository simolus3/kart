import eu.simonbinder.kart.kernel.binary.reader.KernelReader
import java.io.File

fun main() {
    val file = File("vm_outline_lowered.dill")
    val bytes = file.readBytes()

    val components = KernelReader(bytes).readComponent()
}