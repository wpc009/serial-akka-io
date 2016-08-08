package com.segmetics.io

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.util.{ByteString, ByteStringBuilder}
import com.segmetics.io.Serial._
import purejavacomm.{SerialPort, SerialPortEvent, SerialPortEventListener}

import scala.annotation.tailrec

/**
  * Created by wysa on 14-3-26.
  */
private[io] class SerialOperator(port: SerialPort, commander: ActorRef) extends Actor with ActorLogging {

  val out = port.getOutputStream

  context.watch(commander)
  //death pact
  val in = port.getInputStream
  val toNotify = self

  // override def preStart = {
  port.notifyOnDataAvailable(true)
  port.setInputBufferSize(64)
  port.enableReceiveTimeout(1)

  override def postStop = {
    log.info(s"serialport ${port} close")
    commander ! ConfirmedClose
    port.close()
  }
  port.addEventListener(new SerialPortEventListener() {
    var pt = 0

    private def read() = {
      val bsb = new ByteStringBuilder
      val buf = Array.ofDim[Byte](64)
      @tailrec
      def doRead() {
        val count = in.read(buf, 0, 64)
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

    override def serialEvent(event: SerialPortEvent) {
      import purejavacomm.SerialPortEvent
      log.debug("event type {}", event.getEventType)
      event.getEventType match {
        case SerialPortEvent.DATA_AVAILABLE =>
          toNotify ! read()
        //case SerialPortEvent.PE =>
        //case SerialPortEvent.OE =>
        case _ =>
          log.error(s"got unhandled serial event type ${event.getEventType}")
      }

    }
  })
  self ! DataAvailable

  override def receive = {
    case Close =>
      port.close()
      if (sender != commander) sender ! ConfirmedClose
      context.stop(self)

    case Write(data, ack) =>
      out.write(data.toArray)
      out.flush()
      if (ack != NoAck) sender ! ack

    case data: ByteString =>
      //      log.debug("got input {}",data)
      if (data.nonEmpty) commander ! Received(data)
  }

  private object DataAvailable


}

private[io] object SerialOperator {
  def props(port: SerialPort, commander: ActorRef) = Props(classOf[SerialOperator], port, commander)
}
