package org.seekloud.pencil.common

import java.nio.ByteBuffer

import org.bytedeco.javacv.Frame
import org.bytedeco.opencv.opencv_core.Mat

/**
  * Author: Tao Zhang
  * Date: 5/1/2019
  * Time: 8:44 PM
  */
object CvUtils {

  @inline
  final def getDstArray(dst: Array[Byte], size: Int): Array[Byte] = {
    if (dst == null) {
      new Array[Byte](size)
    } else {
      assert(size == dst.length)
      dst
    }
  }

  final def extractImageData(frame: Frame, dstArray: Array[Byte] = null): Array[Byte] = {
    val w = frame.imageWidth
    val h = frame.imageHeight
    val c = frame.imageChannels
    val size = w * h * c

    val arr = getDstArray(dstArray, size)

    val buff = frame.image(0).asInstanceOf[ByteBuffer]
    buff.rewind()
    buff.get(arr)
    arr
  }


  final def extractMatData(mat: Mat, dstArray: Array[Byte] = null): Array[Byte] = {
    val w = mat.cols()
    val h = mat.rows()
    val c = mat.channels()
    val size = w * h * c

    val arr = getDstArray(dstArray, size)

    val buff = mat.createBuffer().asInstanceOf[ByteBuffer]
    buff.rewind()
    buff.get(arr)
    arr
  }

}
