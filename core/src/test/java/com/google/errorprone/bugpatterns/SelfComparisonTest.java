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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Unit tests for {@link SelfComparison} bug pattern.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@RunWith(JUnit4.class)
public class SelfComparisonTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(SelfComparison.class, getClass());

  @Test
  public void positiveCase() {
    compilationHelper
        .addSourceLines(
            "SelfComparisonPositiveCase.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            /**
             * Positive test case for {@link SelfComparison} check.
             *
             * @author bhagwani@google.com (Sumit Bhagwani)
             */
            public class SelfComparisonPositiveCase implements Comparable<Object> {

              public int test1() {
                SelfComparisonPositiveCase obj = new SelfComparisonPositiveCase();
                // BUG: Diagnostic contains: An object is compared to itself
                return obj.compareTo(obj);
              }

              private SelfComparisonPositiveCase obj = new SelfComparisonPositiveCase();

              public int test2() {
                // BUG: Diagnostic contains: An object is compared to itself
                return obj.compareTo(this.obj);
              }

              public int test3() {
                // BUG: Diagnostic contains: An object is compared to itself
                return this.obj.compareTo(obj);
              }

              public int test4() {
                // BUG: Diagnostic contains: An object is compared to itself
                return this.obj.compareTo(this.obj);
              }

              public int test5() {
                // BUG: Diagnostic contains: An object is compared to itself
                return compareTo(this);
              }

              @Override
              public int compareTo(Object o) {
                return 0;
              }

              public static class ComparisonTest implements Comparable<ComparisonTest> {
                private String testField;

                @Override
                public int compareTo(ComparisonTest s) {
                  return testField.compareTo(s.testField);
                }

                public int test1() {
                  ComparisonTest obj = new ComparisonTest();
                  // BUG: Diagnostic contains: An object is compared to itself
                  return obj.compareTo(obj);
                }

                private ComparisonTest obj = new ComparisonTest();

                public int test2() {
                  // BUG: Diagnostic contains: An object is compared to itself
                  return obj.compareTo(this.obj);
                }

                public int test3() {
                  // BUG: Diagnostic contains: An object is compared to itself
                  return this.obj.compareTo(obj);
                }

                public int test4() {
                  // BUG: Diagnostic contains: An object is compared to itself
                  return this.obj.compareTo(this.obj);
                }

                public int test5() {
                  // BUG: Diagnostic contains: An object is compared to itself
                  return compareTo(this);
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negativeCase() {
    compilationHelper
        .addSourceLines(
            "SelfComparisonNegativeCases.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            /**
             * Negative test cases for {@link SelfComparison} check.
             *
             * @author bhagwani@google.com (Sumit Bhagwani)
             */
            public class SelfComparisonNegativeCases implements Comparable<Object> {
              private String field;

              @Override
              public int hashCode() {
                return field != null ? field.hashCode() : 0;
              }

              @Override
              public int compareTo(Object o) {
                if (!(o instanceof SelfComparisonNegativeCases)) {
                  return -1;
                }

                SelfComparisonNegativeCases other = (SelfComparisonNegativeCases) o;
                return field.compareTo(other.field);
              }

              public int test() {
                return Boolean.TRUE.toString().compareTo(Boolean.FALSE.toString());
              }

              public static class CopmarisonTest implements Comparable<CopmarisonTest> {
                private String testField;

                @Override
                public int compareTo(CopmarisonTest obj) {
                  return testField.compareTo(obj.testField);
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void localDatePositiveCase() {
    compilationHelper
        .addSourceLines(
            "LocalDateSelfComparisonPositiveCase.java",
            """
            package com.google.errorprone.bugpatterns.testdata;
            
            import java.time.LocalDate;

            public class LocalDateSelfComparisonPositiveCase {

              public boolean test1() {
                LocalDate date = LocalDate.of(2025, 11, 10);
                // BUG: Diagnostic contains: An object is compared to itself
                return date.isAfter(date);
              }

              private LocalDate date = LocalDate.of(2025, 11, 10);

              public boolean test2() {
                // BUG: Diagnostic contains: An object is compared to itself
                return date.isAfter(this.date);
              }

              public boolean test3() {
                // BUG: Diagnostic contains: An object is compared to itself
                return this.date.isBefore(date);
              }

              public boolean test4() {
                // BUG: Diagnostic contains: An object is compared to itself
                return this.date.isBefore(this.date);
              }
            }\
            """)
        .doTest();
  }

  @Test
  public void timeTypesPositiveCase() {
    compilationHelper
        .addSourceLines(
            "TimeTypesSelfComparisonPositiveCase.java",
            """
            package com.google.errorprone.bugpatterns.testdata;
            
            import java.time.Instant;
            import java.time.LocalDate;
            import java.time.LocalDateTime;
            import java.time.LocalDate;
            import java.util.Date;

            public class TimeTypesSelfComparisonPositiveCase {

              public boolean testInstantAfter(Instant x) {
                // BUG: Diagnostic contains: An object is compared to itself
                return x.isAfter(x);
              }

              public boolean testInstantBefore(Instant x) {
                // BUG: Diagnostic contains: An object is compared to itself
                return x.isBefore(x);
              }

              public boolean testLocalDateAfter(LocalDate x) {
                // BUG: Diagnostic contains: An object is compared to itself
                return x.isAfter(x);
              }

              public boolean testLocalDateBefore(LocalDate x) {
                // BUG: Diagnostic contains: An object is compared to itself
                return x.isBefore(x);
              }

              public boolean testLocalDateTimeAfter(LocalDateTime x) {
                // BUG: Diagnostic contains: An object is compared to itself
                return x.isAfter(x);
              }

              public boolean testLocalDateTimeBefore(LocalDateTime x) {
                // BUG: Diagnostic contains: An object is compared to itself
                return x.isBefore(x);
              }

              public boolean testDateAfter(Date x) {
                // BUG: Diagnostic contains: An object is compared to itself
                return x.after(x);
              }

              public boolean testDateBefore(Date x) {
                // BUG: Diagnostic contains: An object is compared to itself
                return x.before(x);
              }
            }\
            """)
        .doTest();
  }
}
