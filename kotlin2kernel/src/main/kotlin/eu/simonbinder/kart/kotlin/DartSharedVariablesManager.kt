package eu.simonbinder.kart.kotlin

import org.jetbrains.kotlin.backend.common.ir.SharedVariablesManager
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrSetVariable
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol

// we don't apply the shared variables lowering, so we don't have to implement anything here

object DartSharedVariablesManager : SharedVariablesManager {
    override fun declareSharedVariable(originalDeclaration: IrVariable): IrVariable {
        throw NotImplementedError()
    }

    override fun defineSharedValue(
        originalDeclaration: IrVariable,
        sharedVariableDeclaration: IrVariable
    ): IrStatement {
        throw NotImplementedError()
    }

    override fun getSharedValue(sharedVariableSymbol: IrVariableSymbol, originalGet: IrGetValue): IrExpression {
        throw NotImplementedError()
    }

    override fun setSharedValue(sharedVariableSymbol: IrVariableSymbol, originalSet: IrSetVariable): IrExpression {
        throw NotImplementedError()
    }
}