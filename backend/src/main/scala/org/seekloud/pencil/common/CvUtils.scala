package org.seekloud.pencil.common

import java.nio.ByteBuffer

import org.bytedeco.javacv.Frame

/**
  * Author: Tao Zhang
  * Date: 5/1/2019
  * Time: 8:44 PM
  */
object CvUtils {


  def extractImageData(frame: Frame): Array[Byte] = {

    val w = frame.imageWidth
    val h = frame.imageHeight
    val c = frame.imageChannels

    val size = w * h * c

    val buff = frame.image(0).asInstanceOf[ByteBuffer]
    buff.rewind()

    val arr = new Array[Byte](size)
    buff.get(arr)
    arr


  }


  def frameImage2Data(frame: Frame) = {

  }

}
