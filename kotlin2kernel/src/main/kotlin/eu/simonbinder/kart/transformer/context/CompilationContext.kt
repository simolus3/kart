package eu.simonbinder.kart.transformer.context

import eu.simonbinder.kart.transformer.CompilationInfo

interface CompilationContext {
    val info: CompilationInfo
}

val CompilationContext.names get() = info.names