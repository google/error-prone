# Description:
#   A set of tools to generate or analyze java code based on annotations.

licenses(["notice"])  # Apache License 2.0

package(default_visibility = ["//visibility:public"])

java_plugin(
  name = "auto_value_processor",
  deps = ["//external:auto_value-jar"],
  processor_class = "com.google.auto.value.processor.AutoValueProcessor",
)

java_library(
  name = "auto_value",
  exports = ["//external:auto_value-jar"],
  exported_plugins = [":auto_value_processor"],
  visibility = ["//visibility:public"],
)
