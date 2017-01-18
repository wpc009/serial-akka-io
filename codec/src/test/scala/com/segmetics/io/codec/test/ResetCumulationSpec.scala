package com.segmetics.io.codec.test

import java.io.InputStream

import akka.actor.ActorSystem
import akka.util.ByteString
import com.segmetics.io.codec.LengthFieldBasedFrameDecoder
import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by jiaoew on 2017/1/18.
  */
class ResetCumulationSpec extends FlatSpec with Matchers {

  val sys = ActorSystem("a")

  it should "reset easy" in {

    val decoder = new LengthFieldBasedFrameDecoder(200, 0, 2)
    decoder.setLogger(sys.log)
    val context = new FirstErrorContext
    decoder.channelRead(context)
    decoder.channelReset(context)
    decoder.channelRead(context)
    context.msg should be(ByteString(Array(0.toByte, 3.toByte, 0.toByte, 5.toByte, 1.toByte)))
  }

  class FirstErrorContext extends MockContext {
    override val streams: List[InputStream] = List(
      new InputStream {
        val buffer = List(0.toByte, 30.toByte, 0.toByte, 5.toByte, 1.toByte, 2.toByte, 3.toByte, 4.toByte, 5.toByte)
        var index = 0

        override def read(): Int = {
          if (index >= buffer.length) -1
          else {
            val rst = buffer(index)
            index += 1
            rst
          }
        }
      },
      new InputStream {
        val buffer = List(0.toByte, 3.toByte, 0.toByte, 5.toByte, 1.toByte, 2.toByte, 3.toByte, 4.toByte, 5.toByte)
        var index = 0

        override def read(): Int = {
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
