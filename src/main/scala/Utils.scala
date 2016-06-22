import _root_.spray.json.DefaultJsonProtocol
import _root_.spray.json.DefaultJsonProtocol._

import scala.math._
import spray.json._
import DefaultJsonProtocol._
import scala.io.Source.fromURL




case class SampleSingle(hotel_name:String,country:String,city_name:String,
                        rank:Double,lat:Double,long:Double)


//records that contains 2 partners hotels
case class SampleRecord(label:Int,a_hotel_name:String,a_country:String,a_city_name:String,
                        a_rank:Double,a_lat:Double,a_long:Double,
                        b_hotel_name:String,b_country:String,b_city_name:String,
                        b_rank:Double,b_lat:Double,b_long:Double){


  def get_a:SampleSingle = {
    SampleSingle(a_hotel_name,a_country,a_city_name, a_rank,a_lat,a_long)
  }

  def get_b:SampleSingle = {
    SampleSingle(b_hotel_name,b_country,b_city_name, b_rank,b_lat,b_long)
  }

  def toDataPoint():Array[Double] = {
    val lev_dist_name = Levenshtein.distance(a_hotel_name,b_hotel_name)
    val factored_lev_dist_name = (lev_dist_name.toDouble/Math.max(a_hotel_name.size.toDouble,b_hotel_name.size.toDouble))



    // val lev_dist_city_name = Levenshtein.distance(a_city_name,b_city_name)
    val rank_dist = Math.abs(a_rank-b_rank)

    val geo_dist = GeoUtils.dist(a_lat,a_long,b_lat,b_long)

    // println("dist: "+geo_dist)

    /* val a_g_resp = GMaps.getResp(a_hotel_name + " "+a_city_name)
    val b_g_resp = GMaps.getResp(b_hotel_name + " "+b_city_name)
    val a_google_id= a_g_resp.flatMap(_.results.headOption.map(_.place_id))
    val b_google_id= b_g_resp.flatMap(_.results.headOption.map(_.place_id))
    val is_same_google_id = (a_google_id,b_google_id) match {
    case (Some(aid),Some(bid)) => if(aid == bid) 1 else 0
    case _ => 0
    }*/

    // val is_same_google_id = 0


    Array(factored_lev_dist_name,geo_dist,rank_dist)
    //0/1,lev_dist,rank_dist,raw_geo_dist,city_lev_dist,is_same_google_id

  }
}
object SampleRecord{
  def apply(label:Int,a:SampleSingle,b:SampleSingle):SampleRecord =
    SampleRecord(label,a.hotel_name,a.country,a.city_name,a.rank,a.lat,a.long,
      b.hotel_name,b.country,b.city_name,b.rank,b.lat,b.long)

}




object StringUtils{

  val stopWords = Seq("HOTEL","RESORT","SPA","CASA","HOSTEL","GUESTHOUSE","ROOMS","CLUB","MOTEL","HOUSE","LODGE","HTEL","HOTELS","")

  def removeLatin(str:String):String = {
    str.replaceAll("\\p{InLatin-1Supplement}", "")
  }

  def removeStopWords(str:String):String = {
    str.replaceAll("[^a-zA-Z ]", " ")
      .toUpperCase().split(" ").filter(!stopWords.contains(_)).mkString(" ")
  }

  def cleanHotelName(str:String,city_name:String):String = {
    val clean = removeStopWords(removeLatin(str))
    city_name.split(" ").foldLeft(clean)((acc,w) =>
      {
        acc.replaceAll(w.replaceAll("[\\)\\(]","").toUpperCase," ")
      }
    )
  }
}

object Levenshtein {
  def minimum(i1: Int, i2: Int, i3: Int)=min(min(i1, i2), i3)
  def distance(s1:String, s2:String)={
    val dist=Array.tabulate(s2.length+1, s1.length+1){(j,i)=>if(j==0) i else if (i==0) j else 0}

    for(j<-1 to s2.length; i<-1 to s1.length)
      dist(j)(i)=if(s2(j-1)==s1(i-1)) dist(j-1)(i-1)
      else minimum(dist(j-1)(i)+1, dist(j)(i-1)+1, dist(j-1)(i-1)+1)

    dist(s2.length)(s1.length)
  }



  def printDistance(s1:String, s2:String)=println("%s -> %s : %d".format(s1, s2, distance(s1, s2)))
}


object GeoUtils{
  import math.{ sqrt, pow }

  def dist(lat:Double,lon:Double,latb:Double,lonb:Double) :Double = {
    Point(lat,lon).distance(Point(latb,lonb))
  }

  case class Point(val x: Double, val y: Double) {

    def distance(other: Point): Double =
      sqrt(pow(x - other.x, 2) + pow(y - other.y, 2))
  }
}

case class JsonCoords(lat:Double,lng:Double)
object JsonCoords { implicit val f = jsonFormat2(JsonCoords.apply)}
case class JsonBounds(northeast:JsonCoords,southwest:JsonCoords)
object JsonBounds { implicit val f = jsonFormat2(JsonBounds.apply)}
case class JsonGeometry(bounds:Option[JsonBounds],location_type:String, viewport: JsonBounds)
object JsonGeometry { implicit val f = jsonFormat3(JsonGeometry.apply)}
case class JsonAddress(long_name:String,short_name:String,types:Seq[String])
object JsonAddress { implicit val f = jsonFormat3(JsonAddress.apply)}
case class JsonGoogleResult(address_components:Seq[JsonAddress],formatted_address:String,
                            geometry:JsonGeometry,partial_match:Boolean,place_id:String,types:Seq[String]
                             )
object JsonGoogleResult { implicit val f = jsonFormat6(JsonGoogleResult.apply)}
case class GoogleMapResponse(results:Seq[JsonGoogleResult],status:String)
object GoogleMapResponse { implicit val f = jsonFormat2(GoogleMapResponse.apply)}


