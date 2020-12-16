/*
 * Copyright 2017 The Error Prone Authors.
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
import com.google.errorprone.BugCheckerRefactoringTestHelper.FixChoosers;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link LambdaFunctionalInterface}Test */
@RunWith(JUnit4.class)
public class LambdaFunctionalInterfaceTest {
  CompilationTestHelper compilationHelper;

  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(new LambdaFunctionalInterface(), getClass());

  @Before
  public void setUp() {
    compilationHelper =
        CompilationTestHelper.newInstance(LambdaFunctionalInterface.class, getClass());
  }

  @Test
  public void testPositiveCase() {
    compilationHelper.addSourceFile("LambdaFunctionalInterfacePositiveCases.java").doTest();
  }

  @Test
  public void testNegativeCase() {
    compilationHelper.addSourceFile("LambdaFunctionalInterfaceNegativeCases.java").doTest();
  }

  @Test
  public void testRefactoringTwo() {
    refactoringHelper
        .addInputLines(
            "in/TwoLambdaFunctions.java",
            "import java.util.function.Function;",
            "   public class TwoLambdaFunctions {",
            "    private static double find(Function<Double, Long> firstSpecial, ",
            "        Function<Integer, Long> secondSpecial, double mid) {",
            "      secondSpecial.apply(2);",
            "      return firstSpecial.apply(mid);",
            "    }",
            "    public Double getMu() {",
            "      return find(mu -> 0L, nu -> 1L, 3.0);",
            "    }",
            "    public Double getTu() {",
            "      return find(mu -> 2L,  nu -> 3L,4.0);",
            "    }",
            "  }")
        .addOutputLines(
            "out/TwoLambdaFunctions.java",
            "import java.util.function.DoubleToLongFunction;",
            "    import java.util.function.Function;",
            "    import java.util.function.IntToLongFunction;",
            "   public class TwoLambdaFunctions {",
            "    private static double find(DoubleToLongFunction firstSpecial, ",
            "  IntToLongFunction secondSpecial, double mid) {",
            "      secondSpecial.applyAsLong(2);",
            "      return firstSpecial.applyAsLong(mid);",
            "    }",
            "    public Double getMu() {",
            "      return find(mu -> 0L, nu -> 1L, 3.0);",
            "    }",
            "    public Double getTu() {",
            "      return find(mu -> 2L,  nu -> 3L,4.0);",
            "    }",
            "  }")
        .setFixChooser(FixChoosers.FIRST)
        .doTest();
  }

  @Test
  public void testRefactoringInteger() {
    refactoringHelper
        .addInputLines(
            "in/TwoLambdaFunctions.java",
            "import java.util.function.Function;",
            "   public class TwoLambdaFunctions {",
            "    private static int find(Function<Integer, Integer> firstSpecial, ",
            "Function<Integer, Double> secondSpecial, int mid) {",
            "      secondSpecial.apply(2);",
            "      return firstSpecial.apply(mid);",
            "    }",
            "    public Integer getMu() {",
            "      return find(mu -> 0, nu -> 1.1, 3);",
            "    }",
            "    public Integer getTu() {",
            "      return find(mu -> 2,  nu -> 3.2, 4);",
            "    }",
            "  }")
        .addOutputLines(
            "out/TwoLambdaFunctions.java",
            " import java.util.function.Function;",
            "    import java.util.function.IntFunction;",
            "    import java.util.function.IntToDoubleFunction;",
            "   public class TwoLambdaFunctions {",
            "    private static int find(IntFunction<Integer> firstSpecial, ",
            "  IntToDoubleFunction secondSpecial, int mid) {",
            "      secondSpecial.applyAsDouble(2);",
            "      return firstSpecial.apply(mid);",
            "    }",
            "    public Integer getMu() {",
            "      return find(mu -> 0, nu -> 1.1, 3);",
            "    }",
            "    public Integer getTu() {",
            "      return find(mu -> 2,  nu -> 3.2, 4);",
            "    }",
            "  }")
        .setFixChooser(FixChoosers.FIRST)
        .doTest();
  }

  @Test
  public void testRefactoringPrimitiveToGeneric() {
    refactoringHelper
        .addInputLines(
            "in/NumbertoT.java",
            "import java.util.function.Function;",
            "     import java.util.ArrayList; ",
            "     import java.util.List; ",
            "  public class NumbertoT { ",
            "    private static <T extends Number> List<T> numToTFunction(Function<Double, T>",
            "  converter) { ",
            "       List<T> namedNumberIntervals = new ArrayList<>(); ",
            "       T min = converter.apply(2.9); ",
            "       T max = converter.apply(5.6); ",
            "       namedNumberIntervals.add(min); ",
            "       namedNumberIntervals.add(max); ",
            "        return namedNumberIntervals; ",
            "     } ",
            "     public List<Integer> getIntList() { ",
            "       List<Integer> result = numToTFunction(num -> 2); ",
            "       return result; ",
            "     } ",
            "     public List<Double> getDoubleList() { ",
            "       List<Double> result = numToTFunction(num -> 3.2); ",
            "       return result; ",
            "     } ",
            "  }")
        .addOutputLines(
            "out/NumbertoT.java",
            "  import java.util.ArrayList; ",
            "  import java.util.List; ",
            "  import java.util.function.DoubleFunction;",
            "  import java.util.function.Function;",
            "  public class NumbertoT { ",
            "    private static <T extends Number> List<T> numToTFunction(DoubleFunction<T> ",
            "  converter) {",
            "       List<T> namedNumberIntervals = new ArrayList<>(); ",
            "       T min = converter.apply(2.9); ",
            "       T max = converter.apply(5.6); ",
            "       namedNumberIntervals.add(min); ",
            "       namedNumberIntervals.add(max); ",
            "        return namedNumberIntervals; ",
            "     } ",
            "     public List<Integer> getIntList() { ",
            "       List<Integer> result = numToTFunction(num -> 2); ",
            "       return result; ",
            "     } ",
            "     public List<Double> getDoubleList() { ",
            "       List<Double> result = numToTFunction(num -> 3.2); ",
            "       return result; ",
            "     } ",
            "  }")
        .setFixChooser(FixChoosers.FIRST)
        .doTest();
  }

  @Test
  public void testRefactoringGenericToPrimitive() {
    refactoringHelper
        .addInputLines(
            "in/NumbertoT.java",
            "import java.util.function.Function;",
            "  public class NumbertoT { ",
            "   private <T> int sumAll(Function<T, Integer> sizeConv) {",
            "      return sizeConv.apply((T) Integer.valueOf(3));",
            "    }",
            "    public int getSumAll() {",
            "      return sumAll(o -> 2);",
            "    } ",
            "  }")
        .addOutputLines(
            "out/NumbertoT.java",
            "  import java.util.function.Function;",
            "  import java.util.function.ToIntFunction;",
            "  public class NumbertoT { ",
            "   private <T> int sumAll(ToIntFunction<T> sizeConv) {",
            "      return sizeConv.applyAsInt((T) Integer.valueOf(3));",
            "    }",
            "    public int getSumAll() {",
            "      return sumAll(o -> 2);",
            "    } ",
            "  }")
        .setFixChooser(FixChoosers.FIRST)
        .doTest();
  }
}
