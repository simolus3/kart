package eu.simonbinder.kart.transformer.names

import eu.simonbinder.kart.transformer.identifierOrNull
import org.jetbrains.kotlin.backend.jvm.ir.erasedUpperBound
import org.jetbrains.kotlin.codegen.state.md5base64
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable

/**
 * Dart doesn't support method overloads, so we add a hash suffix to each function to make them unique.
 *
 * See also: [org.jetbrains.kotlin.backend.jvm.lower.inlineclasses.InlineClassAbi], which performs a similar procedure
 * for inline classes on the JVM.
 */
object NameMangling {

    fun mangledNameFor(irFunction: IrFunction): String {
        val baseName = irFunction.name.identifierOrNull ?: ""

        return if (irFunction.valueParameters.isNotEmpty()) {
            val suffix = suffixFor(irFunction)
            if (baseName.isEmpty()) suffix else "$baseName-$suffix"
        } else {
            baseName
        }
    }

    private fun suffixFor(function: IrFunction): String {
        assert(function.extensionReceiverParameter == null)

        val descriptor = buildString {
            function.valueParameters.forEach { parameter ->
                val type = parameter.type

                append(type.erasedUpperBound.fqNameWhenAvailable!!)
                if (type.isNullable()) append('?')
                append(';')
            }
        }

        return md5base64(descriptor)
    }

}