package eu.simonbinder.kart.transformer.context

import eu.simonbinder.kart.kernel.members.HasMembers

interface MemberCompilationContext : CompilationContext {
    val target: HasMembers
}