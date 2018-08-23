/*
 * Copyright 2016 The Error Prone Authors.
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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author cushon@google.com (Liam Miller-Cushon) */
@RunWith(JUnit4.class)
public class MockitoCastTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(MockitoCast.class, getClass());
  }

  @Test
  public void defaultAnswerOk() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            "public class Foo {",
            "  public <T> T f(Iterable<T> xs) { return xs.iterator().next(); }",
            "}")
        .addSourceLines(
            "Test.java",
            "import static org.mockito.Mockito.when;",
            "import static org.mockito.Answers.RETURNS_SMART_NULLS;",
            "import org.mockito.Mock;",
            "class Test {",
            "  @Mock Foo l;",
            "  void m(Iterable<Boolean> xs) {",
            "    when(l.f(xs)).thenReturn(false);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void mockAnnotationWithUnsupportedAnswer() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            "public class Foo {",
            "  public <T> T f(Iterable<T> xs) { return xs.iterator().next(); }",
            "}")
        .addSourceLines(
            "Test.java",
            "import static org.mockito.Mockito.when;",
            "import static org.mockito.Answers.RETURNS_SMART_NULLS;",
            "import org.mockito.Mock;",
            "class Test {",
            "  @Mock(answer = RETURNS_SMART_NULLS) Foo l;",
            "  void m(Iterable<Boolean> xs) {",
            "    // BUG: Diagnostic contains: when((Object) l.f(xs)).thenReturn(false);",
            "    when(l.f(xs)).thenReturn(false);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void varInitializedWithUnsupportedAnswer() {
    compilationHelper
        .addSourceLines("Box.java", "public class Box<T> {", "  T f() { return null; }", "}")
        .addSourceLines(
            "Test.java",
            "import org.mockito.Mockito;",
            "class Test {",
            "  Box<Boolean> box = Mockito.mock(Box.class, Mockito.RETURNS_SMART_NULLS);",
            "  void m() {",
            "    // BUG: Diagnostic contains: when((Object) box.f()).thenReturn(false);",
            "    Mockito.when(box.f()).thenReturn(false);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void constructorInitialization() {
    compilationHelper
        .addSourceLines("Box.java", "public class Box<T> {", "  T f() { return null; }", "}")
        .addSourceLines(
            "Test.java",
            "import org.mockito.Mockito;",
            "class Test {",
            "  Box<Boolean> box;",
            "  Test() {",
            "    box = Mockito.mock(Box.class, Mockito.RETURNS_SMART_NULLS);",
            "  }",
            "  void m() {",
            "    // BUG: Diagnostic contains: when((Object) box.f()).thenReturn(false);",
            "    Mockito.when(box.f()).thenReturn(false);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void chainedCallOnUnsupportedMock() {
    compilationHelper
        .addSourceLines(
            "Foo.java", "public class Foo {", "  Bar<Boolean> bar() { return null; }", "}")
        .addSourceLines("Bar.java", "public class Bar<T> {", "  T get() { return null; }", "}")
        .addSourceLines(
            "Test.java",
            "import static org.mockito.Mockito.when;",
            "import static org.mockito.Answers.RETURNS_SMART_NULLS;",
            "import org.mockito.Mock;",
            "class Test {",
            "  @Mock(answer = RETURNS_SMART_NULLS) Foo f;",
            "  void m() {",
            "    // BUG: Diagnostic contains: when((Object) f.bar().get()).thenReturn(false);",
            "    when(f.bar().get()).thenReturn(false);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void concreteSubclassOfGenericType() {
    compilationHelper
        .addSourceLines("Super.java", "public class Super<T> {", "  T f() { return null; }", "}")
        .addSourceLines("Sub.java", "public class Sub extends Super<Boolean> {", "}")
        .addSourceLines(
            "Test.java",
            "import static org.mockito.Mockito.when;",
            "import static org.mockito.Answers.RETURNS_SMART_NULLS;",
            "import org.mockito.Mock;",
            "class Test {",
            "  @Mock(answer = RETURNS_SMART_NULLS) Sub s;",
            "  void m() {",
            "    // BUG: Diagnostic contains: when((Object) s.f()).thenReturn(false);",
            "    when(s.f()).thenReturn(false);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void mockedFieldInstantiationOfGenericType() {
    compilationHelper
        .addSourceLines("Super.java", "public class Super<T> {", "  T f() { return null; }", "}")
        .addSourceLines("Sub.java", "public class Sub extends Super<Boolean> {", "}")
        .addSourceLines(
            "Test.java",
            "import static org.mockito.Mockito.when;",
            "import static org.mockito.Answers.RETURNS_SMART_NULLS;",
            "import org.mockito.Mock;",
            "class Test {",
            "  @Mock(answer = RETURNS_SMART_NULLS) Super<Boolean> s;",
            "  void m() {",
            "    // BUG: Diagnostic contains: when((Object) s.f()).thenReturn(false);",
            "    when(s.f()).thenReturn(false);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void erasureIsNotObject() {
    compilationHelper
        .addSourceLines(
            "Super.java",
            "public class Super<T extends Boolean> {",
            "  T f() { return null; }",
            "}")
        .addSourceLines("Sub.java", "public class Sub extends Super<Boolean> {", "}")
        .addSourceLines(
            "Test.java",
            "import static org.mockito.Mockito.when;",
            "import static org.mockito.Answers.RETURNS_SMART_NULLS;",
            "import org.mockito.Mock;",
            "class Test {",
            "  @Mock(answer = RETURNS_SMART_NULLS) Super<Boolean> s;",
            "  void m() {",
            "    when(s.f()).thenReturn(false);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void uiField() {
    compilationHelper
        .addSourceLines(
            "com/google/gwt/uibinder/client/UiField.java",
            "package com.google.gwt.uibinder.client;",
            "public @interface UiField {}")
        .addSourceLines("Box.java", "public class Box<T> {", "  T get() { return null; }", "}")
        .addSourceLines(
            "Widget.java",
            "import com.google.gwt.uibinder.client.UiField;",
            "public class Widget {",
            "  @UiField Box<Boolean> l;",
            "}")
        .addSourceLines(
            "Test.java",
            "import static org.mockito.Mockito.when;",
            "import static org.mockito.Answers.RETURNS_SMART_NULLS;",
            "class Test {",
            "  Widget w;",
            "  void m(Iterable<Boolean> xs) {",
            "    // BUG: Diagnostic contains: when((Object) w.l.get()).thenReturn(null);",
            "    when(w.l.get()).thenReturn(null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void qualifiedTypeNameHandling() {
    compilationHelper
        .addSourceLines(
            "pkg/Bound.java",
            "package pkg;",
            "public class Bound {",
            "  public static class Inner {}",
            "  public static class Sub extends Inner {}",
            "}")
        .addSourceLines(
            "Foo.java",
            "import pkg.Bound;",
            "public class Foo {",
            "  public <T extends Bound.Inner> T f(Iterable<T> xs) { return xs.iterator().next(); }",
            "}")
        .addSourceLines(
            "Test.java",
            "import static org.mockito.Mockito.when;",
            "import static org.mockito.Answers.RETURNS_SMART_NULLS;",
            "import org.mockito.Mock;",
            "class Test {",
            "  @Mock(answer = RETURNS_SMART_NULLS) Foo l;",
            "  void m(Iterable<pkg.Bound.Sub> xs) {",
            "    // BUG: Diagnostic contains: when((Bound.Inner) l.f(xs)).thenReturn(null);",
            "    when(l.f(xs)).thenReturn(null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void imported() {
    compilationHelper
        .addSourceLines(
            "pkg/Bound.java",
            "package pkg;",
            "public class Bound {",
            "  public static class Inner {}",
            "  public static class Sub extends Inner {}",
            "}")
        .addSourceLines(
            "Foo.java",
            "import pkg.Bound;",
            "public class Foo {",
            "  public <T extends Bound.Inner> T f(Iterable<T> xs) { return xs.iterator().next(); }",
            "}")
        .addSourceLines(
            "Test.java",
            "import static org.mockito.Mockito.when;",
            "import static org.mockito.Answers.RETURNS_SMART_NULLS;",
            "import org.mockito.Mock;",
            "class Test {",
            "  final int ONE = 1;",
            "  final int TWO = 1;",
            "  @Mock(answer = RETURNS_SMART_NULLS) Foo l;",
            "  void m(Iterable<pkg.Bound.Sub> xs) {",
            "    // BUG: Diagnostic contains: when((Bound.Inner) l.f(xs)).thenReturn(null);",
            "    when(l.f(xs)).thenReturn(null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void rawCast() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            "import java.util.List;",
            "public class Foo {",
            "  public <T extends List<?>> T f(Iterable<T> xs) { return xs.iterator().next(); }",
            "}")
        .addSourceLines(
            "Test.java",
            "import java.util.ArrayList;",
            "import static org.mockito.Mockito.when;",
            "import static org.mockito.Answers.RETURNS_SMART_NULLS;",
            "import org.mockito.Mock;",
            "class Test {",
            "  @Mock(answer = RETURNS_SMART_NULLS) Foo l;",
            "  void m(Iterable<ArrayList<String>> xs) {",
            "    // BUG: Diagnostic contains: when((Object) l.f(xs)).thenReturn(null);",
            "    when(l.f(xs)).thenReturn(null);",
            "  }",
            "}")
        .doTest();
  }
}
