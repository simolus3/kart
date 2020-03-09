package eu.simonbinder.kart.kernel

class Source(
    val content: String,
    val importUri: Uri?,
    val fileUri: Uri?
) {

    val lineStarts: List<Int> by lazy {
        val chars = content.toCharArray()
        val lineBreakPositions = mutableListOf<Int>()

        chars.forEachIndexed { index, c ->
            if (c == '\n') {
                lineBreakPositions.add(index)
            }
        }

        return@lazy lineBreakPositions
    }

}