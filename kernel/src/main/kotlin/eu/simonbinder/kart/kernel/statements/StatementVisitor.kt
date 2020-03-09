package eu.simonbinder.kart.kernel.statements

interface StatementVisitor<R> {

    fun defaultStatement(node: Statement): R

    fun visitAssertStatement(node: AssertStatement): R = defaultStatement(node)
    fun visitBlock(node: Block): R = defaultStatement(node)
    fun visitExpressionStatement(node: ExpressionStatement): R = defaultStatement(node)
    fun visitLabeledStatement(node: LabeledStatement): R = defaultStatement(node)
    fun visitEmptyStatement(node: EmptyStatement): R = defaultStatement(node)
    fun visitIfStatement(node: IfStatement): R = defaultStatement(node)
    fun visitVariableDeclaration(node: VariableDeclaration): R = defaultStatement(node)

    // Loops
    fun visitWhile(node: WhileStatement): R = defaultStatement(node)
    fun visitDoWhile(node: DoStatement): R = defaultStatement(node)

    // Control flow
    fun visitReturn(node: ReturnStatement): R = defaultStatement(node)
    fun visitBreak(node: BreakStatement): R = defaultStatement(node)
}