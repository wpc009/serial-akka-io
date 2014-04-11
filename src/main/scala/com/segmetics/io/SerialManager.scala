package com.segmetics.io

import akka.actor.Actor
import com.segmetics.io.Serial._
import purejavacomm.{SerialPort, CommPortIdentifier}
import scala.collection.mutable.ArrayBuffer
import com.segmetics.io.Serial.Open
import scala.util.{Try,Success,Failure}
/**
 * Created by wysa on 14-3-25.
 */
private[io] class SerialManager extends Actor{

  def receive = {
    case ListPorts =>
      val ids= CommPortIdentifier.getPortIdentifiers().asInstanceOf[java.util.Enumeration[CommPortIdentifier]]
      val builder = new ArrayBuffer[String]()
      while(ids.hasMoreElements){
        builder += ids.nextElement().getName
      }
      sender ! builder.toVector
    case c @ Open(port,baudRate,dataBits,parity,stopBits,flowControl) =>
      import purejavacomm.SerialPort._
      Try {
        val id = CommPortIdentifier.getPortIdentifier(port)
        val data = dataBits match {
          case DataBits5 => DATABITS_5
          case DataBits6 => DATABITS_6
          case DataBits7 => DATABITS_7
          case DataBits8 => DATABITS_8
        }
        val stop = stopBits match {
          case OneStopBit => STOPBITS_1
          case OneAndHalfStopBits => STOPBITS_1_5
          case TwoStopBits => STOPBITS_2
        }
        val par = parity match {
          case NoParity => PARITY_NONE
          case EvenParity => PARITY_EVEN
          case OddParity => PARITY_ODD
          case MarkParity => PARITY_MARK
          case SpaceParity => PARITY_SPACE
        }
        val fc = flowControl match {
          case NoFlowControl => FLOWCONTROL_NONE
          case RtsFlowControl => FLOWCONTROL_RTSCTS_IN | FLOWCONTROL_RTSCTS_OUT
          case XonXoffFlowControl => FLOWCONTROL_XONXOFF_IN | FLOWCONTROL_XONXOFF_OUT
        }
        id.open(context.self.toString, 2000) match {
          case sp: SerialPort =>
            sp.setSerialPortParams(baudRate, data, stop, par)
            sp.setFlowControlMode(fc)
            sp
          case _ => throw new RuntimeException(s"$port is not a SerialPort.")
        }


      } match {
        case Success(serialPort) =>
          val operator = context.actorOf(SerialOperator.props(serialPort, sender))
          sender ! Opened(operator, port)
        case Failure(error) =>
          sender ! CommandFailed(c, error)
      }
  }
}
