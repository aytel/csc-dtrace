package com.aytel.csc_balancer.network

import com.aytel.csc_balancer.config.Properties
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import java.util.logging.Logger

class ListenThreadRunnable(private val config: Properties): Runnable {
    override fun run() {
        val listenGroup = NioEventLoopGroup(1)

        val factory: ChannelFactory<NioServerSocketChannel> = object: ChannelFactory<NioServerSocketChannel> {
            override fun newChannel(): NioServerSocketChannel {
                return NioServerSocketChannel()
            }
        }

        try {
            val b = ServerBootstrap()
            b.group(listenGroup, config.workerLoopGroup)
                .channelFactory(factory)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.AUTO_READ, false)
                .childHandler(ListenHandlerInitializer(config))
            val f = b.bind("0.0.0.0", config.listenPort).sync()

            System.err.println("Ready to read.")

            f.channel().closeFuture().sync()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            listenGroup.shutdownGracefully()
        }

    }
}