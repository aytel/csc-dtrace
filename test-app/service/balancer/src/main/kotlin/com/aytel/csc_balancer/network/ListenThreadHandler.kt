package com.aytel.csc_balancer.network

import com.aytel.csc_balancer.config.Properties
import io.netty.bootstrap.Bootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.*
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http.FullHttpRequest
import java.util.logging.Logger
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

        val logger: Logger = Logger.getLogger(ListenThreadHandler::class.simpleName)
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
                    ctx.channel().read()
                } else {
                    channels[i]?.close()
                    logger.warning("Couldn't send message:\n" + future.cause().message)
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

            val listener: ChannelFutureListener = ChannelFutureListener { future ->
                if (future.isSuccess) {
                    inboundChannel.read()
                } else {
                    future.channel().close()
                    flushAndClose(ctx.channel())
                    logger.warning("Couldn't activate channel:\n" + future.cause().message)
                }
            }

            f.addListener(listener)
        }
    }

    override fun channelActive(ctx: ChannelHandlerContext) {
        logger.info("Accepted conn from" + ctx.channel().remoteAddress().toString());

        val sumWeight = config.backends.map { backend -> backend.weight }.sum()
        val randomValue = random.nextDouble(sumWeight)
        i = 0
        var acc = 0.0
        while (acc + config.backends[i].weight < randomValue) {
            acc += config.backends[i].weight
            i++
        }

        logger.info("Trying to send message to " + config.backends[i].address + ":" + config.backends[i].port)

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
        logger.warning(cause.message)
        flushAndClose(ctx.channel())
    }
}