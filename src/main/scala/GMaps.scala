import java.lang.Exception

import _root_.spray.json.DefaultJsonProtocol
import spray.json._
import DefaultJsonProtocol._
import scala.io.Source.fromURL
import scala.io.Source



object GMaps {


  @throws(classOf[java.io.IOException])
  @throws(classOf[java.net.SocketTimeoutException])
  def get(url: String,
          connectTimeout:Int =5000,
          readTimeout:Int =5000,
          requestMethod: String = "GET") = {
    import java.net.{HttpURLConnection, URL}
    val connection = (new URL(url)).openConnection.asInstanceOf[HttpURLConnection]
    connection.setConnectTimeout(connectTimeout)
    connection.setReadTimeout(readTimeout)
    connection.setRequestMethod(requestMethod)
    val inputStream = connection.getInputStream
    val content = scala.io.Source.fromInputStream(inputStream).mkString
    if (inputStream != null) inputStream.close
    content
  }

  def getResp(address:String):Option[GoogleMapResponse] = {
    try {

      val fix_address= address.replace(" ","+")
      //println(fix_address)
      val content = get("http://maps.googleapis.com/maps/api/geocode/json?address=" + fix_address + "&sensor=true")

      val contentJs = content.parseJson
      //println(contentJs + "res")
      val googleRes = contentJs.convertTo[GoogleMapResponse]
     // println(content + "res")
     // println(googleRes.status)
      Some(googleRes)
    } catch {

     // case ioe: java.io.IOException => // handle this
      //case ste: java.net.SocketTimeoutException => // handle this
      case (e:Exception) =>
        println(e.getMessage)
        None
    }
  }
}