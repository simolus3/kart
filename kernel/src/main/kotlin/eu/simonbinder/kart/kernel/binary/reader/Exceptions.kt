package eu.simonbinder.kart.kernel.binary.reader

abstract class KernelReaderException : Exception()

class InvalidKernelVersionException(
    private val actual: Int
) : KernelReaderException() {
    override fun toString(): String = "Unexpected Kernel version: $actual."
}

class ParsingException(
    private val msg: String,
    private val offset: UInt
): KernelReaderException() {

    override fun toString(): String {
        return "Can't read Kernel: $msg at offset $offset"
    }

}