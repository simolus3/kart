package eu.simonbinder.kart.kotlin.lower.interfaces

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.ir.declarations.IrClass

/**
 * Generates a static `$defaultImpl` method for methods in interfaces that have a default implementation. Essentially,
 * we would like to compile
 *
 * ```kotlin
 * interface Base {
 *   fun m1(): Int
 *   fun m2(): Int = m1() * 2
 * }
 *
 * interface Another {
 *   fun m3(): Int
 * }
 *
 * interface Chained : Base, Another {
 *   override fun m3(): Int =  m1() + super.m2();
 * }
 *
 * class Impl : Chained {
 *   override fun m1(): Int = 3
 * }
 * ```
 *
 * to
 *
 * ```dart
 * abstract class Base {
 *   int m1();
 *   int m2();
 *
 *   static int m2$defaultImpl(Base $this) = $this.m1() * 2;
 * }
 *
 * abstract class Another {
 *   int m3();
 * }
 *
 * abstract class Chained implements Base, Another {
 *   static int m3$defaultImpl(Chained $this) = $this.m1() + Base.m2$defaultImpl($this)
 * }
 *
 * class Impl implements Chained {
 *   int m1() => 3;
 *   int m2() => Base.m2$defaultImpl(this);
 *   int m3() => Chained.m3$defaultImpl(this);
 * }
 * ```
 *
 * See also: [org.jetbrains.kotlin.backend.jvm.lower.InterfaceLowering] for the JVM.
 */
class DefaultImplementationsLowering : ClassLoweringPass {
    override fun lower(irClass: IrClass) {
        TODO("Not yet implemented")
    }

}