package eu.simonbinder.kart.transformer.metadata

import eu.simonbinder.kart.kernel.statements.LabeledStatement

class LoopMetaData(
    var hasBreak: Boolean = false,
    var hasContinue: Boolean = false
) {
    /**
     * In Kernel, loops don't automatically become a target for break statements, we need to create a [LabeledStatement]
     * automatically. If the loop contains a `break`, we'll put a labeled statement outside of the loop, which we can
     * then break out of.
     */
    var outerLabelForBreak = LabeledStatement()

    /**
     * There is no continue statement in Kernel, so we need to desugar it. Luckily for us, we can make each statement a
     * target of a break, not only loops. So, we can compile
     * ```kotlin
     * while (someCondition) {
     *   BODY1
     *   if (someOtherCondition) continue;
     *   BODY2
     * }
     * ```
     * to
     * ```
     * while (someCondition) {
     *   L: {
     *     BODY1;
     *     if (someOtherCondition) break L;
     *     BODY2;
     *   }
     * }
     * ```
     *
     * We use a inner [LabeledStatement] if we know the loop has a `continue` in it.
     */
    val innerLabelForContinue = LabeledStatement()
}