package com.segmetics.io.codec

import java.util

import com.segmetics.io.handler.{ChannelContext, HandlerAdapter}
import io.netty.buffer.{ByteBuf, Unpooled}

import scala.annotation.tailrec

/**
  * Created by jiaoew on 2016/12/14.
  */
trait ByteToMessageDecoder extends HandlerAdapter {

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
        cumulation.release
        cumulation = null
        forceFirst = false
      }
      first = cumulation == null
      if (first) cumulation = buf
      else cumulation.writeBytes(buf)
      decode(cumulation, out)
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

  protected def decode(bytes: ByteBuf, out: util.List[Any])

}

