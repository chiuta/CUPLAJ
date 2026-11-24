package ro.trom.cuplaj

import java.net.URLDecoder
import java.net.URLEncoder

/** Invitație: un singur link care configurează telefonul invitat (adrese, cheie, PIN, cameră, rol). */
class Invite(
    val hosts: List<String>,
    val key: String = "",
    val pin: String = "",
    val room: String = "",
    val hub: String = "",
    val role: String = "spectator"
)

object InviteLink {
    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
    private fun dec(s: String) = URLDecoder.decode(s, "UTF-8")

    fun build(i: Invite): String {
        val sb = StringBuilder("cuplaj://join?r=").append(enc(i.role))
        if (i.hosts.isNotEmpty()) sb.append("&h=").append(enc(i.hosts.joinToString(",")))
        if (i.key.isNotEmpty()) sb.append("&k=").append(enc(i.key))
        if (i.pin.isNotEmpty()) sb.append("&p=").append(enc(i.pin))
        if (i.room.isNotEmpty()) sb.append("&c=").append(enc(i.room))
        if (i.hub.isNotEmpty()) sb.append("&n=").append(enc(i.hub))
        return sb.toString()
    }

    /** Întoarce null pentru orice link care nu e o invitație validă; valorile sunt curățate și limitate. */
    fun parse(link: String?): Invite? {
        if (link == null || link.length > 800) return null
        val t = link.trim()
        if (!t.startsWith("cuplaj://join")) return null
        val q = t.substringAfter('?', "")
        val m = HashMap<String, String>()
        try {
            for (part in q.split('&')) {
                if (part.isEmpty()) continue
                val k = part.substringBefore('=')
                val v = dec(part.substringAfter('=', ""))
                m[k] = v.filter { it >= ' ' }.take(200)
            }
        } catch (_: Exception) {
            return null
        }
        val role = m["r"] ?: "spectator"
        if (role != "spectator" && role != "actor") return null // nimeni nu devine Regie dintr-un link
        val hosts = (m["h"] ?: "").split(',').map { it.trim() }
            .filter { it.isNotEmpty() && it.length <= 100 && it.all { c -> c.isLetterOrDigit() || c in ".-:[]_" } }
            .take(8)
        val pin = (m["p"] ?: "").filter { it.isDigit() }.take(8)
        return Invite(hosts, (m["k"] ?: "").take(64), pin, (m["c"] ?: "").take(32), (m["n"] ?: "").take(40), role)
    }
}
