package com.segmetics.io.codec.test

import java.io.InputStream

import akka.actor.ActorSystem
import akka.event.Logging
import akka.util.ByteString
import com.segmetics.io.codec.DelimiterBasedFrameDecoder
import io.netty.buffer.Unpooled
import org.scalatest.{FlatSpec, Matchers}

class DelimiterDecoderSpec extends FlatSpec with Matchers {

  val system = ActorSystem("abc")

  "delimiter decoder" should "simple" in {
    val decoder = new DelimiterBasedFrameDecoder(200, Array('*'.toByte, '\r'.toByte))
    decoder.setLogger(Logging(system, "simple"))
    val context = new MyContext3
    decoder.channelRead(context)
    context.reads.get(0) should be(ByteString(Array('h'.toByte, 'e'.toByte, 'l'.toByte, 'l'.toByte, 'l'.toByte)))
  }

  it should "partial" in {
    val decoder = new DelimiterBasedFrameDecoder(200, Array('*'.toByte, '\r'.toByte))
    decoder.setLogger(Logging(system, "partial"))
    val context = new MyContext2
    decoder.channelRead(context)
    decoder.channelRead(context)
    context.reads.get(0) should be(ByteString(Array('h'.toByte, 'e'.toByte, 'l'.toByte, 'l'.toByte, 'l'.toByte)))

  }

  class MyContext2 extends MockContext {

    val reads = new java.util.ArrayList[Any]()

    override def fireRead(msg: Any): Unit = reads.add(msg)

    override val streams: List[InputStream] = List(
      new InputStream {
        val buffer = List('h'.toByte, 'e'.toByte, 'l'.toByte, 'l'.toByte)
        var index = 0

        override def read() = {
          if (index >= buffer.length) -1
          else {
            val rst = buffer(index)
            index += 1
            rst
          }
        }
      },
      new InputStream {
        val buffer = List('l'.toByte, '*'.toByte, '\r'.toByte, 'w'.toByte, 'h'.toByte, 'e'.toByte, 'l'.toByte, 'l'.toByte)
        var index = 0

        override def read() = {
          if (index >= buffer.length) -1
          else {
            val rst = buffer(index)
            index += 1
            rst
          }
        }
      }
    )
  }

  class MyContext3 extends MockContext {

    val reads = new java.util.ArrayList[Any]()

    override def fireRead(msg: Any): Unit = reads.add(msg)

    override val streams: List[InputStream] = List(
      new InputStream {
        val buffer = List('h'.toByte, 'e'.toByte, 'l'.toByte, 'l'.toByte, 'l'.toByte, '*'.toByte, '\r'.toByte, 'w'.toByte)
        var index = 0

        override def read() = {
          if (index >= buffer.length) -1
          else {
            val rst = buffer(index)
            index += 1
            rst
          }
        }
      }
    )
  }

}
