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
        SourcePathMatcher.create("java/com/google/foo/Bar.java", "java/com/google/foo/Baz.java");
    assertThat(matcher.matches("java/com/google/foo/Bar.java")).isTrue();
    assertThat(matcher.matches("java/com/google/foo/Baz.java")).isTrue();
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
    // Non-java file
    assertThrows(
        IllegalArgumentException.class,
        () -> SourcePathMatcher.create("java/com/google/foo/Bar.proto"));
    assertThrows(
        IllegalArgumentException.class,
        () -> SourcePathMatcher.create("java/com/google/foo/Bar.kt"));
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
  }

  private enum CanonicalizeTestCase {
    // Bazel execroot paths:
    // Absolute paths under /execroot/<workspace_name>/ produced by Bazel in OSS/external builds.
    BAZEL_EXECROOT_MAIN(
        "/execroot/_main/java/com/google/foo/Bar.java", "java/com/google/foo/Bar.java"),
    BAZEL_EXECROOT_CUSTOM(
        "/execroot/my_workspace/src/main/java/com/example/Bar.java",
        "src/main/java/com/example/Bar.java"),

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
}
