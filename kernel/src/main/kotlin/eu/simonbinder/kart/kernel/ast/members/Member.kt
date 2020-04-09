package eu.simonbinder.kart.kernel.ast.members

import eu.simonbinder.kart.kernel.ast.NamedNode
import eu.simonbinder.kart.kernel.Reference
import eu.simonbinder.kart.kernel.ast.HasAnnotations

abstract class Member(reference: Reference? = null) : NamedNode(reference), HasAnnotations