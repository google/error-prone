licenses(["notice"])  # Apache License 2.0

java_library(
    name = "auto_service",
    exported_plugins = [":auto_service_plugin"],
    visibility = ["//visibility:public"],
    exports = ["@auto_service//jar"],
    runtime_deps = ["@guava//jar"],
)

java_plugin(
    name = "auto_service_plugin",
    processor_class = "com.google.auto.service.processor.AutoServiceProcessor",
    deps = [
        "@auto_common//jar",
        "@auto_service//jar",
        "@guava//jar",
    ],
)
