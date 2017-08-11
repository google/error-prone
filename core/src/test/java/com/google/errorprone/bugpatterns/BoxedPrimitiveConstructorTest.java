/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

import com.google.common.io.ByteStreams;
import com.google.errorprone.CompilationTestHelper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link BoxedPrimitiveConstructor}Test */
@RunWith(JUnit4.class)
public class BoxedPrimitiveConstructorTest {

  CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper =
        CompilationTestHelper.newInstance(BoxedPrimitiveConstructor.class, getClass());
  }

  @Test
  public void positive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "public class Test {",
            "  {",
            "    // BUG: Diagnostic contains: byte b = (byte) 0;",
            "    byte b = new Byte((byte) 0);",
            "    // BUG: Diagnostic contains: char c = (char) 0;",
            "    char c = new Character((char) 0);",
            "    // BUG: Diagnostic contains: double d = 0;",
            "    double d = new Double(0);",
            "    // BUG: Diagnostic contains: float f = 0;",
            "    float f = new Float(0);",
            "    // BUG: Diagnostic contains: int i = 0;",
            "    int i = new Integer(0);",
            "    // BUG: Diagnostic contains: long j = 0;",
            "    long j = new Long(0);",
            "    // BUG: Diagnostic contains: short s = (short) 0;",
            "    short s = new Short((short) 0);",
            "    Double dd = d;",
            "    // BUG: Diagnostic contains: float f2 = dd.floatValue();",
            "    float f2 = new Float(dd);",
            "    // BUG: Diagnostic contains: float f3 = (float) d;",
            "    float f3 = new Float(d);",
            "    // BUG: Diagnostic contains: foo(Float.valueOf((float) d));",
            "    foo(new Float(d));",
            "  }",
            "  public void foo(Float f) {}",
            "}")
        .doTest();
  }

  @Test
  public void positiveStrings() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "public class Test {",
            "  {",
            "    // BUG: Diagnostic contains: byte b = Byte.valueOf(\"0\");",
            "    byte b = new Byte(\"0\");",
            "    // BUG: Diagnostic contains: double d = Double.valueOf(\"0\");",
            "    double d = new Double(\"0\");",
            "    // BUG: Diagnostic contains: float f = Float.valueOf(\"0\");",
            "    float f = new Float(\"0\");",
            "    // BUG: Diagnostic contains: int i = Integer.valueOf(\"0\");",
            "    int i = new Integer(\"0\");",
            "    // BUG: Diagnostic contains: long j = Long.valueOf(\"0\");",
            "    long j = new Long(\"0\");",
            "    // BUG: Diagnostic contains: short s = Short.valueOf(\"0\");",
            "    short s = new Short(\"0\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void booleanConstant() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "public class Test {",
            "  static final Boolean CONST = true;",
            "  static final String CONST2 = null;",
            "  {",
            "    // BUG: Diagnostic contains: boolean a = true;",
            "    boolean a = new Boolean(true);",
            "    // BUG: Diagnostic contains: boolean b = false;",
            "    boolean b = new Boolean(false);",
            "    // BUG: Diagnostic contains: boolean c = Boolean.valueOf(CONST);",
            "    boolean c = new Boolean(CONST);",
            "    // BUG: Diagnostic contains: boolean e = true;",
            "    boolean e = new Boolean(\"true\");",
            "    // BUG: Diagnostic contains: boolean f = false;",
            "    boolean f = new Boolean(\"nope\");",
            "    // BUG: Diagnostic contains: boolean g = Boolean.valueOf(CONST2);",
            "    boolean g = new Boolean(CONST2);",
            "    // BUG: Diagnostic contains: System.err.println(Boolean.TRUE);",
            "    System.err.println(new Boolean(\"true\"));",
            "    // BUG: Diagnostic contains: System.err.println(Boolean.FALSE);",
            "    System.err.println(new Boolean(\"false\"));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "public class Test {",
            "  {",
            "    String s = new String((String) null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void autoboxing() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "public abstract class Test {",
            "  abstract int g(Integer x);",
            "  void f(int x) {",
            "    // BUG: Diagnostic contains: int i = x;",
            "    int i = new Integer(x);",
            "    // BUG: Diagnostic contains: i = g(Integer.valueOf(x));",
            "    i = g(new Integer(x));",
            "    // BUG: Diagnostic contains: i = (short) 0;",
            "    i = new Integer((short) 0);",
            "  }",
            "}")
        .doTest();
  }

  // Tests that `new Integer(x).memberSelect` isn't unboxed to x.memberSelect
  // TODO(cushon): we could provide a better fix for byteValue(), but hopefully no one does that?
  @Test
  public void methodCall() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "public abstract class Test {",
            "  abstract int g(Integer x);",
            "  void f(int x) {",
            "    // BUG: Diagnostic contains: int i = Integer.valueOf(x).byteValue();",
            "    int i = new Integer(x).byteValue();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void stringValue() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "public abstract class Test {",
            "  abstract int g(Integer x);",
            "  void f(int x) {",
            "    // BUG: Diagnostic contains: String s = String.valueOf(x);",
            "    String s = new Integer(x).toString();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void compareTo() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "public abstract class Test {",
            "  abstract int g(Integer x);",
            "  void f(int x, Integer y, double d, Double dd, Float f) {",
            "    // BUG: Diagnostic contains: int c1 = Integer.compare(x, y);",
            "    int c1 = new Integer(x).compareTo(y);",
            "    // BUG: Diagnostic contains: int c2 = y.compareTo(Integer.valueOf(x));",
            "    int c2 = y.compareTo(new Integer(x));",
            "    // BUG: Diagnostic contains: int c3 = Float.compare((float) d, f);",
            "    int c3 = new Float(d).compareTo(f);",
            "    // BUG: Diagnostic contains: int c4 = Float.compare(dd.floatValue(), f);",
            "    int c4 = new Float(dd).compareTo(f);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testHashCode() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "public abstract class Test {",
            "  abstract int g(Integer x);",
            "  int f(int x, Integer y, long z, double d, Double dd) {",
            "    // BUG: Diagnostic contains: int h = Integer.hashCode(x);",
            "    int h = new Integer(x).hashCode();",
            "    // BUG: Diagnostic contains: h = Float.hashCode((float) d);",
            "    h = new Float(d).hashCode();",
            "    // BUG: Diagnostic contains: h = Float.hashCode(dd.floatValue());",
            "    h = new Float(dd).hashCode();",
            "    // BUG: Diagnostic contains: return Long.hashCode(z);",
            "    return new Long(z).hashCode();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void hashCodeInJava7() {
    compilationHelper
        .setArgs(Arrays.asList("-source", "7", "-target", "7"))
        .addSourceLines(
            "Test.java",
            "public abstract class Test {",
            "  abstract int g(Integer x);",
            "  int f(long z) {",
            "    // BUG: Diagnostic contains: return Longs.hashCode(z);",
            "    return new Long(z).hashCode();",
            "  }",
            "}")
        .doTest();
  }

  @Rule public final TemporaryFolder tempFolder = new TemporaryFolder();

  public static class Super {}

  public static class Inner extends Super {}

  // TODO(b/30478325): create a better way to write this style of test
  static void addClassToJar(JarOutputStream jos, Class<?> clazz) throws IOException {
    String entryPath = clazz.getName().replace('.', '/') + ".class";
    try (InputStream is = clazz.getClassLoader().getResourceAsStream(entryPath)) {
      jos.putNextEntry(new JarEntry(entryPath));
      ByteStreams.copy(is, jos);
    }
  }

  @Test
  public void incompleteClasspath() throws Exception {
    File libJar = tempFolder.newFile("lib.jar");
    try (FileOutputStream fis = new FileOutputStream(libJar);
        JarOutputStream jos = new JarOutputStream(fis)) {
      addClassToJar(jos, BoxedPrimitiveConstructorTest.class);
      addClassToJar(jos, Inner.class);
    }
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import " + Inner.class.getCanonicalName() + ";",
            "class Test {",
            "  void m() {",
            "    new Inner();",
            "  }",
            "}")
        .setArgs(Arrays.asList("-cp", libJar.toString()))
        .doTest();
  }

  @Test
  public void autoboxWidening() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f(float f) {",
            "    // BUG: Diagnostic contains: (double) f;",
            "    Double d = new Double(f);",
            "    // BUG: Diagnostic contains: (short) (byte) 0;",
            "    Short s = new Short((byte) 0);",
            "  }",
            "}")
        .doTest();
  }
}
