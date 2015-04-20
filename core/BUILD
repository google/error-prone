# Description:
#   Error prone is a library for matching Javac ASTs, and detecting common
#   Java programming mistakes.

package(default_visibility = ["//visibility:public"])

licenses(["notice"])  # Apache 2.0

APPLY_SRCS = [
    "src/main/java/com/google/errorprone/matchers/Description.java",
    "src/main/java/com/google/errorprone/fixes/Replacement.java",
    "src/main/java/com/google/errorprone/fixes/Fix.java",
    "src/main/java/com/google/errorprone/DescriptionListener.java",
]

BUGPATTERN_SRCS = glob(
    ["src/main/java/com/google/errorprone/bugpatterns/**/*.java"],
    exclude = [
        "src/main/java/com/google/errorprone/bugpatterns/BugChecker.java",
        "**/*$*",
    ],
)

TOPLEVEL_SRCS = glob(
    [
        "src/main/java/com/google/errorprone/ErrorProneAnalyzer.java",
        "src/main/java/com/google/errorprone/ErrorProneCompiler.java",
        "src/main/java/com/google/errorprone/ErrorProneJavacJavaCompiler.java",
        "src/main/java/com/google/errorprone/ErrorProneJavaCompiler.java",
        "src/main/java/com/google/errorprone/ErrorProneOptions.java",
        "src/main/java/com/google/errorprone/InvalidCommandLineOptionException.java",
        "src/main/java/com/google/errorprone/analysis/*.java",
        "src/main/java/com/google/errorprone/scanner/*.java",
    ],
)

java_library(
    name = "apply",
    srcs = APPLY_SRCS,
    deps = [
        "//annotation",
        "//external:guava-jar",
        "//external:javac-jar",
        "//external:jsr305_annotations-jar",
        "//third_party/java/auto:auto_value",
    ],
)

java_library(
    name = "toplevel_lib",
    srcs = TOPLEVEL_SRCS,
    deps = [
        ":checker_lib",
        ":core_deps",
        ":core_lib",
    ],
)

java_library(
    name = "resources",
    resources = glob(["**/*.properties"]),
)

java_library(
    name = "core_deps",
    exports = [
        ":apply",
        ":resources",
        "//annotation",
        "//annotations",
        "//external:dataflow-jar",
        "//external:guava-jar",
        "//external:javac-jar",
        "//external:jcip_annotations-jar",
        "//external:jformatstring-jar",
        "//external:jsr305_annotations-jar",
        "//external:pcollections-jar",
        "//third_party/java/auto:auto_value",
    ],
)

java_library(
    name = "core_lib",
    srcs = glob(
        [
            "src/main/java/**/*.java",
        ],
        exclude = APPLY_SRCS + BUGPATTERN_SRCS + TOPLEVEL_SRCS,
    ),
    deps = [":core_deps"],
)

java_library(
    name = "checker_lib",
    srcs = BUGPATTERN_SRCS,
    deps = [
        ":core_deps",
        ":core_lib",
    ],
)

java_library(
    name = "core",
    exports = [
        ":apply",
        ":checker_lib",
        ":core_lib",
        ":toplevel_lib",
    ],
)

java_library(
    name = "test_external",
    exports = [
        "//external:dataflow-jar",
        "//external:guava-jar",
        "//external:guice-jar",
        "//external:guice_assistedinject-jar",
        "//external:guice_servlet-jar",
        "//external:hamcrest_core-jar",
        "//external:hamcrest_library-jar",
        "//external:javac-jar",
        "//external:javacutil-jar",
        "//external:jcip_annotations-jar",
        "//external:jformatstring-jar",
        "//external:jimfs-jar",
        "//external:jsr305_annotations-jar",
        "//external:jsr330_inject-jar",
        "//external:junit-jar",
        "//external:mockito-jar",
        "//external:mockito_core-jar",
        "//external:objenesis-jar",
        "//external:pcollections-jar",
        "//external:protobuf-jar",
        "//external:truth-jar",
        "//third_party/java/auto:auto_value",
    ],
)

TEST_COMMON = [
    "src/test/java/com/google/errorprone/CompilationTestHelper.java",
    "src/test/java/com/google/errorprone/DogfoodErrorProne.java",
    "src/test/java/com/google/errorprone/util/InheritedAnnotation.java",
    "src/test/java/com/google/errorprone/DiagnosticTestHelper.java",
    "src/test/java/com/google/errorprone/ErrorProneInMemoryFileManager.java",
    "src/test/java/com/google/errorprone/ErrorProneTestCompiler.java",
    "src/test/java/com/google/errorprone/NullAnnotationProcessor.java",
    "src/test/java/com/google/errorprone/matchers/CompilerBasedAbstractTest.java",
    "src/test/java/com/google/errorprone/matchers/DescendantOfAbstractTest.java",
]

java_library(
    name = "test_common",
    srcs = TEST_COMMON,
    deps = [
        ":core",
        ":test_external",
    ],
)

TESTDATA = glob(["src/test/java/**/testdata/**"])

TESTS = glob(
    ["src/test/java/**/*.java"],
    exclude = TESTDATA + TEST_COMMON,
)

java_test(
    name = "tests",
    size = "medium",
    srcs = glob(
        TESTS,
        exclude = [
            # broken by langtools / classpath skew
            # TODO(cushon): figure out how to use javac9 properly
            "src/test/java/com/google/errorprone/bugpatterns/threadsafety/GuardedByBinderTest.java",
        ],
    ),
    resources = TESTDATA,
    deps = [
        ":core",
        ":test_common",
        ":test_external",
        "//annotation",
        "//annotations",
    ],
)

java_binary(
    name = "error_prone",
    main_class = "com.google.errorprone.ErrorProneCompiler",
    runtime_deps = [":core"],
)
