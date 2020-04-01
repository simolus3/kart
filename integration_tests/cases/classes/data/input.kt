data class Foo(
    val x: String
)

fun main() {
    val foo = Foo("hello world")
    val foo2 = Foo("hello world")
    val bar = Foo("no")

    println(foo)
    println("foo == foo2: ${foo == foo2}. hash equals: ${foo.hashCode() == foo2.hashCode()}")
    println("foo == bar: ${foo == bar}. hash equals: ${foo.hashCode() == bar.hashCode()}")
    println("foo === foo2: ${foo === bar}")
}