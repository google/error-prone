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

package com.google.errorprone.bugpatterns.flogger;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.bugpatterns.flogger.FloggerRequiredModifiers.Goal;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link FloggerRequiredModifiers}. */
@RunWith(JUnit4.class)
public class FloggerRequiredModifiersTest {
  private BugCheckerRefactoringTestHelper refactoringHelper(Goal goal) {
    return BugCheckerRefactoringTestHelper.newInstance(
        new FloggerRequiredModifiers(goal), getClass());
  }

  @Test
  public void negative() {
    for (Goal goal : Goal.values()) {
      // Re-initialize the bugchecker so we get a fresh run each time
      refactoringHelper(goal)
          .addInputLines(
              "Holder.java",
              "import com.google.common.flogger.FluentLogger;",
              "class Holder {",
              "  public FluentLogger logger;",
              "  public FluentLogger get() {return logger;}",
              "}")
          .expectUnchanged()
          .addInputLines(
              "Test.java",
              "import com.google.common.flogger.FluentLogger;",
              "class Test {",
              "  private static final FluentLogger logger = FluentLogger.forEnclosingClass();",
              "  public void log(FluentLogger l) {l.atInfo().log(\"bland\");}",
              "  public void delegate(Holder h) {h.logger.atInfo().log(\"held\");}",
              "  public void read(Holder h) {h.get().atInfo().log(\"got\");}",
              "}")
          .expectUnchanged()
          .doTest();
    }
  }

  @Test
  public void positive_addsStatic() {
    refactoringHelper(Goal.ADD_STATIC)
        .addInputLines(
            "in/Test.java",
            "import com.google.common.flogger.FluentLogger;",
            "class Test {",
            "  final FluentLogger logger = FluentLogger.forEnclosingClass();",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import com.google.common.flogger.FluentLogger;",
            "class Test {",
            "  static final FluentLogger logger = FluentLogger.forEnclosingClass();",
            "}")
        .doTest();
  }

  @Test
  public void negative_leavesNonFinalNonStatic() {
    refactoringHelper(Goal.ADD_STATIC)
        .addInputLines(
            "in/Test.java",
            "import com.google.common.flogger.FluentLogger;",
            "class Test {",
            "  private FluentLogger logger = FluentLogger.forEnclosingClass();",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void positive_extractsExpression() {
    refactoringHelper(Goal.HOIST_CONSTANT_EXPRESSIONS)
        .addInputLines(
            "in/Test.java",
            "import com.google.common.flogger.FluentLogger;",
            "class Test {",
            "  void doIt() {FluentLogger.forEnclosingClass().atInfo().log();}",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import com.google.common.flogger.FluentLogger;",
            "class Test {",
            "  private static final FluentLogger logger = FluentLogger.forEnclosingClass();",
            "  void doIt() {logger.atInfo().log();}",
            "}")
        .doTest();
  }

  @Test
  public void negative_doesntCreateSelfAssignment() {
    refactoringHelper(Goal.HOIST_CONSTANT_EXPRESSIONS)
        .addInputLines(
            "in/Test.java",
            "import com.google.common.flogger.FluentLogger;",
            "class Test {",
            "  private static final FluentLogger logger;",
            "  static {",
            "    logger = register(FluentLogger.forEnclosingClass());",
            "  }",
            "  private static <T> T register(T t) {return t;}",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void negative_doesntIndirectWrappers() {
    refactoringHelper(Goal.HOIST_CONSTANT_EXPRESSIONS)
        .addInputLines(
            "in/Test.java",
            "import com.google.common.flogger.FluentLogger;",
            "class Test {",
            "  private static FluentLogger logger = register(FluentLogger.forEnclosingClass());",
            "  private static <T> T register(T t) {return t;}",
            "}")
        .expectUnchanged()
        .doTest();
  }

  // People who do this generally do it for good reason, and for interfaces it's required.
  @Test
  public void negative_allowsSiblingLoggerUse() {
    refactoringHelper(Goal.REHOME_FOREIGN_LOGGERS)
        .addInputLines(
            "in/Test.java",
            "import com.google.common.flogger.FluentLogger;",
            "class Test {",
            "  static class A { public A() {B.logger.atInfo().log();}}",
            "  static class B {",
            "    private static FluentLogger logger = FluentLogger.forEnclosingClass();",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void positive_hidesLoggersFromInterfaces() {
    refactoringHelper(Goal.HIDE_LOGGERS_IN_INTERFACES)
        .addInputLines(
            "in/Test.java",
            "import com.google.common.flogger.FluentLogger;",
            "interface Test {",
            "  static FluentLogger logger = FluentLogger.forEnclosingClass();",
            "  default void foo() {logger.atInfo().log();}",
            "}")
        .addOutputLines(
            "in/Test.java",
            "import com.google.common.flogger.FluentLogger;",
            "interface Test {",
            "  default void foo() {",
            "    Private.logger.atInfo().log();",
            "  }",
            "  public static final class Private {",
            "    private Private() {}",
            "    private static final FluentLogger logger = FluentLogger.forEnclosingClass();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_extractsHiddenLoggersForInterfaces() {
    refactoringHelper(Goal.HOIST_CONSTANT_EXPRESSIONS)
        .addInputLines(
            "in/Test.java",
            "import com.google.common.flogger.FluentLogger;",
            "interface Test {",
            "  default void foo() {FluentLogger.forEnclosingClass().atInfo().log();}",
            "}")
        .addOutputLines(
            "in/Test.java",
            "import com.google.common.flogger.FluentLogger;",
            "interface Test {",
            "  default void foo() {",
            "    Private.logger.atInfo().log();",
            "  }",
            "  public static final class Private {",
            "    private Private() {}",
            "    private static final FluentLogger logger = FluentLogger.forEnclosingClass();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_fixesVisibility() {
    refactoringHelper(Goal.MAKE_PRIVATE)
        .addInputLines(
            "in/Test.java",
            "import com.google.common.flogger.FluentLogger;",
            "class Test {",
            "  public static final FluentLogger logger = FluentLogger.forEnclosingClass();",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import com.google.common.flogger.FluentLogger;",
            "class Test {",
            "  private static final FluentLogger logger = FluentLogger.forEnclosingClass();",
            "}")
        .doTest();
  }

  @Test
  public void positive_goalsDontConflict() {
    refactoringHelper(Goal.DEFAULT_ALL_GOALS)
        .addInputLines(
            "in/Test.java",
            "import com.google.common.flogger.FluentLogger;",
            "class Test {",
            "  public FluentLogger logger = FluentLogger.forEnclosingClass();",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import com.google.common.flogger.FluentLogger;",
            "class Test {",
            "  private static final FluentLogger logger = FluentLogger.forEnclosingClass();",
            "}")
        .doTest();
  }

  @Test
  public void positive_replacesInheritedLogger() {
    refactoringHelper(Goal.REHOME_FOREIGN_LOGGERS)
        .addInputLines(
            "in/Parent.java",
            "import com.google.common.flogger.FluentLogger;",
            "class Parent {",
            "  protected static final FluentLogger logger = FluentLogger.forEnclosingClass();",
            "}")
        .addOutputLines(
            "out/Parent.java",
            "import com.google.common.flogger.FluentLogger;",
            "class Parent {",
            "  protected static final FluentLogger logger = FluentLogger.forEnclosingClass();",
            "}")
        .addInputLines(
            "in/Child.java",
            "class Child extends Parent {",
            "  Child() {logger.atInfo().log(\"child\");",
            "           super.logger.atInfo().log(\"super\");",
            "           Parent.logger.atInfo().log(\"parent\");}",
            "}")
        .addOutputLines(
            "out/Child.java",
            "import com.google.common.flogger.FluentLogger;",
            "class Child extends Parent {",
            "  private static final FluentLogger logger = FluentLogger.forEnclosingClass();",
            "  Child() {logger.atInfo().log(\"child\");",
            "           logger.atInfo().log(\"super\");",
            "           logger.atInfo().log(\"parent\");}",
            "}")
        .doTest();
  }

  @Test
  public void positive_doesntCreateSelfReference() {
    refactoringHelper(Goal.REHOME_FOREIGN_LOGGERS)
        .addInputLines(
            "in/Parent.java",
            "import com.google.common.flogger.FluentLogger;",
            "class Parent {",
            "  protected static final FluentLogger logger = FluentLogger.forEnclosingClass();",
            "}")
        .expectUnchanged()
        .addInputLines(
            "in/Child.java",
            "import com.google.common.flogger.FluentLogger;",
            "class Child extends Parent {",
            "  private static final FluentLogger logger = Parent.logger;",
            "  Child() {logger.atInfo().log(\"child\");}",
            "}")
        .addOutputLines(
            "out/Child.java",
            "import com.google.common.flogger.FluentLogger;",
            "class Child extends Parent {",
            // Not an ideal fix, but good enough
            "  private static final FluentLogger flogger = FluentLogger.forEnclosingClass();",
            "  private static final FluentLogger logger = flogger;",
            "  Child() {logger.atInfo().log(\"child\");}",
            "}")
        .doTest();
  }

  @Test
  public void positive_handlesRewritesInMultipleFiles() {
    refactoringHelper(Goal.HIDE_LOGGERS_IN_INTERFACES)
        .addInputLines(
            "in/Parent.java",
            "import com.google.common.flogger.FluentLogger;",
            "interface Parent {",
            "}")
        .expectUnchanged()
        .addInputLines(
            "in/Child.java",
            "import com.google.common.flogger.FluentLogger;",
            "interface Child extends Parent {",
            "  static final FluentLogger logger = FluentLogger.forEnclosingClass();",
            "  default void go() {logger.atInfo().log();}",
            "}")
        .addOutputLines(
            "out/Child.java",
            "import com.google.common.flogger.FluentLogger;",
            "interface Child extends Parent {",
            "  default void go() {Private.logger.atInfo().log();}",
            "  public static final class Private {",
            "    private Private() {}",
            "    private static final FluentLogger logger = FluentLogger.forEnclosingClass();",
            "  }",
            "}")
        .addInputLines(
            "in/Sibling.java",
            "import com.google.common.flogger.FluentLogger;",
            "interface Sibling extends Parent {",
            "  static final FluentLogger logger = FluentLogger.forEnclosingClass();",
            "  default void go() {logger.atInfo().log();}",
            "}")
        .addOutputLines(
            "out/Sibling.java",
            "import com.google.common.flogger.FluentLogger;",
            "interface Sibling extends Parent {",
            "  default void go() {Private.logger.atInfo().log();}",
            "  public static final class Private {",
            "    private Private() {}",
            "    private static final FluentLogger logger = FluentLogger.forEnclosingClass();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_allowsSiblingLoggers() {
    refactoringHelper(Goal.REHOME_FOREIGN_LOGGERS)
        .addInputLines(
            "in/Test.java",
            "import com.google.common.flogger.FluentLogger;",
            "class Test {",
            "  private static final class Inner {",
            "    private static final FluentLogger logger = FluentLogger.forEnclosingClass();",
            "  }",
            "  private void go() {Inner.logger.atInfo().log();}",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void negative_doesntNeedlesslyMoveLoggersToInterfaces() {
    for (Goal goal :
        ImmutableList.of(Goal.REHOME_FOREIGN_LOGGERS, Goal.HIDE_LOGGERS_IN_INTERFACES)) {
      refactoringHelper(goal)
          .addInputLines(
              "in/Test.java",
              "import com.google.common.flogger.FluentLogger;",
              "interface Test {",
              "  class Inner {",
              "    private static final FluentLogger logger = FluentLogger.forEnclosingClass();",
              "    private static final class MoreInner {",
              "      private void go() {logger.atInfo().log();}",
              "    }",
              "  }",
              "}")
          .expectUnchanged()
          .doTest();
    }
  }
}
