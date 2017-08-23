package com.segmetics.io.codec


import java.nio.ByteBuffer
import java.util

import akka.util.ByteString
import com.segmetics.io.handler.ChannelContext
import io.netty.buffer.{ByteBuf, Unpooled}


/**
  * A decoder that splits the received {@link ByteBuf}s by one or more
  * delimiters.  It is particularly useful for decoding the frames which ends
  * with a delimiter such as {@link Delimiters#nulDelimiter() NUL} or
  * {@linkplain Delimiters#lineDelimiter() newline characters}.
  *
  * <h3>Predefined delimiters</h3>
  * <p>
  * {@link Delimiters} defines frequently used delimiters for convenience' sake.
  *
  * <h3>Specifying more than one delimiter</h3>
  * <p>
  * {@link DelimiterBasedFrameDecoder} allows you to specify more than one
  * delimiter.  If more than one delimiter is found in the buffer, it chooses
  * the delimiter which produces the shortest frame.  For example, if you have
  * the following data in the buffer:
  * <pre>
  * +--------------+
  * | ABC\nDEF\r\n |
  * +--------------+
  * </pre>
  * a {@link DelimiterBasedFrameDecoder}({@link Delimiters#lineDelimiter() Delimiters.lineDelimiter()})
  * will choose {@code '\n'} as the first delimiter and produce two frames:
  * <pre>
  * +-----+-----+
  * | ABC | DEF |
  * +-----+-----+
  * </pre>
  * rather than incorrectly choosing {@code '\r\n'} as the first delimiter:
  * <pre>
  * +----------+
  * | ABC\nDEF |
  * +----------+
  * </pre>
  */
object DelimiterBasedFrameDecoder {
  /** Returns true if the delimiters are "\n" and "\r\n".  */
  private def isLineBased(delimiters: Array[ByteBuf]): Boolean = {
    if (delimiters.length != 2) return false
    var a = delimiters(0)
    var b = delimiters(1)
    if (a.capacity < b.capacity) {
      a = delimiters(1)
      b = delimiters(0)
    }
    a.capacity == 2 && b.capacity == 1 && a.getByte(0) == '\r' && a.getByte(1) == '\n' && b.getByte(0) == '\n'
  }

  /**
    * Returns the number of bytes between the readerIndex of the haystack and
    * the first needle found in the haystack.  -1 is returned if no needle is
    * found in the haystack.
    */
  private def indexOf(haystack: ByteBuf, needle: ByteBuf): Int = {
    var i = haystack.readerIndex
    while (i < haystack.writerIndex) {
      var haystackIndex = i
      var needleIndex = 0
      needleIndex = 0
      try {
        while (needleIndex < needle.capacity) {
          if (haystack.getByte(haystackIndex) != needle.getByte(needleIndex)) throw new Exception("hayStack != needle")
          else {
            haystackIndex += 1
            if (haystackIndex == haystack.writerIndex && needleIndex != needle.capacity - 1) return -1
          }
          needleIndex += 1
        }
      } catch {
        case _: Exception =>
      }
      if (needleIndex == needle.capacity) { // Found the needle from the haystack!
        return i - haystack.readerIndex
      }
      i += 1
    }
    -1
  }

  private def validateDelimiter(delimiter: ByteBuf): Unit = {
    if (delimiter == null) throw new NullPointerException("delimiter")
    if (!delimiter.isReadable) throw new IllegalArgumentException("empty delimiter")
  }

  private def validateMaxFrameLength(maxFrameLength: Int): Unit = {
    if (maxFrameLength <= 0) throw new IllegalArgumentException("maxFrameLength must be a positive integer: " + maxFrameLength)
  }
}

class DelimiterBasedFrameDecoder(val maxFrameLength: Int, val stripDelimiter: Boolean, val failFast: Boolean, val ds: util.List[ByteBuf])
  extends ByteToMessageDecoder {
  DelimiterBasedFrameDecoder.validateMaxFrameLength(maxFrameLength)
  if (ds == null) throw new NullPointerException("delimiters")
  if (ds.size == 0) throw new IllegalArgumentException("empty delimiters")
  this.delimiters = new Array[ByteBuf](ds.size())
  var i = 0
  while (i < ds.size()) {
    val d = ds.get(i)
    DelimiterBasedFrameDecoder.validateDelimiter(d)
    this.delimiters(i) = d.slice(d.readerIndex, d.readableBytes)
    i += 1
  }
  final private var delimiters: Array[ByteBuf] = _
  private var discardingTooLongFrame = false
  private var tooLongFrameLength = 0

  def this(maxFrameLength: Int, stripDelimiter: Boolean, delimiter: ByteBuf) {
    this(maxFrameLength, stripDelimiter, true, util.Arrays.asList(delimiter))
  }

  def this(maxFrameLength: Int, delimiter: ByteBuf) {
    this(maxFrameLength, true, delimiter)
  }

  def this(maxFrameLength: Int, delimiter: Array[Byte]) {
    this(maxFrameLength, Unpooled.wrappedBuffer(delimiter))
  }

  /**
    * Return {@code true} if the current instance is a subclass of DelimiterBasedFrameDecoder
    */
  private def isSubclass = getClass ne classOf[DelimiterBasedFrameDecoder]

  @throws[Exception]
  override final protected def decode(ctx: ChannelContext, in: ByteBuf, out: util.List[Any]): Unit = {
    val decoded = decode(ctx, in)
    if (decoded != null) {
      val bs = Array.ofDim[Byte](decoded.readableBytes())
      val readerIndex = decoded.readerIndex()
      decoded.getBytes(readerIndex, bs)
      out.add(ByteString(bs))
    }
  }

  /**
    * Create a frame out of the {@link ByteBuf} and return it.
    *
    * @param   ctx    the { @link ChannelHandlerContext} which this { @link ByteToMessageDecoder} belongs to
    * @param   buffer the { @link ByteBuf} from which to read data
    * @return frame           the { @link ByteBuf} which represent the frame or { @code null} if no frame could
    *         be created.
    */
  @throws[Exception]
  protected def decode(ctx: ChannelContext, buffer: ByteBuf): ByteBuf = {
    // Try all delimiters and choose the delimiter which yields the shortest frame.
    var minFrameLength = Integer.MAX_VALUE
    var minDelim: ByteBuf = null
    for (delim <- delimiters) {
      val frameLength = DelimiterBasedFrameDecoder.indexOf(buffer, delim)
      if (frameLength >= 0 && frameLength < minFrameLength) {
        minFrameLength = frameLength
        minDelim = delim
      }
    }
    if (minDelim != null) {
      val minDelimLength = minDelim.capacity
      var frame: ByteBuf = null
      if (discardingTooLongFrame) { // We've just finished discarding a very large frame.
        // Go back to the initial state.
        discardingTooLongFrame = false
        buffer.skipBytes(minFrameLength + minDelimLength)
        val tooLongFrameLength = this.tooLongFrameLength
        this.tooLongFrameLength = 0
        if (!failFast) fail(tooLongFrameLength)
        return null
      }
      if (minFrameLength > maxFrameLength) { // Discard read frame.
        buffer.skipBytes(minFrameLength + minDelimLength)
        fail(minFrameLength)
        return null
      }
      if (stripDelimiter) {
        frame = buffer.readRetainedSlice(minFrameLength)
        buffer.skipBytes(minDelimLength)
      }
      else frame = buffer.readRetainedSlice(minFrameLength + minDelimLength)
      frame
    } else {
      if (!discardingTooLongFrame) {
        if (buffer.readableBytes > maxFrameLength) { // Discard the content of the buffer until a delimiter is found.
          tooLongFrameLength = buffer.readableBytes
          buffer.skipBytes(buffer.readableBytes)
          discardingTooLongFrame = true
          if (failFast) fail(tooLongFrameLength)
//        } else { // Still discarding the buffer since a delimiter is not found.
//          tooLongFrameLength += buffer.readableBytes
//          buffer.skipBytes(buffer.readableBytes)
        }
      }
      null
    }
  }

  private def fail(frameLength: Long): Unit = {
    if (frameLength > 0) throw new DecoderException("frame length exceeds " + maxFrameLength + ": " + frameLength + " - discarded")
    else throw new DecoderException("frame length exceeds " + maxFrameLength + " - discarding")
  }
}

