import java.net._
import java.io._
import java.nio.channels.Channels
import scala.annotation.tailrec
import scala.math.pow

/*def scalaVersion = {
  val rawVersion = scala.util.Properties.scalaPropOrElse("version.number", "unknown")
  if (rawVersion.endsWith(".final"))
    rawVersion.substring(0, rawVersion.length - 6)
  else
    rawVersion
}*/

def scalaVersion = "2.10"
val scalaTestVersion = "2.0.M6-SNAP7"
val junitVersion = "4.11"  // JUnit depends on hamcrestVersion
val hamcrestVersion = "1.3"
val testngVersion = "6.8"

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

def generateSourceFile(testCount: Int, targetDir: File, packageName: String, importStatements: Array[String], classAnnotations: Array[String], 
                       extendsName: Option[String], withNames: Array[String], scopeBracket: Boolean, scopeDef: String, 
                       testDefFun: (Int) => String, testBodyFun: (Int) => String): File = {
  targetDir.mkdirs()
  val targetFile = new File(targetDir, "ExampleSpec.scala")
  val targetOut = new BufferedWriter(new FileWriter(targetFile))
  try {
    targetOut.write("package " + packageName + "\n\n")
    importStatements.foreach { s =>
      targetOut.write("import " + s + "\n")
    }
    targetOut.write("\n")
    if (classAnnotations.length > 0)
      targetOut.write(classAnnotations.map(n => "@" + n).mkString("\n") + "\n")
    targetOut.write("class ExampleSpec")
    if (extendsName.isDefined)
      targetOut.write(" extends " + extendsName.get)
    if (withNames.length > 0)
      targetOut.write(withNames.map(n => " with " + n).mkString(" "))
    targetOut.write(" {\n")
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

def expectResultBodyFun(x: Int): String = "expectResult(" + (x+1) + ") { " + x + " + 1 }"
def assertEqualsBodyFun(x: Int): String = "assertEquals(" + (x+1) + ", " + x + " + 1)"

// Spec 
def specTestDefFun(x: Int): String = "def increment" + x + "()"
// WordSpec
def wordSpecTestDefFun(x: Int): String = "\"increment " + x + "\" in"
// JUnit
def junitTestDefFun(x: Int): String = "@Test def increment" + x + "()"
// TestNG
def testngTestDefFun(x: Int): String = "@Test def increment" + x + "()"

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

case class Style(name: String, shortName: String, importNames: Array[String], classAnnotations: Array[String], extendsName: Option[String], 
                 mixinNames: Array[String], scopeBracket: Boolean, scopeDef: String, testDefFun: (Int) => String, testBodyFun: (Int) => String, 
                 classpath: String)

if (scalaVersion != "unknown") {
  val scalatestJar = new File("scalatest_" + scalaVersion + "-" + scalaTestVersion + ".jar")
  if (!scalatestJar.exists)
    downloadFile("https://oss.sonatype.org/content/repositories/releases/org/scalatest/scalatest_" + scalaVersion + "/" + scalaTestVersion + "/scalatest_" + scalaVersion + "-" + scalaTestVersion + ".jar", scalatestJar)

    
  val junitJar = new File("junit-" + junitVersion + ".jar")
  if (!junitJar.exists)
    downloadFile("http://repo1.maven.org/maven2/junit/junit/" + junitVersion + "/junit-" + junitVersion + ".jar", junitJar)
    
  val hamcrestJar = new File("hamcrest-core-" + hamcrestVersion + ".jar")
  if (!hamcrestJar.exists)
    downloadFile("http://repo1.maven.org/maven2/org/hamcrest/hamcrest-core/" + hamcrestVersion + "/hamcrest-core-" + hamcrestVersion + ".jar", hamcrestJar)

  val testngJar = new File("testng-" + testngVersion + ".jar")
  if (!testngJar.exists)
    downloadFile("http://repo1.maven.org/maven2/org/testng/testng/" + testngVersion + "/testng-" + testngVersion + ".jar", testngJar)
    
  val baseDir = new File("allMethodTestsInOneFile")
  if (baseDir.exists)
    deleteDir(baseDir)
    
  val statDir = new File(baseDir, "stat")
  statDir.mkdirs()

  val durationFile = new FileWriter(new File(statDir, "duration.csv"))
  val fileCountFile = new FileWriter(new File(statDir, "filecount.csv"))
  val fileSizeFile = new FileWriter(new File(statDir, "filesize.csv"))

  val scalaTestClasspath = scalatestJar.getName
  val junitClasspath = junitJar.getName + File.pathSeparator + hamcrestJar.getName
  val testngClasspath = testngJar.getName + File.pathSeparator + junitClasspath

  val baseOutputDir = new File(baseDir, "output")
  baseOutputDir.mkdirs()

  val baseGeneratedDir = new File(baseDir, "generated")
  baseGeneratedDir.mkdirs()
  
  val styles = 
    Array(
      Style(
        name = "scalatest.Spec",
        shortName = "Spec",
        importNames = Array("org.scalatest.Spec"),
        classAnnotations = Array.empty,
        extendsName = Some("Spec"),
        mixinNames = Array.empty,
        scopeBracket = false,
        scopeDef = "",
        testDefFun = specTestDefFun,
        testBodyFun = expectResultBodyFun,
        classpath = scalaTestClasspath
      ),
      Style(
        name = "scalatest.SpecLike",
        shortName = "SpecLike",
        importNames = Array("org.scalatest.SpecLike"),
        classAnnotations = Array.empty,
        extendsName = Some("SpecLike"),
        mixinNames = Array.empty,
        scopeBracket = false,
        scopeDef = "",
        testDefFun = specTestDefFun,
        testBodyFun = expectResultBodyFun,
        classpath = scalaTestClasspath
      ),
      Style(
        name = "JUnit",
        shortName = "JUnit",
        importNames = Array("org.junit.Assert.assertEquals", "org.junit.Test"),
        classAnnotations = Array.empty,
        extendsName = None,
        mixinNames = Array.empty,
        scopeBracket = false,
        scopeDef = "",
        testDefFun = junitTestDefFun,
        testBodyFun = assertEqualsBodyFun,
        classpath = junitClasspath
      ),
      Style(
        name = "TestNG",
        shortName = "TestNG",
        importNames = Array("org.testng.annotations.Test", "org.testng.AssertJUnit.assertEquals"),
        classAnnotations = Array.empty,
        extendsName = None,
        mixinNames = Array.empty,
        scopeBracket = false,
        scopeDef = "",
        testDefFun = testngTestDefFun,
        testBodyFun = assertEqualsBodyFun,
        classpath = testngClasspath
      )
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

  styles.foreach { style => 
    durationFile.write(style.name)
    durationFile.flush()
    fileCountFile.write(style.name)
    fileCountFile.flush()
    fileSizeFile.write(style.name)
    fileSizeFile.flush()
    try {
      testCounts.foreach { testCount =>
        println("Working on " + style.name + " test count " + testCount + "...")
        val outputDir = getOutputDir(baseOutputDir, testCount)
        val generatedDir = new File(baseGeneratedDir, "generated-" + testCount)

        val generatedSrc = generateSourceFile(
                             testCount, 
                             new File(generatedDir, style.extendsName.getOrElse("") + style.shortName), // target dir
                             style.extendsName.getOrElse("") + style.shortName, // package name
                             style.importNames, // imports
                             style.classAnnotations, // annotations
                             style.extendsName, // extends
                             style.mixinNames, // mixin
                             style.scopeBracket, // scope requires bracket or not
                             style.scopeDef, // scope definition
                             style.testDefFun, 
                             style.testBodyFun)
        val duration = compile(generatedSrc.getAbsolutePath, style.classpath, outputDir.getAbsolutePath)
        durationFile.write("," + duration)
        durationFile.flush()

        val (fileCount, fileSize) = getFileAndByteCount(new File(outputDir, style.extendsName.getOrElse("") + style.shortName))
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

  durationFile.flush()
  durationFile.close()
  fileCountFile.flush()
  fileCountFile.close()
  fileSizeFile.flush()
  fileSizeFile.close()
}
else
  println("ERROR: Unable to detect Scala version.")
