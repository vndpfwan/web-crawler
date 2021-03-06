package io.github.wtog.pipeline

import java.io.RandomAccessFile
import java.util.concurrent._

import io.github.wtog.Main

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future

/**
 * @author : tong.wang
 * @since : 5/20/18 11:01 PM
 * @version : 1.0.0
 */
case class CsvFilePipeline(fileName: Option[String]) extends Pipeline {

  override def process(pageResultItem: (String, Map[String, Any])): Unit = {
    val (pageUrl, resultItems) = pageResultItem
    IOContentCache.add(fileName.getOrElse(pageUrl), resultItems)
  }
}

object IOContentCache {
  private val cache: ConcurrentHashMap[String, ListBuffer[Map[String, Any]]] = new ConcurrentHashMap[String, ListBuffer[Map[String, Any]]]()

  def add(key: String, value: Map[String, Any]) = {
    val listValue = cache.getOrDefault(key, ListBuffer.empty[Map[String, Any]])
    listValue.append(value)
    cache.put(key, listValue)
  }

  def writeContentFile(fileName: String, contentList: ListBuffer[Map[String, Any]]) = {
    if (contentList.nonEmpty) {
      val file = if (fileName.contains("/")) fileName.replace("/", "_") else fileName

      val randomFile = new RandomAccessFile(s"/tmp/web-crawler-${file}-${Main.startTime}.csv", "rw")
      try {
        val fileLength = randomFile.length()
        randomFile.seek(fileLength) //指针指向文件末尾
        fileLength match {
          case 0 ⇒
            val head = contentList.head
            val title = head.keys.mkString(",") + "\n"
            randomFile.write((title).getBytes("UTF-8"))
            val row = head.values.mkString(",") + "\n"
            randomFile.write((row).getBytes("UTF-8"))
            contentList -= head
          case _ ⇒
            contentList.foreach(map ⇒ {
              val row = map.values.mkString(",") + "\n"
              randomFile.write((row).getBytes("UTF-8")) //写入数据
              contentList -= map
            })
        }
      } catch {
        case ex: Throwable ⇒ ex.printStackTrace()
      } finally {
        randomFile.close()
      }
    }
  }

  val expire = {
    def removeExpire() = {
      import collection.JavaConverters._
      val schedule = Executors.newScheduledThreadPool(1)

      schedule.scheduleWithFixedDelay(new Runnable {
        override def run(): Unit = {
          cache.asScala.foreach {
            case (url, list) ⇒ writeContentFile(url, list)
          }
        }
      }, 3, 3, TimeUnit.SECONDS)
    }

    import scala.concurrent.ExecutionContext.Implicits.global

    Future {
      removeExpire()
    }.recover {
      case ex ⇒
        println(ex.getLocalizedMessage)
        removeExpire()
    }
  }

}
