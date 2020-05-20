package com.aytel.csc_balancer.network

import com.aytel.csc_balancer.config.Backend
import io.netty.channel.*
import io.netty.handler.codec.http.HttpObject

class BackendHandler(private val channel: Channel, private val backend: Backend): ChannelInboundHandlerAdapter() {
    override fun channelActive(ctx: ChannelHandlerContext) {
        ctx.read()
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg is HttpObject) {
            channel.writeAndFlush(msg).addListener{ future ->
                if (future.isSuccess) {
                    ctx.channel().read()
                } else {
                    channel.close()
                    ctx.channel().close()
                }
            }
        } else {
            super.channelRead(ctx, msg)
        }
    }

    override fun channelInactive(ctx: ChannelHandlerContext?) {
        ListenThreadHandler.flushAndClose(channel)
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        ListenThreadHandler.flushAndClose(channel)
    }

}
