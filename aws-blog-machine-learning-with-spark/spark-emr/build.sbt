name := "spark-emr"

version := "1.0"

scalaVersion := "2.10.5"

libraryDependencies ++= Seq(
  "com.intentmedia.mario" %% "mario" % "0.1.0",
  "org.apache.spark" %% "spark-core" % "1.3.0" % "provided"
    exclude("com.google.guava", "guava"),
  "org.apache.spark" %% "spark-mllib" % "1.3.0" % "provided"
    exclude("org.codehaus.jackson", "jackson-core-asl")
    exclude("org.codehaus.jackson", "jackson-jaxrs")
    exclude("org.codehaus.jackson", "jackson-mapper-asl")
    exclude("org.codehaus.jackson", "jackson-xc"),
  "org.scalatest" %% "scalatest" % "2.2.4" % "test"
)

mainClass := Some("ModelingWorkflow")

fork := true

test in assembly := {}

excludedJars in assembly <<= (fullClasspath in assembly) map { cp =>
  val excludes = Set(
    "jsp-api-2.1-6.1.14.jar",
    "jsp-2.1-6.1.14.jar",
    "jasper-compiler-5.5.12.jar",
    "commons-beanutils-core-1.8.0.jar",
    "commons-beanutils-1.7.0.jar",
    "servlet-api-2.5-20081211.jar",
    "servlet-api-2.5.jar"
  )
  cp filter { jar => excludes(jar.data.getName) }
}


mergeStrategy in assembly <<= (mergeStrategy in assembly) {
  (old) => {
    case x if x.startsWith("META-INF") => MergeStrategy.discard
    case x if x.endsWith(".html") => MergeStrategy.discard
    case PathList("com", "esotericsoftware", xs @ _*) => MergeStrategy.last // For Log$Logger.class
    case x => old(x)
  }
}
