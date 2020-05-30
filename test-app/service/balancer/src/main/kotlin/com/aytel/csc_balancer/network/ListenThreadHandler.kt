package com.aytel.csc_balancer.network

import com.aytel.csc_balancer.ApplicationMain
import com.aytel.csc_balancer.config.Properties
import io.netty.bootstrap.Bootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.*
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http.FullHttpRequest
import java.net.InetSocketAddress
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.ConsoleHandler
import java.util.logging.LogRecord
import java.util.logging.Logger
import java.util.logging.SimpleFormatter
import kotlin.random.Random

class ListenThreadHandler(private val config: Properties) : ChannelInboundHandlerAdapter() {
    private val random = Random.Default
    private val channels: Array<Channel?> = Array(config.backends.size) {null}
    private var i: Int = 0

    companion object {
        @JvmStatic
        fun flushAndClose(ch: Channel) {
            if (ch.isActive) {
                ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
            }
        }

        lateinit var logger: Logger

        const val logFormat = "%s %s %s %s %s %s\n"
        val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
    }

    init {
        val baseLogger = Logger.getLogger("com.aytel.csc_balancer")
        baseLogger.useParentHandlers = false
        val handler = ConsoleHandler()
        handler.formatter = object: SimpleFormatter() {
            override fun format(record: LogRecord): String {
                val data = record.message.split(" ")
                val requestId = data[0]
                val client = data[1]
                val server = data[2]
                val result = data[3]
                return logFormat.format(requestId, timestampFormat.format(Date()),
                    record.level.localizedName, client, server, result)
            }
        }
        baseLogger.addHandler(handler)

        logger = Logger.getLogger(ListenThreadHandler::class.qualifiedName)
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        val request: FullHttpRequest = if (msg is FullHttpRequest) {
            msg
        } else {
            super.channelRead(ctx, msg)
            return
        }
        sendMessage(ctx, request, i)
    }

    private fun sendMessage(ctx: ChannelHandlerContext, msg: FullHttpRequest, i: Int, retries: Int = 3) {
        val requestId = msg.headers()["X-Request-Id"] ?: "null"
        val client = (ctx.channel().remoteAddress() as InetSocketAddress).address.hostAddress
        val server = config.backends[i].address
        if (channels[i]?.isActive == false) {
            if (retries == 0) {
                flushAndClose(ctx.channel())
            } else {
                activateChannels(ctx, arrayListOf(i))
                sendMessage(ctx, msg, i, retries - 1)
            }
        } else {
            channels[i]?.writeAndFlush(msg)?.addListener { future ->
                if (future.isSuccess) {
                    logger.info("$requestId $client $server OK")
                    ctx.channel().read()
                } else {
                    logger.warning("$requestId $client $server FAIL")
                    channels[i]?.close()
                }
            }
        }
    }

    private fun activateChannels(ctx: ChannelHandlerContext, inds: Collection<Int> = (0 until channels.size).toList()) {
        for (i in inds) {
            val inboundChannel = ctx.channel()
            val b = Bootstrap()

            val factory: ChannelFactory<NioSocketChannel> = object: ChannelFactory<NioSocketChannel> {
                override fun newChannel(): NioSocketChannel {
                    return NioSocketChannel()
                }
            }

            b.group(inboundChannel.eventLoop())
                .channelFactory(factory)
                .handler(BackendInitializer(ctx, config.backends[i]))
                .option(ChannelOption.AUTO_READ, false)
            val f = b.connect(
                config.backends[i].address,
                config.backends[i].port
            )
            channels[i] = f.channel()

            val requestId = "null"
            val client = (ctx.channel().remoteAddress() as InetSocketAddress).address.hostAddress
            val server = config.backends[i].address

            val listener = ChannelFutureListener { future ->
                if (future.isSuccess) {
                    logger.info("$requestId $client $server CONN")
                    inboundChannel.read()
                } else {
                    logger.warning("$requestId $client $server REF")
                    future.channel().close()
                    flushAndClose(ctx.channel())
                }
            }

            f.addListener(listener)
        }
    }

    override fun channelActive(ctx: ChannelHandlerContext) {
        val sumWeight = config.backends.map { backend -> backend.weight }.sum()
        val randomValue = random.nextDouble(sumWeight)
        i = 0
        var acc = 0.0
        while (acc + config.backends[i].weight < randomValue) {
            acc += config.backends[i].weight
            i++
        }

        activateChannels(ctx, listOf(i))
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        for (channel in channels) {
            if (channel != null) {
                flushAndClose(channel)
            }
        }
    }

    override fun channelReadComplete(ctx: ChannelHandlerContext) {
        ctx.flush()
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        val requestId = "null"
        val client = (ctx.channel().remoteAddress() as InetSocketAddress).address.hostAddress
        val server = config.backends[i].address + ":" + config.backends[i].port
        logger.warning("$requestId $client $server EXC")
        System.err.println(cause.message)
        flushAndClose(ctx.channel())
    }
}
