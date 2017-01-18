package com.segmetics.io.codec.test

import java.io.InputStream

import com.segmetics.io.handler.ChannelContext

/**
  * Created by jiaoew on 2017/1/18.
  */
class MockContext extends ChannelContext {

  val streams: List[InputStream] = List(
    new InputStream {
      val buffer = List(0.toByte, 3.toByte, 0.toByte, 5.toByte,
        1.toByte, 2.toByte, 3.toByte, 4.toByte, 5.toByte)
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

  val emptyStream = new InputStream {
    override def read() = -1
  }
  var index = 0
  var msg: Any = 0

  override def fireRead(msg: Any) = {
    this.msg = msg
  }

  override def inputStream() = {
    if (index >= streams.length) {
      emptyStream
    } else {
      val s = streams(index)
      index += 1
      s
    }
  }
}
