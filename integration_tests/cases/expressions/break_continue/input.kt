fun main() {
    var x = 0
    while (true) {
        x = x + 1
        val y = when (x) {
            1 -> continue
            5 -> break
            else -> x + 1
        }

        println(y)
    }
}