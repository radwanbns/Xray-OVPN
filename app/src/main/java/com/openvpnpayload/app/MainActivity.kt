package com.openvpnpayload.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var etHost: EditText
    private lateinit var etPort: EditText
    private lateinit var etUserAgent: EditText
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var tvStatus: TextView

    private val REQUEST_VPN = 1001
    private val REQUEST_NOTIFICATION = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etHost = findViewById(R.id.etHost)
        etPort = findViewById(R.id.etPort)
        etUserAgent = findViewById(R.id.etUserAgent)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        tvStatus = findViewById(R.id.tvStatus)

        // قيم افتراضية
        etHost.setText("example.com")
        etPort.setText("80")
        etUserAgent.setText("Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36")

        btnStart.setOnClickListener { prepareAndStart() }
        btnStop.setOnClickListener { stopService() }

        requestNotificationPermission()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIFICATION
                )
            }
        }
    }

    private fun prepareAndStart() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            startActivityForResult(intent, REQUEST_VPN)
        } else {
            startProxyService()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_VPN && resultCode == RESULT_OK) {
            startProxyService()
        } else if (requestCode == REQUEST_VPN) {
            Toast.makeText(this, "خاص الإذن ديال VPN", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startProxyService() {
        val host = etHost.text.toString().trim()
        val portStr = etPort.text.toString().trim()
        val ua = etUserAgent.text.toString().trim()

        if (host.isEmpty()) {
            Toast.makeText(this, "كتب الـ Host", Toast.LENGTH_SHORT).show()
            return
        }

        val port = portStr.toIntOrNull() ?: 80

        val intent = Intent(this, OpenVpnService::class.java).apply {
            action = OpenVpnService.ACTION_START
            putExtra(OpenVpnService.EXTRA_HOST, host)
            putExtra(OpenVpnService.EXTRA_PORT, port)
            putExtra(OpenVpnService.EXTRA_UA, ua.ifEmpty { "Mozilla/5.0" })
        }

        ContextCompat.startForegroundService(this, intent)
        tvStatus.text = "الحالة: شغال (Proxy على 127.0.0.1:1080)"
        Toast.makeText(this, "تم تشغيل الـ Proxy", Toast.LENGTH_SHORT).show()
    }

    private fun stopService() {
        val intent = Intent(this, OpenVpnService::class.java).apply {
            action = OpenVpnService.ACTION_STOP
        }
        startService(intent)
        tvStatus.text = "الحالة: موقف"
        Toast.makeText(this, "تم الإيقاف", Toast.LENGTH_SHORT).show()
    }
}
