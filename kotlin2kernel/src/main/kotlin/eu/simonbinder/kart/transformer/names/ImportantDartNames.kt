package eu.simonbinder.kart.transformer.names

import eu.simonbinder.kart.kernel.CanonicalName
import eu.simonbinder.kart.kernel.CanonicalName.Companion.CONSTRUCTORS
import eu.simonbinder.kart.kernel.CanonicalName.Companion.METHODS
import eu.simonbinder.kart.kernel.asReference
import eu.simonbinder.kart.kernel.types.InterfaceType

class ImportantDartNames(private val root: CanonicalName) {

    val dartCore = root.getChild("dart:core")

    val objectName = dartCore.getChild("Object")

    val intName = dartCore.getChild("int")
    val numName = dartCore.getChild("num")
    val intType = InterfaceType(classReference = intName.asReference())
    val boolType = InterfaceType(classReference = dartCore.getChild("bool").asReference())
    val doubleType = InterfaceType(classReference = dartCore.getChild("double").asReference())
    val stringType = InterfaceType(classReference = dartCore.getChild("String").asReference())
    val objectType = InterfaceType(classReference = objectName.asReference())

    val objectDefaultConstructor = objectName.getChild(CONSTRUCTORS).getChild("")

    val dartPrint = dartCore.getChild(METHODS).getChild("print").asReference()
    val objectEquals = objectName.getChild(METHODS).getChild("==").asReference()
    val identical = dartCore.getChild(METHODS).getChild("identical").asReference()

    val numPlus = numName.getChild(METHODS).getChild("+").asReference()
    val numMinus = numName.getChild(METHODS).getChild("-").asReference()
    val numTimes = numName.getChild(METHODS).getChild("*").asReference()
    val numTruncatingDivision = numName.getChild(METHODS).getChild("~/").asReference()
    val numMod = numName.getChild(METHODS).getChild("%").asReference()
    val intAnd = intName.getChild(METHODS).getChild("&").asReference()

    val numLess = numName.getChild(METHODS).getChild("<").asReference()
    val numLessEq = numName.getChild(METHODS).getChild("<=").asReference()
    val numMore = numName.getChild(METHODS).getChild(">").asReference()
    val numMoreEq = numName.getChild(METHODS).getChild(">=").asReference()
}