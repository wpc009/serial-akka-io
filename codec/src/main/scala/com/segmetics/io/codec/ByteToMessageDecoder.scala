package com.segmetics.io.codec

import java.util
import java.util.List

import akka.util.ByteString
import com.segmetics.io.handler.{ChannelContext, HandlerAdapter}
import io.netty.buffer.{ByteBuf, Unpooled}
import io.netty.util.internal.StringUtil

import scala.annotation.tailrec

/**
  * Created by jiaoew on 2016/12/14.
  */
object ByteToMessageDecoder {

  def fireChannelRead(ctx: ChannelContext, msgs: util.List[Any], numElements: Int): Unit = {
    msgs match {
      case list: CodecOutputList =>
        var i = 0
        while (i < numElements) {
          ctx.fireRead(list.getUnsafe(i))
          i += 1
        }
      case _ =>
        var i = 0
        while (i < numElements) {
          ctx.fireRead(msgs.get(i))
          i += 1
        }
    }
  }

}

trait ByteToMessageDecoder extends HandlerAdapter {

  import ByteToMessageDecoder._

  var cumulation: ByteBuf = null
  var first: Boolean = false
  var numReads = 0
  var discardAfterReads = 16
  var forceFirst = false

  def setDiscardAfterReads(discard: Int) = discardAfterReads = discard

  override def channelRead(ctx: ChannelContext): Unit = {
    val in = ctx.inputStream()
    val buf = Unpooled.buffer(64)

    @tailrec
    def doRead() {
      val count = buf.writeBytes(in, 64)
      log.debug(s"count is $count")
      if (count > 0) {
        doRead()
      }
    }

    try {
      doRead()
    } catch {
      case e: java.io.IOException =>
        log.warning("read serial exception")
      case e: java.lang.IllegalStateException =>
        log.error(e, "serial state error")
    }
    val out = CodecOutputList.newInstance
    try {
      if (forceFirst && cumulation != null) {
        val bs = Array.ofDim[Byte](cumulation.readableBytes())
        val readerIndex = cumulation.readerIndex()
        cumulation.getBytes(readerIndex, bs)
        ctx.fireDiscardBytes(ByteString(bs))
        cumulation.release
        cumulation = null
        forceFirst = false
      }
      first = cumulation == null
      if (first) cumulation = buf
      else cumulation.writeBytes(buf)
      callDecode(ctx, cumulation, out)
    } finally {
      if (cumulation != null && !cumulation.isReadable) {
        numReads = 0
        cumulation.release
        cumulation = null
      } else if ( {
        numReads += 1
        numReads
      } >= discardAfterReads) {
        numReads = 0
        discardSomeReadBytes()
      }
      val iter = out.iterator()
      while (iter.hasNext) {
        ctx.fireRead(iter.next())
      }
      out.recycle()
    }
  }


  override def channelReset(ctx: ChannelContext): Unit = {
    forceFirst = true
  }

  protected def discardSomeReadBytes() {
    if (cumulation != null && !first && cumulation.refCnt == 1) {
      cumulation.discardSomeReadBytes
    }
  }

  protected def callDecode(ctx: ChannelContext, in: ByteBuf, out: util.List[Any]): Unit = {
    try {
      while (in.isReadable) {
        var outSize = out.size
        if (outSize > 0) {
          fireChannelRead(ctx, out, outSize)
          out.clear()
          outSize = 0
        }
        val oldInputLength = in.readableBytes
        decode(ctx, in, out)
        var isContinue = true
        if (outSize == out.size) {
          if (oldInputLength == in.readableBytes) return
          else isContinue = false
        }
        if (isContinue) {
          if (oldInputLength == in.readableBytes) throw new DecoderException(StringUtil.simpleClassName(getClass) + ".decode() did not read anything but decoded a message.")
        }
      }
    } catch {
      case e: DecoderException => throw e
      case t: Throwable => throw new DecoderException(t)
    }
  }

  protected def internalBuffer: ByteBuf = {
    if (cumulation != null) cumulation
    else Unpooled.EMPTY_BUFFER
  }

  protected def decodeLast(ctx: ChannelContext, in: ByteBuf, out: util.List[Any]): Unit = {
    if (in.isReadable) {
      decode(ctx, in, out)
    }
  }

  protected def decode(ctx: ChannelContext, bytes: ByteBuf, out: util.List[Any])

}

