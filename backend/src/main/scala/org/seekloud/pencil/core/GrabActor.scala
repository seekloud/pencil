package org.seekloud.pencil.core

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, DispatcherSelector, PostStop, Props, Signal, SupervisorStrategy}
import org.bytedeco.javacv.{FFmpegFrameGrabber, Frame}
import org.seekloud.pencil.core.GrabActor.{GrabCommand, GrabEvent}

import scala.util.{Failure, Success, Try}

/**
  * Author: Tao Zhang
  * Date: 5/2/2019
  * Time: 9:03 AM
  */
object GrabActor {

  def apply(
    context: ActorContext[GrabCommand],
    targetPath: String,
    frameCollector: ActorRef[GrabEvent]
  ): Behavior[GrabCommand] = Behaviors.setup[GrabCommand] { context =>
    new GrabActor(context, targetPath, frameCollector)
  }

  sealed trait GrabCommand

  final object StopGrab extends GrabCommand

  private final case class GrabThreadDead(code: Int, message: String = "") extends GrabCommand


  sealed trait GrabEvent

  final case class GrabError(actorName: String, msg: String, ex: Throwable) extends GrabEvent

  final case class GrabFinish(actorName: String) extends GrabEvent

  final case class GrabInformation(
    actorName: String,
    width: Int,
    height: Int,
    pixelFormat: Int,
    frameRate: Double,
    videoCodec: Int,
    videoBitrate: Int,
    audioChannels: Int,
    audioCodec: Int
  ) extends GrabEvent with GrabCommand

  final case class GrabbedFrame(actorName: String, num: Int, grabTs: Long, frame: Frame) extends GrabEvent


  def worker(
    targetPath: String,
    parent: ActorRef[GrabCommand],
    frameCollector: ActorRef[GrabEvent]
  ): Behavior[Nothing] =
    Behaviors.setup { context =>

      val grabber = new FFmpegFrameGrabber(targetPath)
      val parentName = parent.path.name
      val log = context.log

      Behaviors.receiveMessage[Nothing] { _ =>
        Try(grabber.start()) match {
          case Success(_) =>
            val info = GrabInformation(
              parentName,
              grabber.getImageWidth,
              grabber.getImageHeight,
              grabber.getPixelFormat,
              grabber.getVideoFrameRate,
              grabber.getVideoCodec,
              grabber.getVideoBitrate,
              grabber.getAudioChannels,
              grabber.getAudioCodec
            )

            parent ! info
            frameCollector ! info

            var count = 0
            var grabTs = System.currentTimeMillis()
            Try{
              var frame = grabber.grab()
              while (frame != null) {
                val msg = GrabbedFrame(parentName, count, grabTs, frame)
                frameCollector ! msg
                count += 1
                grabTs = System.currentTimeMillis()
                frame = grabber.grab()
              }
            } match {
              case Success(_) =>
                frameCollector ! GrabFinish(parentName)
                Behaviors.stopped
              case Failure(ex) =>
                frameCollector ! GrabError(parentName, "grab error", ex)
                Behaviors.stopped
            }
          case Failure(ex) =>
            frameCollector ! GrabError(parentName, s"can not start grab: $targetPath", ex)
            Behaviors.stopped
        }

      }.receiveSignal {
        case (_, PostStop) =>
          Try(grabber.close()).failed.map{ex =>
            ex.printStackTrace()
            log.warning("GrabActor.worker grab close error:{}", ex.getMessage)
          }
          log.info("GrabActor.worker stopped")
          Behaviors.same
      }
    }


}

class GrabActor(
  context: ActorContext[GrabCommand],
  targetPath: String,
  frameCollector: ActorRef[GrabEvent]
) extends AbstractBehavior[GrabCommand] {

  import GrabActor._

  private val log = context.log
  private val name = context.self.path.name
  private val self = context.self
  private var grabInfo: Option[GrabInformation] = None

  //TODO tmp test
  //  val worker = context.spawn(GrabActor.worker(), "aa", DispatcherSelector.fromConfig(""))
  //  val worker1 = context.spawn(GrabActor.worker(), "aa")

  val grabThread = new Thread(() => {
    val grabber = new FFmpegFrameGrabber(targetPath)

    Try(grabber.start()) match {
      case Failure(ex) =>
        ex.printStackTrace()
        val msg = s"GrabActor[$name] can't start grabber for [$targetPath]"
        context.self ! GrabThreadDead(1, msg)
        frameCollector ! GrabError(name, s"can not start grab: $targetPath", ex)
      case Success(_) =>
        Try {
          var count = 0
          var grabTs = System.currentTimeMillis()

          val info = GrabInformation(
            name,
            grabber.getImageWidth,
            grabber.getImageHeight,
            grabber.getPixelFormat,
            grabber.getVideoFrameRate,
            grabber.getVideoCodec,
            grabber.getVideoBitrate,
            grabber.getAudioChannels,
            grabber.getAudioCodec
          )
          self ! info
          frameCollector ! info
          var frame = grabber.grab()
          while (!Thread.interrupted() && frame != null) {
            val msg = GrabbedFrame(name, count, grabTs, frame)
            frameCollector ! msg
            count += 1
            grabTs = System.currentTimeMillis()
            frame = grabber.grab()
          }
          if (frame == null) {
            context.self ! GrabThreadDead(0)
            frameCollector ! GrabFinish(name)
          }
        }.failed.map { ex =>
          ex.printStackTrace()
          val msg = s"GrabActor[$name] grab error [${ex.getMessage}]"
          context.self ! GrabThreadDead(2, msg)
          frameCollector ! GrabError(name, "grab error", ex)
        }

        Try(grabber.close()).failed.map { ex =>
          ex.printStackTrace()
          GrabThreadDead(1, ex.getMessage)
        }

    }
  })

  grabThread.start()

  override def onMessage(msg: GrabCommand): Behavior[GrabCommand] = {
    msg match {
      case info: GrabInformation =>
        grabInfo = Some(info)
        this
      case StopGrab =>
        grabThread.interrupt()
        Behaviors.stopped
      case GrabThreadDead(code, message) =>
        if (code != 0) {
          log.warning(message)
        }
        Behaviors.stopped
    }
  }

  override def onSignal: PartialFunction[Signal, Behavior[GrabCommand]] = {
    case PostStop =>
      if (!grabThread.isInterrupted) {
        grabThread.interrupt()
      }
      log.info(s"grabActor[{}] stopped", name)
      this
  }

}











