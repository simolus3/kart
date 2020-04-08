package eu.simonbinder.kart.kernel.ast.members

import eu.simonbinder.kart.kernel.ast.NamedNode
import eu.simonbinder.kart.kernel.Reference

abstract class Member(reference: Reference? = null) : NamedNode(reference)