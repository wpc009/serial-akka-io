package com.segmetics.io

import purejavacomm.{SerialPortEvent, SerialPortEventListener, SerialPort}
import akka.actor.{ActorLogging, Props, Actor, ActorRef}
import com.segmetics.io.Serial._
import akka.util.{ByteString, ByteStringBuilder}
import scala.annotation.tailrec

/**
 * Created by wysa on 14-3-26.
 */
private[io] class SerialOperator(port: SerialPort, commander: ActorRef) extends Actor with ActorLogging {
  private object DataAvailable

  context.watch(commander) //death pact

  val out = port.getOutputStream
  val in = port.getInputStream


 // override def preStart = {
    port.notifyOnDataAvailable(true)
    port.setInputBufferSize(64)
    port.enableReceiveTimeout(1)

    val toNotify = self
    port.addEventListener(new SerialPortEventListener() {
      var pt = 0

      private def read() = {
        val bsb = new ByteStringBuilder
        val buf = Array.ofDim[Byte](64)
        @tailrec
        def doRead() {
//          if(pt <64) {
//            val count = in.read(buf, pt, 64 - pt)
            val count = in.read(buf,0,64)
            log.debug("read count {}",count)
            if (count > 0) {
//              log.debug("receive -> {}", buf.slice(0, count).foldLeft(new StringBuilder) { (builder, byte) =>
//                builder.append("0x%02X ".format(byte))
//                builder
//              }.result())
//              pt = pt + count
              bsb ++= buf.slice(0,count)
              doRead()
//            }else{
//              bsb ++= buf.slice(0,pt)
//              pt=0
//            }
//          }else {
//            bsb ++= buf.slice(0,pt)
//            pt=0
          }
        }
        doRead()
        val res = bsb.result()
//        log.debug("get raw {}",res)
        res
      }

      override def serialEvent(event: SerialPortEvent) {
        log.debug(s"got serial event $event")
        import purejavacomm.SerialPortEvent
        log.debug("event type {}",event.getEventType)
        event.getEventType match {
          case SerialPortEvent.DATA_AVAILABLE=>
//            log.debug("doRead")
            toNotify ! read()
          //case SerialPortEvent.PE =>
          //case SerialPortEvent.OE =>
          case _ =>
            log.error(s"got unhandled serial event type ${event.getEventType}")
        }

      }
    })
    self ! DataAvailable //just in case
 // }

  override def postStop = {
    log.info(s"serialport ${port} close")
    commander ! ConfirmedClose
    port.close()
  }

  override def receive = {
    case Close =>
      port.close()
      if (sender != commander) sender ! ConfirmedClose
      context.stop(self)

    case Write(data, ack) =>
      out.write(data.toArray)
      out.flush()
      if (ack != NoAck) sender ! ack

    case data:ByteString =>
//      log.debug("got input {}",data)
      if (data.nonEmpty) commander ! Received(data)
  }


}
private[io] object SerialOperator {
  def props(port: SerialPort, commander: ActorRef) = Props(classOf[SerialOperator],port,commander)
}
