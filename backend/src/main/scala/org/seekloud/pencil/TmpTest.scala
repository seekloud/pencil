package org.seekloud.pencil

/**
  * Author: Tao Zhang
  * Date: 5/1/2019
  * Time: 11:02 PM
  */
object TmpTest {


  def main(args: Array[String]): Unit = {

  }

  def main1(args: Array[String]): Unit = {


    val a = Array(1, 2, 3)
    val b = Array(1, 2, 3)
    println(a == b)
    println(a sameElements b)
    println(a.equals(b))
  }

}
