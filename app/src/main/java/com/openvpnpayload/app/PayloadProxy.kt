package com.openvpnpayload.app

import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Local HTTP Proxy اللي كيحقن الـ Payload قبل الاتصال.
 * الـ Payload بنفس الشكل اللي طلبتي:
 *
 * GET / HTTP/1.1[crlf]
 * Host: [host][crlf]
 * Upgrade: Websocket[crlf]
 * Connection: Keep-Alive[crlf]
 * User-Agent: [ua][crlf]
 * Upgrade: websocket[crlf]
 * [crlf]
 */
class PayloadProxy(
    private val listenPort: Int = 1080,
    private val targetHost: String,
    private val targetPort: Int,
    private val userAgent: String = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36"
) {

    private val TAG = "PayloadProxy"
    private var serverSocket: ServerSocket? = null
    private val isRunning = AtomicBoolean(false)
    private val executor = Executors.newCachedThreadPool()

    fun start() {
        if (isRunning.get()) return

        executor.execute {
            try {
                serverSocket = ServerSocket(listenPort)
                isRunning.set(true)
                Log.i(TAG, "Proxy started on port $listenPort → $targetHost:$targetPort")

                while (isRunning.get()) {
                    try {
                        val client = serverSocket?.accept() ?: break
                        executor.execute { handleClient(client) }
                    } catch (e: IOException) {
                        if (isRunning.get()) {
                            Log.e(TAG, "Accept error: ${e.message}")
                        }
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Failed to start proxy: ${e.message}")
            }
        }
    }

    fun stop() {
        isRunning.set(false)
        try {
            serverSocket?.close()
        } catch (_: IOException) {}
        serverSocket = null
        Log.i(TAG, "Proxy stopped")
    }

    private fun handleClient(client: Socket) {
        var remote: Socket? = null
        try {
            // نبني الـ Payload بنفس الشكل المطلوب
            val payload = buildPayload(targetHost, userAgent)

            remote = Socket(targetHost, targetPort)
            remote.soTimeout = 30000

            val remoteOut = remote.getOutputStream()
            val remoteIn = remote.getInputStream()
            val clientOut = client.getOutputStream()
            val clientIn = client.getInputStream()

            // نرسل الـ Payload أولاً
            remoteOut.write(payload.toByteArray(Charsets.UTF_8))
            remoteOut.flush()
            Log.d(TAG, "Payload injected to $targetHost:$targetPort")

            // بعد ما نحقنو الـ Payload، نبداو ننقلو الداتا فالاتجاهين
            val t1 = Thread { pipe(clientIn, remoteOut, "client→remote") }
            val t2 = Thread { pipe(remoteIn, clientOut, "remote→client") }

            t1.start()
            t2.start()

            t1.join()
            t2.join()

        } catch (e: Exception) {
            Log.e(TAG, "Client handling error: ${e.message}")
        } finally {
            try { client.close() } catch (_: Exception) {}
            try { remote?.close() } catch (_: Exception) {}
        }
    }

    private fun buildPayload(host: String, ua: String): String {
        // بنفس الشكل اللي عطيتيني بالضبط
        return "GET / HTTP/1.1\r\n" +
                "Host: $host\r\n" +
                "Upgrade: Websocket\r\n" +
                "Connection: Keep-Alive\r\n" +
                "User-Agent: $ua\r\n" +
                "Upgrade: websocket\r\n" +
                "\r\n"
    }

    private fun pipe(input: InputStream, output: OutputStream, direction: String) {
        val buffer = ByteArray(8192)
        try {
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                output.write(buffer, 0, read)
                output.flush()
            }
        } catch (e: Exception) {
            Log.d(TAG, "Pipe closed ($direction): ${e.message}")
        }
    }

    fun isRunning(): Boolean = isRunning.get()
}
