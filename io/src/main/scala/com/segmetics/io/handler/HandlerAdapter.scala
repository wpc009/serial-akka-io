package com.segmetics.io.handler

import java.io.InputStream

import akka.event.{LoggingAdapter, NoLogging}

/**
  * Created by jiaoew on 2016/12/15.
  */
trait HandlerAdapter {

  var log: LoggingAdapter = NoLogging

  def setLogger(log: LoggingAdapter): Unit = this.log = log

  def channelRead(ctx: ChannelContext)

  def channelReset(ctx: ChannelContext): Unit = {}

}

trait ChannelContext {

  def fireRead(msg: Any)

  def fireDiscardBytes(msg: Any)

  def inputStream(): InputStream

}
