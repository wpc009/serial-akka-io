package com.segmetics.io.codec

import java.nio.ByteOrder

import akka.util.ByteString
import com.segmetics.io.handler.ChannelContext
import io.netty.buffer.ByteBuf

/**
  * Created by jiaoew on 2016/12/14.
  */
class LengthFieldBasedFrameDecoder(val maxFrameLength: Int,
                                   val lengthFieldOffset: Int,
                                   val lengthFieldLength: Int,
                                   val lengthAdjustment: Int,
                                   val initialBytesToStrip: Int,
                                   val endian: ByteOrder = ByteOrder.BIG_ENDIAN,
                                   val keepDiscarding: Boolean = true) extends ByteToMessageDecoder {

  val lengthFieldEndOffset = lengthFieldOffset + lengthFieldLength

  var tooLongFrameLength = 0L
  var discardingTooLongFrame = false
  var bytesToDiscard = 0L

  def this(maxFrameLength: Int, lengthFieldOffset: Int, lengthFieldLength: Int) =
    this(maxFrameLength, lengthFieldOffset, lengthFieldLength, 0, 0)

  def this(maxFrameLength: Int, lengthFieldOffset: Int, lengthFieldLength: Int, adjustment: Int) =
    this(maxFrameLength, lengthFieldOffset, lengthFieldLength, adjustment, 0)

  def this(maxFrameLength: Int, lengthFieldOffset: Int, lengthFieldLength: Int, adjustment: Int, keep: Boolean) =
    this(maxFrameLength, lengthFieldOffset, lengthFieldLength, adjustment, 0, ByteOrder.BIG_ENDIAN, keep)


  override def decode(in: ByteBuf, out: java.util.List[Any]): Unit = {
    val frame = decode(in)
    log.debug("frame is {}", frame)
    frame.foreach(buf => {
      val bs = Array.ofDim[Byte](buf.readableBytes())
      val readerIndex = buf.readerIndex()
      buf.getBytes(readerIndex, bs)
      out.add(ByteString(bs))
    })
  }

  protected def decode(in: ByteBuf): Option[ByteBuf] = {
//    log.debug("in is {} {}", util.Arrays.toString(in.array()), in.readerIndex())
    if (discardingTooLongFrame && keepDiscarding) {
      var bytesToDiscard = this.bytesToDiscard
      val localBytesToDiscard = Math.min(bytesToDiscard, in.readableBytes).toInt
      in.skipBytes(localBytesToDiscard)
      bytesToDiscard -= localBytesToDiscard
      this.bytesToDiscard = bytesToDiscard
      failIfNecessary(false)
    }
    if (in.readableBytes() < lengthFieldEndOffset)
      return None
    val actualLengthFieldOffset = in.readerIndex + lengthFieldOffset
    var frameLength = getUnadjustedFrameLength(in, actualLengthFieldOffset, lengthFieldLength, endian)
    if (frameLength <= 0) {
      log.debug("frame length {}, too small", frameLength)
      in.skipBytes(lengthFieldEndOffset)
      return None
    }
    frameLength += lengthAdjustment + lengthFieldEndOffset

    if (frameLength < lengthFieldEndOffset) {
      log.debug("frame length {}, end offset {}", frameLength, lengthFieldEndOffset)
      in.skipBytes(lengthFieldEndOffset)
      return None
    }
    if (frameLength > maxFrameLength) {
      log.debug("frame length {}, max frame {}", frameLength, maxFrameLength)
      val discard = frameLength - in.readableBytes()
      tooLongFrameLength = frameLength
      if (discard < 0) {
        in.skipBytes(frameLength.toInt)
      } else {
        discardingTooLongFrame = true
        bytesToDiscard = discard
        in.skipBytes(in.readableBytes())
      }
      failIfNecessary(true)
      return None
    }

    if (in.readableBytes() < frameLength) {
      log.debug("frame length is {}, readable length is {}", frameLength, in.readableBytes())
      return None
    }

    if (initialBytesToStrip > frameLength) {
      log.debug("adjusted frame length is {} is less than {}", frameLength, initialBytesToStrip)
      in.skipBytes(frameLength.toInt)
      return None
    }
    in.skipBytes(initialBytesToStrip)

    val readerIndex = in.readerIndex()
    val actualFrameLength = (frameLength - initialBytesToStrip).toInt
    val frame = extractFrame(in, readerIndex, actualFrameLength)
    in.readerIndex(readerIndex + actualFrameLength)
    Some(frame)

  }

  private def failIfNecessary(firstDetectionOfTooLongFrame: Boolean) {
    if (bytesToDiscard == 0) {
      this.tooLongFrameLength = 0
      discardingTooLongFrame = false
    }
    else {
    }
  }

  protected def extractFrame(buffer: ByteBuf, index: Int, length: Int): ByteBuf = buffer.retainedSlice(index, length)

  protected def getUnadjustedFrameLength(buf: ByteBuf, offset: Int, length: Int, order: ByteOrder): Long = {
    val buf2 = buf.order(order)
    length match {
      case 1 =>
        buf2.getUnsignedByte(offset)
      case 2 =>
        buf2.getUnsignedShort(offset)
      case 3 =>
        buf2.getUnsignedMedium(offset)
      case 4 =>
        buf2.getUnsignedInt(offset)
      case 8 =>
        buf2.getLong(offset)
      case _ =>
        -1
    }
  }

}
