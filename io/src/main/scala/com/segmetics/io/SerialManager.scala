package com.segmetics.io

import akka.actor.{Actor, ActorLogging}
import com.segmetics.io.Serial._
import purejavacomm._

import scala.collection.mutable.ArrayBuffer
import com.segmetics.io.Serial.{Open, PureOpen}
import com.segmetics.io.handler.HandlerAdapter

import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

/**
  * Created by wysa on 14-3-25.
  */
private[io] class SerialManager extends Actor with ActorLogging {

  def openSerial(c: PureOpen): Unit = Try(CommPortIdentifier.getPortIdentifier(c.port)).map(id => {
    id.open(context.self.toString(), 2000)
  }).flatMap(comm => {
    try {
      comm match {
        case sp: SerialPort =>
          log.debug("open port {} succeed, settings params", c.port)
          sp.setSerialPortParams(c.baudRate.getOrElse(9600), c.dataBits.getOrElse(SerialPort.DATABITS_8), c.stopBits.getOrElse(SerialPort.STOPBITS_1), c.parity.getOrElse(SerialPort.PARITY_NONE))
          sp.notifyOnBreakInterrupt(true)
          sp.enableReceiveTimeout(c.timeout.getOrElse(50))
          sp.setFlowControlMode(c.flowControl.getOrElse(SerialPort.FLOWCONTROL_NONE))
          Success(sp)
        case _ =>
          Failure(new purejavacomm.NoSuchPortException())
      }
    } catch {
      case NonFatal(e) =>
        comm.close()
        Failure(e)
    }
  }) match {
    case Success(serialPort: SerialPort) =>
      val operator = context.actorOf(SerialOperator.props(serialPort, sender(), c.handler))
      sender ! Opened(operator, c.port)
    case Failure(error) =>
      sender ! CommandFailed(c, error)
  }


  def receive = {
    case ListPorts =>
      val ids = CommPortIdentifier.getPortIdentifiers().asInstanceOf[java.util.Enumeration[CommPortIdentifier]]
      val builder = new ArrayBuffer[String]()
      while (ids.hasMoreElements) {
        builder += ids.nextElement().getName
      }
      sender ! builder.toVector
    case c: PureOpen =>
      openSerial(c)
    case c@Open(port, baudRate, dataBits, parity, stopBits, flowControl, timeout) =>

      import purejavacomm.SerialPort._

      val data = dataBits map {
        case DataBits5 => DATABITS_5
        case DataBits6 => DATABITS_6
        case DataBits7 => DATABITS_7
        case DataBits8 => DATABITS_8
      }
      val stop = stopBits map {
        case OneStopBit => STOPBITS_1
        case OneAndHalfStopBits => STOPBITS_1_5
        case TwoStopBits => STOPBITS_2
      }
      val par = parity map {
        case NoParity => PARITY_NONE
        case EvenParity => PARITY_EVEN
        case OddParity => PARITY_ODD
        case MarkParity => PARITY_MARK
        case SpaceParity => PARITY_SPACE
      }
      val fc = flowControl map {
        case NoFlowControl => FLOWCONTROL_NONE
        case RtsFlowControl => FLOWCONTROL_RTSCTS_IN | FLOWCONTROL_RTSCTS_OUT
        case XonXoffFlowControl => FLOWCONTROL_XONXOFF_IN | FLOWCONTROL_XONXOFF_OUT
      }
      openSerial(PureOpen(port, baudRate, data, par, stop, fc, timeout))
  }
}
