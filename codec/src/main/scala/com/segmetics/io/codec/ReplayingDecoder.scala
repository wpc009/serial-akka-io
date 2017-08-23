package com.segmetics.io.codec

import com.segmetics.io.handler.ChannelContext
import io.netty.buffer.ByteBuf
import io.netty.util.Signal
import io.netty.util.internal.StringUtil

object ReplayingDecoder {
  private[codec] val REPLAY = Signal.valueOf(classOf[ReplayingDecoder[_]], "REPLAY")
}

abstract class ReplayingDecoder[S] protected(var state_ : S) extends ByteToMessageDecoder {

  import ByteToMessageDecoder._

  final private val replayable = new ReplayingDecoderByteBuf
  private var checkpoint_ = -1

  protected def checkpoint(): Unit = {
    checkpoint_ = internalBuffer.readerIndex
  }

  protected def checkpoint(s: S): Unit = {
    checkpoint()
    state(s)
  }

  protected def state: S = state_

  protected def state(newState: S): S = {
    val oldState = state
    state_ = newState
    oldState
  }

  override def callDecode(ctx: ChannelContext, in: ByteBuf, out: java.util.List[Any]): Unit = {
    replayable.setCumulation(in)
    try {
      while (in.isReadable) {
        checkpoint_ = in.readerIndex
        var isContinue = true
        val oldReaderIndex = checkpoint_
        var outSize = out.size
        if (outSize > 0) {
          fireChannelRead(ctx, out, outSize)
          out.clear()
          outSize = 0
        }
        val oldState = state_
        val oldInputLength = in.readableBytes
        try {
          decode(ctx, replayable, out)
          // Check if this handler was removed before continuing the loop.
          // See https://github.com/netty/netty/issues/1664
          if (outSize == out.size) {
            if (oldInputLength == in.readableBytes && (oldState == state_)) {
              throw new DecoderException(StringUtil.simpleClassName(getClass) + ".decode() must consume the inbound " + "data or change its state if it did not decode anything.")
            } else { // Previous data has been discarded or caused state transition.
              isContinue = false
            }
          }
        } catch {
          case replay: Signal =>
            replay.expect(ReplayingDecoder.REPLAY)
            // Return to the checkpoint (or oldPosition) and retry.
            val checkpoint = this.checkpoint_
            if (checkpoint >= 0) in.readerIndex(checkpoint)
            return
        }
        if (isContinue) {
          if (oldReaderIndex == in.readerIndex && (oldState == state_)) {
            throw new DecoderException(StringUtil.simpleClassName(getClass) + ".decode() method must consume the inbound data " + "or change its state if it decoded something.")
          }
        }
      }
    } catch {
      case cause: Throwable =>
        throw new DecoderException("", cause)
    }
  }
}
