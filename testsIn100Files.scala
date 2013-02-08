import java.net._
import java.io._
import java.nio.channels.Channels
import scala.annotation.tailrec
import scala.math.pow

/*
def scalaVersion = {
  val rawVersion = scala.util.Properties.scalaPropOrElse("version.number", "unknown")
  if (rawVersion.endsWith(".final"))
    rawVersion.substring(0, rawVersion.length - 6)
  else
    rawVersion
}
*/
def scalaVersion = "2.10"
val scalaTestVersion = "2.0.M6-SNAP8"
val specs2Version = "1.13"
val scalazVersion = "6.0.1"

def downloadFile(urlString: String, targetFile: File) {
  println("Downloading " + urlString)
  val url = new URL(urlString)
  val connection = url.openConnection
  val in = connection.getInputStream
  val out = new FileOutputStream(targetFile)
  out getChannel() transferFrom(Channels.newChannel(in), 0, Long.MaxValue)
  in.close()
  out.flush()
  out.close()
}

val classFooter = """
  }
}"""

def generateSourceFile(testCount: Int, targetDir: File, fileNumber: Int, packageName: String, importStatements: Array[String], 
                       extendsName: String, withNames: Array[String], scopeBracket: Boolean, scopeDef: String, 
                       testDefFun: (Int) => String, testBodyFun: (Int) => String): File = {
  targetDir.mkdirs()
  val targetFile = new File(targetDir, "ExampleSpec" + fileNumber + ".scala")
  val targetOut = new BufferedWriter(new FileWriter(targetFile))
  try {
    targetOut.write("package " + packageName + "\n\n")
    importStatements.foreach { s =>
      targetOut.write("import " + s + "\n")
    }
    targetOut.write("\n")
    targetOut.write("class ExampleSpec" + fileNumber + " extends " + extendsName + " " + withNames.map(n => " with " + n).mkString(" ") + " {\n")
    targetOut.write("  " + scopeDef + (if (scopeBracket) "{" else "") + " \n")
    for (x <- 1 to testCount) {
      targetOut.write("    " + testDefFun(x) + " {\n")
      targetOut.write("      " + testBodyFun(x) + "\n")
      targetOut.write("    }\n")
    }
    targetOut.write("  " + (if (scopeBracket) "}" else "") + "\n")
    targetOut.write("}\n")
  }
  finally {
    targetOut.flush()
    targetOut.close()
  }
  targetFile
}

def generateMultipleSourceFiles(testCount: Int, targetDir: File, packageName: String, importStatements: Array[String], 
                       extendsName: String, withNames: Array[String], scopeBracket: Boolean, scopeDef: String, 
                       testDefFun: (Int) => String, testBodyFun: (Int) => String): List[String] = {
  targetDir.mkdirs()
  val totalFiles = 100

  val sourceFiles = for (fileNumber <- 1 to totalFiles) yield
                       generateSourceFile(testCount, targetDir, fileNumber, packageName, importStatements, 
                                          extendsName, withNames, scopeBracket, scopeDef, testDefFun, testBodyFun).getAbsolutePath

  sourceFiles.toList
}

// Using assert(==)
def assert2TestBodyFun(x: Int): String = "assert(" + x + " + 1 == " + (x+1) + ")"
// Using assert(===)
def assert3TestBodyFun(x: Int): String = "assert(" + x + " + 1 === " + (x+1) + ")"
// Using must matchers
def mustTestBodyFun(x: Int): String = x + " + 1 must be (" + (x+1) + ")"

// Spec 
def specTestDefFun(x: Int): String = "def `increment " + x + "`"
// WordSpec
def wordSpecTestDefFun(x: Int): String = "\"increment " + x + "\" in"
//FunSuite
def funSuiteTestDefFun(x: Int): String = "test(\"increment " + x + "\")"
//FunSpec
def funSpecTestDefFun(x: Int): String = "it(\"increment " + x + "\")"
//FreeSpec
def freeSpecTestDefFun(x: Int): String = "\"increment " + x + "\" in"
//FlatSpec
def flatSpecTestDefFun(x: Int): String = "it should \"increment " + x + "\" in"

// Specs2 mutable specification
def generateSpecs2Mutable(testCount: Int, targetDir: File, fileNumber: Int): File = {
  targetDir.mkdirs()
  val targetFile = new File(targetDir, "ExampleSpec" + fileNumber + ".scala")
  val targetOut = new BufferedWriter(new FileWriter(targetFile))
  try {
    targetOut.write("""
package mSpecification
          
import org.specs2.mutable._
          
class ExampleSpec""" + fileNumber + """ extends Specification {
  "Scala" can {
""")
      
    for (x <- 1 to testCount) {
      targetOut.write("    \"increment " + x + "\" in {\n")
      targetOut.write("      " + x + " + 1 must be equalTo (" + (x+1) + ")\n")
      targetOut.write("    }\n")
    }
     
    targetOut.write("  }\n")
    targetOut.write("}")
  }
  finally {
    targetOut.flush()
    targetOut.close()
  }
  targetFile
}

def generateMultipleSpecs2Mutable(testCount: Int, targetDir: File): List[String] = {
  targetDir.mkdirs()
  val totalFiles = 100

  val sourceFiles = for (fileNumber <- 1 to totalFiles) yield generateSpecs2Mutable(testCount, targetDir, fileNumber).getAbsolutePath
  
  sourceFiles.toList
}

// Specs2 immutable specification
def generateSpecs2Immutable(testCount: Int, targetDir: File, fileNumber: Int): File = {
  targetDir.mkdirs()
  val targetFile = new File(targetDir, "ExampleSpec" + fileNumber + ".scala")
  val targetOut = new BufferedWriter(new FileWriter(targetFile))
  try {
    targetOut.write("""
package iSpecification
          
import org.specs2._
          
class ExampleSpec""" + fileNumber + """ extends Specification { def is =
  "Scala can"  ^
""")
      
    for (x <- 1 to testCount)
      targetOut.write("    \"increment " + x + "\"  ! e" + x + "^\n")
     
    targetOut.write("    end\n")
        
    for (x <- 1 to testCount) 
      targetOut.write("def e" + x + " = " + x + " + 1 must be equalTo (" + (x+1) + ")\n")
      
    targetOut.write("""
}""")
  }
  finally {
    targetOut.flush()
    targetOut.close()
  }
  targetFile
}

def generateMultipleSpecs2Immutable(testCount: Int, targetDir: File): List[String] = {
  targetDir.mkdirs()
  val totalFiles = 100

  val sourceFiles = for (fileNumber <- 1 to totalFiles) yield generateSpecs2Immutable(testCount, targetDir, fileNumber).getAbsolutePath

  sourceFiles.toList
}

def compile(srcFiles: List[String], classpath: String, targetDir: String) = {
  import scala.collection.JavaConversions._
  
  val command = List("scalac", "-classpath", classpath, "-d", targetDir) ::: srcFiles
  val builder = new ProcessBuilder(command)
  builder.redirectErrorStream(true)
  val start = System.currentTimeMillis
  val process = builder.start()

  val stdout = new BufferedReader(new InputStreamReader(process.getInputStream))

  var line = "Compiling " + srcFiles.mkString(", ") + "..."
  
  while (line != null) {
    println (line)
    line = stdout.readLine
  }
  
  val end = System.currentTimeMillis
  end - start
}

def getFileAndByteCount(srcDir: File) = {
  @tailrec
  def getFileAndByteCountAcc(dirList: Array[File], fileCount: Long, byteCount: Long): Tuple2[Long, Long] = {
    val (files, subDirs) = dirList.partition(_.isFile)
    val classFiles = files.filter(f => f.getName.endsWith(".class"))
    val newFileCount = fileCount + classFiles.size
    val newByteCount = byteCount + classFiles.map { f => f.length.toLong }.foldLeft(0l) { (a, b) => a + b }
    if (subDirs.isEmpty) 
      (newFileCount, newByteCount)
    else 
      getFileAndByteCountAcc(subDirs.flatMap(d => d.listFiles), newFileCount, newByteCount)
  }
  getFileAndByteCountAcc(srcDir.listFiles, 0l, 0l)
}

def deleteDir(targetDir: File) {
  val children = targetDir.listFiles
  if (children != null) {
    targetDir.listFiles.foreach { child => 
      if (child.isFile) 
        child.delete()
      else
        deleteDir(child)
    }
    targetDir.delete()
  }
  else
    println("Unable to list files in " + targetDir.getAbsolutePath)
}

def getOutputDir(baseOutputDir: File, testCount: Int): File = {
  val outputDirName = "output-" + testCount
  val outputDir = new File(baseOutputDir, outputDirName)
  outputDir.mkdirs()
  outputDir
}

case class Style(name: String, className: String, scopeBracket: Boolean, scopeDef: String, testDefFun: (Int) => String)
case class TestType(name: String, shortName: String, importNames: Array[String], mixinNames: Array[String], testBodyFun: (Int) => String)

if (scalaVersion != "unknown") {
  val scalatestJar = new File("scalatest_" + scalaVersion + "-" + scalaTestVersion + ".jar")
  if (!scalatestJar.exists)
    downloadFile("https://oss.sonatype.org/content/repositories/releases/org/scalatest/scalatest_" + scalaVersion + "/" + scalaTestVersion + "/scalatest_" + scalaVersion + "-" + scalaTestVersion + ".jar", scalatestJar)

  val specs2Jar = new File("specs2_" + scalaVersion + "-" + specs2Version + ".jar")
  if (!specs2Jar.exists)
    downloadFile("https://oss.sonatype.org/content/repositories/releases/org/specs2/specs2_" + scalaVersion + "/" + specs2Version + "/specs2_" + scalaVersion + "-" + specs2Version + ".jar", specs2Jar)

  val specs2ScalazJar = new File("specs2-scalaz-core_" + scalaVersion + "-" + scalazVersion + ".jar")
  if (!specs2ScalazJar.exists)
    downloadFile("https://oss.sonatype.org/content/repositories/releases/org/specs2/specs2-scalaz-core_" + scalaVersion + "/" + scalazVersion + "/specs2-scalaz-core_" + scalaVersion + "-" + scalazVersion + ".jar", specs2ScalazJar)

  val baseDir = new File("testsIn100Files")
  if (baseDir.exists)
    deleteDir(baseDir)
    
  val statDir = new File(baseDir, "stat")
  statDir.mkdirs()

  val durationFile = new FileWriter(new File(statDir, "duration.csv"))
  val fileCountFile = new FileWriter(new File(statDir, "filecount.csv"))
  val fileSizeFile = new FileWriter(new File(statDir, "filesize.csv"))

 val scalaTestClasspath = scalatestJar.getName 

  val specs2Classpath = specs2Jar.getName + File.pathSeparator + specs2ScalazJar.getName

  val baseOutputDir = new File(baseDir, "output")
  baseOutputDir.mkdirs()

  val baseGeneratedDir = new File(baseDir, "generated")
  baseGeneratedDir.mkdirs()

  val styles = 
    Array(
      Style("scalatest.Spec", "Spec", true, "object `Scala can ` ", specTestDefFun), 
      Style("scalatest.WordSpec", "WordSpec", true, "\"Scala\" can ", wordSpecTestDefFun)
    )

  val testTypes = 
    Array(
      TestType("with MustMatchers", "Must", Array("org.scalatest._", "matchers.MustMatchers._"), Array.empty, mustTestBodyFun)
    )

  val testCounts = 
    Array(
        0,
       10, 
       20, 
       30, 
       40, 
       50, 
       60, 
       70, 
       80, 
       90, 
      100
    )
  
  val headers = "TestCount," + testCounts.mkString(",") + "\n"
  durationFile.write(headers)
  fileCountFile.write(headers)
  fileSizeFile.write(headers)

  styles.foreach { style => 
    testTypes.foreach { testType => 
      try {
        durationFile.write(style.name) // Don't write with MustMatchers to get all 4 names to fit on graph
        durationFile.flush()
        fileCountFile.write(style.name)
        fileCountFile.flush()
        fileSizeFile.write(style.name)
        fileSizeFile.flush()
        testCounts.foreach { testCount =>
          println("Working on " + style.className +" " + testType.name + " test count " + testCount + "...")
          val outputDir = getOutputDir(baseOutputDir, testCount)
          val generatedDir = new File(baseGeneratedDir, "generated-" + testCount)

          val generatedSrc = generateMultipleSourceFiles(
                                 testCount, 
                                 new File(generatedDir, style.className + testType.shortName), // target dir
                                 style.className + testType.shortName, // package name
                                 testType.importNames, // imports
                                 style.className, // extends
                                 testType.mixinNames, // mixin
                                 style.scopeBracket, // scope requires bracket or not
                                 style.scopeDef, // scope definition
                                 style.testDefFun, 
                                 testType.testBodyFun)


          val duration = compile(generatedSrc, scalaTestClasspath, outputDir.getAbsolutePath)
          durationFile.write("," + duration)
          durationFile.flush()

          val (fileCount, fileSize) = getFileAndByteCount(new File(outputDir, style.className + testType.shortName))
          fileCountFile.write("," + fileCount)
          fileCountFile.flush()
          fileSizeFile.write("," + fileSize)
          fileSizeFile.flush()
        }
      }
      catch {
        case e: Throwable => 
          e.printStackTrace()
      }
      finally {
        durationFile.write("\n")
        durationFile.flush()
        fileCountFile.write("\n")
        fileCountFile.flush()
        fileSizeFile.write("\n")
        fileSizeFile.flush()
      }
    }
  }

  // Specs2 mutable
  durationFile.write("specs2.mutable.Specification")
  durationFile.flush()
  fileCountFile.write("specs2.mutable.Specification")
  fileCountFile.flush()
  fileSizeFile.write("specs2.mutable.Specification")
  fileSizeFile.flush()
  try {
    testCounts.foreach { testCount =>
      println("Working on specs2.mutable.Specification test count " + testCount + "...")
      val outputDir = getOutputDir(baseOutputDir, testCount)
      val generatedDir = new File(baseGeneratedDir, "generated-" + testCount)

      val generatedSrc = generateMultipleSpecs2Mutable(testCount, new File(generatedDir, "mSpecification"))
      val duration = compile(generatedSrc, specs2Classpath, outputDir.getAbsolutePath)
      durationFile.write("," + duration)
      durationFile.flush()

      val (fileCount, fileSize) = getFileAndByteCount(new File(outputDir, "mSpecification"))
      fileCountFile.write("," + fileCount)
      fileCountFile.flush()
      fileSizeFile.write("," + fileSize)
      fileSizeFile.flush()
    }
  }
  catch {
    case e: Throwable => 
      e.printStackTrace()
  }
  finally {
    durationFile.write("\n")
    durationFile.flush()
    fileCountFile.write("\n")
    fileCountFile.flush()
    fileSizeFile.write("\n")
    fileSizeFile.flush()
  }

  // Specs2 immutable
  durationFile.write("specs2.Specification")
  durationFile.flush()
  fileCountFile.write("specs2.Specification")
  fileCountFile.flush()
  fileSizeFile.write("specs2.Specification")
  fileSizeFile.flush()
  try {
    testCounts.foreach { testCount =>
      println("Working on specs2.Specification test count " + testCount + "...")
      val outputDir = getOutputDir(baseOutputDir, testCount)
      val generatedDir = new File(baseGeneratedDir, "generated-" + testCount)

      val generatedSrc = generateMultipleSpecs2Immutable(testCount, new File(generatedDir, "iSpecification"))
      val duration = compile(generatedSrc, specs2Classpath, outputDir.getAbsolutePath)
      durationFile.write("," + duration)
      durationFile.flush()

      val (fileCount, fileSize) = getFileAndByteCount(new File(outputDir, "iSpecification"))
      fileCountFile.write("," + fileCount)
      fileCountFile.flush()
      fileSizeFile.write("," + fileSize)
      fileSizeFile.flush()
    }
  }
  catch {
    case e: Throwable => 
      e.printStackTrace()
  }
  finally {
    durationFile.write("\n")
    durationFile.flush()
    fileCountFile.write("\n")
    fileCountFile.flush()
    fileSizeFile.write("\n")
    fileSizeFile.flush()
  }

  durationFile.flush()
  durationFile.close()
  fileCountFile.flush()
  fileCountFile.close()
  fileSizeFile.flush()
  fileSizeFile.close()
}
else
  println("ERROR: Unable to detect Scala version.")
