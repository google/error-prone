java_library(
    name = "MyCustomCheck",
    srcs = ["MyCustomCheck.java"],
    deps = [
        "@error_prone//jar",
        "@guava//jar",
        "//third_party/java/auto_service",
    ],
)

java_library(
    name = "NullAnnotationProcessor",
    srcs = ["NullAnnotationProcessor.java"],
)

java_plugin(
    name = "MyCustomCheckPlugin",
    processor_class = "com.google.errorprone.sample.NullAnnotationProcessor",
    deps = [
        ":MyCustomCheck",
        ":NullAnnotationProcessor",
    ],
)

java_library(
    name = "Hello",
    srcs = ["Hello.java"],
    plugins = [":MyCustomCheckPlugin"],
)
