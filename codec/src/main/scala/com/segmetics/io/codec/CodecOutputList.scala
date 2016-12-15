package com.segmetics.io.codec

/**
  * Created by jiaoew on 2016/12/15.
  */

import java.util

import io.netty.util.Recycler
import java.util.AbstractList
import java.util.RandomAccess

import io.netty.util.internal.ObjectUtil.checkNotNull

/**
  * Special {@link AbstractList} implementation which is used within our codec base classes.
  */
object CodecOutputList {
  private val RECYCLER = new Recycler[CodecOutputList]() {
    protected def newObject(handle: Recycler.Handle[CodecOutputList]) = new CodecOutputList(handle)
  }

  private[codec] def newInstance = RECYCLER.get
}

final class CodecOutputList private(val handle: Recycler.Handle[CodecOutputList]) extends util.AbstractList[Any] with RandomAccess {
  private var _size = 0
  // Size of 16 should be good enough for 99 % of all users.
  private var array = new Array[Any](16)
  private var _insertSinceRecycled = false

  def insertSinceRecycled() = _insertSinceRecycled

  def get(index: Int): Any = {
    checkIndex(index)
    array(index)
  }

  def size: Int = _size

  override def add(element: Any): Boolean = {
    checkNotNull(element, "element")
    try {
      insert(size, element)
    } catch {
      case ignore: IndexOutOfBoundsException => {
        // This should happen very infrequently so we just catch the exception and try again.
        expandArray()
        insert(size, element)
      }
    }
    _size += 1
    true
  }

  override def set(index: Int, element: Any): Any = {
    checkNotNull(element, "element")
    checkIndex(index)
    val old = array(index)
    insert(index, element)
    old
  }

  override def add(index: Int, element: Any) {
    checkNotNull(element, "element")
    checkIndex(index)
    if (size == array.length) expandArray()
    if (index != size - 1) System.arraycopy(array, index, array, index + 1, size - index)
    insert(index, element)
    _size += 1
  }

  override def remove(index: Int): Any = {
    checkIndex(index)
    val old = array(index)
    val len = size - index - 1
    if (len > 0) System.arraycopy(array, index + 1, array, index, len)
    _size -= 1
    array(size) = null
    old
  }

  override def clear() {
    // We only set the size to 0 and not null out the array. Null out the array will explicit requested by
    // calling recycle()
    _size = 0
  }

  /**
    * Recycle the array which will clear it and null out all entries in the internal storage.
    */
  private[codec] def recycle() {
    var i = 0
    while (i < size) {
      array(i) = null
      i += 1
    }
    clear()
    _insertSinceRecycled = false
    handle.recycle(this)
  }

  /**
    * Returns the element on the given index. This operation will not do any range-checks and so is considered unsafe.
    */
  private[codec] def getUnsafe(index: Int) = array(index)

  private def checkIndex(index: Int) {
    if (index >= size) throw new IndexOutOfBoundsException
  }

  private def insert(index: Int, element: Any) {
    array(index) = element
    _insertSinceRecycled = true
  }

  private def expandArray() {
    // double capacity
    val newCapacity = array.length << 1
    if (newCapacity < 0) throw new OutOfMemoryError
    val newArray = new Array[Any](newCapacity)
    System.arraycopy(array, 0, newArray, 0, array.length)
    array = newArray
  }
}
