/*
 * Copyright 2026 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.util;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(TestParameterInjector.class)
public class SourcePathMatcherTest {

  @Test
  public void emptyMatcher() {
    SourcePathMatcher matcher = SourcePathMatcher.empty();
    assertThat(matcher.matches("java/com/google/Foo.java")).isFalse();
    assertThrows(NullPointerException.class, () -> matcher.matches((String) null));
  }

  @Test
  public void exactPathMatch() {
    SourcePathMatcher matcher =
        SourcePathMatcher.create(
            "java/com/google/foo/Bar.java",
            "java/com/google/foo/Baz.kt",
            "java/com/google/foo/Build.kts");
    assertThat(matcher.matches("java/com/google/foo/Bar.java")).isTrue();
    assertThat(matcher.matches("java/com/google/foo/Baz.kt")).isTrue();
    assertThat(matcher.matches("java/com/google/foo/Build.kts")).isTrue();
    assertThat(matcher.matches("java/com/google/foo/Other.java")).isFalse();
    assertThat(matcher.matches("java/com/google/foo/Bar.java.tmp")).isFalse();
  }

  @Test
  public void directoryPrefixMatch() {
    SourcePathMatcher matcher =
        SourcePathMatcher.create("java/com/google/foo/", "javatests/com/google/foo/");
    assertThat(matcher.matches("java/com/google/foo/Bar.java")).isTrue();
    assertThat(matcher.matches("java/com/google/foo/sub/Baz.java")).isTrue();
    assertThat(matcher.matches("javatests/com/google/foo/BarTest.java")).isTrue();
    assertThat(matcher.matches("java/com/google/other/Bar.java")).isFalse();
    assertThat(matcher.matches("java/com/google/foobar/Baz.java")).isFalse();
  }

  @Test
  public void directoryPrefixMatch_subsumedPrefixesPruned() {
    SourcePathMatcher matcher =
        SourcePathMatcher.create(
            "java/com/google/foo/", "java/com/google/foo/bar/", "java/com/google/foo/bar/baz/");
    assertThat(matcher.matches("java/com/google/foo/Bar.java")).isTrue();
    assertThat(matcher.matches("java/com/google/foo/bar/Baz.java")).isTrue();
    assertThat(matcher.matches("java/com/google/foo/bar/baz/Quux.java")).isTrue();
    assertThat(matcher.matches("java/com/google/foo/other/Other.java")).isTrue();
    assertThat(matcher.matches("java/com/google/other/Other.java")).isFalse();
  }

  @Test
  public void directoryPrefixMatch_binarySearchOrdering() {
    SourcePathMatcher matcher = SourcePathMatcher.create("b/c/", "b/e/", "m/n/", "z/w/");

    // Before first prefix
    assertThat(matcher.matches("a/Foo.java")).isFalse();
    assertThat(matcher.matches("b/b/Foo.java")).isFalse();

    // Matching first prefix
    assertThat(matcher.matches("b/c/Foo.java")).isTrue();
    assertThat(matcher.matches("b/c/sub/Foo.java")).isTrue();

    // Between b/c/ and b/e/
    assertThat(matcher.matches("b/d/Foo.java")).isFalse();
    assertThat(matcher.matches("b/c-other/Foo.java")).isFalse();

    // Matching second prefix
    assertThat(matcher.matches("b/e/Foo.java")).isTrue();

    // Between b/e/ and m/n/
    assertThat(matcher.matches("f/Foo.java")).isFalse();

    // Matching m/n/
    assertThat(matcher.matches("m/n/Foo.java")).isTrue();

    // Matching last prefix
    assertThat(matcher.matches("z/w/Foo.java")).isTrue();

    // After last prefix
    assertThat(matcher.matches("z/x/Foo.java")).isFalse();
  }

  @Test
  public void invalidPatterns_rejected() {
    // Leading slash
    assertThrows(
        IllegalArgumentException.class,
        () -> SourcePathMatcher.create("/java/com/google/foo/Bar.java"));
    assertThrows(
        IllegalArgumentException.class, () -> SourcePathMatcher.create("/java/com/google/foo/"));
    // Bare directory without trailing slash
    assertThrows(
        IllegalArgumentException.class, () -> SourcePathMatcher.create("java/com/google/foo"));
    // Non-java/kt file
    assertThrows(
        IllegalArgumentException.class,
        () -> SourcePathMatcher.create("java/com/google/foo/Bar.proto"));
    // Empty / slash only
    assertThrows(IllegalArgumentException.class, () -> SourcePathMatcher.create(""));
    assertThrows(IllegalArgumentException.class, () -> SourcePathMatcher.create("/"));
    // Doubled slashes
    assertThrows(
        IllegalArgumentException.class,
        () -> SourcePathMatcher.create("java/com//google/foo/Bar.java"));
    assertThrows(
        IllegalArgumentException.class, () -> SourcePathMatcher.create("java/com//google/foo/"));
    // Wildcards (* and ?) not supported
    assertThrows(
        IllegalArgumentException.class, () -> SourcePathMatcher.create("java/com/google/foo/**"));
    assertThrows(
        IllegalArgumentException.class, () -> SourcePathMatcher.create("java/com/google/foo/*"));
    assertThrows(
        IllegalArgumentException.class, () -> SourcePathMatcher.create("**/*Templates.java"));
    assertThrows(
        IllegalArgumentException.class, () -> SourcePathMatcher.create("java/com/*/Bar.java"));
    assertThrows(
        IllegalArgumentException.class,
        () -> SourcePathMatcher.create("java/com/google/foo/Bar?.java"));
    // Path traversal
    assertThrows(
        IllegalArgumentException.class, () -> SourcePathMatcher.create("java/com/../foo/Bar.java"));
    assertThrows(IllegalArgumentException.class, () -> SourcePathMatcher.create("../foo/Bar.java"));
    assertThrows(
        IllegalArgumentException.class, () -> SourcePathMatcher.create("./java/com/foo/Bar.java"));
    assertThrows(
        IllegalArgumentException.class, () -> SourcePathMatcher.create("java/com/./foo/Bar.java"));
  }

  private enum CanonicalizeTestCase {
    // Bazel execroot paths:
    // Absolute paths under /execroot/<workspace_name>/ produced by Bazel in OSS/external builds.
    BAZEL_EXECROOT_MAIN(
        "/execroot/_main/java/com/google/foo/Bar.java", "java/com/google/foo/Bar.java"),
    BAZEL_EXECROOT_CUSTOM(
        "/execroot/my_workspace/src/main/java/com/example/Bar.java",
        "src/main/java/com/example/Bar.java"),

    // Bazel runfiles paths:
    BAZEL_RUNFILES(
        "/execroot/_main/bazel-out/k8-opt/bin/test.runfiles/_main/java/com/google/foo/Bar.java",
        "java/com/google/foo/Bar.java"),
    BAZEL_RUNFILES_SLASH(
        "/runfiles/my_workspace/java/com/google/foo/Bar.java", "java/com/google/foo/Bar.java"),
    FILE_URI("file:///execroot/_main/java/com/google/foo/Bar.java", "java/com/google/foo/Bar.java"),

    // Relative paths with 'runfiles' or 'execroot' in package names are preserved:
    RELATIVE_RUNFILES_PACKAGE(
        "java/com/google/devtools/build/runfiles/Runfiles.java",
        "java/com/google/devtools/build/runfiles/Runfiles.java"),
    RELATIVE_EXECROOT_PACKAGE(
        "java/com/google/devtools/build/execroot/ExecRoot.java",
        "java/com/google/devtools/build/execroot/ExecRoot.java"),
    RELATIVE_EXECROOT_TOP_LEVEL("execroot/workspace/Foo.java", "execroot/workspace/Foo.java"),
    RELATIVE_RUNFILES_TOP_LEVEL("runfiles/workspace/Foo.java", "runfiles/workspace/Foo.java"),

    // Nested or unanchored directory names matching build roots are preserved:
    UNANCHORED_RUNFILES(
        "/path/to/test.runfiles/my_workspace/java/com/google/foo/Bar.kt",
        "path/to/test.runfiles/my_workspace/java/com/google/foo/Bar.kt"),
    NESTED_RUNFILES_DIR(
        "java/com/example/nested.runfiles/_main/java/com/example/Foo.java",
        "java/com/example/nested.runfiles/_main/java/com/example/Foo.java"),
    NESTED_EXECROOT_DIR(
        "/execroot/_main/java/com/example/execroot/_main/java/com/example/Foo.java",
        "java/com/example/execroot/_main/java/com/example/Foo.java"),

    // Build output directories (blaze-out / bazel-out):
    // Generated source files (e.g., AutoValue, Dagger, Protos) located under
    // blaze-out/<config>/(bin|genfiles)/ or bazel-out/<config>/(bin|genfiles)/.
    BLAZE_OUT_BIN(
        "blaze-out/k8-opt/bin/java/com/google/foo/AutoValue_Bar.java",
        "java/com/google/foo/AutoValue_Bar.java"),
    BLAZE_OUT_GENFILES(
        "blaze-out/k8-opt/genfiles/java/com/google/foo/BarProto.java",
        "java/com/google/foo/BarProto.java"),
    BAZEL_OUT_BIN(
        "bazel-out/k8-fastbuild/bin/java/com/google/foo/AutoValue_Bar.java",
        "java/com/google/foo/AutoValue_Bar.java"),
    BAZEL_OUT_GENFILES(
        "bazel-out/k8-opt/genfiles/java/com/google/foo/BarProto.java",
        "java/com/google/foo/BarProto.java"),

    // Standard relative paths and leading-slash paths:
    LEADING_SLASH("/java/com/google/foo/Bar.java", "java/com/google/foo/Bar.java"),
    STANDARD_RELATIVE("java/com/google/foo/Bar.java", "java/com/google/foo/Bar.java"),
    FILENAME_ONLY("Bar.java", "Bar.java");

    private final String input;
    private final String expected;

    CanonicalizeTestCase(String input, String expected) {
      this.input = input;
      this.expected = expected;
    }
  }

  @Test
  public void canonicalizePath(@TestParameter CanonicalizeTestCase testCase) {
    assertThat(SourcePathMatcher.canonicalizePath(testCase.input)).isEqualTo(testCase.expected);
  }

  @Test
  public void canonicalizePath_nullRejected() {
    assertThrows(NullPointerException.class, () -> SourcePathMatcher.canonicalizePath(null));
  }

  @Test
  public void nestedPath_execroot_notExempted() {
    SourcePathMatcher matcher = SourcePathMatcher.create("java/com/google/allowed/");
    String path =
        "/execroot/_main/java/com/google/disallowed/execroot/_main/java/com/google/allowed/Foo.java";
    assertThat(matcher.matches(SourcePathMatcher.canonicalizePath(path))).isFalse();
  }
}
