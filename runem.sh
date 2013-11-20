set -x
JAVA_OPTS="-server -Xmx1024M -Xms128M"
export JAVA_OPTS
scala tenTestsPerFile.scala
JAVA_OPTS="-server -Xmx2048M -Xms256M -Xss6m"
export JAVA_OPTS
scala allTestsInOneFile.scala
scala testsIn100Files.scala
scala dataTables.scala
scala allMethodTestsInOneFile.scala
scala assertTestsInOneFile.scala
scala allClassTestsInOneFile.scala
scala google-chart.scala
