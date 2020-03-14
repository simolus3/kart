var fieldOnly: String = "Hello world!"

var fieldWithAccessors: Int = 3
    get() = field + 2
    set(value) { field = value * 2 }

var noField: String
    get() = "hi"
    set(value) { }

fun main() {
    println(fieldOnly)

    fieldWithAccessors = 15
    println(fieldWithAccessors)

    println(noField)
}