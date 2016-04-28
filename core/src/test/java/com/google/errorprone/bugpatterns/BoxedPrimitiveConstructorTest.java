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

import com.google.errorprone.CompilationTestHelper;

import org.junit.Before;
import org.junit.Test;
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
            "    // BUG: Diagnostic contains: byte b = Byte.valueOf((byte) 0);",
            "    byte b = new Byte((byte) 0);",
            "    // BUG: Diagnostic contains: char c = Character.valueOf((char) 0);",
            "    char c = new Character((char) 0);",
            "    // BUG: Diagnostic contains: double d = Double.valueOf(0);",
            "    double d = new Double(0);",
            "    // BUG: Diagnostic contains: float f = Float.valueOf(0);",
            "    float f = new Float(0);",
            "    // BUG: Diagnostic contains: int i = Integer.valueOf(0);",
            "    int i = new Integer(0);",
            "    // BUG: Diagnostic contains: long j = Long.valueOf(0);",
            "    long j = new Long(0);",
            "    // BUG: Diagnostic contains: short s = Short.valueOf((short) 0);",
            "    short s = new Short((short) 0);",
            "  }",
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
            "    // BUG: Diagnostic contains: boolean a = Boolean.TRUE;",
            "    boolean a = new Boolean(true);",
            "    // BUG: Diagnostic contains: boolean b = Boolean.FALSE;",
            "    boolean b = new Boolean(false);",
            "    // BUG: Diagnostic contains: boolean c = Boolean.valueOf(CONST);",
            "    boolean c = new Boolean(CONST);",
            "    // BUG: Diagnostic contains: boolean e = Boolean.TRUE;",
            "    boolean e = new Boolean(\"true\");",
            "    // BUG: Diagnostic contains: boolean f = Boolean.FALSE;",
            "    boolean f = new Boolean(\"nope\");",
            "    // BUG: Diagnostic contains: boolean g = Boolean.valueOf(CONST2);",
            "    boolean g = new Boolean(CONST2);",
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
}
