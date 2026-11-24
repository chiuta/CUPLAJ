package ro.trom.cuplaj

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Looper

/**
 * Wi‑Fi Direct: Regia creează un grup (adresă 192.168.49.1), restul telefoanelor se alătură fără router.
 * După alăturare totul funcționează ca pe o rețea locală: hubul e găsit prin baliză și TCP.
 */
@SuppressLint("MissingPermission")
class Wfd(private val c: Context) {
    private val mgr: WifiP2pManager? = try {
        c.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    } catch (_: Exception) {
        null
    }
    private var ch: WifiP2pManager.Channel? = null
    private var rx: BroadcastReceiver? = null
    private var hubGroup = false

    /** Colegii găsiți (nume, adresă MAC) și adresa hubului după alăturare. */
    var onPeer: ((String, String) -> Unit)? = null
    var onGroupOwner: ((String) -> Unit)? = null

    companion object {
        fun permissions(): Array<String> =
            if (Build.VERSION.SDK_INT >= 33) arrayOf(Manifest.permission.NEARBY_WIFI_DEVICES)
            else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

        fun hasPerm(c: Context): Boolean = permissions().all { c.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }

        fun available(c: Context): Boolean = c.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT)
    }

    private fun channel(): WifiP2pManager.Channel? {
        if (ch == null) ch = mgr?.initialize(c, Looper.getMainLooper(), null)
        return ch
    }

    /** Regia: creează grupul. */
    fun startGroup(done: (Boolean) -> Unit) {
        val m = mgr
        val k = channel()
        if (m == null || k == null || !hasPerm(c)) return done(false)
        hubGroup = true
        m.removeGroup(k, object : WifiP2pManager.ActionListener {
            override fun onSuccess() = create(m, k, done)
            override fun onFailure(r: Int) = create(m, k, done)
        })
    }

    private fun create(m: WifiP2pManager, k: WifiP2pManager.Channel, done: (Boolean) -> Unit) {
        m.createGroup(k, object : WifiP2pManager.ActionListener {
            override fun onSuccess() = done(true)
            override fun onFailure(r: Int) = done(false)
        })
    }

    /** Client: caută grupuri/telefoane Wi‑Fi Direct și raportează grupul când e alăturat. */
    fun scan(): Boolean {
        val m = mgr
        val k = channel()
        if (m == null || k == null || !hasPerm(c)) return false
        val r = object : BroadcastReceiver() {
            override fun onReceive(context: Context, i: Intent) {
                when (i.action) {
                    WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> m.requestPeers(k) { list ->
                        for (d: WifiP2pDevice in list.deviceList) onPeer?.invoke(d.deviceName.ifEmpty { d.deviceAddress }, d.deviceAddress)
                    }
                    WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> m.requestConnectionInfo(k) { info: WifiP2pInfo? ->
                        if (info != null && info.groupFormed && !info.isGroupOwner) {
                            info.groupOwnerAddress?.hostAddress?.let { onGroupOwner?.invoke(it) }
                        }
                    }
                }
            }
        }
        rx = r
        val f = IntentFilter()
        f.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        f.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        try {
            c.registerReceiver(r, f)
        } catch (_: Exception) {
            return false
        }
        m.discoverPeers(k, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {}
            override fun onFailure(r: Int) {}
        })
        return true
    }

    /** Client: cere alăturarea la telefonul Regiei (proprietarul grupului poate primi o invitație de confirmat). */
    fun join(mac: String, done: (Boolean) -> Unit) {
        val m = mgr
        val k = channel()
        if (m == null || k == null) return done(false)
        val cfg = WifiP2pConfig()
        cfg.deviceAddress = mac
        cfg.wps.setup = WpsInfo.PBC
        cfg.groupOwnerIntent = 0
        m.connect(k, cfg, object : WifiP2pManager.ActionListener {
            override fun onSuccess() = done(true)
            override fun onFailure(r: Int) = done(false)
        })
    }

    fun stop() {
        try {
            rx?.let { c.unregisterReceiver(it) }
        } catch (_: Exception) {
        }
        rx = null
        val m = mgr
        val k = ch
        if (m != null && k != null) {
            try {
                m.stopPeerDiscovery(k, null)
                if (hubGroup) m.removeGroup(k, null)
            } catch (_: Exception) {
            }
        }
        hubGroup = false
    }
}
