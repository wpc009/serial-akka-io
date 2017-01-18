package com.segmetics.io

import java.io.InputStream

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.util.ByteString
import com.segmetics.io.Serial._
import com.segmetics.io.handler.{ChannelContext, DefaultHandler, HandlerAdapter}
import purejavacomm.{SerialPort, SerialPortEvent, SerialPortEventListener}

/**
  * Created by wysa on 14-3-26.
  */
private[io] class SerialOperator(port: SerialPort,
                                 commander: ActorRef,
                                 handler: HandlerAdapter) extends Actor with ActorLogging {

  val out = port.getOutputStream

  context.watch(commander)
  handler.setLogger(log)

  val channelContext = new ChannelContext {
    override def fireRead(msg: Any): Unit = self ! msg

    override def inputStream(): InputStream = port.getInputStream

    override def fireDiscardBytes(msg: Any): Unit = msg match {
      case bs: ByteString =>
        log.debug("discarded {}", bs)
        commander ! Discarded(bs)
      case _ =>
    }
  }

  port.notifyOnDataAvailable(true)
  port.setInputBufferSize(64)
  port.enableReceiveTimeout(1)

  override def postStop = {
    log.info(s"serial port ${port} close")
    commander ! ConfirmedClose
    port.close()
  }

  port.addEventListener(new SerialPortEventListener() {
    override def serialEvent(event: SerialPortEvent) {
      import purejavacomm.SerialPortEvent
      log.debug("event type {}", event.getEventType)
      event.getEventType match {
        case SerialPortEvent.DATA_AVAILABLE =>
          handler.channelRead(channelContext)
        case _ =>
          log.error(s"got unhandled serial event type ${event.getEventType}")
      }

    }
  })

  override def receive = {
    case Close =>
      port.close()
      if (sender != commander) sender ! ConfirmedClose
      context.stop(self)

    case Write(data, ack, reset) =>
      out.write(data.toArray)
      out.flush()
      if (ack != NoAck) sender ! ack
      if (reset) handler.channelReset(channelContext)

    case Reset =>
      handler.channelReset(channelContext)

    case data: ByteString =>
      log.debug("got input {}", data)
      if (data.nonEmpty) commander ! Received(data)
  }

}

private[io] object SerialOperator {
  def props(port: SerialPort, commander: ActorRef, handler: HandlerAdapter): Props =
    Props(classOf[SerialOperator], port, commander, handler)
}
