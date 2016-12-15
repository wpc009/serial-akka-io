package com.segmetics.io

import akka.actor._
import akka.io.IO.Extension
import akka.util.ByteString
import com.segmetics.io.handler.{DefaultHandler, HandlerAdapter}

/**
  * Created by wysa on 14-3-26.
  */
object Serial extends ExtensionId[SerialExt] with ExtensionIdProvider {

  override def lookup = Serial

  override def createExtension(system: ExtendedActorSystem): SerialExt = new SerialExt(system)

  override def get(system: ActorSystem): SerialExt = super.get(system)

  /** Messages used by the serial IO. */
  sealed trait Message

  /** Messages that are sent to the serial port. */
  sealed trait Command extends Message

  /** Messages received from the serial port. */
  sealed trait Event extends Message

  /** Command that may be sent to the manager actor. */
  sealed trait ManagerCommand extends Command

  // Communication with manager

  /** Command that may be sent to the operator actor. */
  sealed trait OperatorCommand extends Command

  sealed trait DataBits

  sealed trait Parity

  sealed trait StopBits

  sealed trait FlowControl

  /** The port was closed. Either by request or by an external event (i.e. unplugging) */
  sealed trait Closed extends Event

  /** Ack for a write. */
  trait AckEvent extends Event

  case class CommandFailed(command: Command, reason: Throwable)

  /** Open a serial port. Response: Opened | CommandFailed */
  case class Open(port: String, baudRate: Option[Int] = None,
                  dataBits: Option[DataBits] = None,
                  parity: Option[Parity] = None,
                  stopBits: Option[StopBits] = None,
                  flowControl: Option[FlowControl] = None,
                  timeout: Option[Int] = None) extends ManagerCommand

  case class PureOpen(port: String, baudRate: Option[Int] = None,
                  dataBits: Option[Int] = None, parity: Option[Int] = None,
                  stopBits: Option[Int] = None, flowControl:Option[Int] = None,
                  timeout: Option[Int] = None, handler: HandlerAdapter = new DefaultHandler) extends ManagerCommand

  /**
    * Serial port is now open.
    * Communication is handled by the operator actor.
    * The sender of the Open message will now receive incoming communication from the
    * serial port.
    */
  case class Opened(operator: ActorRef, port: String) extends Event

  /** Available serial ports. */
  case class Ports(ports: Vector[String]) extends Event

  /** Data was received on the serial port. */
  case class Received(data: ByteString) extends Event

  /** Write data on the serial port. Response: ack (if ack != NoAck) */
  case class Write(data: ByteString, ack: AckEvent = NoAck) extends OperatorCommand

  object DataBits8 extends DataBits

  object DataBits7 extends DataBits

  object DataBits6 extends DataBits

  object DataBits5 extends DataBits

  object NoParity extends Parity

  object EvenParity extends Parity

  object OddParity extends Parity

  object MarkParity extends Parity

  object SpaceParity extends Parity

  object OneStopBit extends StopBits

  object TwoStopBits extends StopBits

  object OneAndHalfStopBits extends StopBits

  // Communication with Operator

  object NoFlowControl extends FlowControl

  object RtsFlowControl extends FlowControl

  object XonXoffFlowControl extends FlowControl

  /** List all available serial ports. Response: Ports | CommandFailed */
  case object ListPorts extends ManagerCommand


  /** Request that the operator should close the port. Response: Closed */
  case object Close extends OperatorCommand

  /**
    * Closed on request
    */
  case object ConfirmedClose extends Closed

  /**
    * Closed by external events
    */
  case object ExceptionClose extends Closed

  /** Special ack event (is not sent). */
  object NoAck extends AckEvent

  /**
    * Java API
    * @param port
    * @param baudRate
    */
  def open(port: String, baudRate: Int) =
    Open(port, Some(baudRate), Some(DataBits8), Some(NoParity), Some(OneStopBit), Some(NoFlowControl))

  /**
    * Java API
    */
  def open(port: String, baudRate: Int, dataBits: Int, stopBits: Int, parity: Int, timeout: Int) =
    PureOpen(port, Some(baudRate), Some(dataBits), Some(parity), Some(stopBits), Some(0), Some(timeout))

  /**
    * Java API
    */
  def open(port: String, baudRate: Int, dataBits: Int, stopBits: Int, parity: Int, timeout: Int, handler: HandlerAdapter) =
    PureOpen(port, Some(baudRate), Some(dataBits), Some(parity), Some(stopBits), Some(0), Some(timeout), handler)
  /**
    * Java API
    */
  def close() = Close

  /**
    * Java API
    * @param bytes
    * @return
    */
  def write(bytes: ByteString) = Write(bytes)

}


class SerialExt(system: ExtendedActorSystem) extends Extension {

  lazy val manager = system.actorOf(Props[SerialManager], "IO-Serial")
}
