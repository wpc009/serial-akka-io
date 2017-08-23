package com.segmetics.io.codec

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.SwappedByteBuf
import io.netty.buffer.Unpooled
import io.netty.util.ByteProcessor
import io.netty.util.Signal
import io.netty.util.internal.StringUtil
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.channels.GatheringByteChannel
import java.nio.channels.ScatteringByteChannel
import java.nio.charset.Charset


/**
  * Special {@link ByteBuf} implementation which is used by the {@link ReplayingDecoder}
  */
object ReplayingDecoderByteBuf {
  private val REPLAY = ReplayingDecoder.REPLAY
  private[codec] val EMPTY_BUFFER = new ReplayingDecoderByteBuf(Unpooled.EMPTY_BUFFER)

  private def reject = new UnsupportedOperationException("not a replayable operation")

  try EMPTY_BUFFER.terminate()

}

final class ReplayingDecoderByteBuf private[codec]() extends ByteBuf {
  private var buffer: ByteBuf = _
  private var terminated = false
  private var swapped: SwappedByteBuf = null

  def this(buffer: ByteBuf) {
    this()
    setCumulation(buffer)
  }

  private[codec] def setCumulation(buffer: ByteBuf): Unit = {
    this.buffer = buffer
  }

  private[codec] def terminate(): Unit = {
    terminated = true
  }

  override def capacity: Int = if (terminated) buffer.capacity
  else Integer.MAX_VALUE

  override def capacity(newCapacity: Int) = throw ReplayingDecoderByteBuf.reject

  override def maxCapacity: Int = capacity

  override def alloc: ByteBufAllocator = buffer.alloc

  override def isReadOnly = false

  @SuppressWarnings(Array("deprecation")) override def asReadOnly: ByteBuf = Unpooled.unmodifiableBuffer(this)

  override def isDirect: Boolean = buffer.isDirect

  override def hasArray = false

  override def array = throw new UnsupportedOperationException

  override def arrayOffset = throw new UnsupportedOperationException

  override def hasMemoryAddress = false

  override def memoryAddress = throw new UnsupportedOperationException

  override def clear = throw ReplayingDecoderByteBuf.reject

  override def equals(obj: Any): Boolean = this == obj

  override def compareTo(buffer: ByteBuf) = throw ReplayingDecoderByteBuf.reject

  override def copy = throw ReplayingDecoderByteBuf.reject

  override def copy(index: Int, length: Int): ByteBuf = {
    checkIndex(index, length)
    buffer.copy(index, length)
  }

  override def discardReadBytes = throw ReplayingDecoderByteBuf.reject

  override def ensureWritable(writableBytes: Int) = throw ReplayingDecoderByteBuf.reject

  override def ensureWritable(minWritableBytes: Int, force: Boolean) = throw ReplayingDecoderByteBuf.reject

  override def duplicate = throw ReplayingDecoderByteBuf.reject

  override def retainedDuplicate = throw ReplayingDecoderByteBuf.reject

  override def getBoolean(index: Int): Boolean = {
    checkIndex(index, 1)
    buffer.getBoolean(index)
  }

  override def getByte(index: Int): Byte = {
    checkIndex(index, 1)
    buffer.getByte(index)
  }

  override def getUnsignedByte(index: Int): Short = {
    checkIndex(index, 1)
    buffer.getUnsignedByte(index)
  }

  override def getBytes(index: Int, dst: Array[Byte], dstIndex: Int, length: Int): ByteBuf = {
    checkIndex(index, length)
    buffer.getBytes(index, dst, dstIndex, length)
    this
  }

  override def getBytes(index: Int, dst: Array[Byte]): ByteBuf = {
    checkIndex(index, dst.length)
    buffer.getBytes(index, dst)
    this
  }

  override def getBytes(index: Int, dst: ByteBuffer) = throw ReplayingDecoderByteBuf.reject

  override def getBytes(index: Int, dst: ByteBuf, dstIndex: Int, length: Int): ByteBuf = {
    checkIndex(index, length)
    buffer.getBytes(index, dst, dstIndex, length)
    this
  }

  override def getBytes(index: Int, dst: ByteBuf, length: Int) = throw ReplayingDecoderByteBuf.reject

  override def getBytes(index: Int, dst: ByteBuf) = throw ReplayingDecoderByteBuf.reject

  override def getBytes(index: Int, out: GatheringByteChannel, length: Int) = throw ReplayingDecoderByteBuf.reject

  override def getBytes(index: Int, out: FileChannel, position: Long, length: Int) = throw ReplayingDecoderByteBuf.reject

  override def getBytes(index: Int, out: OutputStream, length: Int) = throw ReplayingDecoderByteBuf.reject

  override def getInt(index: Int): Int = {
    checkIndex(index, 4)
    buffer.getInt(index)
  }

  override def getIntLE(index: Int): Int = {
    checkIndex(index, 4)
    buffer.getIntLE(index)
  }

  override def getUnsignedInt(index: Int): Long = {
    checkIndex(index, 4)
    buffer.getUnsignedInt(index)
  }

  override def getUnsignedIntLE(index: Int): Long = {
    checkIndex(index, 4)
    buffer.getUnsignedIntLE(index)
  }

  override def getLong(index: Int): Long = {
    checkIndex(index, 8)
    buffer.getLong(index)
  }

  override def getLongLE(index: Int): Long = {
    checkIndex(index, 8)
    buffer.getLongLE(index)
  }

  override def getMedium(index: Int): Int = {
    checkIndex(index, 3)
    buffer.getMedium(index)
  }

  override def getMediumLE(index: Int): Int = {
    checkIndex(index, 3)
    buffer.getMediumLE(index)
  }

  override def getUnsignedMedium(index: Int): Int = {
    checkIndex(index, 3)
    buffer.getUnsignedMedium(index)
  }

  override def getUnsignedMediumLE(index: Int): Int = {
    checkIndex(index, 3)
    buffer.getUnsignedMediumLE(index)
  }

  override def getShort(index: Int): Short = {
    checkIndex(index, 2)
    buffer.getShort(index)
  }

  override def getShortLE(index: Int): Short = {
    checkIndex(index, 2)
    buffer.getShortLE(index)
  }

  override def getUnsignedShort(index: Int): Int = {
    checkIndex(index, 2)
    buffer.getUnsignedShort(index)
  }

  override def getUnsignedShortLE(index: Int): Int = {
    checkIndex(index, 2)
    buffer.getUnsignedShortLE(index)
  }

  override def getChar(index: Int): Char = {
    checkIndex(index, 2)
    buffer.getChar(index)
  }

  override def getFloat(index: Int): Float = {
    checkIndex(index, 4)
    buffer.getFloat(index)
  }

  override def getDouble(index: Int): Double = {
    checkIndex(index, 8)
    buffer.getDouble(index)
  }

  override def getCharSequence(index: Int, length: Int, charset: Charset): CharSequence = {
    checkIndex(index, length)
    buffer.getCharSequence(index, length, charset)
  }

  override def hashCode = throw ReplayingDecoderByteBuf.reject

  override def indexOf(fromIndex: Int, toIndex: Int, value: Byte): Int = {
    if (fromIndex == toIndex) return -1
    if (Math.max(fromIndex, toIndex) > buffer.writerIndex) throw ReplayingDecoderByteBuf.REPLAY
    buffer.indexOf(fromIndex, toIndex, value)
  }

  override def bytesBefore(value: Byte): Int = {
    val bytes = buffer.bytesBefore(value)
    if (bytes < 0) throw ReplayingDecoderByteBuf.REPLAY
    bytes
  }

  override def bytesBefore(length: Int, value: Byte): Int = bytesBefore(buffer.readerIndex, length, value)

  override def bytesBefore(index: Int, length: Int, value: Byte): Int = {
    val writerIndex = buffer.writerIndex
    if (index >= writerIndex) throw ReplayingDecoderByteBuf.REPLAY
    if (index <= writerIndex - length) return buffer.bytesBefore(index, length, value)
    val res = buffer.bytesBefore(index, writerIndex - index, value)
    if (res < 0) throw ReplayingDecoderByteBuf.REPLAY
    else res
  }

  override def forEachByte(processor: ByteProcessor): Int = {
    val ret = buffer.forEachByte(processor)
    if (ret < 0) throw ReplayingDecoderByteBuf.REPLAY
    else ret
  }

  override def forEachByte(index: Int, length: Int, processor: ByteProcessor): Int = {
    val writerIndex = buffer.writerIndex
    if (index >= writerIndex) throw ReplayingDecoderByteBuf.REPLAY
    if (index <= writerIndex - length) return buffer.forEachByte(index, length, processor)
    val ret = buffer.forEachByte(index, writerIndex - index, processor)
    if (ret < 0) throw ReplayingDecoderByteBuf.REPLAY
    else ret
  }

  override def forEachByteDesc(processor: ByteProcessor): Int = if (terminated) buffer.forEachByteDesc(processor)
  else throw ReplayingDecoderByteBuf.reject

  override def forEachByteDesc(index: Int, length: Int, processor: ByteProcessor): Int = {
    if (index + length > buffer.writerIndex) throw ReplayingDecoderByteBuf.REPLAY
    buffer.forEachByteDesc(index, length, processor)
  }

  override def markReaderIndex: ByteBuf = {
    buffer.markReaderIndex
    this
  }

  override def markWriterIndex = throw ReplayingDecoderByteBuf.reject

  override def order: ByteOrder = buffer.order

  override def order(endianness: ByteOrder): ByteBuf = {
    if (endianness == null) throw new NullPointerException("endianness")
    if (endianness eq order) return this
    var swapped = this.swapped
    if (swapped == null) {
      swapped = new SwappedByteBuf(this)
      this.swapped = swapped
    }
    swapped
  }

  override def isReadable: Boolean = if (terminated) buffer.isReadable
    else true

  override def isReadable(size: Int): Boolean = if (terminated) buffer.isReadable(size)
    else true

  override def readableBytes: Int = if (terminated) buffer.readableBytes
  else Integer.MAX_VALUE - buffer.readerIndex

  override def readBoolean: Boolean = {
    checkReadableBytes(1)
    buffer.readBoolean
  }

  override def readByte: Byte = {
    checkReadableBytes(1)
    buffer.readByte
  }

  override def readUnsignedByte: Short = {
    checkReadableBytes(1)
    buffer.readUnsignedByte
  }

  override def readBytes(dst: Array[Byte], dstIndex: Int, length: Int): ByteBuf = {
    checkReadableBytes(length)
    buffer.readBytes(dst, dstIndex, length)
    this
  }

  override def readBytes(dst: Array[Byte]): ByteBuf = {
    checkReadableBytes(dst.length)
    buffer.readBytes(dst)
    this
  }

  override def readBytes(dst: ByteBuffer) = throw ReplayingDecoderByteBuf.reject

  override def readBytes(dst: ByteBuf, dstIndex: Int, length: Int): ByteBuf = {
    checkReadableBytes(length)
    buffer.readBytes(dst, dstIndex, length)
    this
  }

  override def readBytes(dst: ByteBuf, length: Int) = throw ReplayingDecoderByteBuf.reject

  override def readBytes(dst: ByteBuf): ByteBuf = {
    checkReadableBytes(dst.writableBytes)
    buffer.readBytes(dst)
    this
  }

  override def readBytes(out: GatheringByteChannel, length: Int) = throw ReplayingDecoderByteBuf.reject

  override def readBytes(out: FileChannel, position: Long, length: Int) = throw ReplayingDecoderByteBuf.reject

  override def readBytes(length: Int): ByteBuf = {
    checkReadableBytes(length)
    buffer.readBytes(length)
  }

  override def readSlice(length: Int): ByteBuf = {
    checkReadableBytes(length)
    buffer.readSlice(length)
  }

  override def readRetainedSlice(length: Int): ByteBuf = {
    checkReadableBytes(length)
    buffer.readRetainedSlice(length)
  }

  override def readBytes(out: OutputStream, length: Int) = throw ReplayingDecoderByteBuf.reject

  override def readerIndex: Int = buffer.readerIndex

  override def readerIndex(readerIndex: Int): ByteBuf = {
    buffer.readerIndex(readerIndex)
    this
  }

  override def readInt: Int = {
    checkReadableBytes(4)
    buffer.readInt
  }

  override def readIntLE: Int = {
    checkReadableBytes(4)
    buffer.readIntLE
  }

  override def readUnsignedInt: Long = {
    checkReadableBytes(4)
    buffer.readUnsignedInt
  }

  override def readUnsignedIntLE: Long = {
    checkReadableBytes(4)
    buffer.readUnsignedIntLE
  }

  override def readLong: Long = {
    checkReadableBytes(8)
    buffer.readLong
  }

  override def readLongLE: Long = {
    checkReadableBytes(8)
    buffer.readLongLE
  }

  override def readMedium: Int = {
    checkReadableBytes(3)
    buffer.readMedium
  }

  override def readMediumLE: Int = {
    checkReadableBytes(3)
    buffer.readMediumLE
  }

  override def readUnsignedMedium: Int = {
    checkReadableBytes(3)
    buffer.readUnsignedMedium
  }

  override def readUnsignedMediumLE: Int = {
    checkReadableBytes(3)
    buffer.readUnsignedMediumLE
  }

  override def readShort: Short = {
    checkReadableBytes(2)
    buffer.readShort
  }

  override def readShortLE: Short = {
    checkReadableBytes(2)
    buffer.readShortLE
  }

  override def readUnsignedShort: Int = {
    checkReadableBytes(2)
    buffer.readUnsignedShort
  }

  override def readUnsignedShortLE: Int = {
    checkReadableBytes(2)
    buffer.readUnsignedShortLE
  }

  override def readChar: Char = {
    checkReadableBytes(2)
    buffer.readChar
  }

  override def readFloat: Float = {
    checkReadableBytes(4)
    buffer.readFloat
  }

  override def readDouble: Double = {
    checkReadableBytes(8)
    buffer.readDouble
  }

  override def readCharSequence(length: Int, charset: Charset): CharSequence = {
    checkReadableBytes(length)
    buffer.readCharSequence(length, charset)
  }

  override def resetReaderIndex: ByteBuf = {
    buffer.resetReaderIndex
    this
  }

  override def resetWriterIndex = throw ReplayingDecoderByteBuf.reject

  override def setBoolean(index: Int, value: Boolean) = throw ReplayingDecoderByteBuf.reject

  override def setByte(index: Int, value: Int) = throw ReplayingDecoderByteBuf.reject

  override def setBytes(index: Int, src: Array[Byte], srcIndex: Int, length: Int) = throw ReplayingDecoderByteBuf.reject

  override def setBytes(index: Int, src: Array[Byte]) = throw ReplayingDecoderByteBuf.reject

  override def setBytes(index: Int, src: ByteBuffer) = throw ReplayingDecoderByteBuf.reject

  override def setBytes(index: Int, src: ByteBuf, srcIndex: Int, length: Int) = throw ReplayingDecoderByteBuf.reject

  override def setBytes(index: Int, src: ByteBuf, length: Int) = throw ReplayingDecoderByteBuf.reject

  override def setBytes(index: Int, src: ByteBuf) = throw ReplayingDecoderByteBuf.reject

  override def setBytes(index: Int, in: InputStream, length: Int) = throw ReplayingDecoderByteBuf.reject

  override def setZero(index: Int, length: Int) = throw ReplayingDecoderByteBuf.reject

  override def setBytes(index: Int, in: ScatteringByteChannel, length: Int) = throw ReplayingDecoderByteBuf.reject

  override def setBytes(index: Int, in: FileChannel, position: Long, length: Int) = throw ReplayingDecoderByteBuf.reject

  override def setIndex(readerIndex: Int, writerIndex: Int) = throw ReplayingDecoderByteBuf.reject

  override def setInt(index: Int, value: Int) = throw ReplayingDecoderByteBuf.reject

  override def setIntLE(index: Int, value: Int) = throw ReplayingDecoderByteBuf.reject

  override def setLong(index: Int, value: Long) = throw ReplayingDecoderByteBuf.reject

  override def setLongLE(index: Int, value: Long) = throw ReplayingDecoderByteBuf.reject

  override def setMedium(index: Int, value: Int) = throw ReplayingDecoderByteBuf.reject

  override def setMediumLE(index: Int, value: Int) = throw ReplayingDecoderByteBuf.reject

  override def setShort(index: Int, value: Int) = throw ReplayingDecoderByteBuf.reject

  override def setShortLE(index: Int, value: Int) = throw ReplayingDecoderByteBuf.reject

  override def setChar(index: Int, value: Int) = throw ReplayingDecoderByteBuf.reject

  override def setFloat(index: Int, value: Float) = throw ReplayingDecoderByteBuf.reject

  override def setDouble(index: Int, value: Double) = throw ReplayingDecoderByteBuf.reject

  override def skipBytes(length: Int): ByteBuf = {
    checkReadableBytes(length)
    buffer.skipBytes(length)
    this
  }

  override def slice = throw ReplayingDecoderByteBuf.reject

  override def retainedSlice = throw ReplayingDecoderByteBuf.reject

  override def slice(index: Int, length: Int): ByteBuf = {
    checkIndex(index, length)
    buffer.slice(index, length)
  }

  override def retainedSlice(index: Int, length: Int): ByteBuf = {
    checkIndex(index, length)
    buffer.slice(index, length)
  }

  override def nioBufferCount: Int = buffer.nioBufferCount

  override def nioBuffer = throw ReplayingDecoderByteBuf.reject

  override def nioBuffer(index: Int, length: Int): ByteBuffer = {
    checkIndex(index, length)
    buffer.nioBuffer(index, length)
  }

  override def nioBuffers = throw ReplayingDecoderByteBuf.reject

  override def nioBuffers(index: Int, length: Int): Array[ByteBuffer] = {
    checkIndex(index, length)
    buffer.nioBuffers(index, length)
  }

  override def internalNioBuffer(index: Int, length: Int): ByteBuffer = {
    checkIndex(index, length)
    buffer.internalNioBuffer(index, length)
  }

  override def toString(index: Int, length: Int, charset: Charset): String = {
    checkIndex(index, length)
    buffer.toString(index, length, charset)
  }

  override def toString(charsetName: Charset) = throw ReplayingDecoderByteBuf.reject

  override def toString: String = StringUtil.simpleClassName(this) + '(' + "ridx=" + readerIndex + ", " + "widx=" + writerIndex + ')'

  override def isWritable = false

  override def isWritable(size: Int) = false

  override def writableBytes = 0

  override def maxWritableBytes = 0

  override def writeBoolean(value: Boolean) = throw ReplayingDecoderByteBuf.reject

  override def writeByte(value: Int) = throw ReplayingDecoderByteBuf.reject

  override def writeBytes(src: Array[Byte], srcIndex: Int, length: Int) = throw ReplayingDecoderByteBuf.reject

  override def writeBytes(src: Array[Byte]) = throw ReplayingDecoderByteBuf.reject

  override def writeBytes(src: ByteBuffer) = throw ReplayingDecoderByteBuf.reject

  override def writeBytes(src: ByteBuf, srcIndex: Int, length: Int) = throw ReplayingDecoderByteBuf.reject

  override def writeBytes(src: ByteBuf, length: Int) = throw ReplayingDecoderByteBuf.reject

  override def writeBytes(src: ByteBuf) = throw ReplayingDecoderByteBuf.reject

  override def writeBytes(in: InputStream, length: Int) = throw ReplayingDecoderByteBuf.reject

  override def writeBytes(in: ScatteringByteChannel, length: Int) = throw ReplayingDecoderByteBuf.reject

  override def writeBytes(in: FileChannel, position: Long, length: Int) = throw ReplayingDecoderByteBuf.reject

  override def writeInt(value: Int) = throw ReplayingDecoderByteBuf.reject

  override def writeIntLE(value: Int) = throw ReplayingDecoderByteBuf.reject

  override def writeLong(value: Long) = throw ReplayingDecoderByteBuf.reject

  override def writeLongLE(value: Long) = throw ReplayingDecoderByteBuf.reject

  override def writeMedium(value: Int) = throw ReplayingDecoderByteBuf.reject

  override def writeMediumLE(value: Int) = throw ReplayingDecoderByteBuf.reject

  override def writeZero(length: Int) = throw ReplayingDecoderByteBuf.reject

  override def writerIndex: Int = buffer.writerIndex

  override def writerIndex(writerIndex: Int) = throw ReplayingDecoderByteBuf.reject

  override def writeShort(value: Int) = throw ReplayingDecoderByteBuf.reject

  override def writeShortLE(value: Int) = throw ReplayingDecoderByteBuf.reject

  override def writeChar(value: Int) = throw ReplayingDecoderByteBuf.reject

  override def writeFloat(value: Float) = throw ReplayingDecoderByteBuf.reject

  override def writeDouble(value: Double) = throw ReplayingDecoderByteBuf.reject

  override def setCharSequence(index: Int, sequence: CharSequence, charset: Charset) = throw ReplayingDecoderByteBuf.reject

  override def writeCharSequence(sequence: CharSequence, charset: Charset) = throw ReplayingDecoderByteBuf.reject

  private def checkIndex(index: Int, length: Int): Unit = {
    if (index + length > buffer.writerIndex) throw ReplayingDecoderByteBuf.REPLAY
  }

  private def checkReadableBytes(readableBytes: Int): Unit = {
    if (buffer.readableBytes < readableBytes) throw ReplayingDecoderByteBuf.REPLAY
  }

  override def discardSomeReadBytes = throw ReplayingDecoderByteBuf.reject

  override def refCnt: Int = buffer.refCnt

  override def retain = throw ReplayingDecoderByteBuf.reject

  override def retain(increment: Int) = throw ReplayingDecoderByteBuf.reject

  override def touch: ByteBuf = {
    buffer.touch
    this
  }

  override def touch(hint: Any): ByteBuf = {
    buffer.touch(hint)
    this
  }

  override def release = throw ReplayingDecoderByteBuf.reject

  override def release(decrement: Int) = throw ReplayingDecoderByteBuf.reject

  override def unwrap = throw ReplayingDecoderByteBuf.reject
}

