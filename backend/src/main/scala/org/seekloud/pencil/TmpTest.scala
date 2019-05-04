package org.seekloud.pencil

import akka.actor.ActorSystem
import akka.actor.typed.{ActorRef, Behavior, DispatcherSelector}
import akka.actor.typed.scaladsl.Behaviors
import akka.dispatch.MessageDispatcher
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.seekloud.pencil.Boot.system

import scala.concurrent.Future
import scala.util.{Random, Try}

/**
  * Author: Tao Zhang
  * Date: 5/1/2019
  * Time: 11:02 PM
  */
object TmpTest {


  def main(args: Array[String]): Unit = {

    import akka.actor.typed.scaladsl.adapter._

    import org.seekloud.pencil.common.AppSettings._

    val system: ActorSystem = ActorSystem("pencil", config)

    val executor: MessageDispatcher = system.dispatchers.lookup("akka.actor.my-blocking-dispatcher")

    //val blockingDispatcher: DispatcherSelector = DispatcherSelector.fromConfig("akka.actor.my-blocking-dispatcher")


    for (i <- 0 until 16) {
      Future {
        println(s"--------------------------------- sleep future begin $i")
        Thread.sleep(10000 + (i * 20))
        println(s"---------------------------------- sleep future end $i")
      }(executor)
    }


    def manager(): Behavior[Int] = Behaviors.setup[Int] { context =>

      for (i <- 0 until 100) {
        val n = 5
        val worker = context.spawn[Int](sleepWork2(context.self), s"$i", DispatcherSelector.fromConfig("akka.actor.my-blocking-dispatcher"))
        println(s"start: $i")
        worker ! n
      }

      var workingCount = 0
      var workDone = 0

      println("-------------------- All start --------------------")
      Behaviors.receiveMessage[Int] { msg =>
        if (msg < 0) {
          workingCount += 1
          println(s"got working begin [$msg], workingCount=$workingCount, workDone=$workDone")
        } else {
          workingCount -= 1
          workDone += 1
          println(s"got working end [$msg], workingCount=$workingCount, workDone=$workDone")
        }
        Behaviors.same
      }

    }

    def sleepWork(parent: ActorRef[Int]) = Behaviors.receive[Int] { (context, msg) =>
      val name = context.self.path.name.toInt
      msg match {
        case n: Int =>
          val t = Random.nextInt(3000) + (n * 1000)
          println(s"$name got sleep $t ms")
          parent ! (-1 * name)
          Thread.sleep(t)
          parent ! name
          println(s"$name sleep done.")

      }
      Behaviors.stopped
    }


    def sleepWork2(parent: ActorRef[Int]): Behavior[Int] =
      Behaviors.setup{ context =>
        val name = context.self.path.name.toInt

        println(s"---  init sleep [$name]")
        Thread.sleep(10 * 1000)
        println(s"+++  init sleep  [$name] DONE.")


        Behaviors.receiveMessage[Int] { msg =>
          msg match {
            case n: Int =>
              val t = Random.nextInt(3000) + (n * 1000)
              println(s"$name got sleep $t ms")
              parent ! (-1 * name)
              Thread.sleep(t)
              parent ! name
              println(s"$name sleep done.")

          }
          Behaviors.stopped
      }


    }

    system.spawn(manager(), "manager")
    println("BEGIN!")


  }

  def main3(args: Array[String]): Unit = {

    val grabber = new FFmpegFrameGrabber("data/video/VID_20181123_214724.mp4")

    println(s"getImageWidth: ${grabber.getImageWidth}")
    println(s"getImageHeight: ${grabber.getImageHeight}")
    println(s"getPixelFormat: ${grabber.getPixelFormat}")
    println(s"getAudioChannels: ${grabber.getAudioChannels}")
    println(s"getFrameRate: ${grabber.getFrameRate}")
    println(s"getVideoBitrate: ${grabber.getVideoBitrate}")
    println(s"getVideoCodec: ${grabber.getVideoCodec}")


    println("----------------------------")
    println("----------------------------")
    println("----------------------------")
    grabber.start()

    println(s"getImageWidth: ${grabber.getImageWidth}")
    println(s"getImageHeight: ${grabber.getImageHeight}")
    println(s"getPixelFormat: ${grabber.getPixelFormat}")
    println(s"getAudioChannels: ${grabber.getAudioChannels}")
    println(s"getFrameRate: ${grabber.getFrameRate}")
    println(s"getVideoBitrate: ${grabber.getVideoBitrate}")
    println(s"getVideoCodec: ${grabber.getVideoCodec}")


    grabber.close()


  }

  def main2(args: Array[String]): Unit = {

    Try {
      println("000")
      Thread.sleep(3000)
      println("111")
    }

    Try {
      println("222")
    }

  }

  def main1(args: Array[String]): Unit = {


    val a = Array(1, 2, 3)
    val b = Array(1, 2, 3)
    println(a == b)
    println(a sameElements b)
    println(a.equals(b))


  }

}
