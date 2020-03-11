fun main() {
    val x = try {
        1 / 0
    } catch (e: Any?) {
        2
    }
}