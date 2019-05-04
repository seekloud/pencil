package org.seekloud.pencil.core

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}
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

  final case class GrabbedFrame(actorName: String, num: Int, grabTs: Long, frame: Frame) extends GrabEvent

}

class GrabActor(
  context: ActorContext[GrabCommand],
  targetPath: String,
  frameCollector: ActorRef[GrabEvent]
) extends AbstractBehavior[GrabCommand] {

  import GrabActor._

  private val log = context.log
  private val name = context.self.path.name

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
    }
  })

  grabThread.start()

  override def onMessage(msg: GrabCommand): Behavior[GrabCommand] = {
    msg match {
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











