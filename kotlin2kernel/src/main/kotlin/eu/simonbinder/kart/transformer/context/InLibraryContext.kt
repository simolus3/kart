package eu.simonbinder.kart.transformer.context

import eu.simonbinder.kart.kernel.members.Library

class InLibraryContext(parent: GlobalCompilationContext, val library: Library) : CompilationContext by parent