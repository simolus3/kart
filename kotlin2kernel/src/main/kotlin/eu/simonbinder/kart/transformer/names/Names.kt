package eu.simonbinder.kart.transformer.names

import eu.simonbinder.kart.kernel.CanonicalName
import eu.simonbinder.kart.kernel.Reference
import eu.simonbinder.kart.kernel.types.DartType
import eu.simonbinder.kart.kernel.types.DynamicType
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.types.IrDynamicType
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.nameForIrSerialization
import org.jetbrains.kotlin.name.Name

class Names {

    val root = CanonicalName.root()
    val dartNames = ImportantDartNames(root)

    private val fileToName: MutableMap<IrFile, Reference> = mutableMapOf()
    private val declarationToName: MutableMap<IrDeclaration, Reference> = mutableMapOf()

    fun nameFor(declaration: IrDeclaration): Reference = declarationToName.computeIfAbsent(declaration) {
        // todo support class members
        val library = nameFor(declaration.file)

        Reference(library.canonicalName!!
            .getChild("@methods")
            .getChild(declaration.nameForIrSerialization.identifier))
    }

    fun nameFor(file: IrFile): Reference = fileToName.computeIfAbsent(file) {
        val fqn = file.fqName
        val name = root.getSubChild(if (fqn.isRoot) listOf("rootKt") else fqn.pathSegments().map(Name::getIdentifier))
        Reference(name)
    }
}