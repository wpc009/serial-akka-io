package com.segmetics.io

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory

/**
 * Created by wysa on 15/5/30.
 */
object TestMain {

  def main(args: Array[String]): Unit = {
    val port = "/dev/tty.usbserial-DN00MZAW"
    val system = ActorSystem("Example",ConfigFactory.load("reference_test.conf"))
    val actor = system.actorOf(Props(classOf[Example],port), "e1")
    actor ! "Hello World"
    system.awaitTermination()

  }
}
