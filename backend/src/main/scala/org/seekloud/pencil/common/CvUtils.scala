package org.seekloud.pencil.common

import java.nio.ByteBuffer

import org.bytedeco.javacv.Frame

/**
  * Author: Tao Zhang
  * Date: 5/1/2019
  * Time: 8:44 PM
  */
object CvUtils {

  def extractImageData(frame: Frame, dstArray: Array[Byte] = null): Array[Byte] = {
    val w = frame.imageWidth
    val h = frame.imageHeight
    val c = frame.imageChannels
    val size = w * h * c

    val arr =
      if (dstArray == null) {
        new Array[Byte](size)
      } else {
        assert(size == dstArray.length)
        dstArray
      }

    val buff = frame.image(0).asInstanceOf[ByteBuffer]
    buff.rewind()
    buff.get(arr)
    arr
  }

}
