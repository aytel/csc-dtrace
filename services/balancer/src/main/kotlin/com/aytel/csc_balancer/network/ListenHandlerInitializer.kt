package com.aytel.csc_balancer.network

import com.aytel.csc_balancer.config.Properties
import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpServerCodec

class ListenHandlerInitializer(private val config: Properties) : ChannelInitializer<SocketChannel>() {

    @Throws(Exception::class)
    override fun initChannel(ch: SocketChannel) {
        val p = ch.pipeline()
        //p.addLast(HttpServerCodec())
        //p.addLast(HttpObjectAggregator(MAX_SIZE))
        p.addLast(ListenThreadHandler(config))
    }

    companion object {
        private const val MAX_SIZE = 1024 * 1024
    }
}
