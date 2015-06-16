addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.13.0")

runMain in Compile <<= Defaults.runMainTask(fullClasspath in Compile, runner in (Compile, run))
