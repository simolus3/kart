package eu.simonbinder.kart.transformer.context

import eu.simonbinder.kart.kernel.members.Library
import eu.simonbinder.kart.transformer.CompilationInfo

class GlobalCompilationContext(override val info: CompilationInfo) : CompilationContext {

    fun inLibrary(library: Library) = InLibraryContext(this, library)

}