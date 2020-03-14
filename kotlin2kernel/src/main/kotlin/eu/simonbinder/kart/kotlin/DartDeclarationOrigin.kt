package eu.simonbinder.kart.kotlin

import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl

interface DartDeclarationOrigin : IrDeclarationOrigin {

    object LOWERED_INIT_BLOCK: IrDeclarationOriginImpl("LOWERED_INIT_BLOCK")

}