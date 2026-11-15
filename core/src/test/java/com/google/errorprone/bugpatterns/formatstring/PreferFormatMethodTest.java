/*
 * Copyright 2026 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.formatstring;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class PreferFormatMethodTest {

  private final BugCheckerRefactoringTestHelper helper =
      BugCheckerRefactoringTestHelper.newInstance(PreferFormatMethod.class, getClass());

  @Test
  public void positiveConstructor_stringFormat() {
    helper
        .addInputLines(
            "Test.java",
            """
            import com.google.errorprone.annotations.FormatMethod;
            import com.google.errorprone.annotations.FormatString;

            class Test {
              static class MyException extends Exception {
                MyException(String message) {
                  super(message);
                }

                @FormatMethod
                MyException(@FormatString String format, Object... args) {
                  super(String.format(format, args));
                }
              }

              void test(String user, int id) throws MyException {
                throw new MyException(String.format("User %s has invalid id %d", user, id));
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.google.errorprone.annotations.FormatMethod;
            import com.google.errorprone.annotations.FormatString;

            class Test {
              static class MyException extends Exception {
                MyException(String message) {
                  super(message);
                }

                @FormatMethod
                MyException(@FormatString String format, Object... args) {
                  super(String.format(format, args));
                }
              }

              void test(String user, int id) throws MyException {
                throw new MyException("User %s has invalid id %d", user, id);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void positiveConstructor_stringFormatted() {
    helper
        .addInputLines(
            "Test.java",
            """
            import com.google.errorprone.annotations.FormatMethod;
            import com.google.errorprone.annotations.FormatString;

            class Test {
              static class MyException extends Exception {
                MyException(String message) {
                  super(message);
                }

                @FormatMethod
                MyException(@FormatString String format, Object... args) {
                  super(String.format(format, args));
                }
              }

              void test(String user, int id) throws MyException {
                throw new MyException("User %s has invalid id %d".formatted(user, id));
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.google.errorprone.annotations.FormatMethod;
            import com.google.errorprone.annotations.FormatString;

            class Test {
              static class MyException extends Exception {
                MyException(String message) {
                  super(message);
                }

                @FormatMethod
                MyException(@FormatString String format, Object... args) {
                  super(String.format(format, args));
                }
              }

              void test(String user, int id) throws MyException {
                throw new MyException("User %s has invalid id %d", user, id);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void positiveConstructor_causeSwap() {
    helper
        .addInputLines(
            "Test.java",
            """
            import com.google.errorprone.annotations.FormatMethod;
            import com.google.errorprone.annotations.FormatString;

            class Test {
              static class MyException extends Exception {
                MyException(String message, Throwable cause) {
                  super(message, cause);
                }

                @FormatMethod
                MyException(Throwable cause, @FormatString String format, Object... args) {
                  super(String.format(format, args), cause);
                }
              }

              void test(String item, Throwable cause) throws MyException {
                throw new MyException(String.format("Failed loading %s", item), cause);
              }

              void testFormatted(String item, Throwable cause) throws MyException {
                throw new MyException("Failed loading %s".formatted(item), cause);
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.google.errorprone.annotations.FormatMethod;
            import com.google.errorprone.annotations.FormatString;

            class Test {
              static class MyException extends Exception {
                MyException(String message, Throwable cause) {
                  super(message, cause);
                }

                @FormatMethod
                MyException(Throwable cause, @FormatString String format, Object... args) {
                  super(String.format(format, args), cause);
                }
              }

              void test(String item, Throwable cause) throws MyException {
                throw new MyException(cause, "Failed loading %s", item);
              }

              void testFormatted(String item, Throwable cause) throws MyException {
                throw new MyException(cause, "Failed loading %s", item);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void positiveStaticAndInstanceMethods_withPrefixArgs() {
    helper
        .addInputLines(
            "Test.java",
            """
            import com.google.errorprone.annotations.FormatMethod;
            import com.google.errorprone.annotations.FormatString;

            class Test {
              static class Validator {
                static void check(boolean condition, String message) {}

                @FormatMethod
                static void check(boolean condition, @FormatString String format, Object... args) {}

                Validator addError(Throwable cause, String message) {
                  return this;
                }

                @FormatMethod
                Validator addError(Throwable cause, @FormatString String format, Object... args) {
                  return this;
                }
              }

              void test(Validator v, Throwable cause, String key, int count) {
                Validator.check(count > 0, String.format("Invalid count %d for %s", count, key));
                v.addError(cause, "Failed on %s (%d)".formatted(key, count));
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.google.errorprone.annotations.FormatMethod;
            import com.google.errorprone.annotations.FormatString;

            class Test {
              static class Validator {
                static void check(boolean condition, String message) {}

                @FormatMethod
                static void check(boolean condition, @FormatString String format, Object... args) {}

                Validator addError(Throwable cause, String message) {
                  return this;
                }

                @FormatMethod
                Validator addError(Throwable cause, @FormatString String format, Object... args) {
                  return this;
                }
              }

              void test(Validator v, Throwable cause, String key, int count) {
                Validator.check(count > 0, "Invalid count %d for %s", count, key);
                v.addError(cause, "Failed on %s (%d)", key, count);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void positiveForwardingFormatStringParameter() {
    helper
        .addInputLines(
            "Test.java",
            """
            import com.google.errorprone.annotations.FormatMethod;
            import com.google.errorprone.annotations.FormatString;

            class Test {
              static class MyException extends Exception {
                MyException(String message) {
                  super(message);
                }

                @FormatMethod
                MyException(@FormatString String format, Object... args) {
                  super(String.format(format, args));
                }
              }

              @FormatMethod
              MyException wrap(@FormatString String format, Object... args) {
                return new MyException(String.format(format, args));
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.google.errorprone.annotations.FormatMethod;
            import com.google.errorprone.annotations.FormatString;

            class Test {
              static class MyException extends Exception {
                MyException(String message) {
                  super(message);
                }

                @FormatMethod
                MyException(@FormatString String format, Object... args) {
                  super(String.format(format, args));
                }
              }

              @FormatMethod
              MyException wrap(@FormatString String format, Object... args) {
                return new MyException(format, args);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negativeSelfDelegationInFormatMethod() {
    helper
        .addInputLines(
            "Test.java",
            """
            import com.google.errorprone.annotations.FormatMethod;
            import com.google.errorprone.annotations.FormatString;

            class Test {
              static class MyException extends Exception {
                MyException(String message) {
                  super(message);
                }

                @FormatMethod
                MyException(@FormatString String format, Object... args) {
                  this(String.format(format, args));
                }

                static MyException create(String message) {
                  return new MyException(message);
                }

                @FormatMethod
                static MyException create(@FormatString String format, Object... args) {
                  return create(String.format(format, args));
                }
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void negativeShadowedByNonFormatOverload() {
    helper
        .addInputLines(
            "Test.java",
            """
            import com.google.errorprone.annotations.FormatMethod;
            import com.google.errorprone.annotations.FormatString;

            class Test {
              static class BadlyDesignedException extends Exception {
                BadlyDesignedException(String message) {
                  super(message);
                }

                BadlyDesignedException(String message, Throwable cause) {
                  super(message, cause);
                }

                @FormatMethod
                BadlyDesignedException(@FormatString String format, Object... args) {
                  super(String.format(format, args));
                }
              }

              void testShadowed(Throwable t) throws BadlyDesignedException {
                // Unwrapping to ("Failed: %s", t) would resolve to (String, Throwable) instead!
                throw new BadlyDesignedException(String.format("Failed: %s", t));
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void negativeNonConstantOrLocaleOrZeroArgs() {
    helper
        .addInputLines(
            "Test.java",
            """
            import com.google.errorprone.annotations.FormatMethod;
            import com.google.errorprone.annotations.FormatString;
            import java.util.Locale;

            class Test {
              static class MyException extends Exception {
                MyException(String message) {
                  super(message);
                }

                @FormatMethod
                MyException(@FormatString String format, Object... args) {
                  super(String.format(format, args));
                }
              }

              void test(String dynamicFormat, String arg) throws MyException {
                throw new MyException(String.format(dynamicFormat, arg));
              }

              void testLocale(String arg) throws MyException {
                throw new MyException(String.format(Locale.US, "Hello %s", arg));
              }

              void testZeroArgs() throws MyException {
                throw new MyException(String.format("No format args"));
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void negativeInaccessibleFormatMethod() {
    helper
        .addInputLines(
            "Helper.java",
            """
            import com.google.errorprone.annotations.FormatMethod;
            import com.google.errorprone.annotations.FormatString;

            class Helper {
              static void log(String message) {}

              @FormatMethod
              private static void log(@FormatString String format, Object... args) {}
            }
            """)
        .expectUnchanged()
        .addInputLines(
            "Test.java",
            """
            class Test {
              void test(String arg) {
                Helper.log(String.format("Hello %s", arg));
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void positiveInheritedFormatMethodAndConstantFormatString() {
    helper
        .addInputLines(
            "Test.java",
            """
            import com.google.errorprone.annotations.FormatMethod;
            import com.google.errorprone.annotations.FormatString;

            class Test {
              private static final String TEMPLATE = "Step %s failed after %d attempts";

              static class BaseLogger {
                void log(String message) {}
              }

              static class SubLogger extends BaseLogger {
                @FormatMethod
                void log(@FormatString String format, Object... args) {}

                void logInternal(String step, int attempts) {
                  log(String.format(TEMPLATE, step, attempts));
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.google.errorprone.annotations.FormatMethod;
            import com.google.errorprone.annotations.FormatString;

            class Test {
              private static final String TEMPLATE = "Step %s failed after %d attempts";

              static class BaseLogger {
                void log(String message) {}
              }

              static class SubLogger extends BaseLogger {
                @FormatMethod
                void log(@FormatString String format, Object... args) {}

                void logInternal(String step, int attempts) {
                  log(TEMPLATE, step, attempts);
                }
              }
            }
            """)
        .doTest();
  }
}
