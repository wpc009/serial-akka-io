package com.segmetics.io.handler

import java.io.InputStream

import akka.event.{LoggingAdapter, NoLogging}

/**
  * Created by jiaoew on 2016/12/15.
  */
trait HandlerAdapter {

  var log: LoggingAdapter = NoLogging

  def setLogger(log: LoggingAdapter) = this.log = log

  def channelRead(ctx: ChannelContext)
}

trait ChannelContext {

  def fireRead(msg: Any)

  def inputStream(): InputStream

}
