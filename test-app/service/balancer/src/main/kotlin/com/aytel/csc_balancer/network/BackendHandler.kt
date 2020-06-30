package com.aytel.csc_balancer.network

import com.aytel.csc_balancer.config.Backend
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.http.FullHttpResponse
import java.net.InetSocketAddress
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.ConsoleHandler
import java.util.logging.LogRecord
import java.util.logging.Logger
import java.util.logging.SimpleFormatter

class BackendHandler(private val channel: Channel, private val backend: Backend): ChannelInboundHandlerAdapter() {
    private var lastRid: String = "-"

    companion object {
        val logger: Logger = Logger.getLogger(BackendHandler::class.qualifiedName)

    }

    override fun channelActive(ctx: ChannelHandlerContext) {
        ctx.read()
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg is FullHttpResponse) {
            val requestId = msg.headers()["X-Request-Id"] ?: "-"
            lastRid = requestId;
            val server = (ctx.channel().remoteAddress() as InetSocketAddress).address.hostAddress
            val client = (channel.remoteAddress() as InetSocketAddress).address.hostAddress
            val status = msg.status().code().toString()
            channel.writeAndFlush(msg).addListener{ future ->
                if (future.isSuccess) {
                    logger.info("$requestId - - $client $server $status")
                    ctx.channel().read()
                } else {
                    logger.info("$requestId - - $client $server $status")
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
        val server = (ctx.channel().remoteAddress() as InetSocketAddress).address.hostAddress
        val client = (channel.remoteAddress() as InetSocketAddress).address.hostAddress
        ListenThreadHandler.logger.warning("$lastRid - - $client $server EXC")
        ListenThreadHandler.flushAndClose(channel)
    }

}
