enablePlugins(ScalaJSPlugin)
enablePlugins(JSDependenciesPlugin)

name := "pyramids"

version := "0.1"

scalaVersion := "2.12.8"





// This is an application with a main method
scalaJSUseMainModuleInitializer := true

libraryDependencies ++= Seq(
  "com.lihaoyi" % "utest_sjs1.0.0-M7_2.12" % "0.6.7" % "test",
  "org.scala-js" %%% "scalajs-dom" % "0.9.6" ,
  "be.doeraene" %%% "scalajs-jquery" % "0.9.4"
)
jsDependencies ++= Seq(
  "org.webjars" % "jquery" % "2.2.1" / "jquery.js" minified "jquery.min.js",
  ProvidedJS / "js/web3/web3.min.js",
  ProvidedJS / "js/ipfs/index.js",
  ProvidedJS / "js/jszip/jszip.min.js"
  //ProvidedJS / "js/buffer/index.js" ,
 // ProvidedJS / "js/ieee754/index.js",
 // ProvidedJS / "js/base64-js/base64js.min.js"
)

testFrameworks += new TestFramework("utest.runner.Framework")
jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv()

val genDirPath = new java.io.File("src/main/webapp/js")


crossTarget in(Compile, fastOptJS) := genDirPath
crossTarget in(Compile, fullOptJS) := genDirPath
crossTarget in(Compile, packageJSDependencies) := genDirPath
