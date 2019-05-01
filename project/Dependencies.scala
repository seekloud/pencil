import sbt._

/**
  * User: Taoz
  * Date: 6/13/2017
  * Time: 9:38 PM
  */
object Dependencies {


  val slickV = "3.2.3"
  val akkaV = "2.5.22"
  val akkaHttpV = "10.1.8"
  val scalaXmlV = "1.1.0"
  val circeVersion = "0.9.3"
  val scalaJsDomV = "0.9.6"

  val akkaSeq = Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaV withSources(),
    "com.typesafe.akka" %% "akka-actor-typed" % akkaV withSources(),
    "com.typesafe.akka" %% "akka-slf4j" % akkaV,
    "com.typesafe.akka" %% "akka-stream" % akkaV
  )

  val akkaHttpSeq = Seq(
    "com.typesafe.akka" %% "akka-http" % akkaHttpV,
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpV
  )

  val circeSeq = Seq(
    "io.circe" %% "circe-core" % circeVersion,
    "io.circe" %% "circe-generic" % circeVersion,
    "io.circe" %% "circe-parser" % circeVersion
  )

  val slick = "com.typesafe.slick" %% "slick" % slickV
  val slickCodeGen = "com.typesafe.slick" %% "slick-codegen" % slickV

  val scalaXml = "org.scala-lang.modules" %% "scala-xml" % "1.0.6"
  val nscalaTime = "com.github.nscala-time" %% "nscala-time" % "2.16.0"
  val hikariCP = "com.zaxxer" % "HikariCP" % "2.6.2"
  val logback = "ch.qos.logback" % "logback-classic" % "1.2.3"
  val codec = "commons-codec" % "commons-codec" % "1.10"
  val postgresql = "org.postgresql" % "postgresql" % "9.4.1208"
  val asynchttpclient = "org.asynchttpclient" % "async-http-client" % "2.0.32"
  val ehcache = "net.sf.ehcache" % "ehcache" % "2.10.4"


  val backendDependencies: Seq[ModuleID] =
    Dependencies.akkaSeq ++
    Dependencies.akkaHttpSeq ++
    Dependencies.circeSeq ++
    Seq(
      Dependencies.scalaXml,
      Dependencies.slick,
      Dependencies.slickCodeGen,
      Dependencies.nscalaTime,
      Dependencies.hikariCP,
      Dependencies.logback,
      Dependencies.codec,
      Dependencies.postgresql,
      Dependencies.asynchttpclient,
      Dependencies.ehcache
    )


  
  

  val javacppVersion = "1.5"

  // Platform classifier for native library dependencies
  //val platform = org.bytedeco.javacpp.Loader.getPlatform
  private val platforms = IndexedSeq("windows-x86_64", "linux-x86_64")

  // Libraries with native dependencies
  private val bytedecoPresetLibs = Seq(
    "opencv" -> s"4.0.1-$javacppVersion",
    "ffmpeg" -> s"4.1.3-$javacppVersion").flatMap {
    case (lib, ver) => Seq(
      // Add both: dependency and its native binaries for the current `platform`
      "org.bytedeco" % lib % ver withSources() withJavadoc(),
      "org.bytedeco" % lib % ver classifier platforms(0) classifier platforms(1)
    )
  }

  val bytedecoLibs: Seq[ModuleID] = Seq(
    "org.bytedeco"            % "javacpp"         % javacppVersion withSources() withJavadoc(),
    "org.bytedeco"            % "javacv"          % javacppVersion withSources() withJavadoc(),
  ) ++ bytedecoPresetLibs


  val testLibs = Seq(
    "com.typesafe.akka" %% "akka-testkit" % akkaV % "test",
    "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaV % "test",
    "org.scalatest" %% "scalatest" % "3.0.7" % "test"
  )

}
