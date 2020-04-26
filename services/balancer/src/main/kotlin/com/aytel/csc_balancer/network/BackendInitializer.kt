package com.aytel.csc_balancer.network

import com.aytel.csc_balancer.config.Backend
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel

class BackendInitializer(private val inboundCtx: ChannelHandlerContext, private val backend: Backend)
    : ChannelInitializer<SocketChannel>() {
    override fun initChannel(ch: SocketChannel) {
        val p = ch.pipeline()
        p.addLast(BackendHandler(inboundCtx.channel(), backend))
    }
}