import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.tree.DecisionTree
import org.apache.spark.mllib.tree.model.DecisionTreeModel
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.Row
import org.apache.spark.{SparkContext, SparkConf}
import StringUtils._
/**
 * Created by gilad on 18/06/16.
 */
object Parser {
  def main( args:Array[String] ):Unit = {



    val match_path = args(0)
    val part1_path = args(1)
    val part2_path = args(2)

    val conf = new SparkConf().setAppName("Matcher")


    val sc = new SparkContext(conf)
    val sqlContext = new org.apache.spark.sql.SQLContext(sc)
    import sqlContext.implicits._



    val matching = sc.textFile(match_path,2)
    val splited_matching_h =   matching.map(_.split(","))

    val splited_matching=splited_matching_h.mapPartitionsWithIndex { (idx, iter) => if (idx == 0) iter.drop(1) else iter }
    val match_rdd:RDD[SampleRecord] = splited_matching.map{
      rec=>
        SampleRecord(1,
          cleanHotelName(rec(0),rec(2)), rec(1), removeLatin(rec(2)),rec(3).toDouble,rec(4).toDouble,rec(5).toDouble,
        cleanHotelName(rec(6),rec(8)),rec(7),removeLatin(rec(8)),rec(9).toDouble,rec(10).toDouble,rec(11).toDouble)
    }


//prepare a new rdd - combine non-matching and matching samples rdds
//took a_hotel from matching list and combine with b_hotel from the reversed matching list (i a_hotel-> n-i b_hotel)
//this wasn't good cause we got only hotels that are very far from each other


    match_rdd.foreach{x=>
      val dp = x.toDataPoint
      println(x.a_hotel_name +"=="+x.b_hotel_name+"--"+dp(0) + " -- " + dp(1) + " -- " + dp(2))
    }
    val matching_df = match_rdd.sortBy(_.a_hotel_name,true).zipWithIndex.map(x=>(x._2,x._1)).toDF

    val no_matching_df = matching_df.as("a").
      join(matching_df.as("b")).
      where($"a._2.a_country" ===  $"b._2.b_country" and( $"a._1" !==  $"b._1" )  and ($"b._2.b_hotel_name"!=="")  )
      .select($"a._2.a_hotel_name",$"a._2.a_country",$"a._2.a_city_name",$"a._2.a_rank",$"a._2.a_lat",$"a._2.a_long"
        ,$"b._2.b_hotel_name",$"b._2.b_country",$"b._2.b_city_name",$"b._2.b_rank",$"b._2.a_lat",$"b._2.b_long")

    val no_matching_rdd = no_matching_df.map{
      case Row(a_hotel_name: String,
      a_country: String,
      a_city_name: String,
      a_rank: Double,
      a_lat: Double,
      a_long: Double,
      b_hotel_name:  String,
      b_country:  String,
      b_city_name:  String,
      b_rank: Double,
      b_lat: Double,
      b_long: Double) =>
        SampleRecord(0,a_hotel_name,a_country,a_city_name,a_rank,a_lat,a_long,b_hotel_name,b_country,b_city_name,b_rank,b_lat,b_long)

}

    no_matching_rdd.foreach{x=>
      val dp = x.toDataPoint
      println(x.a_hotel_name +"<>"+x.b_hotel_name+"--"+dp(0) + " -- " + dp(1) + " -- " + dp(2))
    }


val match_labeled = match_rdd.map{
m=>
  new LabeledPoint(m.label,Vectors.dense(m.toDataPoint()))
}

val no_match_labeled = no_matching_rdd.map{
m=>
  new LabeledPoint(m.label,Vectors.dense(m.toDataPoint()))
}

val training_set = match_labeled ++ no_match_labeled

val splits = training_set.randomSplit(Array(0.7, 0.3))
val (trainingData, testData) = (splits(0), splits(1))

val numClasses = 2
val categoricalFeaturesInfo = Map[Int, Int](2->6)
val impurity = "gini"
val maxDepth = 5
val maxBins = 32

val model = DecisionTree.trainClassifier(trainingData, numClasses, categoricalFeaturesInfo,
impurity, maxDepth, maxBins)

val labelAndPreds = testData.map { point =>
val prediction = model.predict(point.features)
(point.label, prediction)
}


val testErr = labelAndPreds.filter(r => r._1 != r._2).count().toDouble / testData.count()
println("Test Error = " + testErr)
println("Learned classification tree model:\n" + model.toDebugString)

// Save and load model
model.save(sc, "target/tmp/myDecisionTreeClassificationModel")



val sameModel = DecisionTreeModel.load(sc, "target/tmp/myDecisionTreeClassificationModel")
val part1_rdd = sc.textFile(part1_path).map(_.split(","))
val part2_rdd = sc.textFile(part2_path).map(_.split(","))

val spl1_kv = part1_rdd.zipWithIndex.map(x=>(x._1(1),(x._2,x._1)))
val spl2_kv = part2_rdd.zipWithIndex.map(x=>(x._1(1),(x._2,x._1)))

//join on country
val features_rdd =
spl1_kv.join(spl2_kv).map { x =>
 val (side_a,side_b) = x._2
  val r1 = side_a._2
  val r2 = side_b._2

  ((side_a,side_b),SampleRecord(0,
     cleanHotelName(r1(0),r1(2)), r1(1), removeLatin(r1(2)), r1(3).toDouble, r1(4).toDouble, r1(5).toDouble,
     cleanHotelName(r2(0),r2(2)), r2(1), removeLatin(r2(2)), r2(3).toDouble, r2(4).toDouble, r2(5).toDouble))
}

features_rdd.foreach{
r=>
  val f_vec = r._2.toDataPoint()
  val (side_a,side_b) = r._1
  val pred = sameModel.predict(Vectors.dense(f_vec))
  if(pred==1.0)
    println(s"${r._2.a_hotel_name} -- ${r._2.b_hotel_name}, id_a: ${side_a._1}, id_b: ${side_b._1},${f_vec(0)},${f_vec(1)}")
}

}
}
