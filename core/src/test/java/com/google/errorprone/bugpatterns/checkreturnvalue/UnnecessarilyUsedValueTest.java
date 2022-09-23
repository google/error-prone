/*
 * Copyright 2022 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.checkreturnvalue;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class UnnecessarilyUsedValueTest {

  private final BugCheckerRefactoringTestHelper helper =
      BugCheckerRefactoringTestHelper.newInstance(UnnecessarilyUsedValue.class, getClass());

  @Test
  public void testMethods() {
    helper
        .addInputLines(
            "Client.java",
            "package com.google.frobber;",
            "public final class Client {",
            "  public void varNotUnused() {",
            "    var notUnused = ignorable();",
            "  }",
            "  public void varUnused() {",
            "    var unused = ignorable();",
            "  }",
            "  public void varUnusedFoo() {",
            "    var unusedFoo = ignorable();",
            "  }",
            "  public void objectUnused() {",
            "    Object unused = ignorable();",
            "  }",
            "  public void objectUnusedFoo() {",
            "    Object unusedFoo = ignorable();",
            "  }",
            "  public void reuseOfUnusedVariable(String unused) {",
            "    unused = ignorable();",
            "  }",
            "  @com.google.errorprone.annotations.CanIgnoreReturnValue",
            "  public String ignorable() {",
            "    return \"hi\";",
            "  }",
            "}")
        .addOutputLines(
            "Client.java",
            "package com.google.frobber;",
            "public final class Client {",
            "  public void varNotUnused() {",
            "    var notUnused = ignorable();",
            "  }",
            "  public void varUnused() {",
            "    ignorable();",
            "  }",
            "  public void varUnusedFoo() {",
            "    var unusedFoo = ignorable();",
            "  }",
            "  public void objectUnused() {",
            "    ignorable();",
            "  }",
            "  public void objectUnusedFoo() {",
            "    Object unusedFoo = ignorable();",
            "  }",
            "  public void reuseOfUnusedVariable(String unused) {",
            "    ignorable();",
            "  }",
            "  @com.google.errorprone.annotations.CanIgnoreReturnValue",
            "  public String ignorable() {",
            "    return \"hi\";",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testConstructors() {
    helper
        .addInputLines(
            "Client.java",
            "package com.google.frobber;",
            "public final class Client {",
            "  public void varNotUnused() {",
            "    var notUnused = new Client();",
            "  }",
            "  public void varUnused() {",
            "    var unused = new Client();",
            "  }",
            "  public void varUnusedFoo() {",
            "    var unusedFoo = new Client();",
            "  }",
            "  public void objectUnused() {",
            "    Object unused = new Client();",
            "  }",
            "  public void objectUnusedFoo() {",
            "    Object unusedFoo = new Client();",
            "  }",
            "  public void reuseOfUnusedVariable(Client unused) {",
            "    unused = new Client();",
            "  }",
            "  @com.google.errorprone.annotations.CanIgnoreReturnValue",
            "  public Client() {",
            "  }",
            "}")
        .addOutputLines(
            "Client.java",
            "package com.google.frobber;",
            "public final class Client {",
            "  public void varNotUnused() {",
            "    var notUnused = new Client();",
            "  }",
            "  public void varUnused() {",
            "    new Client();",
            "  }",
            "  public void varUnusedFoo() {",
            "    var unusedFoo = new Client();",
            "  }",
            "  public void objectUnused() {",
            "    new Client();",
            "  }",
            "  public void objectUnusedFoo() {",
            "    Object unusedFoo = new Client();",
            "  }",
            "  public void reuseOfUnusedVariable(Client unused) {",
            "    new Client();",
            "  }",
            "  @com.google.errorprone.annotations.CanIgnoreReturnValue",
            "  public Client() {",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testTryWithResources() {
    helper
        .addInputLines(
            "Client.java",
            "package com.google.frobber;",
            "public final class Client {",
            "  public void varNotUnused() throws Exception  {",
            "    try (java.io.Closeable unused = getCloseable()) {",
            "    }",
            "  }",
            "  @com.google.errorprone.annotations.CanIgnoreReturnValue",
            "  private java.io.Closeable getCloseable() {",
            "    return null;",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }
}
