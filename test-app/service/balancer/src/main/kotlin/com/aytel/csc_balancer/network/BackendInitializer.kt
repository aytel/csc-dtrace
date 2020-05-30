package com.aytel.csc_balancer.network

import com.aytel.csc_balancer.config.Backend
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpClientCodec
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpServerCodec

class BackendInitializer(private val inboundCtx: ChannelHandlerContext, private val backend: Backend)
    : ChannelInitializer<SocketChannel>() {
    override fun initChannel(ch: SocketChannel) {
        val p = ch.pipeline()
        p.addLast(HttpClientCodec())
        p.addLast(HttpServerCodec())
        p.addLast(HttpObjectAggregator(MAX_SIZE))
        p.addLast(BackendHandler(inboundCtx.channel(), backend))
    }

    companion object {
        private const val MAX_SIZE = 1024 * 1024
    }
}