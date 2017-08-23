package com.segmetics.io.codec.test

import java.io.InputStream

import org.scalatest.{FlatSpec, Matchers}

class ReplayingDecoderSpec extends FlatSpec with Matchers {

  "replaying decoder" should "without partial" in {

  }

  it should "with partial" in {

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
      }
    )
  }

}
