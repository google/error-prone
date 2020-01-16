/*
 * Copyright 2020 The Error Prone Authors.
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

/** {@link ForEachIterable}Test */
@RunWith(JUnit4.class)
public class ForEachIterableTest {

  @Test
  public void positive() {
    BugCheckerRefactoringTestHelper.newInstance(new ForEachIterable(), getClass())
        .addInputLines(
            "in/Test.java",
            "import java.util.Iterator;",
            "abstract class Test<T> {",
            "  abstract void doSomething(T element);",
            "  void iteratorFor(Iterable<T> list) {",
            "    for (Iterator<T> iterator = list.iterator(); iterator.hasNext(); ) {",
            "      doSomething(iterator.next());",
            "    }",
            "  }",
            "  void iteratorWhile(Iterable<T> list) {",
            "    Iterator<T> iterator = list.iterator();",
            "    while (iterator.hasNext()) {",
            "      doSomething(iterator.next());",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import java.util.Iterator;",
            "abstract class Test<T> {",
            "  abstract void doSomething(T element);",
            "  void iteratorFor(Iterable<T> list) {",
            "    for (T element : list) {",
            "      doSomething(element);",
            "    }",
            "  }",
            "  void iteratorWhile(Iterable<T> list) {",
            "    for (T element : list) {",
            "      doSomething(element);",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void reuseVariable() {
    BugCheckerRefactoringTestHelper.newInstance(new ForEachIterable(), getClass())
        .addInputLines(
            "in/Test.java",
            "import java.util.Iterator;",
            "abstract class Test<T> {",
            "  abstract void doSomething(T element);",
            "  void iteratorWhile(Iterable<T> list) {",
            "    Iterator<T> iterator = list.iterator();",
            "    while (iterator.hasNext()) {",
            "      T t = iterator.next();",
            "      doSomething(t);",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import java.util.Iterator;",
            "abstract class Test<T> {",
            "  abstract void doSomething(T element);",
            "  void iteratorWhile(Iterable<T> list) {",
            "    for (T t : list) {",
            "      doSomething(t);",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void wildcard() {
    BugCheckerRefactoringTestHelper.newInstance(new ForEachIterable(), getClass())
        .addInputLines(
            "in/Test.java",
            "import java.util.Iterator;",
            "abstract class Test {",
            "  abstract void doSomething(Object element);",
            "  void iteratorWhile(Iterable<?> list) {",
            "    Iterator<?> iterator = list.iterator();",
            "    while (iterator.hasNext()) {",
            "      doSomething(iterator.next());",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import java.util.Iterator;",
            "abstract class Test {",
            "  abstract void doSomething(Object element);",
            "  void iteratorWhile(Iterable<?> list) {",
            "    for (Object element : list) {",
            "      doSomething(element);",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void empty() {
    BugCheckerRefactoringTestHelper.newInstance(new ForEachIterable(), getClass())
        .addInputLines(
            "in/Test.java",
            "import java.util.Iterator;",
            "abstract class Test {",
            "  abstract void doSomething(Object element);",
            "  void iteratorWhile(Iterable<?> list) {",
            "    Iterator<?> iterator = list.iterator();",
            "    while (iterator.hasNext()) {",
            "      iterator.next();",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import java.util.Iterator;",
            "abstract class Test {",
            "  abstract void doSomething(Object element);",
            "  void iteratorWhile(Iterable<?> list) {",
            "    for (Object element : list) {",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void wildcardExtends() {
    BugCheckerRefactoringTestHelper.newInstance(new ForEachIterable(), getClass())
        .addInputLines(
            "in/Test.java",
            "import java.util.Iterator;",
            "abstract class Test {",
            "  abstract void doSomething(String element);",
            "  void iteratorWhile(Iterable<? extends String> list) {",
            "    Iterator<? extends String> iterator = list.iterator();",
            "    while (iterator.hasNext()) {",
            "      iterator.next();",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import java.util.Iterator;",
            "abstract class Test {",
            "  abstract void doSomething(String element);",
            "  void iteratorWhile(Iterable<? extends String> list) {",
            "    for (String element : list) {",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    CompilationTestHelper.newInstance(ForEachIterable.class, getClass())
        .addSourceLines(
            "in/Test.java",
            "import java.util.Iterator;",
            "abstract class Test<T> {",
            "  abstract void doSomething(T element);",
            "  void forUpdate(Iterable<T> list) {",
            "    for (Iterator<T> it = list.iterator(); it.hasNext(); it.next()) {",
            "      doSomething(it.next());",
            "    }",
            "  }",
            "  void forMultiVariable(Iterable<T> list) {",
            "    for (Iterator<T> iterator = list.iterator(), y = null; iterator.hasNext(); ) {",
            "      doSomething(iterator.next());",
            "    }",
            "  }",
            "  void forTwoStep(Iterable<T> list) {",
            "    for (Iterator<T> iterator = list.iterator(); iterator.hasNext(); ) {",
            "      doSomething(iterator.next());",
            "      doSomething(iterator.next());",
            "    }",
            "  }",
            "  void whileTwoStep(Iterable<T> list) {",
            "    Iterator<T> iterator = list.iterator();",
            "    while (iterator.hasNext()) {",
            "      doSomething(iterator.next());",
            "      doSomething(iterator.next());",
            "    }",
            "  }",
            "  void whileUseOutsideLoop(Iterable<T> list) {",
            "    Iterator<T> iterator = list.iterator();",
            "    while (iterator.hasNext()) {",
            "      doSomething(iterator.next());",
            "    }",
            "    doSomething(iterator.next());",
            "  }",
            "  void forIteratorUse(Iterable<?> list) {",
            "    Iterator<?> iterator = list.iterator();",
            "    while (iterator.hasNext()) {",
            "      iterator.next();",
            "      iterator.remove();",
            "    }",
            "  }",
            "}")
        .doTest();
  }
}
