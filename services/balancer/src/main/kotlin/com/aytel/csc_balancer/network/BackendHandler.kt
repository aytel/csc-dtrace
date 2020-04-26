package com.aytel.csc_balancer.network

import com.aytel.csc_balancer.config.Backend
import io.netty.channel.*

class BackendHandler(private val channel: Channel, private val backend: Backend): ChannelInboundHandlerAdapter() {
    override fun channelActive(ctx: ChannelHandlerContext) {
        ctx.read()
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        channel.writeAndFlush(msg).addListener ({future: ChannelFuture ->
            if (future.isSuccess) {
                ctx.channel().read()
            } else {
                future.channel().close()
            }
        } as ChannelFutureListener)
    }

    override fun channelInactive(ctx: ChannelHandlerContext?) {
        ListenThreadHandler.flushAndClose(channel)
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        ListenThreadHandler.flushAndClose(channel)
    }

}
