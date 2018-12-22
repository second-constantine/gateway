package io.secondconstantine.gateway

import org.apache.logging.log4j.LogManager
import java.io.DataInputStream
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket

private val log = LogManager.getLogger()

fun main(args: Array<String>) {
    if (args.size < 2) {
        log.fatal("""
            Example:
                for server [input_port] [output_port]:
                    ./gateway.jar 1222 2122
                for client [gateway_server_host] [gateway_server_port] [target_url] [target_port]:
                    ./gateway.jar myserver.com 1222 localhost 22
            """)
        return
    }

    if (args.size < 4) {
        val clientPort = Integer.parseInt(args[0])
        val ownerPort = Integer.parseInt(args[1])
        runServer(clientPort, ownerPort)
    } else {
        val serverUrl = args[0]
        val serverPort = Integer.parseInt(args[1])
        val targetUrl = args[2]
        val targetPort = Integer.parseInt(args[3])
        do {
            runClient(serverUrl, serverPort, targetUrl, targetPort)
            Thread.sleep(GatewayThread.TIMEOUT.toLong())
        } while (true)
    }

}

private fun runClient(serverUrl: String, serverPort: Int, targetUrl: String, targetPort: Int) {
    try {
        log.info("Server: $serverUrl:$serverPort")
        log.info("Target: $targetUrl:$targetPort")
        Socket(serverUrl, serverPort).use { server ->
            val osServer = server.getOutputStream()
            val isServer = DataInputStream(server.getInputStream())
            Socket(targetUrl, targetPort).use { target ->
                val osHost = target.getOutputStream()
                val isHost = DataInputStream(target.getInputStream())
                var isConnected = true
                val clientThread = GatewayThread(
                        inputClient = isServer,
                        outputHost = osHost,
                        callback = Runnable {
                            isConnected = false
                        }
                )
                clientThread.start()
                val hostThread = GatewayThread(
                        inputClient = isHost,
                        outputHost = osServer,
                        callback = Runnable {
                            isConnected = false
                        })
                hostThread.start()
                log.info("connect to $target")
                while (isConnected) {
                    Thread.sleep(GatewayThread.TIMEOUT.toLong())
                }
                log.warn("reconnect!!!!")
            }
        }
    } catch (ioe: IOException) {
        log.error(ioe.localizedMessage, ioe)
    }
}

private fun runServer(clientPort: Int, ownerPort: Int) {
    log.info("Autodetect server mode. Traffic redirection $clientPort -> $ownerPort (ports)")
    var life = true
    while (life) {
        try {
            ServerSocket(clientPort).use { clientServerSocket ->
                val client = clientServerSocket.accept()
                val osClient = client.getOutputStream()
                val isClient = DataInputStream(client.getInputStream())
                ServerSocket(ownerPort).use { ownerServerSocket ->
                    val owner = ownerServerSocket.accept()
                    val osOwner = owner.getOutputStream()
                    val isOwner = DataInputStream(owner.getInputStream())
                    var isConnected = true
                    val clientThread = GatewayThread(
                            inputClient = isClient,
                            outputHost = osOwner,
                            callback = Runnable { isConnected = false }
                    )
                    clientThread.start()
                    val hostThread = GatewayThread(
                            inputClient = isOwner,
                            outputHost = osClient,
                            callback = Runnable { isConnected = false }
                    )
                    hostThread.start()
                    log.info("client connect to $owner")
                    while (isConnected) {
                        Thread.sleep(GatewayThread.TIMEOUT.toLong())
                    }
                    log.warn("reconnect!!!!")
                }
            }
        } catch (ioe: IOException) {
            log.error(ioe.localizedMessage)
            life = false
        }
    }
}