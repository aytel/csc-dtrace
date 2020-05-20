package com.aytel.csc_balancer.config

import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import java.io.FileInputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths

class Properties(path: String) {
    val listenPort: Int
    val workerLoopGroup: EventLoopGroup
    val backends: MutableList<Backend> = mutableListOf()


    init {
        val config = java.util.Properties()
        if (Files.exists(Paths.get(path))) {
            try {
                val inputStream = FileInputStream(path)
                config.load(inputStream)
            } catch (e: IOException) {
                throw ExceptionInInitializerError(e)
            } catch (e: NullPointerException) {
                throw ExceptionInInitializerError(e)
            }
        } else {
            throw ExceptionInInitializerError()
        }
        listenPort = Integer.parseInt(config.getProperty("listen_port", "23179"))
        workerLoopGroup = NioEventLoopGroup(Runtime.getRuntime().availableProcessors())

        var i = 0
        while (true) {
            val addressKey = "backend.$i.address"
            val portKey = "backend.$i.port"
            val weightKey = "backend.$i.weight"

            val address: String? = config.getProperty(addressKey, null)
            val portString: String? = config.getProperty(portKey, null)
            val weightString: String? = config.getProperty(weightKey, null)

            if (address == null || portString == null || weightString == null) {
                break;
            }

            backends.add(Backend(address, portString.toInt(), weightString.toDouble()))

            i += 1
        }
    }
}