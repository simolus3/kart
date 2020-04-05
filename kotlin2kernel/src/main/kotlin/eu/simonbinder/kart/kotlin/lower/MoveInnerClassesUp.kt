package eu.simonbinder.kart.kotlin.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.lower.parents
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

/**
 * Moves transformed inner classes up so that they're a top-level child node of files.
 */
class MoveInnerClassesUp : FileLoweringPass {

    override fun lower(irFile: IrFile) {
        irFile.acceptVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                if (element is IrDeclarationContainer) {
                    // Run over cached list because visitClass might mutate it
                    for (child in element.declarations.toList()) {
                        child.acceptVoid(this)
                    }
                } else {
                    element.acceptChildrenVoid(this)
                }
            }

            override fun visitClass(declaration: IrClass) {
                // First, raise subclasses of subclasses into first-level subclasses
                for (child in declaration.declarations) {
                    if (child !is IrClass) continue

                    child.acceptVoid(this)
                }

                // Next, we'll raise subclasses next to this class
                val parent = declaration.parent as IrDeclarationContainer
                val subClasses = declaration.declarations.filterIsInstance<IrClass>()
                declaration.declarations.removeAll(subClasses)

                subClasses.forEach(parent::addChild)
            }
        })
    }

}