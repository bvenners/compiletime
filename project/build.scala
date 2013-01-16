import sbt._
import Keys._
import java.io.{FileWriter, BufferedWriter}

object TestsBuild extends Build {
  
  val scalaVersionToUse = "2.9.0"
  val testCount = 1500
  
  def libDependencies = Seq(
     "org.scalatest" %% "scalatest" % "2.0.M4-SNAPSHOT" % "test", 
     "org.specs2" % "specs2_2.9.2" % "1.12.1" % "test"
  )
  
  lazy val scalatest = Project("gentests", file("."))
    .settings(
      organization := "org.scalatest",
      version := "1.0.0",
      scalaVersion := scalaVersionToUse,
      libraryDependencies ++= libDependencies, 
      resolvers ++= Seq("Sonatype Public" at "https://oss.sonatype.org/content/groups/public",
                        "Local Maven" at "file://" + Path.userHome + "/.m2/repository",
                        "snapshots" at "http://oss.sonatype.org/content/repositories/snapshots",
                        "releases"  at "http://oss.sonatype.org/content/repositories/releases"),
      genScalaTestSpecTask, 
      genScalaTestSpecAssertTask, 
      genScalaTestSpecWithMustMatchersTask,
      genScalaTestWordSpecTask,
      genScalaTestWordSpecAssertTask,
      genMutableSpecTask,
      genImmutableSpecTask,
      sourceGenerators in Test <+= 
         (baseDirectory, sourceManaged in Test) map generateScalaTestSpec,
      sourceGenerators in Test <+= 
         (baseDirectory, sourceManaged in Test) map generateScalaTestSpecAssert,
      sourceGenerators in Test <+= 
         (baseDirectory, sourceManaged in Test) map generateScalaTestSpecWithMustMatchers, 
      sourceGenerators in Test <+= 
         (baseDirectory, sourceManaged in Test) map generateScalaTestWordSpec, 
      sourceGenerators in Test <+= 
         (baseDirectory, sourceManaged in Test) map generateScalaTestWordSpecAssert, 
      sourceGenerators in Test <+= 
         (baseDirectory, sourceManaged in Test) map generateScalaTestWordSpecWithMustMatchers, 
      sourceGenerators in Test <+= 
         (baseDirectory, sourceManaged in Test) map generateMutableSpec, 
      sourceGenerators in Test <+= 
         (baseDirectory, sourceManaged in Test) map generateImmutableSpec
    )
  
  def targetDirName = 
    if (scalaVersionToUse.startsWith("2.10"))
      "scala-2.10"
    else
      "scala-" + scalaVersionToUse
    
  def generateScalaTestSpec(basedir: File, outDir: File): Seq[File] = {
    val targetDir = outDir / "scala" / "stspec"
    targetDir.mkdirs()
    val targetFile = new File(targetDir, "ExampleSpec.scala")
    val targetOut = new BufferedWriter(new FileWriter(targetFile))
    try {
      targetOut.write("""
package stspec
          
import org.scalatest._
          
class ExampleSpec extends Spec {
  object `Scala can` {
""")
      
      for (x <- 1 to testCount) {
        targetOut.write("    def `increment " + x + "` {\n")
        targetOut.write("      expectResult(" + (x+1) + ") {" + x + " + 1 }" + "\n")
        targetOut.write("    }\n\n")
      }
      
      targetOut.write("""
          
  }
}""")
    }
    finally {
      targetOut.flush()
      targetOut.close()
    }
    Seq(targetFile)
  }
  
  val genScalaTestSpec = TaskKey[Unit]("genstspec", "Generate ScalaTest Spec")
  val genScalaTestSpecTask = genScalaTestSpec <<= (sourceManaged in Compile, sourceManaged in Test) map { (mainTargetDir: File, testTargetDir: File) =>
    generateScalaTestSpec(new File("."), new File("target/" + targetDirName + "/src_managed/test"))
  }
  
  def generateScalaTestSpecAssert(basedir: File, outDir: File): Seq[File] = {
    val targetDir = outDir / "scala" / "stspecassert"
    targetDir.mkdirs()
    val targetFile = new File(targetDir, "ExampleSpec.scala")
    val targetOut = new BufferedWriter(new FileWriter(targetFile))
    try {
      targetOut.write("""
package stspecassert
          
import org.scalatest._
          
class ExampleSpec extends Spec {
  object `Scala can` {
""")
      
      for (x <- 1 to testCount) {
        targetOut.write("    def `increment " + x + "` {\n")
        targetOut.write("      assert(" + x + " + 1 === " + (x+1) + ")" + "\n")
        targetOut.write("    }\n\n")
      }
      
      targetOut.write("""
          
  }
}""")
    }
    finally {
      targetOut.flush()
      targetOut.close()
    }
    Seq(targetFile)
  }
  
  val genScalaTestSpecAssert = TaskKey[Unit]("genstspec", "Generate ScalaTest Spec")
  val genScalaTestSpecAssertTask = genScalaTestSpecAssert <<= (sourceManaged in Compile, sourceManaged in Test) map { (mainTargetDir: File, testTargetDir: File) =>
    generateScalaTestSpecAssert(new File("."), new File("target/" + targetDirName + "/src_managed/test"))
  }
  
  def generateScalaTestSpecWithMustMatchers(basedir: File, outDir: File): Seq[File] = {
    val targetDir = outDir / "scala" / "stspecmust"
    targetDir.mkdirs()
    val targetFile = new File(targetDir, "ExampleSpec.scala")
    val targetOut = new BufferedWriter(new FileWriter(targetFile))
    try {
      targetOut.write("""
package stspecmust
          
import org.scalatest._
          
class ExampleSpec extends Spec with MustMatchers {
  object `Scala can` {
""")
      
      for (x <- 1 to testCount) {
        targetOut.write("    def `increment " + x + "` {\n")
        targetOut.write("      " + x + " + 1 must equal (" + (x+1) + ")\n")
        targetOut.write("    }\n\n")
      }
      
      targetOut.write("""
          
  }
}""")
    }
    finally {
      targetOut.flush()
      targetOut.close()
    }
    Seq(targetFile)
  }
  
  val genScalaTestSpecWithMustMatchers = TaskKey[Unit]("genstspecmustmatchers", "Generate ScalaTest Spec with MustMatchers")
  val genScalaTestSpecWithMustMatchersTask = genScalaTestSpecWithMustMatchers <<= (sourceManaged in Compile, sourceManaged in Test) map { (mainTargetDir: File, testTargetDir: File) =>
    generateScalaTestSpec(new File("."), new File("target/" + targetDirName + "/src_managed/test"))
  }
  
  // WordSpec
  def generateScalaTestWordSpec(basedir: File, outDir: File): Seq[File] = {
    val targetDir = outDir / "scala" / "wordspec"
    targetDir.mkdirs()
    val targetFile = new File(targetDir, "ExampleSpec.scala")
    val targetOut = new BufferedWriter(new FileWriter(targetFile))
    try {
      targetOut.write("""
package wordspec
          
import org.scalatest._
          
class ExampleSpec extends WordSpec {
  "Scala" can {
""")
      
      for (x <- 1 to testCount) {
        targetOut.write("    \"increment " + x + "\" in {\n")
        targetOut.write("      expectResult(" + (x+1) + ") {" + x + " + 1 }" + "\n")
        targetOut.write("    }\n\n")
      }
      
      targetOut.write("""
          
  }
}""")
    }
    finally {
      targetOut.flush()
      targetOut.close()
    }
    Seq(targetFile)
  }
  
  val genScalaTestWordSpec = TaskKey[Unit]("genwordspec", "Generate ScalaTest WordSpec")
  val genScalaTestWordSpecTask = genScalaTestWordSpec <<= (sourceManaged in Compile, sourceManaged in Test) map { (mainTargetDir: File, testTargetDir: File) =>
    generateScalaTestWordSpec(new File("."), new File("target/" + targetDirName + "/src_managed/test"))
  }
  
  def generateScalaTestWordSpecAssert(basedir: File, outDir: File): Seq[File] = {
    val targetDir = outDir / "scala" / "wordspecassert"
    targetDir.mkdirs()
    val targetFile = new File(targetDir, "ExampleSpec.scala")
    val targetOut = new BufferedWriter(new FileWriter(targetFile))
    try {
      targetOut.write("""
package wordspecassert
          
import org.scalatest._
          
class ExampleSpec extends WordSpec {
  "Scala" can {
""")
      
      for (x <- 1 to testCount) {
        targetOut.write("    \"increment " + x + "\" in {\n")
        targetOut.write("      assert(" + x + " + 1 === " + (x+1) + ")" + "\n")
        targetOut.write("    }\n\n")
      }
      
      targetOut.write("""
          
  }
}""")
    }
    finally {
      targetOut.flush()
      targetOut.close()
    }
    Seq(targetFile)
  }
  
  val genScalaTestWordSpecAssert = TaskKey[Unit]("genwordspec", "Generate ScalaTest WordSpec")
  val genScalaTestWordSpecAssertTask = genScalaTestWordSpecAssert <<= (sourceManaged in Compile, sourceManaged in Test) map { (mainTargetDir: File, testTargetDir: File) =>
    generateScalaTestWordSpecAssert(new File("."), new File("target/" + targetDirName + "/src_managed/test"))
  }
  
  // WordSpec with MustMatchers
  def generateScalaTestWordSpecWithMustMatchers(basedir: File, outDir: File): Seq[File] = {
    val targetDir = outDir / "scala" / "wordspecmust"
    targetDir.mkdirs()
    val targetFile = new File(targetDir, "ExampleSpec.scala")
    val targetOut = new BufferedWriter(new FileWriter(targetFile))
    try {
      targetOut.write("""
package wordspecmust
          
import org.scalatest._
          
class ExampleSpec extends WordSpec with MustMatchers {
  "Scala" can {
""")
      
      for (x <- 1 to testCount) {
        targetOut.write("    \"increment " + x + "\" in {\n")
        targetOut.write("      " + x + " + 1 must equal (" + (x+1) + ")\n")
        targetOut.write("    }\n\n")
      }
      
      targetOut.write("""
          
  }
}""")
    }
    finally {
      targetOut.flush()
      targetOut.close()
    }
    Seq(targetFile)
  }
  
  val genScalaTestWordSpecMustMatchers = TaskKey[Unit]("genwordspecmustmatchers", "Generate ScalaTest WordSpec with MustMatchers")
  val genScalaTestWordSpecMustMatchersTask = genScalaTestWordSpecMustMatchers <<= (sourceManaged in Compile, sourceManaged in Test) map { (mainTargetDir: File, testTargetDir: File) =>
    generateScalaTestWordSpecWithMustMatchers(new File("."), new File("target/" + targetDirName + "/src_managed/test"))
  }
  
  // Mutable Specification
  def generateMutableSpec(basedir: File, outDir: File): Seq[File] = {
    val targetDir = outDir / "scala" / "mspecification"
    targetDir.mkdirs()
    val targetFile = new File(targetDir, "ExampleSpec.scala")
    val targetOut = new BufferedWriter(new FileWriter(targetFile))
    try {
      targetOut.write("""
package mspecification
          
import org.specs2.mutable._
          
class ExampleSpec extends Specification {
  "Scala" can {
""")
      
      for (x <- 1 to testCount) {
        targetOut.write("    \"increment " + x + "\" in {\n")
        targetOut.write("      " + x + " + 1 must be_== (" + (x+1) + ")\n")
        targetOut.write("    }\n\n")
      }
      
      targetOut.write("""
          
  }
}""")
    }
    finally {
      targetOut.flush()
      targetOut.close()
    }
    Seq(targetFile)
  }
  
  val genMutableSpec = TaskKey[Unit]("genmutablespec", "Generate Specs2 Mutable WordSpec")
  val genMutableSpecTask = genMutableSpec <<= (sourceManaged in Compile, sourceManaged in Test) map { (mainTargetDir: File, testTargetDir: File) =>
    generateMutableSpec(new File("."), new File("target/" + targetDirName + "/src_managed/test"))
  }
  
  // Immutable Specification
  def generateImmutableSpec(basedir: File, outDir: File): Seq[File] = {
    val targetDir = outDir / "scala" / "imspecification"
    targetDir.mkdirs()
    val targetFile = new File(targetDir, "ExampleSpec.scala")
    val targetOut = new BufferedWriter(new FileWriter(targetFile))
    try {
      targetOut.write("""
package imspecification
          
import org.specs2._
          
class ExampleSpec extends Specification { def is =
  "Scala can"  ^
""")
      
      for (x <- 1 to testCount)
        targetOut.write("    \"increment " + x + "\"  ! e" + x + "^\n")
      
      targetOut.write("    end\n")
        
      for (x <- 1 to testCount) 
        targetOut.write("def e" + x + " = " + x + " + 1 must be_== (" + (x+1) + ")\n")
      
      targetOut.write("""
}""")
    }
    finally {
      targetOut.flush()
      targetOut.close()
    }
    Seq(targetFile)
  }
  
  val genImmutableSpec = TaskKey[Unit]("genmutablespec", "Generate Specs2 Immutable WordSpec")
  val genImmutableSpecTask = genImmutableSpec <<= (sourceManaged in Compile, sourceManaged in Test) map { (mainTargetDir: File, testTargetDir: File) =>
    generateImmutableSpec(new File("."), new File("target/" + targetDirName + "/src_managed/test"))
  }
}