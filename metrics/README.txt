The MetricsResults.html file contains the summary generated from CodePro Analytics.
It is customized to report the LOC and Number of Characters per file.
It also contains per method data on the Cyclomatic Complexity (distinct number of paths in the code) on each method.

The complexity metric per method is parsed using the parseMetrics.py python script and generates a csv file.
The csv file (error-prone/methodComplexities.csv) has the format fileName:methodName,complexityValue
