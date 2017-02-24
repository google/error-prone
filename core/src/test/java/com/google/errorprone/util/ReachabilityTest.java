/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static java.util.stream.Collectors.toList;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.SwitchTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.SwitchTree;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/** {@link Reachability}Test. */
@RunWith(Parameterized.class)
public class ReachabilityTest {

  /** Reports an error if the first case in a switch falls through to the second. */
  @BugPattern(name = "FirstCaseFallsThrough", category = JDK, summary = "", severity = ERROR)
  public static class FirstCaseFallsThrough extends BugChecker implements SwitchTreeMatcher {

    @Override
    public Description matchSwitch(SwitchTree tree, VisitorState state) {
      if (tree.getCases().size() != 2 || tree.getCases().get(0).getStatements().isEmpty()) {
        return NO_MATCH;
      }
      return Reachability.canCompleteNormally(
              Iterables.getLast(tree.getCases().get(0).getStatements()))
          ? describeMatch(tree.getCases().get(1))
          : NO_MATCH;
    }
  }

  private final String[] lines;

  public ReachabilityTest(String[] lines) {
    this.lines = lines;
  }

  @Test
  public void test() {
    CompilationTestHelper.newInstance(FirstCaseFallsThrough.class, getClass())
        .addSourceLines(
            "in/Test.java",
            "import java.io.*;",
            "import java.nio.file.*;",
            "class Test {",
            "  void f(int x) {",
            "    switch (x) {",
            "      case 1:",
            Joiner.on('\n').join(lines),
            "      default:",
            "        break;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Parameters
  public static List<Object[]> parameters() {
    String[][] parameters = {
      {
        "int a = 1;", //
        "int b = 2;",
        "break;",
      },
      {
        "System.err.println();", //
        "// BUG: Diagnostic contains:",
      },
      {
        "int a = 1;",
        "int b = 2;",
        "class L {}",
        ";;",
        "assert false;",
        "label: System.err.println();",
        "// BUG: Diagnostic contains:",
      },
      {
        "if (true) {", //
        "} else {",
        "  break;",
        "}",
        "// BUG: Diagnostic contains:",
      },
      {
        "if (true) {", //
        "  break;",
        "}",
        "// BUG: Diagnostic contains:",
      },
      {
        "if (true) {", //
        "  break;",
        "} else {",
        "}",
        "// BUG: Diagnostic contains:",
      },
      {
        "if (true) {", //
        "  break;",
        "} else {",
        "  break;",
        "}",
      },
      {
        "switch (42) {", //
        "  case 0:",
        "  case 1:",
        "  case 2:",
        "}",
        "// BUG: Diagnostic contains:",
      },
      {
        "switch (42) {", //
        "}",
        "// BUG: Diagnostic contains:",
      },
      {
        "switch (42) {",
        "  case 0:",
        "    break;",
        "  case 1:",
        "}",
        "// BUG: Diagnostic contains:",
      },
      {
        "switch (42) {", //
        "  default:",
        "    break;",
        "}",
        "// BUG: Diagnostic contains:",
      },
      {
        "switch (42) {", //
        "  default:",
        "    return;",
        "}",
      },
      {
        "while (true) {", //
        "}",
      },
      {
        "while (true) {", //
        "  break;",
        "}",
        "// BUG: Diagnostic contains:",
      },
      {
        "while (true) {", //
        "  return;",
        "}",
      },
      {
        "while (x == 0) {}", //
        "// BUG: Diagnostic contains:",
      },
      {
        "loop: do {", //
        "  continue loop;",
        "} while (x == 0);",
        "// BUG: Diagnostic contains:",
      },
      {
        "loop: do {", //
        "  continue loop;",
        "} while (true);",
      },
      {
        "int i = 1;",
        "loop: do {",
        "  i++;",
        "  continue loop;",
        "} while (i == 0);",
        "// BUG: Diagnostic contains:",
      },
      {
        "try {",
        "  Files.readAllBytes(Paths.get(\"file\"));",
        "  return;",
        "} catch (IOException e) {",
        "  return;",
        "}",
      },
      {
        "try {",
        "  try {",
        "    Files.readAllBytes(Paths.get(\"file\"));",
        "    return;",
        "  } catch (NullPointerException e) {",
        "    return;",
        "  }",
        "} catch (IOException e) {",
        "  return;",
        "}",
      },
      {
        "try {",
        "  Files.readAllBytes(Paths.get(\"file\"));",
        "  return;",
        "} catch (IOException e) {",
        "  return;",
        "} finally {",
        "}",
      },
      {
        "try {", //
        "  //",
        "} catch (Throwable t) {",
        "  return;",
        "} finally {",
        "  return;",
        "}",
      },
      {
        "try {", //
        "  return;",
        "} catch (Throwable t) {",
        "  //",
        "} finally {",
        "  return;",
        "}",
      },
      {
        "int y = 0;",
        "while (true) {",
        "  if (y++ > 10) {",
        "    return;",
        "  }",
        "  if (y-- < 10) {",
        "    return;",
        "  }",
        "}",
      },
      {
        "int y = 0;",
        "while (true) {",
        "  do {",
        "    switch (y) {",
        "      case 0:",
        "        continue;", // continue target is do/while, not switch or outer while
        "    }",
        "  } while (y > 0);",
        "  break;",
        "}",
        "// BUG: Diagnostic contains:",
      },
      {
        "try {",
        "  if (Files.readAllBytes(Paths.get(\"file\")) != null) {}",
        "  return;",
        "} catch (IOException e) {",
        "}",
        "// BUG: Diagnostic contains:",
      },
      {
        "try {",
        "  Files.readAllBytes(Paths.get(\"file\"));",
        "} catch (IOException e) {",
        "  throw new IOError(e);",
        "}",
        "// BUG: Diagnostic contains:",
      },
      {
        "try {",
        "  Files.readAllBytes(Paths.get(\"file\"));",
        "  return;",
        "} catch (IOException e) {",
        "  throw new IOError(e);",
        "}",
      },
      {
        "throw new AssertionError();",
      },
      {
        "for (;;) {}",
      },
      {
        "System.exit(1);", //
      },
      {
        "{", //
        "  System.exit(1);",
        "  throw new AssertionError();",
        "}",
      },
      {
        "l:", //
        "do {",
        "  break l;",
        "} while (true);",
        "System.err.println();",
        "// BUG: Diagnostic contains:",
      },
    };
    return Arrays.stream(parameters).map(x -> new Object[] {x}).collect(toList());
  }
}
