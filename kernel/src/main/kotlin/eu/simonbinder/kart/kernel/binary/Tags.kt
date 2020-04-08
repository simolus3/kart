package eu.simonbinder.kart.kernel.binary

object Tags {
    const val NOTHING = 0u
    const val SOMETHING = 1u

    const val CLASS = 2u
    const val FUNCTION_NODE = 3u
    const val FIELD = 4u
    const val CONSTRUCTOR = 5u
    const val PROCEDURE = 6u

    const val FIELD_INITIALIZER = 8u
    const val SUPER_INITIALIZER = 9u
    const val REDIRECTING_INITIALIZER = 10u

    const val INVALID_EXPRESSION = 19u
    const val VARIABLE_GET = 20u
    const val VARIABLE_SET = 21u
    const val PROPERTY_GET = 22u
    const val PROPERTY_SET = 23u
    const val STATIC_GET = 26u
    const val STATIC_SET = 27u
    const val METHOD_INVOCATION = 28u
    const val STATIC_INVOCATION = 30u
    const val CONSTRUCTOR_INVOCATION = 31u
    const val NOT = 33u
    const val LOGICAL_EXPRESSION = 34u
    const val CONDITIONAL_EXPRESSION = 35u
    const val STRING_CONCATENATION = 36u
    const val IS_EXPRESSION = 37u
    const val AS_EXPRESSION = 38u
    const val STRING_LITERAL = 39u
    const val DOUBLE_LITERAL = 40u
    const val TRUE_LITERAL = 41u
    const val FALSE_LITERAL = 42u
    const val NULL_LITERAL = 43u
    const val THIS = 46u
    const val THROW = 48u
    const val POSITIVE_INT_LITERAL = 55u
    const val NEGATIVE_INT_LITERAL = 56u
    const val BIG_INT_LITERAL = 57u
    const val BLOCK_EXPRESSION = 82u

    const val EXPRESSION_STATEMENT = 61u
    const val BLOCK = 62u
    const val EMPTY_STATEMENT = 63u
    const val LABELED_STATEMENT = 65u
    const val BREAK_STATEMENT = 66u
    const val WHILE_STATEMENT = 67u
    const val DO_STATEMENT = 68u
    const val IF_STATEMENT = 73u
    const val RETURN_STATEMENT = 74u
    const val TRY_CATCH = 75u
    const val TRY_FINALLY = 76u

    const val VARIABLE_DECLARATION = 78u

    const val BOTTOM_TYPE = 89u
    const val INVALID_TYPE = 90u
    const val DYNAMIC_TYPE = 91u
    const val VOID_TYPE = 92u
    const val INTERFACE_TYPE = 93u
    const val SIMPLE_INTERFACE_TYPE = 96u
    const val NEVER_TYPE = 98u

    const val SPECIALIZED_INT_LITERAL = 144u
    const val SPECIALIZED_INT_LITERAL_BIAS = 3u
    const val SPECIALIZED_VARIABLE_GET = 128u
    const val SPECIALIZED_VARIABLE_SET = 136u

    const val MAGIC = 0x90ABCDEFu

    fun withoutSpecializedPayload(tag: UInt): UInt {
        // All "special" tags (variable get + set, int literal) have their highest  bit set.
        // In that case, the lower 3 bit indicate the payload
        return if (tag and 0x80u == 0x80u) tag and 0xF8u else tag
    }

    fun specializedPayload(tag: UInt): UInt = tag and 0x7u
    fun unbiasedSpecializedPayload(tag: UInt) = specializedPayload(tag).toInt() - 3
}