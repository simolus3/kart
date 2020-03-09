package eu.simonbinder.kart.kernel

enum class DartVersion (val major: Int, val minor: Int, val kernelVersion: Int) {
    DART_2_7(2, 7, 36);

    companion object {
        val LATEST = DART_2_7
    }
}