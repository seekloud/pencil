package org.seekloud.pencil

import akka.actor.ActorSystem
import akka.actor.typed.DispatcherSelector
import akka.dispatch.MessageDispatcher

/**
  * Author: Tao Zhang
  * Date: 4/29/2019
  * Time: 11:28 PM
  */
object Boot {

  import org.seekloud.pencil.common.AppSettings._

  implicit val system: ActorSystem = ActorSystem("pencil", config)

  implicit val executor: MessageDispatcher = system.dispatchers.lookup("akka.actor.my-blocking-dispatcher")

  val blockingDispatcher: DispatcherSelector = DispatcherSelector.fromConfig("my-blocking-dispatcher")


  def main(args: Array[String]): Unit = {
    //println("hello world.")


    val b = 192.toByte
    println(b)

  }

}
