package org.seekloud.pencil.core

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}
import org.bytedeco.javacv.{FFmpegFrameGrabber, Frame}
import org.seekloud.pencil.Boot
import org.seekloud.pencil.core.GrabActor.{GrabCommand, GrabEvent}

import scala.util.{Failure, Success, Try}

/**
  * Author: Tao Zhang
  * Date: 5/2/2019
  * Time: 9:03 AM
  */
object GrabActor {

  def apply(
    targetPath: String,
    frameCollector: ActorRef[GrabEvent]
  ): Behavior[GrabCommand] = Behaviors.setup[GrabCommand] { context =>
    new GrabActor(context, targetPath, frameCollector)
  }

  sealed trait GrabCommand

  final object StopGrab extends GrabCommand with WorkerCommand

  private final object GrabWorkerStopped extends GrabCommand


  sealed trait GrabEvent

  final case class GrabError(actorName: String, msg: String, ex: Throwable) extends GrabEvent

  final case class GrabFinish(actorName: String) extends GrabEvent

  sealed trait WorkerCommand

  private final object NextGrab extends WorkerCommand

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
  ): Behavior[WorkerCommand] =
    Behaviors.setup { context =>

      val grabber = new FFmpegFrameGrabber(targetPath)
      val parentName = parent.path.name
      val log = context.log

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
          context.self ! NextGrab
        case Failure(ex) =>
          frameCollector ! GrabError(parentName, s"can not start grab: $targetPath", ex)
          context.self ! StopGrab
      }

      var count = 0

      Behaviors.receiveMessage[WorkerCommand] { msg =>
        context.self ! NextGrab
        msg match {
          case NextGrab =>
            count += 1
            val grabTs = System.currentTimeMillis()
            Try(grabber.grab()) match {
              case Success(null) =>
                frameCollector ! GrabFinish(parentName)
                context.self ! StopGrab
                Behaviors.same
              case Success(frame) =>
                frameCollector ! GrabbedFrame(parentName, count, grabTs, frame)
                context.self ! NextGrab
                Behaviors.same
              case Failure(ex) =>
                frameCollector ! GrabError(parentName, "grab error", ex)
                Behaviors.stopped
            }
          case StopGrab =>
            frameCollector ! GrabFinish(parentName)
            Behaviors.stopped
        }
      }.receiveSignal {
        case (_, PostStop) =>
          Try(grabber.close()).failed.map { ex =>
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
  private var grabInfo: Option[GrabInformation] = None

  private val worker = context.spawn(
    GrabActor.worker(targetPath, context.self, frameCollector),
    "worker",
    Boot.blockingDispatcher)

  context.watchWith(worker, GrabWorkerStopped)

  override def onMessage(msg: GrabCommand): Behavior[GrabCommand] = {
    msg match {
      case info: GrabInformation =>
        grabInfo = Some(info)
        this
      case StopGrab =>
        worker ! StopGrab
        Behaviors.stopped
      case GrabWorkerStopped =>

        Behaviors.stopped
    }
  }

  override def onSignal: PartialFunction[Signal, Behavior[GrabCommand]] = {
    case PostStop =>
      log.info(s"grabActor[{}] stopped", name)
      this
  }

}











