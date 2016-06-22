name := "spark_match"

version := "1.0"
assemblyMergeStrategy in assembly := {
  case PathList("javax", "servlet", xs @ _*)         => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith ".html" => MergeStrategy.first
  case "application.conf"                            => MergeStrategy.concat
  case "unwanted.txt"                                => MergeStrategy.discard
  case "META-INF/MANIFEST.MF" =>  MergeStrategy.discard
  case x => MergeStrategy.first
  //   val oldStrategy = (assemblyMergeStrategy in assembly).value
  // oldStrategy(x)
}


scalaVersion := "2.10.4"



val sparkVersion = "1.6.1"





resolvers ++= Seq(
  "Typesafe" at "http://repo.typesafe.com/typesafe/releases/",
  "Java.net Maven2 Repository" at "http://download.java.net/maven/2/"
)

libraryDependencies ++= Seq(
"org.apache.spark" %% "spark-core" % sparkVersion  % "provided",
"org.apache.spark" %% "spark-sql" % sparkVersion % "provided")

libraryDependencies += "org.apache.spark" % "spark-mllib_2.10" % "1.6.1"  % "provided"

libraryDependencies += "io.spray" %%  "spray-json" % "1.3.2" % "provided"


resolvers += "Bintray sbt plugin releases"  at "http://dl.bintray.com/sbt/sbt-plugin-releases/"
resolvers += Resolver.sonatypeRepo("releases")

resolvers += Resolver.mavenLocal

