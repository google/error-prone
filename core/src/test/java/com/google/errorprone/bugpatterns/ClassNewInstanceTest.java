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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link ClassNewInstance}Test */
@RunWith(JUnit4.class)
public class ClassNewInstanceTest {

  private BugCheckerRefactoringTestHelper testHelper;

  @Before
  public void setUp() {
    testHelper = BugCheckerRefactoringTestHelper.newInstance(new ClassNewInstance(), getClass());
  }

  @Test
  public void differentHandles() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  void f() {",
            "    try {",
            "      getClass().newInstance();",
            "    } catch (InstantiationException e1) {",
            "      e1.printStackTrace();",
            "    } catch (IllegalAccessException e2) {",
            "      e2.printStackTrace();",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test {",
            "  void f() {",
            "    try {",
            "      getClass().getDeclaredConstructor().newInstance();",
            "    } catch (InstantiationException e1) {",
            "      e1.printStackTrace();",
            "    } catch (IllegalAccessException e2) {",
            "      e2.printStackTrace();",
            "    } catch (ReflectiveOperationException e2) {",
            "      throw new LinkageError(e2.getMessage(), e2);",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void existingRoeCase() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  void f() {",
            "    try {",
            "      getClass().newInstance();",
            "    } catch (InstantiationException e) {",
            "    } catch (ReflectiveOperationException e) {",
            "      // ¯\\_(ツ)_/¯",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test {",
            "  void f() {",
            "    try {",
            "      getClass().getDeclaredConstructor().newInstance();",
            "    } catch (InstantiationException e) {",
            "    } catch (ReflectiveOperationException e) {",
            "      // ¯\\_(ツ)_/¯",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  void f() {",
            "    try {",
            "      getClass().newInstance();",
            "    } catch (InstantiationException e) {",
            "      e.printStackTrace();",
            "    } catch (IllegalAccessException e) {",
            "      e.printStackTrace();",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test {",
            "  void f() {",
            "    try {",
            "      getClass().getDeclaredConstructor().newInstance();",
            "    } catch (ReflectiveOperationException e) {",
            "      e.printStackTrace();",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveUnion() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  void f() {",
            "    try {",
            "      getClass().newInstance();",
            "    } catch (InstantiationException | IllegalAccessException e0) {",
            "      e0.printStackTrace();",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test {",
            "  void f() {",
            "    try {",
            "      getClass().getDeclaredConstructor().newInstance();",
            "    } catch (ReflectiveOperationException e0) {",
            "      e0.printStackTrace();",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveROE() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  void f() {",
            "    try {",
            "      getClass().newInstance();",
            "    } catch (ReflectiveOperationException e) {",
            "      e.printStackTrace();",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test {",
            "  void f() {",
            "    try {",
            "      getClass().getDeclaredConstructor().newInstance();",
            "    } catch (ReflectiveOperationException e) {",
            "      e.printStackTrace();",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void throwsException() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  void f() throws Exception {",
            "    getClass().newInstance();",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test {",
            "  void f() throws Exception {",
            "    getClass().getDeclaredConstructor().newInstance();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void throwsROE() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  void f() throws ReflectiveOperationException {",
            "    getClass().newInstance();",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test {",
            "  void f() throws ReflectiveOperationException {",
            "    getClass().getDeclaredConstructor().newInstance();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void throwsIndividual() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  void f() throws InstantiationException, IllegalAccessException {",
            "    getClass().newInstance();",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import java.lang.reflect.InvocationTargetException;",
            "class Test {",
            "  void f()",
            "      throws InstantiationException, IllegalAccessException,"
                + " InvocationTargetException,",
            "          NoSuchMethodException {",
            "    getClass().getDeclaredConstructor().newInstance();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  void f() throws Exception {",
            "    getClass().getDeclaredConstructor().newInstance();",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void catchesDoesntThrow() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "import java.io.IOException;",
            "class Test {",
            "  void f() throws IOException {",
            "    try {",
            "      getClass().newInstance();",
            "    } catch (ReflectiveOperationException e) {}",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import java.io.IOException;",
            "class Test {",
            "  void f() throws IOException {",
            "    try {",
            "      getClass().getDeclaredConstructor().newInstance();",
            "    } catch (ReflectiveOperationException e) {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeThrows() {
    testHelper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  void f() throws Exception {",
            "    getClass().getDeclaredConstructor().newInstance();",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void mergeWhitespace() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  void f() {",
            "    try {",
            "      getClass().newInstance();",
            "    } catch (InstantiationException e) {",
            "      // uh oh",
            "    } catch (IllegalAccessException e) {",
            "      // uh oh",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test {",
            "  void f() {",
            "    try {",
            "      getClass().getDeclaredConstructor().newInstance();",
            "    } catch (ReflectiveOperationException e) {",
            "      // uh oh",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void overlap() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  void f() throws Exception {",
            "    getClass().newInstance().getClass().newInstance();",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test {",
            "  void f() throws Exception {",
            "    getClass().getDeclaredConstructor().newInstance()"
                + ".getClass().getDeclaredConstructor().newInstance();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void inCatch() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  void f() throws Exception {",
            "    try {",
            "      getClass().newInstance();",
            "    } catch (InstantiationException e) {",
            "      getClass().newInstance();",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test {",
            "  void f() throws Exception {",
            "    try {",
            "      getClass().getDeclaredConstructor().newInstance();",
            "    } catch (ReflectiveOperationException e) {",
            "      getClass().getDeclaredConstructor().newInstance();",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void withFinally() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  void f() throws Exception {",
            "    try {",
            "      getClass().newInstance();",
            "    } finally {}",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test {",
            "  void f() throws Exception {",
            "    try {",
            "      getClass().getDeclaredConstructor().newInstance();",
            "    } finally {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void inCatchRepeated() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  void f() throws InstantiationException, IllegalAccessException {",
            "    try {",
            "      getClass().newInstance();",
            "    } catch (InstantiationException e) {",
            "      getClass().newInstance();",
            "    } catch (IllegalAccessException e) {",
            "      getClass().newInstance();",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import java.lang.reflect.InvocationTargetException;",
            "class Test {",
            "  void f()",
            "      throws InstantiationException, IllegalAccessException,"
                + " InvocationTargetException,",
            "          NoSuchMethodException {",
            "    try {",
            "      getClass().getDeclaredConstructor().newInstance();",
            "    } catch (InstantiationException e) {",
            "      getClass().getDeclaredConstructor().newInstance();",
            "    } catch (ReflectiveOperationException e) {",
            "      getClass().getDeclaredConstructor().newInstance();",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void additionalCatchClause() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  Object f() {",
            "    try {",
            "      return getClass().newInstance();",
            "    } catch (InstantiationException ex) {",
            "      // Suppress exception.",
            "    } catch (IllegalAccessException ex) {",
            "      // Suppress exception.",
            "    } catch (ExceptionInInitializerError ex) {",
            "      // Suppress exception.",
            "    } catch (SecurityException ex) {",
            "      // Suppress exception.",
            "    }",
            "    return null;",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test {",
            "  Object f() {",
            "    try {",
            "      return getClass().getDeclaredConstructor().newInstance();",
            "    } catch (ReflectiveOperationException ex) {",
            "      // Suppress exception.",
            "    } catch (ExceptionInInitializerError ex) {",
            "      // Suppress exception.",
            "    } catch (SecurityException ex) {",
            "      // Suppress exception.",
            "    }",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void catchAndThrows() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  Object f() throws InstantiationException, IllegalAccessException {",
            "    try {",
            "      return getClass().newInstance();",
            "    } catch (ReflectiveOperationException ex) {",
            "      return getClass().newInstance();",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import java.lang.reflect.InvocationTargetException;",
            "class Test {",
            "  Object f()",
            "      throws InstantiationException, IllegalAccessException,"
                + " InvocationTargetException,",
            "          NoSuchMethodException {",
            "    try {",
            "      return getClass().getDeclaredConstructor().newInstance();",
            "    } catch (ReflectiveOperationException ex) {",
            "      return getClass().getDeclaredConstructor().newInstance();",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void mixedMulticatch() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  void f() {",
            "    try {",
            "      getClass().newInstance();",
            "    } catch (InstantiationException e) {",
            "      // InstantiationException",
            "    } catch (IllegalAccessException | NullPointerException e) {",
            "      throw new AssertionError(e);",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test {",
            "  void f() {",
            "    try {",
            "      getClass().getDeclaredConstructor().newInstance();",
            "    } catch (InstantiationException e) {",
            "      // InstantiationException",
            "    } catch (ReflectiveOperationException | NullPointerException e) {",
            "      throw new AssertionError(e);",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void freshVar() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  void f(Exception e) {",
            "    try {",
            "      getClass().newInstance();",
            "    } catch (InstantiationException e1) {",
            "      // one",
            "    } catch (IllegalAccessException e1) {",
            "      // two",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test {",
            "  void f(Exception e) {",
            "    try {",
            "      getClass().getDeclaredConstructor().newInstance();",
            "    } catch (InstantiationException e1) {",
            "      // one",
            "    } catch (IllegalAccessException e1) {",
            "      // two",
            "    } catch (ReflectiveOperationException e1) {",
            "      throw new LinkageError(e1.getMessage(), e1);",
            "    }",
            "  }",
            "}")
        .doTest();
  }
}
