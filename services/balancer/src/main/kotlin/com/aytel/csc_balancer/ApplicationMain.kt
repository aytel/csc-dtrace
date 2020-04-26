package com.aytel.csc_balancer

import java.nio.file.Paths
import java.util.*

import com.aytel.csc_balancer.config.Properties
import com.aytel.csc_balancer.network.ListenThreadRunnable

object ApplicationMain {
    private val PROPERTY_PATH: String = System.getProperty(
        "properties",
        Paths.get(Paths.get("").toAbsolutePath().toString(), "balancer.properties").toString()
    )

    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val config = Properties(PROPERTY_PATH)

        val listenThread = Thread(ListenThreadRunnable(config))
        listenThread.start()
        listenThread.join()
    }
}
