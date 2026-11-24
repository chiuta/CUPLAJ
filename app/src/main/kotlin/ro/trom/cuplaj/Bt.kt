package ro.trom.cuplaj

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import java.util.UUID

/**
 * Bluetooth clasic (RFCOMM): același protocol ca pe TCP, peste un canal Bluetooth.
 * Fără asociere obligatorie (canal „insecure”): accesul rămâne protejat de PIN, codul camerei și cheia de rețea.
 */
@SuppressLint("MissingPermission")
object Bt {
    val SERVICE: UUID = UUID.fromString("7c6a0c5e-3d10-4f6e-9a55-2b1c8f0d4a71")

    @Volatile
    lateinit var ctx: Context

    fun permissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= 31) arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
        else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

    fun hasPerm(c: Context): Boolean = permissions().all { c.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }

    fun adapter(c: Context): BluetoothAdapter? = try {
        (c.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    } catch (_: Exception) {
        null
    }

    fun available(c: Context): Boolean = c.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH) && adapter(c) != null

    /** Client: deschide canalul către hubul de la adresa MAC dată. */
    fun connect(mac: String): Chan {
        val ad = adapter(ctx) ?: throw java.io.IOException("no bluetooth")
        if (!ad.isEnabled) throw java.io.IOException("bluetooth off")
        try {
            ad.cancelDiscovery()
        } catch (_: Exception) {
        }
        val dev = ad.getRemoteDevice(mac.uppercase())
        val sock = dev.createInsecureRfcommSocketToServiceRecord(SERVICE)
        try {
            sock.connect()
        } catch (e: Exception) {
            try {
                sock.close()
            } catch (_: Exception) {
            }
            throw e
        }
        return StreamChan(sock.inputStream, sock.outputStream, { try { sock.close() } catch (_: Exception) {} })
    }

    /** Hub: primește conexiuni Bluetooth și le dă hubului. */
    class Server(private val c: Context, private val hub: Hub) {
        @Volatile
        private var run = false
        private var ss: BluetoothServerSocket? = null

        fun start(): Boolean {
            val ad = adapter(c) ?: return false
            if (!ad.isEnabled || !hasPerm(c)) return false
            run = true
            try {
                ss = ad.listenUsingInsecureRfcommWithServiceRecord("CUPLAJ", SERVICE)
            } catch (_: Exception) {
                run = false
                return false
            }
            Thread {
                while (run) {
                    try {
                        val s = ss?.accept() ?: break
                        hub.adopt(StreamChan(s.inputStream, s.outputStream, { try { s.close() } catch (_: Exception) {} }))
                    } catch (_: Exception) {
                        if (!run) break
                        try {
                            Thread.sleep(300)
                        } catch (_: Exception) {
                        }
                    }
                }
            }.apply {
                isDaemon = true
                start()
            }
            return true
        }

        fun stop() {
            run = false
            try {
                ss?.close()
            } catch (_: Exception) {
            }
            ss = null
        }
    }

    /** Cere ca telefonul să fie vizibil prin Bluetooth 5 minute (ecranul Regiei). */
    fun discoverableIntent(): Intent =
        Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)

    /** Client: caută telefoane apropiate care rulează hub CUPLAJ (se verifică prin SDP) și le raportează. */
    class Scanner(private val c: Context, private val onDev: (name: String, mac: String) -> Unit) {
        private var rx: BroadcastReceiver? = null
        private val asked = HashSet<String>()

        private fun check(d: BluetoothDevice) {
            val has = try {
                d.uuids?.any { it.uuid == SERVICE }
            } catch (_: Exception) {
                null
            }
            if (has == true) {
                onDev(d.name ?: d.address, d.address)
            } else if (has == null || !asked.contains(d.address)) {
                asked.add(d.address)
                try {
                    d.fetchUuidsWithSdp()
                } catch (_: Exception) {
                }
            }
        }

        @Suppress("DEPRECATION")
        private fun dev(i: Intent): BluetoothDevice? =
            if (Build.VERSION.SDK_INT >= 33) i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            else i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

        @Suppress("DEPRECATION")
        fun start(): Boolean {
            val ad = adapter(c) ?: return false
            if (!ad.isEnabled || !hasPerm(c)) return false
            try {
                for (d in ad.bondedDevices) check(d)
            } catch (_: Exception) {
            }
            val r = object : BroadcastReceiver() {
                override fun onReceive(context: Context, i: Intent) {
                    val d = dev(i) ?: return
                    when (i.action) {
                        BluetoothDevice.ACTION_FOUND -> check(d)
                        BluetoothDevice.ACTION_UUID -> {
                            val arr = i.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID)
                            if (arr != null && arr.any { (it as? android.os.ParcelUuid)?.uuid == SERVICE }) onDev(d.name ?: d.address, d.address)
                        }
                    }
                }
            }
            rx = r
            try {
                val f = IntentFilter(BluetoothDevice.ACTION_FOUND)
                f.addAction(BluetoothDevice.ACTION_UUID)
                c.registerReceiver(r, f)
                ad.startDiscovery()
            } catch (_: Exception) {
                return false
            }
            return true
        }

        fun stop() {
            try {
                rx?.let { c.unregisterReceiver(it) }
            } catch (_: Exception) {
            }
            rx = null
            try {
                adapter(c)?.cancelDiscovery()
            } catch (_: Exception) {
            }
        }
    }
}
