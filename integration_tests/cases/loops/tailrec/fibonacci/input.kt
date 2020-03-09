package fibonacci

tailrec fun fibonacci(n: Long, a: Long = 0L, b: Long = 1L): Long = when (n) {
    0L -> a
    1L -> b
    else -> fibonacci(n - 1L, b, a + b)
}

fun main() {
    println()
}