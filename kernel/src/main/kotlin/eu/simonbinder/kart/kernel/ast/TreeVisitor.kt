package eu.simonbinder.kart.kernel.ast

import eu.simonbinder.kart.kernel.ast.expressions.Arguments
import eu.simonbinder.kart.kernel.ast.expressions.Expression
import eu.simonbinder.kart.kernel.ast.expressions.ExpressionVisitor
import eu.simonbinder.kart.kernel.ast.expressions.NamedExpression
import eu.simonbinder.kart.kernel.ast.members.Class
import eu.simonbinder.kart.kernel.ast.members.Component
import eu.simonbinder.kart.kernel.ast.members.Library
import eu.simonbinder.kart.kernel.ast.members.Member
import eu.simonbinder.kart.kernel.ast.members.MemberVisitor
import eu.simonbinder.kart.kernel.ast.members.initializers.FieldInitializer
import eu.simonbinder.kart.kernel.ast.members.initializers.Initializer
import eu.simonbinder.kart.kernel.ast.members.initializers.RedirectingInitializer
import eu.simonbinder.kart.kernel.ast.members.initializers.SuperInitializer
import eu.simonbinder.kart.kernel.ast.statements.Catch
import eu.simonbinder.kart.kernel.ast.statements.Statement
import eu.simonbinder.kart.kernel.ast.statements.StatementVisitor
import eu.simonbinder.kart.kernel.types.TypeParameter

interface TreeVisitor<R> :
    ExpressionVisitor<R>,
    StatementVisitor<R>,
    MemberVisitor<R> {

    fun defaultTreeNode(node: TreeNode): R

    override fun defaultExpression(node: Expression): R = defaultTreeNode(node)
    override fun defaultStatement(node: Statement): R = defaultTreeNode(node)
    override fun defaultMember(node: Member): R = defaultTreeNode(node)

    fun visitArguments(node: Arguments): R = defaultTreeNode(node)
    fun visitCatch(node: Catch): R = defaultTreeNode(node)
    fun visitComponent(node: Component): R = defaultTreeNode(node)
    fun visitFunctionNode(node: FunctionNode): R = defaultTreeNode(node)
    fun visitLibrary(node: Library): R = defaultTreeNode(node)
    fun visitClass(node: Class): R = defaultTreeNode(node)
    fun visitNamedExpression(node: NamedExpression): R = defaultTreeNode(node)
    fun visitTypeParameter(node: TypeParameter): R = defaultTreeNode(node)

    fun defaultInitializer(node: Initializer): R = defaultTreeNode(node)
    fun visitFieldInitializer(node: FieldInitializer): R = defaultTreeNode(node)
    fun visitSuperInitializer(node: SuperInitializer): R = defaultInitializer(node)
    fun visitRedirectingInitializer(node: RedirectingInitializer): R = defaultInitializer(node)
}