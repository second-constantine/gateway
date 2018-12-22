package io.secondconstantine.gateway

import org.apache.logging.log4j.LogManager
import java.io.DataInputStream
import java.io.IOException
import java.io.OutputStream

class GatewayThread(
        private val inputClient: DataInputStream,
        private val outputHost: OutputStream,
        private val callback: Runnable,
        private var life: Boolean = true
) : Thread() {

    override fun run() {
        while (life) {
            val bytes = ByteArray(1000)
            try {
                val bytesRead = inputClient.read(bytes)
                if (bytesRead == -1) {
                    break
                }
                if (bytesRead > 0) {
                    outputHost.write(bytes, 0, bytesRead)
                }
            } catch (e: IOException) {
                life = false
                log.error(e)
            }
        }
        callback.run()
    }

    companion object {
        private val log = LogManager.getLogger()
        const val TIMEOUT = 1000
    }
}