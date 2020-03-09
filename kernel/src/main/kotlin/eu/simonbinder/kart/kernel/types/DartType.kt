package eu.simonbinder.kart.kernel.types

import eu.simonbinder.kart.kernel.Node

interface DartType : Node {
    val nullability: Nullability
}
