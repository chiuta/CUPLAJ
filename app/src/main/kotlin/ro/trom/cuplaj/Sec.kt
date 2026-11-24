package ro.trom.cuplaj

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Cifrare AES-128-GCM cu o cheie de rețea partajată (derivată din fraza hubului). */
object Sec {
    private val rnd = SecureRandom()

    /** Derivă cheia din frază; gol = fără cifrare. */
    fun deriveKey(secret: String): ByteArray? {
        val s = secret.trim()
        if (s.isEmpty()) return null
        val f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        return f.generateSecret(PBEKeySpec(s.toCharArray(), "CUPLAJ-v1".toByteArray(), 10000, 128)).encoded
    }

    fun newPassphrase(): String {
        val abc = "abcdefghjkmnpqrstuvwxyz23456789"
        return (1..4).joinToString("-") { (1..4).map { abc[rnd.nextInt(abc.length)] }.joinToString("") }
    }

    fun randomBytes(n: Int): ByteArray = ByteArray(n).also { rnd.nextBytes(it) }

    fun encrypt(key: ByteArray, nonce: ByteArray, pt: ByteArray): ByteArray {
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
        return c.doFinal(pt)
    }

    fun decrypt(key: ByteArray, nonce: ByteArray, ct: ByteArray): ByteArray? = try {
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
        c.doFinal(ct)
    } catch (_: Exception) {
        null
    }
}

/** G.711 μ-law: 16 biți/8 kHz (320 octeți) ⇄ 8 biți (160 octeți). */
object G711 {
    private fun encSample(s0: Int): Int {
        var s = s0
        val sign = if (s < 0) 0x80 else 0
        if (s < 0) s = -s
        if (s > 32635) s = 32635
        s += 0x84
        var e = 7
        var m = 0x4000
        while (e > 0 && (s and m) == 0) {
            e--
            m = m shr 1
        }
        val mant = (s shr (e + 3)) and 0x0F
        return (sign or (e shl 4) or mant).inv() and 0xFF
    }

    private val dec = IntArray(256).also { t ->
        for (i in 0 until 256) {
            val u = i.inv() and 0xFF
            val sign = u and 0x80
            val e = (u shr 4) and 7
            val m = u and 0x0F
            var v = ((m shl 3) + 0x84) shl e
            v -= 0x84
            t[i] = if (sign != 0) -v else v
        }
    }

    /** PCM 16 biți little-endian → μ-law. */
    fun encode(pcm: ByteArray, off: Int = 0, len: Int = pcm.size - off): ByteArray {
        val n = len / 2
        val out = ByteArray(n)
        for (i in 0 until n) {
            val s = (pcm[off + 2 * i].toInt() and 0xFF) or (pcm[off + 2 * i + 1].toInt() shl 8)
            out[i] = encSample(s.toShort().toInt()).toByte()
        }
        return out
    }

    fun decode(u: ByteArray, off: Int = 0, len: Int = u.size - off): ByteArray {
        val out = ByteArray(len * 2)
        for (i in 0 until len) {
            val v = dec[u[off + i].toInt() and 0xFF]
            out[2 * i] = (v and 0xFF).toByte()
            out[2 * i + 1] = ((v shr 8) and 0xFF).toByte()
        }
        return out
    }
}
