
# To record compile times of various numbers of tests contained in the same file:
scala allTestsInOneFile.scala
# The data will sho up in the allTestsInOneFile directory

# To record compile times of various numbers of tests contained with 10 tests per file:
scala tenTestsPerFile.scala
# The data will sho up in the tenTestsPerFile directory

# To generate line graphs of the results:
scala google-chart.scala
# The HTML will show up in:
# allTestsInOneFile/allTestsInOneFile-graph.html
# tenTestsPerFile/tenTestsPerFile-graph.html
