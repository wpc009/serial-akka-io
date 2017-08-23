package com.segmetics.io.codec

case class DecoderException(message: String, cause: Throwable = null) extends Exception
