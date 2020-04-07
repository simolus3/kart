package eu.simonbinder.kart.kotlin.lower.interfaces

import eu.simonbinder.kart.kotlin.DartBackendContext
import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.addMember
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isInterface

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
 * See also:
 *  - [org.jetbrains.kotlin.backend.jvm.lower.InterfaceLowering] for the JVM.
 *  - [InterfaceSuperCallsLowering], which lowers super calls in interfaces when they point to a default implementation
 *  - [InterfaceDelegationLowering], which adds delegating methods to child classes
 */
class DefaultImplementationsLowering(private val context: DartBackendContext) : ClassLoweringPass {
    override fun lower(irClass: IrClass) {
        if (!irClass.isInterface) return

        val addedMembers = mutableListOf<IrSimpleFunction>()

        irClass.functions.forEach { function ->
            if (function.modality != Modality.ABSTRACT && function.origin != IrDeclarationOrigin.FAKE_OVERRIDE) {
                val staticDefaultImpl = context.declarationFactory.staticDefaultImplForInterface(function)
                addedMembers += staticDefaultImpl

                function.body = null
                // todo should we make the old function abstract as well?
            }
        }

        addedMembers.forEach {
            it.parent = irClass
            irClass.addMember(it)
        }
    }
}
