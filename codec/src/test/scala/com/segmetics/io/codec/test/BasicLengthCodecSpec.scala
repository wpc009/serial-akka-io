package com.segmetics.io.codec.test

import java.io.InputStream

import akka.actor.ActorSystem
import akka.event.Logging
import akka.util.ByteString
import com.segmetics.io.codec.LengthFieldBasedFrameDecoder
import com.segmetics.io.handler.ChannelContext
import org.scalatest._

/**
  * Created by jiaoew on 2016/12/15.
  */
class BasicLengthCodecSpec extends FlatSpec with Matchers {

  "length based " should "offset 2" in {
    val decoder = new LengthFieldBasedFrameDecoder(200, 2, 2)
    val context = new MockContext
    decoder.channelRead(context)
    context.msg should be(ByteString(Array(0.toByte, 3.toByte, 0.toByte, 5.toByte, 1.toByte, 2.toByte, 3.toByte, 4.toByte, 5.toByte)))
  }

  it should "offset 0" in {
    val decoder2 = new LengthFieldBasedFrameDecoder(200, 0, 2)
    val context = new MockContext
    decoder2.channelRead(context)
    context.msg should be(ByteString(Array(0.toByte, 3.toByte, 0.toByte, 5.toByte, 1.toByte)))
  }

  it should "adjust 3" in {
    val decoder = new LengthFieldBasedFrameDecoder(200, 0, 2, 3)
    val context = new MockContext
    decoder.channelRead(context)
    context.msg should be(ByteString(Array(0.toByte, 3.toByte, 0.toByte, 5.toByte,
      1.toByte, 2.toByte, 3.toByte, 4.toByte)))
  }

  it should "strip 2 and offset 2 and adjust -1" in {
    val decoder = new LengthFieldBasedFrameDecoder(200, 2, 2, -1, 2)
    val context = new MockContext
    decoder.channelRead(context)
    context.msg should be(ByteString(Array(0.toByte, 5.toByte,
      1.toByte, 2.toByte, 3.toByte, 4.toByte)))
  }

  val sys = ActorSystem("a")

  it should "packet slicing" in {
    val decoder = new LengthFieldBasedFrameDecoder(200, 2, 2, -1, 2)
    val context = new MyContext2
//    decoder.setLogger(Logging.getLogger(sys, "abc"))

    decoder.channelRead(context)
    context.msg should be (0)
    decoder.channelRead(context)
    context.msg should be(ByteString(Array(0.toByte, 5.toByte,
      1.toByte, 2.toByte, 3.toByte, 4.toByte)))
  }

  it should "two pack" in {
    val decoder = new LengthFieldBasedFrameDecoder(200, 0, 2, 0, 2)
    val context = new MyContext3
    decoder.setLogger(Logging.getLogger(sys, "abc"))

    decoder.channelRead(context)
    context.msg should be(ByteString(Array(1.toByte, 2.toByte, 3.toByte)))
    decoder.channelRead(context)
    context.msg should be(ByteString(Array(4.toByte, 5.toByte)))
  }

  class MyContext2 extends MockContext {
    override val streams: List[InputStream] = List(
      new InputStream {
        val buffer = List(0.toByte, 3.toByte, 0.toByte, 5.toByte)
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
        val buffer = List(1.toByte, 2.toByte, 3.toByte, 4.toByte, 5.toByte)
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
    override val streams: List[InputStream] = List(
      new InputStream {
        val buffer = List(0.toByte, 3.toByte, 1.toByte, 2.toByte, 3.toByte, 0.toByte, 2.toByte, 4.toByte, 5.toByte, 6.toByte)
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
