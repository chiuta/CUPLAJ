package ro.trom.cuplaj

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.aware.AttachCallback
import android.net.wifi.aware.DiscoverySessionCallback
import android.net.wifi.aware.PeerHandle
import android.net.wifi.aware.PublishConfig
import android.net.wifi.aware.PublishDiscoverySession
import android.net.wifi.aware.SubscribeConfig
import android.net.wifi.aware.SubscribeDiscoverySession
import android.net.wifi.aware.WifiAwareManager
import android.net.wifi.aware.WifiAwareNetworkInfo
import android.net.wifi.aware.WifiAwareNetworkSpecifier
import android.net.wifi.aware.WifiAwareSession
import android.os.Build
import android.os.Handler
import android.os.Looper
import java.net.Inet6Address
import java.net.ServerSocket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Wi‑Fi Aware (NAN): telefoanele compatibile se găsesc și se leagă direct, fără router și fără invitație.
 * Disponibil doar pe unele telefoane (Android 10+ cu suport hardware); orice eroare e ignorată, celelalte legături rămân.
 */
@SuppressLint("MissingPermission", "NewApi")
object Aware {
    private const val SERVICE = "cuplaj"
    private const val PASS = "CUPLAJ-aware-1" // rețeaua Aware e doar un transport; accesul îl păzesc PIN-ul, camera și cheia

    fun available(c: Context): Boolean =
        Build.VERSION.SDK_INT >= 29 && c.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_AWARE)

    fun hasPerm(c: Context): Boolean = Wfd.hasPerm(c)

    private fun mgr(c: Context): WifiAwareManager? = try {
        c.getSystemService(Context.WIFI_AWARE_SERVICE) as? WifiAwareManager
    } catch (_: Exception) {
        null
    }

    private val main = Handler(Looper.getMainLooper())

    // ------------------------------------------------------------------ hub (publicare)
    class Hub(private val c: Context, private val hub: ro.trom.cuplaj.Hub) {
        private var session: WifiAwareSession? = null
        private var pub: PublishDiscoverySession? = null
        private var ss: ServerSocket? = null

        @Volatile
        private var run = false
        private val cbs = ArrayList<ConnectivityManager.NetworkCallback>()

        fun start(): Boolean {
            val m = mgr(c) ?: return false
            if (!available(c) || !hasPerm(c) || !m.isAvailable) return false
            run = true
            try {
                val server = ServerSocket(0)
                ss = server
                Thread {
                    while (run) {
                        try {
                            hub.adopt(SocketChan(server.accept()))
                        } catch (_: Exception) {
                            if (!run) break
                        }
                    }
                }.apply { isDaemon = true; start() }
                m.attach(object : AttachCallback() {
                    override fun onAttached(s: WifiAwareSession) {
                        session = s
                        s.publish(
                            PublishConfig.Builder().setServiceName(SERVICE).build(),
                            object : DiscoverySessionCallback() {
                                override fun onPublishStarted(p: PublishDiscoverySession) {
                                    pub = p
                                }

                                override fun onMessageReceived(peer: PeerHandle, message: ByteArray) {
                                    accept(peer)
                                }
                            },
                            main
                        )
                    }
                }, main)
            } catch (_: Exception) {
                stop()
                return false
            }
            return true
        }

        private fun accept(peer: PeerHandle) {
            val p = pub ?: return
            val server = ss ?: return
            try {
                val spec = WifiAwareNetworkSpecifier.Builder(p, peer).setPskPassphrase(PASS).setPort(server.localPort).build()
                val req = NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI_AWARE).setNetworkSpecifier(spec).build()
                val cb = object : ConnectivityManager.NetworkCallback() {}
                synchronized(cbs) { cbs.add(cb) }
                (c.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).requestNetwork(req, cb)
            } catch (_: Exception) {
            }
        }

        fun stop() {
            run = false
            try {
                ss?.close()
            } catch (_: Exception) {
            }
            val cm = c.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            synchronized(cbs) {
                for (cb in cbs) try { cm.unregisterNetworkCallback(cb) } catch (_: Exception) {}
                cbs.clear()
            }
            try {
                pub?.close()
                session?.close()
            } catch (_: Exception) {
            }
            pub = null
            session = null
        }
    }

    // ------------------------------------------------------------------ client (abonare)
    private var cSession: WifiAwareSession? = null
    private var sub: SubscribeDiscoverySession? = null
    private val peers = HashMap<String, PeerHandle>()
    private var netCb: ConnectivityManager.NetworkCallback? = null

    /** Caută hub‑uri Aware; `onHub` primește o cheie internă (aw:N) și un nume. */
    fun scan(c: Context, onHub: (key: String, name: String) -> Unit): Boolean {
        val m = mgr(c) ?: return false
        if (!available(c) || !hasPerm(c) || !m.isAvailable) return false
        try {
            m.attach(object : AttachCallback() {
                override fun onAttached(s: WifiAwareSession) {
                    cSession = s
                    s.subscribe(
                        SubscribeConfig.Builder().setServiceName(SERVICE).build(),
                        object : DiscoverySessionCallback() {
                            override fun onSubscribeStarted(d: SubscribeDiscoverySession) {
                                sub = d
                            }

                            override fun onServiceDiscovered(peer: PeerHandle, info: ByteArray?, filter: List<ByteArray>?) {
                                val k = "aw:" + (peers.size + 1)
                                synchronized(peers) { peers[k] = peer }
                                onHub(k, "Wi‑Fi Aware")
                            }
                        },
                        main
                    )
                }
            }, main)
        } catch (_: Exception) {
            return false
        }
        return true
    }

    /** Folosit de Net: cere rețeaua Aware către hub și deschide un canal TCP prin ea (blochează până la 15 s). */
    fun connect(c: Context, key: String): Chan {
        val peer = synchronized(peers) { peers[key] } ?: throw java.io.IOException("aware peer")
        val d = sub ?: throw java.io.IOException("aware session")
        val cm = c.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        d.sendMessage(peer, 1, "hi".toByteArray())
        val spec = WifiAwareNetworkSpecifier.Builder(d, peer).setPskPassphrase(PASS).build()
        val req = NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI_AWARE).setNetworkSpecifier(spec).build()
        val latch = CountDownLatch(1)
        var net: Network? = null
        var addr: Inet6Address? = null
        var port = 0
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(n: Network, caps: NetworkCapabilities) {
                val info = caps.transportInfo as? WifiAwareNetworkInfo ?: return
                if (info.peerIpv6Addr != null && info.port > 0) {
                    net = n
                    addr = info.peerIpv6Addr
                    port = info.port
                    latch.countDown()
                }
            }
        }
        netCb?.let { try { cm.unregisterNetworkCallback(it) } catch (_: Exception) {} }
        netCb = cb
        cm.requestNetwork(req, cb)
        if (!latch.await(15, TimeUnit.SECONDS)) throw java.io.IOException("aware timeout")
        val s = net!!.socketFactory.createSocket(addr, port)
        return SocketChan(s)
    }

    fun stopScan() {
        try {
            sub?.close()
            cSession?.close()
        } catch (_: Exception) {
        }
        sub = null
        cSession = null
        synchronized(peers) { peers.clear() }
    }
}
