package ro.trom.cuplaj

import android.content.Context
import java.util.Locale

/** Traduceri în 8 limbi, din assets/i18n.tsv (cheie TAB en TAB ro TAB fr TAB it TAB es TAB pt TAB de TAB hu). */
object L {
    val codes = listOf("en", "ro", "fr", "it", "es", "pt", "de", "hu")
    val names = listOf("English", "Română", "Français", "Italiano", "Español", "Português", "Deutsch", "Magyar")
    private val tbl = HashMap<String, Array<String>>()
    var lang = "en"

    fun load(ctx: Context) {
        if (tbl.isNotEmpty()) return
        ctx.assets.open("i18n.tsv").bufferedReader(Charsets.UTF_8).useLines { lines ->
            for (line in lines) {
                if (line.isBlank() || line.startsWith("#")) continue
                val p = line.split("\t")
                if (p.size >= 9) tbl[p[0]] = Array(8) { i -> p[i + 1].replace("\\n", "\n") }
            }
        }
    }

    fun detect(): String {
        val l = Locale.getDefault().language
        return if (codes.contains(l)) l else "en"
    }

    fun t(k: String): String {
        val a = tbl[k] ?: return k
        val i = codes.indexOf(lang).let { if (it < 0) 0 else it }
        val s = a[i]
        return if (s.isEmpty()) a[0] else s
    }

    fun t(k: String, vararg args: Any): String = try {
        String.format(t(k), *args)
    } catch (_: Exception) {
        t(k)
    }
}
