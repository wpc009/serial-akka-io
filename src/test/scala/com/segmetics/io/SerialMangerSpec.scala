package com.segmetics.io

import akka.actor.ActorSystem
import akka.io._
import akka.testkit._
import org.scalatest.{FlatSpecLike, Matchers, BeforeAndAfterAll}
import Serial._
import scala.concurrent.duration._
import akka.util.ByteString
import scala.language.postfixOps

class SerialMangerSpec extends TestKit(ActorSystem("SerialManagerSpec"))
  with BeforeAndAfterAll
  with FlatSpecLike
  with Matchers
  with ImplicitSender {





  "Serial" should "list ports" in {
    IO(Serial) ! ListPorts
    val Ports(ports) = expectMsgType[Ports]
    println("Found serial ports: " + ports.mkString(", "))
  }

  "[Serial] echo test " should "echo back" in {
    val echoWorlds = ByteString("Hello World")
    IO(Serial) ! Open("/dev/ttyUSB0",9600,DataBits8,NoParity,OneStopBit,NoFlowControl)
    expectMsgPF(1 seconds){
      case Opened(op,_) =>
        op ! Write(echoWorlds)
        true
      case _ =>
        false
    }

    expectMsgPF(1 seconds){
      case Received(data:ByteString) =>
        data.equals(echoWorlds)
      case _ =>
        false
    }
  }

  override protected def afterAll(): Unit = system.shutdown()

}