package io.wt

import java.util.ServiceLoader

import io.wt.processor.PageProcessor

import scala.io.StdIn

/**
  * @author : tong.wang
  * @since : 6/14/18 11:40 PM
  * @version : 1.0.0
  */
object Main {

  def main(args: Array[String]): Unit = {
    import scala.collection.JavaConverters._

    val services = ServiceLoader.load(classOf[PageProcessor]).asScala
    val processorList = services.zip(Stream from 1)

    System.getenv("PASS_PLATFORM") match {
      case "openshift" =>
        processorList.map(it => it._1).foreach(it => Spider(pageProcessor = it).start())
      case _ =>
        println("show page processor list: ")
        println("  0. all")
        for ((service, order) <- processorList) {
          println(s"  ${order}. ${service.getClass.getSimpleName}")
        }

        println("\nchoose number to execute.")
        println("input like 1,2,3 means to execute 1 and 2 and 3 processor")

        val chooseNumber = StdIn.readLine()
        val chosen = chooseNumber.split(",").distinct

        val executeProcessor = if (chosen.isEmpty || chooseNumber.contains("0")) {
          println("execute all processor")
          processorList.map(it => it._1)
        } else {
          processorList.collect {
            case (processor, order) if chosen.contains(order.toString) => processor
          }
        }

        executeProcessor.foreach(it => Spider(pageProcessor = it).start())

    }
  }
}