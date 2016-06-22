# spark_mlib_match
##Data
1. Find the data files under /data/*.csv   - partner1.csv, partner2.csv, training.csv </br>
2. The data files are a result of </br>  
	a.openning the /data/match.xksx file with a Spreadshit tool, replacing all "," with ""
		and saving each sheet as csv. </br>
	b. for each csv, remove headers: cat file.csv | tail -n +2 > no_headers_file.csv </br>

##Build
1. sbt assembly plugin required (already exists under project/assembly.sbt) </br>
2. Build command:  sbt assembly</br>
3. The executable sohuld be found under target/scala-2.10/  (the folder will be created after the build ) </br>
##SPARK
1. Spark 1.6.1 is required
##Run using spark standalone
1. move the jar into your spark environment  (for instance /home/spark/jars)
2. move the data files into spark host  (for instance - /home/spark/data/)
3. submit command:
	Remove any existing model:

	rm -rf  /home/spark/target/tmp/myDecisionTreeClassificationModel/*

	run:

	spark-submit  /home/spark/jars/spark_match-assembly-1.0.jar  /home/spark/data/training.csv  /home/spark/data/partner1.csv  /home/spark/data/partner2.csv  > result.csv
##Result
1. The result is a Csv file containing the ids of the algorithm suggested matches in the format
  partner1  partner2
  	3             44

 2. the ids are the position of the hotel in the original xlsx file 
	
#ML Model
##Ceaning the data: for each hotel  - 
	a. remove latin tokens
	b. remove tokens of the city name from the hotel name
	c. remove stop words from the hotel names 
##Classes: 
1 - matching
0 - not maching
##Blocking
Country field

##DataPoints
joining the 2 partners lists on the Countr Field and make vector of hotel_a,hotel_b,Features 

##Features:
Array(lev_dist_name,rank_dist,geo_dist)
where lev_dist_name is levenshtein distance, normalized with the hotel name length

#Training set:
Based on training.csv which contains only matching samples.
For adding non-matching samples, we join the training set with itself on
(country_a=country_b and id_a!=id_b) 

We have:
Match: 500 samples
No Match: 55240 samples


#
##Spark Model params
val numClasses = 2
val categoricalFeaturesInfo = Map[Int, Int](2->6)
val impurity = "gini"
val maxDepth = 5
val maxBins = 32

val model = DecisionTree.trainClassifier(trainingData, numClasses, categoricalFeaturesInfo,
impurity, maxDepth, maxBins)


##Other thoughts:
Using google maps to get the location_id for each hotel. Might be too slow.
