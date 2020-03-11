fun main() {
    foo(j = 3L)
}

fun foo(i: Long = 3L, j: Long) {
    println("i = $i, j = $j")
}