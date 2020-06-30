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
    private var lastRid: String = "-"

    companion object {
        @JvmStatic
        fun flushAndClose(ch: Channel) {
            if (ch.isActive) {
                ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
            }
        }

        val logger: Logger

        const val logFormat = "%s %s %s %s %s %s %s %s %s\n"
        val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")

        init {
            val baseLogger = Logger.getLogger("com.aytel.csc_balancer")
            baseLogger.useParentHandlers = false
            val handler = ConsoleHandler()
            handler.formatter = object: SimpleFormatter() {
                override fun format(record: LogRecord): String {
                    val data = record.message.split(" ")
                    val requestId = data[0]
                    val spanId = data[1]
                    val parId = data[2]
                    val src = data[3]
                    val dst = data[4]
                    val result = data[5]
                    val self = System.getenv("DCKR_NAME")
                    return logFormat.format(requestId, spanId, parId, timestampFormat.format(Date()),
                        record.level.localizedName, src, dst, self, result)
                }
            }
            baseLogger.addHandler(handler)

            logger = Logger.getLogger(ListenThreadHandler::class.qualifiedName)
        }
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        val request: FullHttpRequest = if (msg is FullHttpRequest) {
            msg
        } else {
            super.channelRead(ctx, msg)
            return
        }
        val spanId = UUID.randomUUID().toString()
        val parId = request.headers()["X-Span-Id"] ?: "-"
        request.headers()["X-Span-Id"] = spanId
        request.headers()["X-Par-Id"] = parId
        sendMessage(ctx, request, i)
    }

    private fun sendMessage(ctx: ChannelHandlerContext, msg: FullHttpRequest, i: Int, retries: Int = 3) {
        val requestId = msg.headers()["X-Request-Id"] ?: "-"
        lastRid = requestId
        val spanId = msg.headers()["X-Span-Id"]
        val parId = msg.headers()["X-Par-Id"]
        val src = (ctx.channel().remoteAddress() as InetSocketAddress).address.hostAddress
        val dst = config.backends[i].address
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
                    logger.info("$requestId $spanId $parId $src $dst SUBM")
                    ctx.channel().read()
                } else {
                    logger.warning("$requestId $spanId $parId $src $dst FAIL")
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
            val src = (ctx.channel().remoteAddress() as InetSocketAddress).address.hostAddress
            val dst = config.backends[i].address

            val listener = ChannelFutureListener { future ->
                if (future.isSuccess) {
                    logger.info("$lastRid - - $src $dst CONN")
                    inboundChannel.read()
                } else {
                    logger.warning("$lastRid - - $src $dst REF")
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
        val src = (ctx.channel().remoteAddress() as InetSocketAddress).address.hostAddress
        logger.warning("$lastRid - - $src - EXC")
        flushAndClose(ctx.channel())
    }
}
