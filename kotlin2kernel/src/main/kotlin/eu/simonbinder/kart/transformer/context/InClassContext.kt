package eu.simonbinder.kart.transformer.context

import eu.simonbinder.kart.kernel.ast.members.Class

class InClassContext(
    parent: CompilationContext,
    override val target: Class
): CompilationContext by parent, MemberCompilationContext