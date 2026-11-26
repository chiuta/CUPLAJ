package ro.trom.cuplaj

/**
 * Telephone eras. The era is a year; what matters is the kind it falls into:
 * 0 rotary (<1990), 1 keypad (<2000), 2 mobile (<2020), 3 smartphone (<2040),
 * 4 wearables (<2060, whispered calls, nod or shake to answer), 5 shared mind (2060+, no handset).
 * Kinds 3-5 are the 2025 / 2045 / 2080 eras added in 1.10.
 */
object Era {
    val ALL = intArrayOf(1985, 1995, 2003, 2025, 2045, 2080)
    const val ROTARY = 0
    const val KEYPAD = 1
    const val MOBILE = 2
    const val SMART = 3
    const val WEAR = 4
    const val MIND = 5

    fun kind(e: Int): Int = when {
        e < 1990 -> ROTARY
        e < 2000 -> KEYPAD
        e < 2020 -> MOBILE
        e < 2040 -> SMART
        e < 2060 -> WEAR
        else -> MIND
    }

    /** Maximum length of a text message in this era. */
    fun msgLimit(e: Int): Int = when (kind(e)) {
        ROTARY, KEYPAD -> 500
        MOBILE -> 160
        SMART -> 500
        WEAR -> 200
        else -> 60
    }

    /** True when the phone has digits to dial (rotary, keypad, mobile, smartphone). Wearables and shared mind call by choosing a name. */
    fun hasDigits(e: Int): Boolean = kind(e) <= SMART

    class Sweep(val eras: List<Int>, val stepMs: Long)

    /**
     * "Time travel" cue text: `1985,1995,2003,2025,2045,2080;4000` steps through the eras, one every 4000 ms.
     * Returns null for a plain single era. Years must be 1900..2200, at most 12 steps, step 1.5 to 30 seconds (default 4 s).
     */
    fun parseSweep(text: String): Sweep? {
        val semi = text.indexOf(';')
        val list = if (semi >= 0) text.substring(0, semi) else text
        if (!list.contains(',')) return null
        val eras = list.split(',').mapNotNull { it.trim().toIntOrNull() }.filter { it in 1900..2200 }.take(12)
        if (eras.size < 2) return null
        val step = (if (semi >= 0) text.substring(semi + 1).trim().toLongOrNull() else null) ?: 4000L
        return Sweep(eras, step.coerceIn(1500L, 30000L))
    }

    /** First era of a cue text, whether single or a sweep; null if it is not a valid era. */
    fun firstEra(text: String): Int? {
        val s = parseSweep(text)
        if (s != null) return s.eras[0]
        return text.trim().toIntOrNull()?.takeIf { it in 1900..2200 }
    }
}
