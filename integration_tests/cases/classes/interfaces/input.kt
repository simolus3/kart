interface Base {
    fun m1(): Int
    fun m2(): Int = m1() * 2
}

interface Another {
    fun m3(): Int
}

interface Chained : Base, Another {
    override fun m3(): Int = m2() + super.m2();
}

class Impl : Chained {
    override fun m1(): Int = 3
    override fun m2(): Int = 1
}

fun main() {
    println(Impl().m3())
}