package eu.simonbinder.kart.transformer.names

import eu.simonbinder.kart.kernel.CanonicalName
import eu.simonbinder.kart.kernel.Name as DartName
import eu.simonbinder.kart.kernel.Reference
import eu.simonbinder.kart.kernel.asReference
import eu.simonbinder.kart.kotlin.DartBackendContext
import eu.simonbinder.kart.transformer.identifierOrNull
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class Names(private val backendContext: DartBackendContext) {

    val root = CanonicalName.root()
    val dartNames = ImportantDartNames(root)

    private val fileToName: MutableMap<IrFile, Reference> = mutableMapOf()
    private val declarationToName: MutableMap<IrDeclaration, Reference> = mutableMapOf()

    fun nameFor(declaration: IrDeclaration): Reference = declarationToName.getOrPut(declaration) {
        val containingPackage = declaration.getPackageFragment()
        val library = if (containingPackage is IrExternalPackageFragment) {
            nameForFqn(containingPackage.fqName)
        } else {
            nameFor(declaration.file)
        }

        val baseName = if (declaration.parent is IrClass) {
            nameFor(declaration.parentAsClass).canonicalName!!
        } else {
            library.canonicalName!!
        }

        val changedUnqualifiedName = backendContext.changedUnqualifiedNames[declaration]

        val name = when (declaration) {
            is IrField -> {
                val fieldName = changedUnqualifiedName ?: declaration.name.identifier
                // In Kernel, Fields and getters/setters are weirdly mixed up (one can use a StaticGet that refers to a
                // getter, for instance). To cleanly separate fields and their accessors, we always suffix the field.
                baseName.getChild(CanonicalName.FIELDS).getChild(fieldName + "_field")
            }
            is IrSimpleFunction -> {
                if (declaration.isGetter || declaration.isSetter) {
                    val nameOfField = changedUnqualifiedName ?:
                        declaration.correspondingPropertySymbol!!.descriptor.name.identifier
                    baseName
                        .getChild(if (declaration.isGetter) CanonicalName.GETTERS else CanonicalName.SETTERS)
                        .getChild(nameOfField)
                } else {
                    val functionName = changedUnqualifiedName ?: declaration.name.identifier
                    baseName.getChild(CanonicalName.METHODS).getChild(functionName)
                }
            }
            is IrClass -> {
                val nameOfClass = changedUnqualifiedName ?: declaration.name.identifier
                baseName.getChild(nameOfClass)
            }
            is IrConstructor -> {
                val nameOfConstructor = changedUnqualifiedName ?: NameMangling.mangledNameFor(declaration)
                baseName.getChild(CanonicalName.CONSTRUCTORS).getChild(nameOfConstructor)
            }
            else -> TODO()
        }

        name.asReference()
    }

    fun simpleNameFor(declaration: IrDeclaration, includeField: Boolean = true): DartName {
        val overriddenName = backendContext.changedUnqualifiedNames[declaration]
        if (overriddenName != null) return DartName(overriddenName)

        return if (declaration.isGetter || declaration.isSetter) {
            simpleNameFor((declaration as IrSimpleFunction).correspondingPropertySymbol!!.owner, false)
        } else if (includeField && declaration is IrField) {
            DartName(declaration.name.identifier + "_field")
        }  else if (declaration is IrFunction) {
            DartName(NameMangling.mangledNameFor(declaration))
        } else {
            DartName(declaration.nameForIrSerialization.identifier)
        }
    }

    fun nameFor(file: IrFile): Reference = fileToName.computeIfAbsent(file) {
        nameForFqn(file.fqName)
    }

    private fun nameForFqn(fqn: FqName): Reference {
        val name = root.getSubChild(if (fqn.isRoot) listOf("rootKt") else fqn.pathSegments().map(Name::getIdentifier))
        return name.asReference()
    }
}