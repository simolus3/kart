class Foo(val x: String) {

    inner class Bar {
        override fun toString(): String {
            return "${this@Foo}.Bar"
        }
    }

    override fun toString(): String {
        return "Foo($x)"
    }
}

fun main() {
    val foo = Foo("hi")
    val bar = foo.Bar()

    println(bar)
}