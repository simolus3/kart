package eu.simonbinder.kart.transformer.names

import eu.simonbinder.kart.kernel.CanonicalName
import eu.simonbinder.kart.kernel.asReference
import eu.simonbinder.kart.kernel.types.InterfaceType

class ImportantDartNames(private val root: CanonicalName) {

    val dartCore = root.getChild("dart:core")

    val objectName = dartCore.getChild("Object")

    val intName = dartCore.getChild("int")
    val numName = dartCore.getChild("num")
    val intType = InterfaceType(classReference = intName.asReference())
    val doubleType = InterfaceType(classReference = dartCore.getChild("double").asReference())
    val stringType = InterfaceType(classReference = dartCore.getChild("String").asReference())
    val objectType = InterfaceType(classReference = objectName.asReference())

    val dartPrint = dartCore.getChild("@methods").getChild("print").asReference()
    val objectEquals = objectName.getChild("@methods").getChild("==").asReference()
    val identical = dartCore.getChild("@methods").getChild("identical").asReference()

    val numPlus = numName.getChild("@methods").getChild("+").asReference()
    val numMinus = numName.getChild("@methods").getChild("-").asReference()
    val intAnd = intName.getChild("@methods").getChild("&").asReference()
}