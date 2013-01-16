import java.io._
import scala.io.Source

case class DataRow(name: String, dataArray: Array[Long])
case class Csv(headers: Array[String], rows: Array[DataRow])
case class Chart(name: String, title: String, data: Csv)

def readCsv(filePath: String): Csv = { // Does not cater for csv escape, should be fine for our data.
  val lines = Source.fromFile(filePath).getLines().toList
  if (lines.length == 0)
    throw new RuntimeException(filePath + " is empty.")
  val table = lines.map(_.split(",").toList).transpose
  val headers = table(0).map(_.trim).toArray
  val rows = table.drop(1).map { dataList => 
    val name = dataList(0)
    val dataArray = dataList.drop(1).map(_.toLong).toArray
    DataRow(name, dataArray)
  }.toArray
  new Csv(headers, rows)
}

def filterCsv(csv: Csv, filterFun: DataRow => Boolean): Csv = 
  Csv(csv.headers, csv.rows.filter(filterFun(_)))

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
        "    title: '" + chart.title + "'\n" + 
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
        "<div id=\"" + chart.name + "_div\" style=\"width: 900px; height: 500px;\"></div>"
      }.mkString("\n"))
    }
  </body>
</html>

val durationData = readCsv("csv/duration.csv")
val fileSizeData = readCsv("csv/filesize.csv")
val fileCountData = readCsv("csv/filecount.csv")

val googleGraphDir = new File("google-graphs")
if (!googleGraphDir.exists)
  googleGraphDir.mkdirs()

val allFile = new FileWriter(new File(googleGraphDir, "all.html"))

allFile.write(
  generateLineChart(
    Array(
      Chart("duration100", "Duration (miliseconds)", filterCsv(durationData, _.name.toLong <= 100)), 
      Chart("duration500", "Duration (miliseconds)", filterCsv(durationData, (row: DataRow) => row.name.toLong == 0 || row.name.toLong >= 100)), 
      Chart("filesize100", "File Size (bytes)", filterCsv(fileSizeData, _.name.toLong <= 100)), 
      Chart("filesize500", "File Size (bytes)", filterCsv(fileSizeData, (row: DataRow) => row.name.toLong == 0 || row.name.toLong >= 100)), 
      Chart("filecount100", "File Count", filterCsv(fileCountData, _.name.toLong <= 100)), 
      Chart("filecount500", "File Count", filterCsv(fileCountData, (row: DataRow) => row.name.toLong == 0 || row.name.toLong >= 100))
    )
  ).toString
)
allFile.flush()
allFile.close()
