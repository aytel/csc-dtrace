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
    companion object {
        val logger: Logger

        const val logFormat = "%s %s %s %s %s %s\n"
        val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")

        init {
            /*val baseLogger = Logger.getLogger("com.aytel.csc_balancer")
            baseLogger.useParentHandlers = false
            val handler = ConsoleHandler()
            handler.formatter = object : SimpleFormatter() {
                override fun format(record: LogRecord): String {
                    val data = record.message.split(" ")
                    val requestId = data[0]
                    val client = data[1]
                    val server = data[2]
                    val result = data[3]
                    return logFormat.format(
                        requestId, timestampFormat.format(Date()),
                        record.level.localizedName, client, server, result
                    )
                }
            }
            baseLogger.addHandler(handler)*/

            logger = Logger.getLogger(BackendHandler::class.qualifiedName)
        }
    }

    override fun channelActive(ctx: ChannelHandlerContext) {
        ctx.read()
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg is FullHttpResponse) {
            val requestId = msg.headers()["X-Request-Id"] ?: "null"
            val server = (ctx.channel().remoteAddress() as InetSocketAddress).address.hostAddress
            val client = (channel.remoteAddress() as InetSocketAddress).address.hostAddress
            val status = msg.status().code().toString()
            channel.writeAndFlush(msg).addListener{ future ->
                if (future.isSuccess) {
                    logger.info("$requestId $client $server $status")
                    ctx.channel().read()
                } else {
                    logger.info("$requestId $client $server $status")
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
        val requestId = "null"
        val server = (ctx.channel().remoteAddress() as InetSocketAddress).address.hostAddress
        val client = (channel.remoteAddress() as InetSocketAddress).address.hostAddress
        ListenThreadHandler.logger.warning("$requestId $client $server EXC")
        //System.err.println(cause.message)
        ListenThreadHandler.flushAndClose(channel)
    }

}
