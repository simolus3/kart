package eu.simonbinder.kart.transformer.context

import eu.simonbinder.kart.kernel.ast.members.HasMembers

interface MemberCompilationContext : CompilationContext {
    val target: HasMembers
}