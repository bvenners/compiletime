set -x
scala tenTestsPerFile.scala
. addmem.sh
scala allTestsInOneFile.scala
scala testsIn100Files.scala
scala dataTables.scala
scala google-chart.scala
