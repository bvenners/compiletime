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
val scalaTestVersion = "2.0"
val specs2Version = "2.3.4"
val scalazVersion = "7.0.4"

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

sealed trait Align
object Left extends Align
object Right extends Align
  
def padValue(value: String, columnWidth:Int, align: Align): String = {
  val padCount = columnWidth - value.length
  require(padCount >= 0)
  align match {
    case Left =>
      value + (" " * padCount)
    case Right => 
      (" " * padCount) + value
  }
}

val leftColHeader = "\"left\""
val rightColHeader = "\"right\""
val sumColHeader = "\"sum\""
  
def generateSourceFile(testCount: Int, targetDir: File): File = {
  targetDir.mkdirs()
  val targetFile = new File(targetDir, "ExampleSpec.scala")
  val targetOut = new BufferedWriter(new FileWriter(targetFile))
  
  val leftColValueWidth = testCount.toString.length
  val sumColValueWidth = (testCount + 1).toString.length
  val leftColWidth = List(leftColValueWidth, leftColHeader.length).max
  val rightColWidth = rightColHeader.length
  val sumColWidth = List(sumColValueWidth, sumColHeader.length).max
  
  try {
    targetOut.write("package WordSpecMust\n\n")
    
    targetOut.write("import org.scalatest._\n")
    targetOut.write("import prop.TableDrivenPropertyChecks._\n\n")
    
    targetOut.write("class ExampleSpec extends WordSpec with Matchers {\n\n")
    
    targetOut.write("  \"Scala\" can {\n")
    targetOut.write("    \"increment integers\" in {\n\n")
    
    targetOut.write("      val examples = \n")
    targetOut.write("        Table(\n")
    
    // print headers
    targetOut.write("          (" + padValue(leftColHeader, leftColWidth, Left) + ", " + padValue(rightColHeader, rightColWidth, Left) + ", " + padValue(sumColHeader, sumColWidth, Left) + ")" + (if (testCount > 0) ", \n" else "\n"))
    // print data rows
    for (x <- 1 to testCount) {
      targetOut.write("          (" + padValue(x.toString, leftColWidth, Right) + ", " + padValue("1", rightColWidth, Right) + ", " + padValue((x + 1).toString, sumColWidth, Right) + ")" + (if (x < testCount) ", \n" else "\n"))
    }
    targetOut.write("        )\n\n")

    targetOut.write("      forAll(examples) { (left, right, sum) => \n")
    targetOut.write("        left + right should be (sum)\n")
    targetOut.write("      }\n")
    
    targetOut.write("    }\n")
    targetOut.write("  }\n")
    targetOut.write("}\n")
  }
  finally {
    targetOut.flush()
    targetOut.close()
  }
  targetFile
}

def generateShapelessSourceFile(testCount: Int, targetDir: File): File = {
  targetDir.mkdirs()
  val targetFile = new File(targetDir, "ExampleSpec.scala")
  val targetOut = new BufferedWriter(new FileWriter(targetFile))

  val leftColValueWidth = testCount.toString.length
  val sumColValueWidth = (testCount + 1).toString.length
  val leftColWidth = List(leftColValueWidth, leftColHeader.length).max
  val rightColWidth = rightColHeader.length
  val sumColWidth = List(sumColValueWidth, sumColHeader.length).max

  try {
    targetOut.write("package shapelessTables\n\n")

    targetOut.write("import org.scalatest._\n")
    targetOut.write("import shapeless.TableChecker._\n")
    targetOut.write("import shapelessTable._\n\n")

    targetOut.write("class ExampleSpec extends WordSpec with Matchers {\n\n")

    targetOut.write("  \"Scala\" can {\n")
    targetOut.write("    \"increment integers\" in {\n\n")

    targetOut.write("      val examples = \n")
    targetOut.write("        Table(\n")

    // print headers
    targetOut.write("          (" + padValue(leftColHeader, leftColWidth, Left) + ", " + padValue(rightColHeader, rightColWidth, Left) + ", " + padValue(sumColHeader, sumColWidth, Left) + ")" + (if (testCount > 0) ", \n" else "\n"))
    // print data rows
    for (x <- 1 to testCount) {
      targetOut.write("          (" + padValue(x.toString, leftColWidth, Right) + ", " + padValue("1", rightColWidth, Right) + ", " + padValue((x + 1).toString, sumColWidth, Right) + ")" + (if (x < testCount) ", \n" else "\n"))
    }
    targetOut.write("        )\n\n")

    targetOut.write("      forAll(examples) { case (left, right, sum) => \n")
    targetOut.write("        left + right should be (sum)\n")
    targetOut.write("      }\n")

    targetOut.write("    }\n")
    targetOut.write("  }\n")
    targetOut.write("}\n")
  }
  finally {
    targetOut.flush()
    targetOut.close()
  }
  targetFile
}

// Specs2 mutable specification
def generateSpecs2Mutable(testCount: Int, targetDir: File): File = {
  targetDir.mkdirs()
  val targetFile = new File(targetDir, "ExampleSpec.scala")
  val targetOut = new BufferedWriter(new FileWriter(targetFile))
  
  val leftColValueWidth = testCount.toString.length
  val sumColValueWidth = (testCount + 1).toString.length
  val leftColWidth = List(leftColValueWidth, leftColHeader.length).max
  val rightColWidth = rightColHeader.length
  val sumColWidth = List(sumColValueWidth, sumColHeader.length).max
  
  try {    
    targetOut.write("package mSpecification\n\n")
    
    targetOut.write("import org.specs2.mutable._\n")
    targetOut.write("import org.specs2.matcher.DataTables\n\n")
    
    targetOut.write("class ExampleSpec extends Specification with DataTables {\n\n")
    
    targetOut.write("  \"Scala\" can {\n")
    targetOut.write("    \"increment integers\" in {\n\n")
    
    // print headers
    targetOut.write("          " + padValue(leftColHeader, leftColWidth, Left) + " | " + padValue(rightColHeader, rightColWidth, Left) + " | " + padValue(sumColHeader, sumColWidth, Left) + " |" + (if (testCount > 0) "\n" else "> {\n"))
    // print data rows
    for (x <- 1 to testCount) {
      targetOut.write("          " + padValue(x.toString, leftColWidth, Right) + " ! " + padValue("1", rightColWidth, Right) + " ! " + padValue((x + 1).toString, sumColWidth, Right) + " |" + (if (x < testCount) "\n" else "> {\n"))
    }
    targetOut.write("            (left, right, sum) => left + right must_== sum\n")
    targetOut.write("          }\n")
    
    targetOut.write("    }\n")
    targetOut.write("  }\n")
    targetOut.write("}\n")
  }
  finally {
    targetOut.flush()
    targetOut.close()
  }
  targetFile
}

def compile(srcFile: String, classpath: String, targetDir: String) = {
  import scala.collection.JavaConversions._

  val command = List("scalac", "-classpath", classpath, "-d", targetDir, srcFile)
  val builder = new ProcessBuilder(command)
  builder.redirectErrorStream(true)
  val start = System.currentTimeMillis
  val process = builder.start()

  val stdout = new BufferedReader(new InputStreamReader(process.getInputStream))

  var line = "Compiling " + srcFile + "..."
  while (line != null) {
    println (line)
    line = stdout.readLine
  }
  
  val end = System.currentTimeMillis
  end - start
}

def jar(jarFileName: String, classesDir: String) = {
  import scala.collection.JavaConversions._

  val command = List("jar", "cf", jarFileName, "-C", classesDir, ".")
  val builder = new ProcessBuilder(command)
  builder.redirectErrorStream(true)
  val start = System.currentTimeMillis
  val process = builder.start()

  val stdout = new BufferedReader(new InputStreamReader(process.getInputStream))

  var line = "Creating jar file " + jarFileName + "..."
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

if (scalaVersion != "unknown") {
  val scalatestJar = new File("scalatest_" + scalaVersion + "-" + scalaTestVersion + ".jar")
  if (!scalatestJar.exists)
    downloadFile("https://oss.sonatype.org/content/repositories/releases/org/scalatest/scalatest_" + scalaVersion + "/" + scalaTestVersion + "/scalatest_" + scalaVersion + "-" + scalaTestVersion + ".jar", scalatestJar)

  val specs2Jar = new File("specs2_" + scalaVersion + "-" + specs2Version + ".jar")
  if (!specs2Jar.exists)
    downloadFile("https://oss.sonatype.org/content/repositories/releases/org/specs2/specs2_" + scalaVersion + "/" + specs2Version + "/specs2_" + scalaVersion + "-" + specs2Version + ".jar", specs2Jar)

  val specs2ScalazJar = new File("scalaz-core_" + scalaVersion + "-" + scalazVersion + ".jar")
  if (!specs2ScalazJar.exists)
    downloadFile("https://oss.sonatype.org/content/repositories/releases/org/scalaz/scalaz-core_" + scalaVersion + "/" + scalazVersion + "/scalaz-core_" + scalaVersion + "-" + scalazVersion + ".jar", specs2ScalazJar)

  val shapelessJar = new File("shapeless_2.10.2-2.0.0-SNAPSHOT.jar")
  if (!shapelessJar.exists)
    downloadFile("http://oss.sonatype.org/content/repositories/snapshots/com/chuusai/shapeless_2.10.2/2.0.0-SNAPSHOT/shapeless_2.10.2-2.0.0-SNAPSHOT.jar", shapelessJar)

  val baseDir = new File("dataTables")
  if (baseDir.exists)
    deleteDir(baseDir)

  val shapelessTableJar = new File("shapeless-table.jar")
  if (!shapelessTableJar.exists) {
    val shapelessTableSource = new File("ShapelessTable.scala")
    val shapelessTableClassDir = new File(baseDir, "shapeless-table")
    shapelessTableClassDir.mkdirs()
    compile(shapelessTableSource.getAbsolutePath, shapelessJar.getName, shapelessTableClassDir.getAbsolutePath)
    jar(shapelessTableJar.getName, shapelessTableClassDir.getAbsolutePath)
  }
    
  val statDir = new File(baseDir, "stat")
  statDir.mkdirs()

  val durationFile = new FileWriter(new File(statDir, "duration.csv"))
  val fileCountFile = new FileWriter(new File(statDir, "filecount.csv"))
  val fileSizeFile = new FileWriter(new File(statDir, "filesize.csv"))

  val scalaTestClasspath = scalatestJar.getName

  val specs2Classpath = specs2Jar.getName + File.pathSeparator + specs2ScalazJar.getName

  val shapelessClasspath = scalatestJar.getName + File.pathSeparator + shapelessJar.getName + File.pathSeparator + shapelessTableJar.getName

  val baseOutputDir = new File(baseDir, "output")
  baseOutputDir.mkdirs()

  val baseGeneratedDir = new File(baseDir, "generated")
  baseGeneratedDir.mkdirs()

  val testCounts = 
    Array(
       10, 
       20, 
       30, 
       40, 
       50, 
       60, 
       70, 
       80, 
       90, 
      100, 
      200, 
      300, 
      400, 
      500, 
      600, 
      700, 
      800, 
      900, 
     1000
    )
  
  val headers = "TestCount," + testCounts.mkString(",") + "\n"
  durationFile.write(headers)
  fileCountFile.write(headers)
  fileSizeFile.write(headers)

  // ScalaTest WordSpec
  durationFile.write("scalatest.WordSpec")
  durationFile.flush()
  fileCountFile.write("scalatest.WordSpec")
  fileCountFile.flush()
  fileSizeFile.write("scalatest.WordSpec")
  fileSizeFile.flush()
  try {
    testCounts.foreach { testCount =>
      println("Working on scalatest.WordSpec test count " + testCount + "...")
      val outputDir = getOutputDir(baseOutputDir, testCount)
      val generatedDir = new File(baseGeneratedDir, "generated-" + testCount)
      
      val generatedSrc = generateSourceFile(testCount, new File(generatedDir, "WordSpecMust"))
      val duration = compile(generatedSrc.getAbsolutePath, scalaTestClasspath, outputDir.getAbsolutePath)
      durationFile.write("," + duration)
      durationFile.flush()

      val (fileCount, fileSize) = getFileAndByteCount(new File(outputDir, "WordSpecMust"))
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

      val generatedSrc = generateSpecs2Mutable(testCount, new File(generatedDir, "mSpecification"))
      val duration = compile(generatedSrc.getAbsolutePath, specs2Classpath, outputDir.getAbsolutePath)
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

  // Shapeless Table
  durationFile.write("shapeless")
  durationFile.flush()
  fileCountFile.write("shapeless")
  fileCountFile.flush()
  fileSizeFile.write("shapeless")
  fileSizeFile.flush()

  try {
    testCounts.foreach { testCount =>
      println("Working on Shapeless test count " + testCount + "...")
      val outputDir = getOutputDir(baseOutputDir, testCount)
      val generatedDir = new File(baseGeneratedDir, "generated-" + testCount)

      val generatedSrc = generateShapelessSourceFile(testCount, new File(generatedDir, "shapelessTables"))
      val duration = compile(generatedSrc.getAbsolutePath, shapelessClasspath, outputDir.getAbsolutePath)
      durationFile.write("," + duration)
      durationFile.flush()

      val (fileCount, fileSize) = getFileAndByteCount(new File(outputDir, "shapelessTables"))
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
