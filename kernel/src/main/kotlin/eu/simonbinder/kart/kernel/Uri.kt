package eu.simonbinder.kart.kernel

data class Uri(val content: String) {

    companion object {
        fun file(path: String): Uri {
            return Uri("file:///$path")
        }
    }

}