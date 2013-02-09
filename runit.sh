JAVA_OPTS="-server -Xmx2048M -Xms256M"
export JAVA_OPTS
ASPECTJ_HOME="/usr/artima/aspectj1.7"
export ASPECTJ_HOME
rm -rf profiling
scala profiling.scala
# scala allMethodTestsInOneFile.scala
# scala allClassTestsInOneFile.scala
# scala allTestsInOneFile.scala
# scala dataTables.scala
# scala google-chart.scala
