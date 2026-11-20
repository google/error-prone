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

package com.google.errorprone.matchers;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.errorprone.BugPattern;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.sun.source.tree.VariableTree;
import javax.lang.model.element.ElementKind;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link CompileTimeConstantExpressionMatcher}Test */
@RunWith(JUnit4.class)
public class CompileTimeConstantExpressionMatcherTest {
  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(
          CompileTimeConstantExpressionMatcherTester.class, getClass());

  @Test
  public void matchesLiteralsAndStaticFinals() {
    testHelper
        .addSourceLines(
            "Test.java",
            """
            public class Test {
              private final String final_string = "bap";
              private final int final_int = 29;
              private static final int static_final_int = 29;

              public void m() {
                // BUG: Diagnostic contains: true
                String s1 = "boop";
                // BUG: Diagnostic contains: true
                String s2 = "boop" + final_string;
                // BUG: Diagnostic contains: true
                int int2 = 42;
                // BUG: Diagnostic contains: true
                Integer int3 = 42 * final_int;
                // BUG: Diagnostic contains: true
                Integer int4 = 12 - static_final_int;
                // BUG: Diagnostic contains: true
                boolean bool4 = false;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void nullLiteral() {
    testHelper
        .addSourceLines(
            "Test.java",
            """
            public class Test {
              private static final String static_final_string = null;

              public void m() {
                // BUG: Diagnostic contains: true
                String s1 = null;
                // BUG: Diagnostic contains: false
                String s2 = static_final_string;
                // BUG: Diagnostic contains: true
                String s3 = (String) null;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void doesNotMatchNonLiterals() {
    testHelper
        .addSourceLines(
            "Test.java",
            """
            package test;

            public class Test {
              private final int nonfinal_int;

              public Test(int i) {
                nonfinal_int = i;
              }

              public void m(String s) {
                // BUG: Diagnostic contains: false
                String s1 = s;
                // BUG: Diagnostic contains: false
                int int2 = s.length();
                // BUG: Diagnostic contains: false
                Integer int3 = nonfinal_int;
                // BUG: Diagnostic contains: false
                Integer int4 = 14 * nonfinal_int;
                // BUG: Diagnostic contains: true
                boolean bool4 = false;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void finalCompileTimeConstantMethodParameters() {
    testHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.errorprone.annotations.CompileTimeConstant;

            public class Test {
              public void m1(final @CompileTimeConstant String s) {
                // BUG: Diagnostic contains: true
                String s1 = s;
              }

              public void m2(@CompileTimeConstant String s) {
                s = null;
                // BUG: Diagnostic contains: false
                String s2 = s;
              }

              public void m3(final String s) {
                // BUG: Diagnostic contains: false
                String s3 = s;
              }

              public void m4(@CompileTimeConstant String s) {
                // BUG: Diagnostic contains: true
                String s4 = s;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void finalCompileTimeConstantConstructorParameters() {
    testHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.errorprone.annotations.CompileTimeConstant;

            public class Test {
              public Test(final @CompileTimeConstant String s) {
                // BUG: Diagnostic contains: true
                String s1 = s;
              }

              public Test(@CompileTimeConstant String s, int foo) {
                s = null;
                // BUG: Diagnostic contains: false
                String s2 = s;
              }

              public Test(final String s, boolean foo) {
                // BUG: Diagnostic contains: false
                String s3 = s;
              }

              public Test(@CompileTimeConstant String s, long foo) {
                // BUG: Diagnostic contains: true
                String s4 = s;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void conditionalExpression() {
    testHelper
        .addSourceLines(
            "Test.java",
            """
            abstract class Test {
              abstract boolean g();

              public void m(boolean flag) {
                // BUG: Diagnostic contains: false
                boolean bool1 = flag ? true : g();
                // BUG: Diagnostic contains: false
                boolean bool2 = flag ? g() : false;
                // BUG: Diagnostic contains: true
                boolean bool3 = flag ? true : false;
                // BUG: Diagnostic contains: true
                String str = "a" + (flag ? "b" : "c") + "d";
              }
            }
            """)
        .doTest();
  }

  @Test
  public void parentheses() {
    testHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.errorprone.annotations.CompileTimeConstant;

            abstract class Test {
              public void m(@CompileTimeConstant String ctc) {
                // BUG: Diagnostic contains: true
                String a = (ctc);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void concatenatedStrings() {
    testHelper
        .addSourceLines(
            "Test.java",
            """
            package test;

            import com.google.errorprone.annotations.CompileTimeConstant;

            abstract class Test {
              public void m(@CompileTimeConstant String ctc, String nonCtc) {
                // BUG: Diagnostic contains: true
                String a = "foo" + ctc;
                // BUG: Diagnostic contains: true
                String b = ctc + "foo";
                // BUG: Diagnostic contains: false
                String c = nonCtc + "foo";
                // BUG: Diagnostic contains: false
                String d = nonCtc + ctc;
                // BUG: Diagnostic contains: true
                String e = "foo" + (ctc == null ? "" : "");
                // BUG: Diagnostic contains: true
                String f = "foo" + 1;
                // BUG: Diagnostic contains: true
                String g = "foo" + 3.14;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void qualifiedAndRecordAndInterfaceFields() {
    testHelper
        .addSourceLines(
            "Test.java",
            """
            package test;

            import com.google.common.collect.ImmutableList;
            import com.google.errorprone.annotations.CompileTimeConstant;

            public class Test {
              interface HolderInterface {
                @CompileTimeConstant ImmutableList<String> LIST = ImmutableList.of("a");
              }

              record HolderRecord(@CompileTimeConstant String s) {
                void m(HolderRecord other) {
                  // BUG: Diagnostic contains: true
                  String s1 = s;
                  // BUG: Diagnostic contains: true
                  String s2 = this.s;
                  // BUG: Diagnostic contains: true
                  String s3 = other.s;
                }
              }

              @CompileTimeConstant final String s;

              public Test(@CompileTimeConstant String s) {
                this.s = s;
              }

              public void m(Test other) {
                // BUG: Diagnostic contains: true
                String s1 = this.s;
                // BUG: Diagnostic contains: true
                String s2 = other.s;
                // BUG: Diagnostic contains: true
                ImmutableList<String> list = HolderInterface.LIST;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void classLiteralsAndEnumConstants() {
    testHelper
        .addSourceLines(
            "Test.java",
            """
            package test;

            import com.google.common.collect.ImmutableList;
            import java.math.RoundingMode;
            import java.time.DayOfWeek;

            public class Test {
              enum CustomEnum {
                FIRST,
                SECOND
              }

              public void m() {
                // BUG: Diagnostic contains: true
                Class<?> c1 = String.class;
                // BUG: Diagnostic contains: true
                Class<?> c2 = int.class;
                // BUG: Diagnostic contains: true
                Class<?> c3 = void.class;
                // BUG: Diagnostic contains: true
                Class<?> c4 = String[].class;
                // BUG: Diagnostic contains: true
                ImmutableList<Class<?>> classes = ImmutableList.of(String.class, int.class);
                // BUG: Diagnostic contains: true
                DayOfWeek d = DayOfWeek.MONDAY;
                // BUG: Diagnostic contains: true
                RoundingMode r = RoundingMode.HALF_UP;
                // BUG: Diagnostic contains: true
                CustomEnum e = CustomEnum.FIRST;
                // BUG: Diagnostic contains: false
                String enumToString = CustomEnum.FIRST.toString();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void compatibleConstructors() {
    testHelper
        .addSourceLines(
            "Test.java",
            """
            package test;

            import com.google.errorprone.annotations.CompileTimeConstant;
            import java.math.BigDecimal;
            import java.math.BigInteger;
            import java.math.MathContext;
            import java.math.RoundingMode;
            import java.util.Locale;
            import java.util.UUID;

            public class Test {
              public void m(
                  @CompileTimeConstant String ctcString,
                  String nonCtcString,
                  @CompileTimeConstant char[] ctcCharArray) {
                // BUG: Diagnostic contains: true
                String s1 = new String();
                // BUG: Diagnostic contains: true
                String s2 = new String(ctcString);
                // BUG: Diagnostic contains: false
                String s3 = new String(nonCtcString);
                // BUG: Diagnostic contains: false
                String s4 = new String(ctcCharArray);
                // BUG: Diagnostic contains: true
                Integer i1 = new Integer(42);
                // BUG: Diagnostic contains: true
                BigInteger bi1 = new BigInteger("12345");
                // BUG: Diagnostic contains: true
                BigInteger bi2 = new BigInteger("ff", 16);
                // BUG: Diagnostic contains: false
                BigInteger bi3 = new BigInteger(nonCtcString);
                // BUG: Diagnostic contains: false
                BigInteger biAnon = new BigInteger("12345") {};
                // BUG: Diagnostic contains: true
                MathContext mc = new MathContext(10, RoundingMode.HALF_UP);
                // BUG: Diagnostic contains: true
                BigDecimal bd1 = new BigDecimal("12.34", MathContext.DECIMAL64);
                // BUG: Diagnostic contains: true
                BigDecimal bd2 = new BigDecimal(42L);
                // BUG: Diagnostic contains: false
                BigDecimal bd3 = new BigDecimal(BigInteger.ONE);
                // BUG: Diagnostic contains: false
                BigDecimal bd4 = new BigDecimal(ctcCharArray);
                // BUG: Diagnostic contains: true
                Locale loc = new Locale("en", "US");
                // BUG: Diagnostic contains: true
                UUID uuid = new UUID(0L, 1L);
                // BUG: Diagnostic contains: false
                Object obj = new Object();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void compatibleFields() {
    testHelper
        .addSourceLines(
            "Test.java",
            """
            package test;

            import java.io.PrintStream;
            import java.math.BigDecimal;
            import java.math.BigInteger;
            import java.math.MathContext;
            import java.nio.charset.Charset;
            import java.nio.charset.StandardCharsets;
            import java.time.Duration;
            import java.time.Instant;
            import java.time.LocalDate;
            import java.time.LocalDateTime;
            import java.time.LocalTime;
            import java.time.OffsetDateTime;
            import java.time.OffsetTime;
            import java.time.Period;
            import java.time.ZoneOffset;
            import java.time.format.DateTimeFormatter;
            import java.time.temporal.IsoFields;
            import java.time.temporal.JulianFields;
            import java.time.temporal.TemporalField;
            import java.time.temporal.WeekFields;
            import java.util.Collections;
            import java.util.Comparator;
            import java.util.List;
            import java.util.Locale;

            public class Test {
              private static final Object UNANNOTATED_STATIC_FINAL = new Object();

              public void m() {
                // BUG: Diagnostic contains: true
                Boolean b = Boolean.TRUE;
                // BUG: Diagnostic contains: true
                Class<Integer> intType = Integer.TYPE;
                // BUG: Diagnostic contains: true
                Comparator<String> cmp = String.CASE_INSENSITIVE_ORDER;
                // BUG: Diagnostic contains: true
                BigInteger bi = BigInteger.TEN;
                // BUG: Diagnostic contains: true
                BigDecimal bd = BigDecimal.ZERO;
                // BUG: Diagnostic contains: true
                MathContext mc = MathContext.DECIMAL128;
                // BUG: Diagnostic contains: true
                Duration d = Duration.ZERO;
                // BUG: Diagnostic contains: true
                Instant inst = Instant.EPOCH;
                // BUG: Diagnostic contains: true
                LocalDate ld = LocalDate.EPOCH;
                // BUG: Diagnostic contains: true
                LocalDateTime ldt = LocalDateTime.MIN;
                // BUG: Diagnostic contains: true
                LocalTime lt = LocalTime.MIDNIGHT;
                // BUG: Diagnostic contains: true
                OffsetDateTime odt = OffsetDateTime.MAX;
                // BUG: Diagnostic contains: true
                OffsetTime ot = OffsetTime.MIN;
                // BUG: Diagnostic contains: true
                Period p = Period.ZERO;
                // BUG: Diagnostic contains: true
                ZoneOffset zo = ZoneOffset.UTC;
                // BUG: Diagnostic contains: true
                DateTimeFormatter dtf = DateTimeFormatter.ISO_INSTANT;
                // BUG: Diagnostic contains: true
                TemporalField tf1 = IsoFields.DAY_OF_QUARTER;
                // BUG: Diagnostic contains: true
                TemporalField tf2 = JulianFields.JULIAN_DAY;
                // BUG: Diagnostic contains: true
                WeekFields wf = WeekFields.ISO;
                // BUG: Diagnostic contains: true
                List<?> emptyList = Collections.EMPTY_LIST;
                // BUG: Diagnostic contains: true
                Locale root = Locale.ROOT;
                // BUG: Diagnostic contains: true
                Charset utf8 = StandardCharsets.UTF_8;
                // BUG: Diagnostic contains: false
                PrintStream out = System.out;
                // BUG: Diagnostic contains: false
                Object unannotated = UNANNOTATED_STATIC_FINAL;
              }
            }
            """)
        .doTest();
  }

  /** A test-only bugpattern for testing {@link CompileTimeConstantExpressionMatcher}. */
  @BugPattern(severity = WARNING, summary = "")
  public static final class CompileTimeConstantExpressionMatcherTester extends BugChecker
      implements VariableTreeMatcher {
    @Override
    public Description matchVariable(VariableTree tree, VisitorState state) {
      if (!getSymbol(tree).getKind().equals(ElementKind.LOCAL_VARIABLE)) {
        return Description.NO_MATCH;
      }
      return buildDescription(tree)
          .setMessage(
              CompileTimeConstantExpressionMatcher.instance().matches(tree.getInitializer(), state)
                  ? "true"
                  : "false")
          .build();
    }
  }
}
