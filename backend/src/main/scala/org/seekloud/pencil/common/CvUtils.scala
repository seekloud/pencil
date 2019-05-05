package org.seekloud.pencil.common

import java.io.File
import java.nio.ByteBuffer

import opencv_cookbook.OpenCVUtils
import org.bytedeco.javacv.{Frame, OpenCVFrameConverter}
import org.bytedeco.opencv.opencv_core.{Mat, Point, Scalar, Size}
import org.bytedeco.opencv.global.{opencv_imgproc => OpenCvImgProc}
import org.bytedeco.opencv.opencv_core

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

  def resize(srcImg: Mat, dstImg: Mat, dstWidth: Int, dstHeight: Int): Unit = {
    val dSize = new Size(dstWidth, dstHeight)
    OpenCvImgProc.resize(srcImg, dstImg, dSize)
  }


  def drawRectangle(
    img: Mat,
    x: Int,
    y: Int,
    w: Int,
    h: Int,
    color: (Int, Int, Int),
    thickness: Int = 2
  ): Unit = {

    val lineType = OpenCvImgProc.LINE_AA

    val r = color._1
    val g = color._2
    val b = color._3

    OpenCvImgProc.rectangle(
      img,
      new Point(x, y),
      new Point(x + w, y + h),
      new Scalar(b, g, r, 0),
      thickness,
      lineType,
      0
    )

  }


  def putText(
    img: Mat,
    text: String,
    x: Int,
    y: Int,
    size: Int,
    color: (Int, Int, Int),
    fontScale: Double = 1.0,
    thickness: Int = 2,
  ): Unit = {

    val lineType = OpenCvImgProc.LINE_AA

    OpenCvImgProc.putText(
      img,
      text,
      new Point(x, y),
      OpenCvImgProc.FONT_HERSHEY_COMPLEX,
      fontScale,
      new Scalar(color._3, color._2, color._1, 0),
      thickness,
      lineType,
      false
    )
  }


  def main(args: Array[String]): Unit = {
//    testDrawRectangle()
//    testResize()
    testImgFormatChange()
  }


  def testDrawRectangle(): Unit = {
    val img = OpenCVUtils.loadAndShowOrExit(new File("data/image/image01.jpg"))
    drawRectangle(img, 100, 100, 300, 300, (0, 255, 0))
    OpenCVUtils.show(img, "testDrawRectangle")
  }

  def testResize(): Unit = {
    val img = OpenCVUtils.loadAndShowOrExit(new File("data/image/image01.jpg"))
    val dstImg = new Mat()
    resize(img, dstImg, 400, 300)
    OpenCVUtils.show(dstImg, "testResize")
  }

  def testImgFormatChange(): Unit = {
    val matImage: Mat = OpenCVUtils.loadOrExit(new File("data/image/image01.jpg"))

    OpenCVUtils.show(matImage, "in pic")

    val converter = new OpenCVFrameConverter.ToMat()

    val frame: Frame = converter.convert(matImage)

    val matImage2: Mat = converter.convert(frame)
    OpenCVUtils.show(matImage2, "out pic")
  }

}
