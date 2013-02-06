import java.io._
import scala.io.Source

case class DataRow(name: String, dataArray: Array[Long])
case class Csv(headers: Array[String], rows: Array[DataRow], minTestCount: Int, maxTestCount: Int)
case class Chart(name: String, title: String, data: Csv)

def readCsv(srcDir: File, filePath: String): Csv = { // Does not cater for csv escape, should be fine for our data.
  val lines = Source.fromFile(new File(srcDir, filePath)).getLines().toList
  if (lines.length == 0)
    throw new RuntimeException(filePath + " is empty.")
  val tableWithPossiblyUnequalRowsLengths = lines.map(_.split(",").toList)
  val headersCount = tableWithPossiblyUnequalRowsLengths(0).length
  if (headersCount <= 1)
    throw new RuntimeException(filePath + " does not contains valid data.")
  val minTestCount = tableWithPossiblyUnequalRowsLengths(0)(1).toInt
  val maxTestCount = tableWithPossiblyUnequalRowsLengths(0)(headersCount -1).toInt
   // This makes all rows the same, so I can transpose (rows may not be the same because of an OOM exception)
  val tableWithEqualRowLengths = for (xs <- tableWithPossiblyUnequalRowsLengths) yield xs.padTo(headersCount, "0")
  // val table = lines.map(_.split(",").toList).transpose
  val table = tableWithEqualRowLengths.transpose
  val headers = table(0).map(_.trim).toArray
  val rows = table.drop(1).map { dataList => 
    val name = dataList(0)
    val dataArray = dataList.drop(1).map(_.toLong).toArray
    DataRow(name, dataArray)
  }.toArray
  new Csv(headers, rows, minTestCount, maxTestCount)
}

def filterCsv(csv: Csv, filterFun: DataRow => Boolean): Csv = {
  val filteredRows = csv.rows.filter(filterFun(_))
  val testCounts = filteredRows.map(_.name.toInt)
  val minTestCount = testCounts.min
  val maxTestCount = testCounts.max
  Csv(csv.headers, filteredRows, minTestCount, maxTestCount)
}

def generateLineChart(charts: Array[Chart]) = 
  <html>
  <head>
    <script type="text/javascript" src="https://www.google.com/jsapi"></script>
    <script type="text/javascript">
    {
      scala.xml.Unparsed("google.load(\"visualization\", \"1\", {packages:[\"corechart\"]});\n" +
      "google.setOnLoadCallback(drawChart);\n" + 
      "function drawChart() {\n" + 
      charts.map { chart => 
        "  var " + chart.name + "Data = google.visualization.arrayToDataTable([\n" + 
        "      [" + chart.data.headers.map("'" + _ + "'").mkString(",") + "],\n" +
        chart.data.rows.map(d => "    ['" + d.name + "'," + d.dataArray.mkString(",") + "]").mkString(",\n") +
        "  ]);" + "\n" + 
        "  var " + chart.name + "Options = {\n" + 
        "    title: '" + chart.title + "',\n" + 
        "    legend: { position: 'bottom' },\n" + 
        "    colors: ['blue', 'green', 'yellow', 'red']\n" +
        "  };\n" + 
        "  var " + chart.name + "Chart = new google.visualization.LineChart(document.getElementById('" + chart.name + "_div'));\n" + 
        "  " + chart.name + "Chart.draw(" + chart.name + "Data, " + chart.name + "Options);\n"
      }.mkString("\n") + 
      "}")
    }
    </script>
  </head>
  <body>
    {
      scala.xml.Unparsed(charts.map { chart =>
        "<table><tr><td><div id=\"" + chart.name + "_div\" style=\"width: 1100px; height: 550px;\"></div></td><td></td></tr></table>"
      }.mkString("\n"))
    }
  </body>
</html>

def generateChartFile(srcDir: File, targetFile: File) {

  val durationData = readCsv(srcDir, "duration.csv")
  val fileSizeData = readCsv(srcDir, "filesize.csv")
  val fileCountData = readCsv(srcDir, "filecount.csv")

  val graphFile = new FileWriter(targetFile)

  println(durationData.minTestCount + " " + durationData.maxTestCount + " " + durationData.rows.length)
  val durationSmall =
    if (durationData.maxTestCount != 1000 && durationData.rows.length != 11) 
      List(Chart("duration100", "Compile time (milliseconds)", filterCsv(durationData, _.name.toLong <= 100)))
    else
      List.empty[Chart]

  val durationLarge =
    if (durationData.maxTestCount > 100)
      List[Chart](Chart("duration500", "Compile time (milliseconds)", filterCsv(durationData, (row: DataRow) => row.name.toLong == 0 || row.name.toLong >= 100)))
    else
      List.empty[Chart]

  val fileSizeSmall =
    if (durationData.maxTestCount != 1000 && durationData.rows.length != 11) 
      List(Chart("filesize100", "File Size (bytes)", filterCsv(fileSizeData, _.name.toLong <= 100)))
    else
      List.empty[Chart]

  val fileSizeLarge = 
    if (fileSizeData.maxTestCount > 100)
      List[Chart](Chart("filesize500", "File Size (bytes)", filterCsv(fileSizeData, (row: DataRow) => row.name.toLong == 0 || row.name.toLong >= 100)))
    else
      List.empty[Chart]

  val fileCountSmall =
    if (durationData.maxTestCount != 1000 && durationData.rows.length != 11) 
      List(Chart("filecount100", "File Count", filterCsv(fileCountData, _.name.toLong <= 100)))
    else
      List.empty[Chart]

  val fileCountLarge =
    if (fileCountData.maxTestCount > 100)
      List[Chart](Chart("filecount500", "File Count", filterCsv(fileCountData, (row: DataRow) => row.name.toLong == 0 || row.name.toLong >= 100)))
    else
      List.empty[Chart]

  val chartList: List[Chart] = 
   durationSmall ::: durationLarge ::: fileSizeSmall ::: fileSizeLarge ::: fileCountSmall ::: fileCountLarge

  graphFile.write(
    generateLineChart(
      chartList.toArray
    ).toString
  )
  graphFile.flush()
  graphFile.close()
  
  println("Generated " + targetFile.getAbsolutePath)
}

val allTestsInOneFileDir = new File("allTestsInOneFile")
val allTestsInOneFileStatDir = new File(allTestsInOneFileDir, "stat")
if (allTestsInOneFileStatDir.exists) 
  generateChartFile(allTestsInOneFileStatDir, new File(allTestsInOneFileDir, "allTestsInOneFile-graph.html"))
else
  println("allTestsInOneFile/stat directory does not exist, allTestsInOneFile/allTestsInOneFile-graph.html will not be generated.")
  
val tenTestsPerFileDir = new File("tenTestsPerFile")
val tenTestsPerFileStatDir = new File(tenTestsPerFileDir, "stat")
if (tenTestsPerFileStatDir.exists)
  generateChartFile(tenTestsPerFileStatDir, new File(tenTestsPerFileDir, "tenTestsPerFile-graph.html"))
else
  println("tenTestsPerFile/stat directory does not exist, tenTestsPerFile/tenTestsPerFile-graph.html will not be generated.")
  
val testsIn100FilesDir = new File("testsIn100Files")
val testsIn100FilesStatDir = new File(testsIn100FilesDir, "stat")
if (testsIn100FilesStatDir.exists)
  generateChartFile(testsIn100FilesStatDir, new File(testsIn100FilesDir, "testsIn100Files-graph.html"))
else
  println("testsIn100Files/stat directory does not exist, testsIn100Files/tenTestsPerFile-graph.html will not be generated.")

val dataTablesDir = new File("dataTables")
val dataTablesStatDir = new File(dataTablesDir, "stat")
if (dataTablesStatDir.exists)
  generateChartFile(dataTablesStatDir, new File(dataTablesDir, "dataTables-graph.html"))
else
  println("dataTables/stat directory does not exist, dataTables/dataTables-graph.html will not be generated.")
  
val allMethodTestsInOneFileDir = new File("allMethodTestsInOneFile")
val allMethodTestsInOneFileStatDir = new File(allMethodTestsInOneFileDir, "stat")
if (allMethodTestsInOneFileStatDir.exists)
  generateChartFile(allMethodTestsInOneFileStatDir, new File(allMethodTestsInOneFileDir, "allMethodTestsInOneFile-graph.html"))
else
  println("allMethodTestsInOneFile/stat directory does not exist, allMethodTestsInOneFile/allMethodTestsInOneFile-graph.html will not be generated.")
  
val assertTestsInOneFileDir = new File("assertTestsInOneFile")
val assertTestsInOneFileStatDir = new File(assertTestsInOneFileDir, "stat")
if (assertTestsInOneFileStatDir.exists)
  generateChartFile(assertTestsInOneFileStatDir, new File(assertTestsInOneFileDir, "assertTestsInOneFile-graph.html"))
else
  println("assertTestsInOneFile/stat directory does not exist, assertTestsInOneFile/assertTestsInOneFile-graph.html will not be generated.")
  
val allClassTestsInOneFileDir = new File("allClassTestsInOneFile")
val allClassTestsInOneFileStatDir = new File(allClassTestsInOneFileDir, "stat")
if (allClassTestsInOneFileStatDir.exists) 
  generateChartFile(allClassTestsInOneFileStatDir, new File(allClassTestsInOneFileDir, "allClassTestsInOneFile-graph.html"))
else
  println("allClassTestsInOneFile/stat directory does not exist, allClassTestsInOneFile/allClassTestsInOneFile-graph.html will not be generated.")
