name := "aws-blog-power-redshift-analytics-with-amazonml-spark"

version := "1.0"

scalaVersion := "2.11.5"

libraryDependencies ++= Seq("org.apache.spark" %% "spark-core" % "2.0.0",
                            "com.amazonaws" % "aws-java-sdk"  % "1.10.46" % "provided",
                            "com.amazonaws" % "aws-java-sdk-core" % "1.10.46" % "provided")

