package com.aytel.csc_balancer

import java.nio.file.Paths
import java.util.*

import com.aytel.csc_balancer.config.Properties
import com.aytel.csc_balancer.network.ListenThreadHandler
import com.aytel.csc_balancer.network.ListenThreadRunnable
import java.text.SimpleDateFormat
import java.util.logging.ConsoleHandler
import java.util.logging.LogRecord
import java.util.logging.Logger
import java.util.logging.SimpleFormatter

object ApplicationMain {
    private val PROPERTY_PATH: String = System.getProperty(
        "properties",
        Paths.get(Paths.get("").toAbsolutePath().toString(), "balancer.properties").toString()
    )

    const val logFormat = "%s, %s, %s, %s, %s, %s\n"
    val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")

    init {

    }

    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val config = Properties(PROPERTY_PATH)

        val listenThread = Thread(ListenThreadRunnable(config))
        listenThread.start()
        listenThread.join()
    }
}
