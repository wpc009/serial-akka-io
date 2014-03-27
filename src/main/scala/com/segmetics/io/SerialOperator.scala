package com.segmetics.io

import purejavacomm.{SerialPortEvent, SerialPortEventListener, SerialPort}
import akka.actor.{Props, Actor, ActorRef}
import com.segmetics.io.Serial._
import akka.util.ByteStringBuilder

/**
 * Created by wysa on 14-3-26.
 */
private[io] class SerialOperator(port: SerialPort, commander: ActorRef) extends Actor {
  private object DataAvailable

  context.watch(commander) //death pact

  val out = port.getOutputStream
  val in = port.getInputStream

  override def preStart = {
    port.notifyOnDataAvailable(true)
    port.enableReceiveTimeout(1)
    val toNotify = self
    port.addEventListener(new SerialPortEventListener() {
      override def serialEvent(event: SerialPortEvent) {
        toNotify ! DataAvailable
      }
    })
    self ! DataAvailable //just in case
  }

  override def postStop = {
    commander ! Closed
    port.close
  }

  override def receive = {
    case Close =>
      port.close
      if (sender != commander) sender ! Closed
      context.stop(self)

    case Write(data, ack) =>
      out.write(data.toArray)
      out.flush
      if (ack != NoAck) sender ! ack

    case DataAvailable =>
      val data = read()
      if (data.nonEmpty) commander ! Received(data)
  }

  private def read() = {
    val bsb = new ByteStringBuilder
    def doRead() {
      val data = in.read()
      if (data != -1) {
        bsb += data.toByte
        doRead
      }
    }
    doRead()
    bsb.result
  }
}
private[io] object SerialOperator {
  def props(port: SerialPort, commander: ActorRef) = Props(classOf[SerialOperator],port,commander)
}