/*
 * Copyright 2012 The Error Prone Authors.
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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
@RunWith(JUnit4.class)
public class ReturnValueIgnoredTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(ReturnValueIgnored.class, getClass());

  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(ReturnValueIgnored.class, getClass());

  @Test
  public void testPositiveCases() {
    compilationHelper.addSourceFile("ReturnValueIgnoredPositiveCases.java").doTest();
  }

  @Test
  public void testNegativeCase() {
    compilationHelper.addSourceFile("ReturnValueIgnoredNegativeCases.java").doTest();
  }

  @Test
  public void function() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.function.Function;",
            "class Test {",
            "  void f(Function<Integer, Integer> f) {",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    f.apply(0);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void consumer() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.function.Consumer;",
            "class Test {",
            "  void f(Consumer<Integer> f) {",
            "    f.accept(0);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void functionVoid() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.function.Function;",
            "class Test {",
            "  void f(Function<Integer, Void> f) {",
            "    f.apply(0);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void ignoreInTests() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import static org.junit.Assert.fail;",
            "import java.util.function.Function;",
            "class Test {",
            "  void f(Function<Integer, Integer> f) {",
            "    try {",
            "      f.apply(0);",
            "      fail();",
            "    } catch (Exception expected) {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void stream() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    \"\".codePoints().count();",
            "    \"\".codePoints().forEach(i -> {});",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void javaTime() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.time.Duration;",
            "import java.time.LocalDate;",
            "import java.time.ZoneId;",
            "class Test {",
            "  void f() {",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    Duration.ZERO.plusDays(2);",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    Duration.ZERO.toDays();",
            // We ignore parse() methods on java.time types
            "    Duration.parse(\"PT20.345S\");",
            "    LocalDate.parse(\"2007-12-03\");",
            // We ignore of() methods on java.time types
            "    LocalDate.of(1985, 5, 31);",
            // We ignore ZoneId.of() -- it's effectively a parse() method
            "    ZoneId.of(\"America/New_York\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void optionalStaticMethods() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Optional;",
            "class Test {",
            "  void optional() {",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    Optional.empty();",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    Optional.of(42);",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    Optional.ofNullable(null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void optionalInstanceMethods() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Optional;",
            "class Test {",
            "  void optional() {",
            "    Optional<Integer> optional = Optional.of(42);",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    optional.filter(v -> v > 40);",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    optional.flatMap(v -> Optional.of(v + 1));",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    optional.get();",
            "    optional.ifPresent(v -> {});",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    optional.isPresent();",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    optional.map(v -> v + 1);",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    optional.orElse(40);",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    optional.orElseGet(() -> 40);",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    optional.orElseThrow(() -> new RuntimeException());",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void optionalInstanceMethods_jdk9() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Optional;",
            "class Test {",
            "  void optional() {",
            "    Optional<Integer> optional = Optional.of(42);",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    optional.or(() -> Optional.empty());",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void optionalInstanceMethods_jdk10() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Optional;",
            "class Test {",
            "  void optional() {",
            "    Optional<Integer> optional = Optional.of(42);",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    optional.orElseThrow();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void optionalInstanceMethods_jdk11() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Optional;",
            "class Test {",
            "  void optional() {",
            "    Optional<Integer> optional = Optional.of(42);",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    optional.isEmpty();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void timeUnitApis() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import static java.util.concurrent.TimeUnit.MILLISECONDS;",
            "class Test {",
            "  void timeUnit() {",
            "    long ms = 4200;",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    MILLISECONDS.toNanos(ms);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void issue1565_enumDeclaration() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.function.Function;",
            "enum Test {",
            "  A;",
            "  void f(Function<Integer, Integer> f) {",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    f.apply(0);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void issue1363_dateTimeFormatterBuilder() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.time.format.DateTimeFormatterBuilder;",
            "class Test {",
            "  void f() {",
            "    DateTimeFormatterBuilder formatter = new DateTimeFormatterBuilder();",
            "    formatter.appendZoneId();",
            "    formatter.optionalEnd();",
            "    formatter.padNext(5);",
            "    formatter.parseCaseSensitive();",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    formatter.toFormatter();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void issue876() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.nio.file.Path;",
            "abstract class Test {",
            "  void test(Path p) {",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    E e = p::toRealPath;",
            "  }",
            "  abstract <T> void a(T t);",
            "  public interface E {",
            "    void run() throws Exception;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void collectionContains() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  void test(java.util.List p) {",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    p.contains(null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void mapMethods() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Map;",
            "public final class Test {",
            "  void doTest(Map<Integer, Integer> map) {",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    map.isEmpty();",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    map.size();",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    map.entrySet();",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    map.keySet();",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    map.values();",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    map.containsKey(42);",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    map.containsValue(42);",
            "  }",
            "  void doTest(Map.Entry<Integer, Integer> entry) {",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    entry.getKey();",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    entry.getValue();",
            "    entry.setValue(42);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void mapMethods_java11() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Map;",
            "class Test {",
            "  void doTest() {",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    Map.of(42, 42);",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    Map.entry(42, 42);",
            "  }",
            "  void doTest(Map<Integer, Integer> map) {",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    Map.copyOf(map);",
            "  }",
            "  void doTest(Map.Entry<Integer, Integer>... entries) {",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    Map.ofEntries(entries);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void methodReferenceToObject() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.function.Function;",
            "abstract class Test {",
            "  void test(Function<Integer, Long> fn) {",
            "    foo(fn::apply);",
            "  }",
            "  void foo(Function<Integer, Object> fn) {",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void integers() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f() throws Exception {",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    Integer.reverse(2);",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    new Integer(2).doubleValue();",
            // We ignore the following "parsing" style methods:
            "    Integer.decode(\"1985\");",
            "    Integer.parseInt(\"1985\");",
            "    Integer.parseInt(\"1985\", 10);",
            "    Integer.parseUnsignedInt(\"1985\");",
            "    Integer.parseUnsignedInt(\"1985\", 10);",
            "    Integer.valueOf(\"1985\");",
            "    Integer.valueOf(\"1985\", 10);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void constructors() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f() throws Exception {",
            // TODO: we haven't yet enabled constructor checking for basic types yet,
            //   just making sure it doesn't crash
            "    new String(\"Hello\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testProtoMessageNewBuilder() {
    compilationHelper
        .addSourceLines(
            "test.java",
            "import com.google.protobuf.Duration;",
            "class Test {",
            "  public void proto_newBuilder() {",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    Duration.newBuilder();",
            "    Duration.Builder builder = Duration.newBuilder();",
            "    Duration duration = Duration.newBuilder().setSeconds(4).build();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testProtoMessageBuildBuildPartial() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.protobuf.Duration;",
            "final class Test {",
            "  public void proto_build() {",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    Duration.newBuilder().setSeconds(4).build();",
            "    Duration duration = Duration.newBuilder().setSeconds(4).build();",
            "  }",
            "  public void proto_buildPartial() {",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    Duration.newBuilder().setSeconds(4).buildPartial();",
            "    Duration duration = Duration.newBuilder().setSeconds(4).buildPartial();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void refactoring() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import java.util.Optional;",
            "import java.util.stream.Stream;",
            "final class Test {",
            "  public void f() {",
            "    Optional.of(42);",
            "    Optional.of(42).orElseThrow(AssertionError::new);",
            "    Stream.of(Optional.of(42)).forEach(o -> o.orElseThrow(AssertionError::new));",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.util.Optional;",
            "import java.util.stream.Stream;",
            "final class Test {",
            "  public void f() {",
            "    Optional.of(42).orElseThrow(AssertionError::new);",
            "    Stream.of(Optional.of(42)).forEach(o -> o.orElseThrow(AssertionError::new));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testIterableHasNext() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Iterator;",
            "final class Test {",
            "  private static class CustomIterator implements Iterator<String> {",
            "    @Override public boolean hasNext() { return true; }",
            "    public boolean hasNext(boolean unused) { return true; }",
            "    @Override public String next() { return \"hi\"; }",
            "    public boolean nonInterfaceMethod() { return true; }",
            "  }",
            "  public void iteratorHasNext() {",
            "    CustomIterator iterator = new CustomIterator();",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    iterator.hasNext();",
            "    iterator.next();", // this is OK (some folks next their way through an Iterator)
            "    iterator.hasNext(true);", // this is OK (it's an overload but not on the interface)
            "    iterator.nonInterfaceMethod();", // this is OK (it's not an interface method)
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testCollectionToArray() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "final class Test {",
            "  private static final ImmutableList<Long> LIST = ImmutableList.of(42L);",
            "  public void collectionToArray() {",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    LIST.toArray();",
            // Collection.toArray(T[]) is fine, since it _can_ dump the collection contents into the
            // passed-in array *if* the array is large enough (in which case, you don't have to
            // check the return value of the method call).
            "    LIST.toArray(new Long[0]);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testCollectionToArray_java8() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "final class Test {",
            "  private static final ImmutableList<Long> LIST = ImmutableList.of(42L);",
            "  public void collectionToArray() {",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    LIST.toArray(Long[]::new);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void objectMethods() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void test(Test t, Object o) {",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    t.equals(o);",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    o.equals(t);",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    t.hashCode();",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    t.getClass();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void charSequenceMethods() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void test(CharSequence cs) {",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    cs.charAt(0);",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    cs.chars();",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    cs.codePoints();",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    cs.length();",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    cs.subSequence(1, 2);",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    cs.toString();",
            "  }",
            "  void test(StringBuilder sb) {",
            "    sb.append(\"hi\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void enumMethods() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.concurrent.TimeUnit;",
            "class Test {",
            "  void test(Enum e) {",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    e.getDeclaringClass();",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    e.name();",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    e.ordinal();",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    TimeUnit.valueOf(\"MILLISECONDS\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void enumMethodsOnSubtype() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.lang.invoke.VarHandle;",
            "class Test {",
            "  void test(VarHandle.AccessMode accessMode) {",
            "    accessMode.methodName();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void throwableMethods() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void test(Throwable t) {",
            // These 2 APIs are OK to ignore (they just return this)
            "    t.fillInStackTrace();",
            "    t.initCause(new Throwable());",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    t.getCause();",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    t.getLocalizedMessage();",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    t.getMessage();",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    t.getStackTrace();",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    t.getSuppressed();",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    t.toString();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void objectsMethods() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Objects;",
            "class Test {",
            "  void test(Object o) {",
            // These APIs are OK to ignore
            "    Objects.checkFromIndexSize(0, 1, 2);",
            "    Objects.checkFromToIndex(0, 1, 2);",
            "    Objects.checkIndex(0, 1);",
            "    Objects.requireNonNull(o);",
            "    Objects.requireNonNull(o, \"message\");",
            "    Objects.requireNonNull(o, () -> \"messageSupplier\");",
            "    Objects.requireNonNullElse(o, new Object());",
            "    Objects.requireNonNullElseGet(o, () -> new Object());",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    Objects.compare(\"B\", \"a\", String.CASE_INSENSITIVE_ORDER);",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    Objects.deepEquals(new Object(), new Object());",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    Objects.equals(new Object(), new Object());",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    Objects.hash(new Object(), new Object());",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    Objects.hashCode(o);",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    Objects.isNull(o);",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    Objects.nonNull(o);",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    Objects.toString(o);",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    Objects.toString(o, \"defaultValue\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void classMethods() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void test(Class<?> c) throws Exception {",
            "    Class.forName(\"java.sql.Date\");",
            "    c.getMethod(\"toString\");",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    c.desiredAssertionStatus();",
            "  }",
            "}")
        .doTest();
  }
}
