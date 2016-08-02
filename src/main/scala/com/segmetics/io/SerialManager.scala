package com.segmetics.io

import akka.actor.{ ActorLogging,Actor}
import com.segmetics.io.Serial._
import purejavacomm.{PureJavaSerialPort, SerialPort, CommPortIdentifier}
import scala.collection.mutable.ArrayBuffer
import com.segmetics.io.Serial.{Open, PureOpen}
import scala.util.{Try,Success,Failure}
/**
 * Created by wysa on 14-3-25.
 */
private[io] class SerialManager extends Actor with ActorLogging{

  def openSerial(c: PureOpen) = {
    Try {
      c match {
        case PureOpen(port, baudRate, data, par, stop, fc, timeout) =>
          val id = CommPortIdentifier.getPortIdentifier(port)
          id.open(context.self.toString, 2000) match {
            case sp: PureJavaSerialPort =>
            log.debug("open port {} succeed, settings params",port)
            if(baudRate.nonEmpty&&data.nonEmpty&&stop.nonEmpty&&par.nonEmpty){
              sp.setSerialPortParams(baudRate.get, data.get, stop.get, par.get)
            }
            // sp.setSerialPortParams(baudRate, data, stop, par)
            sp.notifyOnBreakInterrupt(true)
            val receiveTimeout = timeout.getOrElse(50)
            sp.enableReceiveTimeout(receiveTimeout)
            //            sp.enableReceiveFraming(50)
            if(fc.nonEmpty){
              sp.setFlowControlMode(fc.get)
            }
            sp
            case _ => throw new RuntimeException(s"$port is not a SerialPort.")
          }
      }
    } match {
      case Success(serialPort) =>
      val operator = context.actorOf(SerialOperator.props(serialPort, sender()))
      sender ! Opened(operator, c.port)
      case Failure(error) =>
      sender ! CommandFailed(c, error)
    }
  }

  def receive = {
    case ListPorts =>
      val ids= CommPortIdentifier.getPortIdentifiers().asInstanceOf[java.util.Enumeration[CommPortIdentifier]]
      val builder = new ArrayBuffer[String]()
      while(ids.hasMoreElements){
        builder += ids.nextElement().getName
      }
      sender ! builder.toVector
    case c: PureOpen =>
      openSerial(c)
    case c @ Open(port,baudRate,dataBits,parity,stopBits,flowControl,timeout) =>
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
