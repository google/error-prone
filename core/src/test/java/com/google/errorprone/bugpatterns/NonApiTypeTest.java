/*
 * Copyright 2023 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link NonApiType}. */
@RunWith(JUnit4.class)
public final class NonApiTypeTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(NonApiType.class, getClass())
          // Indicate that we're compiling "publicly visible code"
          // See ErrorProneOptions.COMPILING_PUBLICLY_VISIBLE_CODE
          .setArgs("-XepCompilingPubliclyVisibleCode");

  @Test
  public void listImplementations() {
    helper
        .addSourceLines(
            "Test.java",
            "public class Test {",
            "  // BUG: Diagnostic contains: java.util.List",
            "  private void test1(java.util.ArrayList value) {}",
            "  // BUG: Diagnostic contains: java.util.List",
            "  private void test1(java.util.LinkedList value) {}",
            "}")
        .doTest();
  }

  @Test
  public void setImplementations() {
    helper
        .addSourceLines(
            "Test.java",
            "public class Test {",
            "  // BUG: Diagnostic contains: java.util.Set",
            "  private void test1(java.util.HashSet value) {}",
            "  // BUG: Diagnostic contains: java.util.Set",
            "  private void test1(java.util.LinkedHashSet value) {}",
            "  // BUG: Diagnostic contains: java.util.Set",
            "  private void test1(java.util.TreeSet value) {}",
            "}")
        .doTest();
  }

  @Test
  public void mapImplementations() {
    helper
        .addSourceLines(
            "Test.java",
            "public class Test {",
            "  // BUG: Diagnostic contains: java.util.Map",
            "  private void test1(java.util.HashMap value) {}",
            "  // BUG: Diagnostic contains: java.util.Map",
            "  private void test1(java.util.LinkedHashMap value) {}",
            "  // BUG: Diagnostic contains: java.util.Map",
            "  private void test1(java.util.TreeMap value) {}",
            "}")
        .doTest();
  }

  @Test
  public void guavaOptionals() {
    helper
        .addSourceLines(
            "Test.java",
            "import com.google.common.base.Optional;",
            "public class Test {",
            "  // BUG: Diagnostic contains: java.util.Optional",
            "  public Optional<String> middleName() { return Optional.of(\"alfred\"); }",
            "  // BUG: Diagnostic contains: java.util.Optional",
            "  public void setMiddleName(Optional<String> middleName) {}",
            "}")
        .doTest();
  }

  @Test
  public void jdkOptionals() {
    helper
        .addSourceLines(
            "Test.java",
            "import java.util.Optional;",
            "public class Test {",
            "  public Optional<String> middleName() { return Optional.of(\"alfred\"); }",
            "  // BUG: Diagnostic contains: Avoid Optional parameters",
            "  public void setMiddleName(Optional<String> middleName) {}",
            "}")
        .doTest();
  }

  @Test
  public void immutableFoos() {
    helper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableCollection;",
            "import com.google.common.collect.ImmutableList;",
            "import com.google.common.collect.ImmutableMap;",
            "import com.google.common.collect.ImmutableSet;",
            "public class Test {",
            // ImmutableFoos as return types are great!
            "  public ImmutableCollection testImmutableCollection() { return null; }",
            "  public ImmutableList testImmutableList() { return null; }",
            "  public ImmutableMap testImmutableMap() { return null; }",
            "  public ImmutableSet testImmutableSet() { return null; }",
            // ImmutableFoos as method parameters are less great...
            "  // BUG: Diagnostic contains: java.util.Collection",
            "  public void test1(ImmutableCollection<String> values) {}",
            "  // BUG: Diagnostic contains: java.util.List",
            "  public void test2(ImmutableList<String> values) {}",
            "  // BUG: Diagnostic contains: java.util.Map",
            "  public void test3(ImmutableMap<String, String> values) {}",
            "  // BUG: Diagnostic contains: java.util.Set",
            "  public void test4(ImmutableSet<String> values) {}",
            "}")
        .doTest();
  }

  @Test
  public void primitiveArrays() {
    helper
        .addSourceLines(
            "Test.java",
            "public class Test {",
            "  // BUG: Diagnostic contains: ImmutableIntArray",
            "  public int[] testInts() { return null; }",
            "  // BUG: Diagnostic contains: ImmutableDoubleArray",
            "  public void testDoubles1(double[] values) {}",
            "  // BUG: Diagnostic contains: ImmutableDoubleArray",
            "  public void testDoubles2(Double[] values) {}",
            "}")
        .doTest();
  }

  @Test
  public void protoTime() {
    helper
        .addSourceLines(
            "Test.java",
            "import com.google.protobuf.Duration;",
            "import com.google.protobuf.Timestamp;",
            "public class Test {",
            "  // BUG: Diagnostic contains: java.time.Duration",
            "  public Duration test() { return null; }",
            "  // BUG: Diagnostic contains: java.time.Instant",
            "  public void test(Timestamp timestamp) {}",
            "}")
        .doTest();
  }

  @Test
  public void varargs() {
    helper
        .addSourceLines(
            "Test.java",
            "import com.google.protobuf.Timestamp;",
            "public class Test {",
            // TODO(kak): we should _probably_ flag this too
            "  public void test(Timestamp... timestamps) {}",
            "}")
        .doTest();
  }

  @Test
  public void typeArguments() {
    helper
        .addSourceLines(
            "Test.java",
            "import com.google.protobuf.Timestamp;",
            "import java.util.List;",
            "import java.util.Map;",
            "public class Test {",
            "  // BUG: Diagnostic contains: java.time.Instant",
            "  public void test1(List<Timestamp> timestamps) {}",
            "  // BUG: Diagnostic contains: java.time.Instant",
            "  public void test2(List<Map<String, Timestamp>> timestamps) {}",
            "}")
        .doTest();
  }

  @Test
  public void nonPublicApisInPublicClassAreNotFlagged() {
    helper
        .addSourceLines(
            "Test.java",
            "import com.google.protobuf.Timestamp;",
            "public class Test {",
            "  void test1(Timestamp timestamp) {}",
            "  protected void test2(Timestamp timestamp) {}",
            "  private void test3(Timestamp timestamp) {}",
            "}")
        .doTest();
  }

  @Test
  public void publicApisInNonPublicClassAreNotFlagged() {
    helper
        .addSourceLines(
            "Test.java",
            "import com.google.protobuf.Timestamp;",
            "class Test {",
            "  public void test1(Timestamp timestamp) {}",
            "}")
        .doTest();
  }

  @Test
  public void normalApisAreNotFlagged() {
    helper
        .addSourceLines(
            "Test.java",
            "public class Test {",
            "  public Test(int a) {}",
            "  public int doSomething() { return 42; }",
            "  public void doSomething(int a) {}",
            "}")
        .doTest();
  }

  @Test
  public void streams() {
    helper
        .addSourceLines(
            "Test.java",
            "import java.util.stream.Stream;",
            "public class Test {",
            "  // BUG: Diagnostic contains: NonApiType",
            "  public Test(Stream<String> iterator) {}",
            "  // BUG: Diagnostic contains: NonApiType",
            "  public void methodParam(Stream<String> iterator) {}",
            "}")
        .doTest();
  }

  @Test
  public void iterators() {
    helper
        .addSourceLines(
            "Test.java",
            "import java.util.Iterator;",
            "public class Test {",
            "  // BUG: Diagnostic contains: NonApiType",
            "  public Iterator<String> returnType() { return null; }",
            "}")
        .doTest();
  }
}
