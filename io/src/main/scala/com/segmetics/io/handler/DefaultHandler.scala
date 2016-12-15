package com.segmetics.io.handler

import java.io.InputStream
import java.util

import akka.event.LoggingAdapter
import akka.util.ByteStringBuilder

import scala.annotation.tailrec

/**
  * Created by jiaoew on 2016/12/15.
  */
class DefaultHandler extends HandlerAdapter {

  override def channelRead(ctx: ChannelContext): Unit = {
    ctx.fireRead(ctx.inputStream())
  }

  private def read(in: InputStream) = {
    val bsb = new ByteStringBuilder
    val buf = Array.ofDim[Byte](64)
    @tailrec
    def doRead() {
      val count = in.read(buf, 0, 64)
      log.debug("count {}, value {}", count, util.Arrays.toString(buf))
      if (count > 0) {
        bsb ++= buf.slice(0, count)
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
    val res = bsb.result()
    res
  }
}
