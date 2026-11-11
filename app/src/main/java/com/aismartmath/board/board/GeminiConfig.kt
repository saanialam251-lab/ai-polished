package com.aismartmath.board.board

/**
 * PUT YOUR OWN API KEYS HERE. Fill any of them; leave the others as "".
 * The app tries them in this order: OpenAI key 1, OpenAI key 2, Gemini key 1, Gemini key 2,
 * and moves to the next one automatically when a key is refused or has run out.
 *
 * Gemini keys: https://aistudio.google.com/apikey
 * OpenAI keys: https://platform.openai.com/api-keys  (the OpenAI API needs credit on platform.openai.com;
 *              a ChatGPT Plus subscription does not include it)
 *
 * Careful: if your GitHub repository is public, do not commit real keys here. Use a private repository.
 */
object GeminiConfig {
    const val API_KEY_1 = "AQ.Ab8RN6JmUiLV9rAC4ej3kr-jaGt6g2rs-3XDXxI2j-ZnmsjSQQ"
    const val API_KEY_2 = "AQ.Ab8RN6IGQWIh6dsmdPVHCH7QUOOIzdNABUHbjXZlBVxIlDRypg"

    const val OPENAI_KEY_1 = "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"
    const val OPENAI_KEY_2 = "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"

    /** The OpenAI model to use. If it is refused, gpt-4o-mini is tried next. */
    const val OPENAI_MODEL = "gpt-4o"
}
