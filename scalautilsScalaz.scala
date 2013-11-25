import java.net._
import java.io._
import java.nio.channels.Channels
import scala.annotation.tailrec

def scalaVersion = "2.10"
val scalautilsVersion = "2.0"
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

def generateSourceFile(testCount: Int, targetDir: File, packageName: String, importStatements: Array[String],
                       equalFun: (Int) => String): File = {
  targetDir.mkdirs()
  val targetFile = new File(targetDir, "ExampleSpec.scala")
  val targetOut = new BufferedWriter(new FileWriter(targetFile))
  try {
    targetOut.write("package " + packageName + "\n\n")
    importStatements.foreach { s =>
      targetOut.write("import " + s + "\n")
    }
    targetOut.write("\n")
    targetOut.write("class ExampleSpec {\n")
    for (x <- 1 to testCount)
      targetOut.write("    " + equalFun(x) + "\n")
    targetOut.write("}\n")
  }
  finally {
    targetOut.flush()
    targetOut.close()
  }
  targetFile
}

def equalFun(x: Int): String = x + " + 1 === " + (x+1)

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

val scalautilsJar = new File("scalautils_" + scalaVersion + "-" + scalautilsVersion + ".jar")
if (!scalautilsJar.exists)
  downloadFile("https://oss.sonatype.org/content/repositories/releases/org/scalautils/scalautils_" + scalaVersion + "/" + scalautilsVersion + "/scalautils_" + scalaVersion + "-" + scalautilsVersion + ".jar", scalautilsJar)

val scalazJar = new File("scalaz-core_" + scalaVersion + "-" + scalazVersion + ".jar")
if (!scalazJar.exists)
  downloadFile("https://oss.sonatype.org/content/repositories/releases/org/scalaz/scalaz-core_" + scalaVersion + "/" + scalazVersion + "/scalaz-core_" + scalaVersion + "-" + scalazVersion + ".jar", scalazJar)

val baseDir = new File("scalautilsScalaz")
if (baseDir.exists)
  deleteDir(baseDir)

val statDir = new File(baseDir, "stat")
statDir.mkdirs()

val durationFile = new FileWriter(new File(statDir, "duration.csv"))
val fileCountFile = new FileWriter(new File(statDir, "filecount.csv"))
val fileSizeFile = new FileWriter(new File(statDir, "filesize.csv"))

val baseOutputDir = new File(baseDir, "output")
baseOutputDir.mkdirs()

val baseGeneratedDir = new File(baseDir, "generated")
baseGeneratedDir.mkdirs()

val scalautilsClasspath = scalautilsJar.getName
val scalazClasspath = scalazJar.getName

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

durationFile.write("scalautils")
durationFile.flush()
fileCountFile.write("scalautils")
fileCountFile.flush()
fileSizeFile.write("scalautils")
fileSizeFile.flush()

testCounts.foreach { testCount =>
  println("Working on scalautils test count " + testCount + "...")
  val outputDir = getOutputDir(baseOutputDir, testCount)
  val generatedDir = new File(baseGeneratedDir, "generated-" + testCount)

  val generatedSrc = generateSourceFile(
    testCount,
    new File(generatedDir, "scalautils"), // target dir
    "scalautils", // package name
    Array("org.scalautils.TypeCheckedTripleEquals._"), // imports
    equalFun)
  val duration = compile(generatedSrc.getAbsolutePath, scalautilsClasspath, outputDir.getAbsolutePath)
  durationFile.write("," + duration)
  durationFile.flush()

  val (fileCount, fileSize) = getFileAndByteCount(new File(outputDir, "scalautils"))
  fileCountFile.write("," + fileCount)
  fileCountFile.flush()
  fileSizeFile.write("," + fileSize)
  fileSizeFile.flush()
}

durationFile.write("\n")
durationFile.flush()
fileCountFile.write("\n")
fileCountFile.flush()
fileSizeFile.write("\n")
fileSizeFile.flush()

durationFile.write("scalaz")
durationFile.flush()
fileCountFile.write("scalaz")
fileCountFile.flush()
fileSizeFile.write("scalaz")
fileSizeFile.flush()

testCounts.foreach { testCount =>
  println("Working on scalaz test count " + testCount + "...")
  val outputDir = getOutputDir(baseOutputDir, testCount)
  val generatedDir = new File(baseGeneratedDir, "generated-" + testCount)

  val generatedSrc = generateSourceFile(
    testCount,
    new File(generatedDir, "scalaz"), // target dir
    "scalaz", // package name
    Array("Scalaz._"), // imports
    equalFun)
  val duration = compile(generatedSrc.getAbsolutePath, scalazClasspath, outputDir.getAbsolutePath)
  durationFile.write("," + duration)
  durationFile.flush()

  val (fileCount, fileSize) = getFileAndByteCount(new File(outputDir, "scalaz"))
  fileCountFile.write("," + fileCount)
  fileCountFile.flush()
  fileSizeFile.write("," + fileSize)
  fileSizeFile.flush()
}

durationFile.flush()
durationFile.close()
fileCountFile.flush()
fileCountFile.close()
fileSizeFile.flush()
fileSizeFile.close()