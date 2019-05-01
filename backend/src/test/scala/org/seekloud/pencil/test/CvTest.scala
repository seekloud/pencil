package org.seekloud.pencil.test

import org.bytedeco.javacv.OpenCVFrameConverter
import org.bytedeco.opencv.opencv_core.{Mat, Scalar, Size}
import org.bytedeco.opencv.global.{opencv_core => cvCore}
import org.seekloud.UnitSpec
import org.seekloud.pencil.common.CvUtils

/**
  * Author: Tao Zhang
  * Date: 5/1/2019
  * Time: 9:14 PM
  */
class CvTest extends UnitSpec {


  "CvUtils" should "extract byte array from frame correctly" in {

    val c = 3
    val w = 300
    val h = 300

    val b = 156
    val g = 243
    val r = 103

    val size = w * h * c
    val expectedArray = new Array[Byte](size)
    for( i <- 0 until size) {
      expectedArray(i) = i % 3 match {
        case 0 => b.toByte
        case 1 => g.toByte
        case 2 => r.toByte
      }
    }

    val mat =
      new Mat(
        new Size(w, h),
        cvCore.CV_8UC3,
        new Scalar(b, g, r, 0)
      )

    val converter = new OpenCVFrameConverter.ToMat()
    val frame = converter.convert(mat)

    val resultArray = CvUtils.extractImageData(frame)

    val resultArray2 = new Array[Byte](size)
    val resultArray3 = CvUtils.extractImageData(frame, resultArray2)


//    println("arr1:" + resultArray.mkString(","))
//    println("arr2:" + expectedArray.mkString(","))

    assert( resultArray.sameElements(expectedArray) )
    assert( resultArray2.sameElements(expectedArray) )
    assert( resultArray2.eq(resultArray3) )
  }


}
