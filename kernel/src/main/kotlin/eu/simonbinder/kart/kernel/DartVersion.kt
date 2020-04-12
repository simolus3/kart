package eu.simonbinder.kart.kernel

data class DartVersion (val major: Int, val minor: Int, val kernelVersion: Int) {

    companion object {
        private val DART_8_DEV_OLD = DartVersion(2, 8, 40)
        val DART_2_8 = DartVersion(2, 8, 41)

        val DEFAULT_FOR_OUTPUT = /*DART_8_DEV_OLD*/ DART_2_8

        val LATEST = DART_2_8
        val OLDEST_SUPPORTED = DART_8_DEV_OLD

        val SUPPORTED_KERNEL_VERSIONS = OLDEST_SUPPORTED.kernelVersion..LATEST.kernelVersion
    }
}