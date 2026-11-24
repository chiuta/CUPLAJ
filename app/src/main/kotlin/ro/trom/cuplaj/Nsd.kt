package ro.trom.cuplaj

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo

/** Descoperire mDNS/DNS‑SD: găsește hubul chiar și când baliza UDP de difuzare e blocată de router. */
class Nsd(private val c: Context) {
    private val mgr: NsdManager? = try {
        c.getSystemService(Context.NSD_SERVICE) as? NsdManager
    } catch (_: Exception) {
        null
    }
    private var reg: NsdManager.RegistrationListener? = null
    private var disc: NsdManager.DiscoveryListener? = null
    private val pending = ArrayDeque<NsdServiceInfo>()
    private var resolving = false

    companion object {
        const val TYPE = "_cuplaj._tcp."
    }

    fun advertise(name: String, port: Int) {
        val m = mgr ?: return
        val info = NsdServiceInfo()
        info.serviceName = ("CUPLAJ " + name).take(40)
        info.serviceType = TYPE
        info.port = port
        val l = object : NsdManager.RegistrationListener {
            override fun onRegistrationFailed(i: NsdServiceInfo?, e: Int) {}
            override fun onUnregistrationFailed(i: NsdServiceInfo?, e: Int) {}
            override fun onServiceRegistered(i: NsdServiceInfo?) {}
            override fun onServiceUnregistered(i: NsdServiceInfo?) {}
        }
        reg = l
        try {
            m.registerService(info, NsdManager.PROTOCOL_DNS_SD, l)
        } catch (_: Exception) {
            reg = null
        }
    }

    fun unadvertise() {
        try {
            reg?.let { mgr?.unregisterService(it) }
        } catch (_: Exception) {
        }
        reg = null
    }

    fun discover(onHub: (name: String, host: String, port: Int) -> Unit) {
        val m = mgr ?: return
        if (disc != null) return
        val l = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(t: String?, e: Int) {}
            override fun onStopDiscoveryFailed(t: String?, e: Int) {}
            override fun onDiscoveryStarted(t: String?) {}
            override fun onDiscoveryStopped(t: String?) {}
            override fun onServiceLost(i: NsdServiceInfo?) {}
            override fun onServiceFound(i: NsdServiceInfo) {
                synchronized(pending) { pending.add(i) }
                next(m, onHub)
            }
        }
        disc = l
        try {
            m.discoverServices(TYPE, NsdManager.PROTOCOL_DNS_SD, l)
        } catch (_: Exception) {
            disc = null
        }
    }

    @Suppress("DEPRECATION")
    private fun next(m: NsdManager, onHub: (String, String, Int) -> Unit) {
        val i: NsdServiceInfo
        synchronized(pending) {
            if (resolving) return
            i = pending.removeFirstOrNull() ?: return
            resolving = true
        }
        try {
            m.resolveService(i, object : NsdManager.ResolveListener {
                override fun onResolveFailed(s: NsdServiceInfo?, e: Int) {
                    synchronized(pending) { resolving = false }
                    next(m, onHub)
                }

                override fun onServiceResolved(s: NsdServiceInfo) {
                    val h = s.host?.hostAddress
                    if (h != null) onHub(s.serviceName.removePrefix("CUPLAJ ").trim(), h, s.port)
                    synchronized(pending) { resolving = false }
                    next(m, onHub)
                }
            })
        } catch (_: Exception) {
            synchronized(pending) { resolving = false }
        }
    }

    fun stopDiscover() {
        try {
            disc?.let { mgr?.stopServiceDiscovery(it) }
        } catch (_: Exception) {
        }
        disc = null
        synchronized(pending) {
            pending.clear()
            resolving = false
        }
    }
}
