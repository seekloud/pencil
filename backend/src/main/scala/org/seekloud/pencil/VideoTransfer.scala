package org.seekloud.pencil

import java.io.File

import org.bytedeco.javacv.{FFmpegFrameGrabber, FFmpegFrameRecorder}


/**
  * Author: Tao Zhang
  * Date: 4/27/2019
  * Time: 6:40 PM
  */
object VideoTransfer {


  def main(args: Array[String]): Unit = {


    val srcFile = "D:/workstation/sbt/HelloJavaCV/data/video/VID_20181123_214724.mp4"
    val outDir = new File("D:\\workstation\\sbt\\HelloJavaCV\\data\\out")

    val outFile = "rec001.mp4"
    val outTarget = new File(outDir, outFile)



    val grabber = new FFmpegFrameGrabber(srcFile)
    grabber.start()

    val pixelFormat = grabber.getPixelFormat

    val recorder =
      new FFmpegFrameRecorder(
        outTarget,
        grabber.getImageWidth / 2,
        grabber.getImageHeight / 2,
        grabber.getAudioChannels
      )

    recorder.setInterleaved(true)
    //recorder.setPixelFormat(pixelFormat)
    recorder.setFrameRate(grabber.getFrameRate)

    //recorder.setVideoOption("crf", "25")
    // 2000 kb/s, 720P视频的合理比特率范围
    recorder.setVideoBitrate(2000000)
    //recorder.setVideoBitrate(grabber.getVideoBitrate)
    //println(s"getVideoBitrate: ${grabber.getVideoBitrate}")
    // h264编/解码器
    //recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264)
    recorder.setVideoCodec(grabber.getVideoCodec)
    println(s"getVideoCodec: ${grabber.getVideoCodec}")


    recorder.start()

    var frame = grabber.grabFrame(true, true, true, false)

    var lastTs = frame.timestamp.toDouble / 1000
    var currentTime = System.nanoTime()
    var frameCount = 0

    val beginTime = System.currentTimeMillis()

    while (frame != null) {
      frameCount += 1
      recorder.record(frame)

//      if (frame.image != null) {
//        recorder.recordImage(
//          frame.imageWidth,
//          frame.imageHeight,
//          frame.imageDepth,
//          frame.imageChannels,
//          frame.imageStride,
//          pixelFormat,
//          frame.image: _*)
//      }

      //recorder.recordSamples(frame.samples: _*)
      frame = grabber.grab()
      if (frame != null) {
        val ts = frame.timestamp.toDouble / 1000
        val ct = System.nanoTime()
        val d1 = ts - lastTs
        val d2 = (ct - currentTime) / 1000000
        println(s"w: ${frame.imageWidth}, h: ${frame.imageHeight}")
        println(s"fc=$frameCount, d1= $d1 ms, d2= $d2 ms ")
        lastTs = ts
        currentTime = ct
      }
    }

    grabber.close()
    recorder.close()


    println(s"DONE, total time: ${System.currentTimeMillis() - beginTime}")

  }

}
