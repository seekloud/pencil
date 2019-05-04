package org.seekloud.pencil.core

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{Behavior, PostStop, Signal}
import org.bytedeco.ffmpeg.avutil.AVFrame
import org.bytedeco.ffmpeg.global.{avcodec => FFmpegAvCodec}
import org.bytedeco.ffmpeg.global.{avutil => FFmpegAvUtil}
import org.bytedeco.javacv.{FFmpegFrameRecorder, Frame}
import org.seekloud.pencil.core.RecordActor.RecordCommand

import scala.language.postfixOps
import scala.util.Try
import scala.concurrent.duration._


/**
  * Author: Tao Zhang
  * Date: 5/3/2019
  * Time: 11:44 AM
  */
object RecordActor {

  def apply(
    path: String,
    width: Int,
    height: Int,
    audioChannels: Int,
    pixelFormat: Int = FFmpegAvUtil.AV_PIX_FMT_BGR24,
    frameRate: Int = 30,
    videoBitrate: Int = 1500000,
    videoCodec: Int = FFmpegAvCodec.AV_CODEC_ID_H264
  ): Behavior[RecordCommand] = Behaviors.setup[RecordCommand] { context =>
    Behaviors.withTimers { timers =>
      new RecordActor(
        context,
        timers,
        path,
        width,
        height,
        audioChannels,
        pixelFormat,
        frameRate,
        videoBitrate,
        videoCodec)
    }
  }

  sealed trait RecordCommand

  final case class RecordFrame(originalFrame: Frame, processedFrame: Option[Frame]) extends RecordCommand

  final object NoMoreFrame extends RecordCommand

  private final case class RecordThreadDead(code: Int, message: String = "") extends RecordCommand

  private final object RecordThreadFinish extends RecordCommand

}

class RecordActor(
  context: ActorContext[RecordCommand],
  timers: TimerScheduler[RecordCommand],
  path: String,
  width: Int,
  height: Int,
  audioChannels: Int,
  pixelFormat: Int,
  frameRate: Double = 30.0,
  videoBitrate: Int = 1500000,
  videoCodec: Int = FFmpegAvCodec.AV_CODEC_ID_H264
) extends AbstractBehavior[RecordCommand] {

  import RecordActor._

  private val log = context.log

  private val name = context.self.path.name
  private val self = context.self

  private var recordThreadFinishTask = false
  private var waiterThreadCount = 0

  private val queue = new java.util.concurrent.LinkedBlockingQueue[RecordFrame]()

  private val recordThread = new Thread(() => {

    val recorder = new FFmpegFrameRecorder(path, width, height, audioChannels)
    recorder.setFrameRate(frameRate)
    recorder.setVideoCodec(videoCodec)
    recorder.setVideoBitrate(videoBitrate)
    recorder.start()

    while (!Thread.interrupted()) {

      try {

        val recordFrame = queue.take()
        val imageFrame = recordFrame.processedFrame match {
          case Some(pFrame) => pFrame
          case None => recordFrame.originalFrame
        }
        val audioFrame = recordFrame.originalFrame


//        val pixelFormat0 = imageFrame.opaque match {
//          case frame: AVFrame =>
//            frame.format()
//          case _ =>
//            FFmpegAvUtil.AV_PIX_FMT_NONE
//        }

        if (imageFrame.image != null) {
          recorder.recordImage(
            imageFrame.imageWidth,
            imageFrame.imageHeight,
            imageFrame.imageDepth,
            imageFrame.imageChannels,
            imageFrame.imageStride,
            pixelFormat,
            imageFrame.image: _*)
        }

        if (audioFrame.samples != null) {
          recorder.recordSamples(audioFrame.sampleRate, audioFrame.audioChannels, audioFrame.samples: _*)
        }

      } catch {
        case _: InterruptedException => //ignore.
        case ex: Exception =>
          self ! RecordThreadDead(1, ex.getMessage)
      }
    }

    Try(recorder.close()).failed.map { ex =>
      ex.printStackTrace()
      RecordThreadDead(1, ex.getMessage)
    }

    self ! RecordThreadFinish

  })


  override def onMessage(msg: RecordCommand): Behavior[RecordCommand] = {
    msg match {
      case r: RecordFrame =>
        if (!queue.offer(r))
          log.error("recordActor[{}] offer frame error!", name)
        this
      case RecordThreadFinish =>
        recordThreadFinishTask = true
        this
      case NoMoreFrame =>
        if (recordThreadFinishTask) {
          Behaviors.stopped
        } else {
          if (!recordThread.isInterrupted && queue.isEmpty) {
            recordThread.interrupt()
          }
          waiterThreadCount += 1
          if (waiterThreadCount > 20) {
            log.error("recordActor[{}] waiterThreadCount: {}. maybe some error.", (name, waiterThreadCount))
            Behaviors.stopped
          } else {
            timers.startSingleTimer(NoMoreFrame, NoMoreFrame, 500 millis)
            this
          }
        }
      case RecordThreadDead(_, message) =>
        log.error("recordActor[{}] RecordThreadDead:{}", (name, message))
        Behaviors.stopped
    }
  }

  override def onSignal: PartialFunction[Signal, Behavior[RecordCommand]] = {
    case PostStop =>
      if (!recordThread.isInterrupted) {
        recordThread.interrupt()
      }
      log.info(s"recordActor[{}] stopped", name)
      this
  }
}



