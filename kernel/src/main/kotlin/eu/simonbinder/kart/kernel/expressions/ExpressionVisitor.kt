package eu.simonbinder.kart.kernel.expressions

interface ExpressionVisitor<R> {

    fun defaultExpression(node: Expression): R

    // Logic

    fun visitConditionalExpression(node: ConditionalExpression): R = defaultExpression(node)
    fun visitLogicalExpression(node: LogicalExpression): R = defaultExpression(node)
    fun visitNot(node: Not): R = defaultExpression(node)

    // Literals

    fun <L> defaultLiteral(node: Literal<L>): R = defaultExpression(node)

    fun visitStringLiteral(node: StringLiteral): R = defaultLiteral(node)
    fun visitDoubleLiteral(node: DoubleLiteral): R = defaultLiteral(node)
    fun visitIntegerLiteral(node: IntegerLiteral): R = defaultLiteral(node)
    fun visitBooleanLiteral(node: BooleanLiteral): R = defaultLiteral(node)
    fun visitNullLiteral(node: NullLiteral): R = defaultLiteral(node)

    // Invocations

    fun visitStaticInvocation(node: StaticInvocation): R = defaultExpression(node)
    fun visitMethodInvocation(node: MethodInvocation): R = defaultExpression(node)

    // Variables

    fun visitVariableGet(node: VariableGet): R = defaultExpression(node)
    fun visitVariableSet(node: VariableSet): R = defaultExpression(node)

    // Casts

    fun visitAsExpression(node: AsExpression): R = defaultExpression(node)
    fun visitIsExpression(node: IsExpression): R = defaultExpression(node)
    fun visitNullCheck(node: NullCheck): R = defaultExpression(node)

    // Misc

    fun visitBlockExpression(node: BlockExpression): R = defaultExpression(node)
    fun visitStringConcatenation(node: StringConcatenation): R = defaultExpression(node)
    fun visitInvalidExpression(node: InvalidExpression): R = defaultExpression(node)
    fun visitThrow(node: Throw): R = defaultExpression(node)
}