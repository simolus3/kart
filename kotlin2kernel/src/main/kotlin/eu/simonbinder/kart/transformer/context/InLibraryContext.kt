package eu.simonbinder.kart.transformer.context

import eu.simonbinder.kart.kernel.ast.members.Library

class InLibraryContext(
    parent: GlobalCompilationContext,
    override val target: Library
): CompilationContext by parent, MemberCompilationContext {
    val library: Library get() = target
}