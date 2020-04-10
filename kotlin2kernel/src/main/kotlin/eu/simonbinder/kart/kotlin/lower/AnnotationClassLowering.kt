package eu.simonbinder.kart.kotlin.lower

import eu.simonbinder.kart.kotlin.DartBackendContext
import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.util.getPropertyField
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.primaryConstructor

/**
 * In Kotlin IR, the primary constructor of an annotation class doesn't have a body. Since we need a body for Dart
 * compilation, this generates the default body that will just set the according fields.
 */
class AnnotationClassLowering(private val context: DartBackendContext) : ClassLoweringPass {

    private val anyConstructor = context.irBuiltIns.anyClass.owner.primaryConstructor!!

    override fun lower(irClass: IrClass) {
        if (irClass.kind != ClassKind.ANNOTATION_CLASS) return

        val constructor = irClass.primaryConstructor!!
        if (constructor.body != null) return

        constructor.createDefaultBody()
    }

    private fun IrConstructor.createDefaultBody() {
        val builder = context.createIrBuilder(symbol).at(this)
        val klass = parentAsClass
        val thisInKlass = klass.thisReceiver!!

        body = builder.irBlockBody {
            for (param in valueParameters) {
                val associatedField = klass.getPropertyField(param.name.identifier)!!.owner
                +irSetField(irGet(thisInKlass), associatedField, irGet(param))
            }

            +irDelegatingConstructorCall(anyConstructor)
        }
    }

}