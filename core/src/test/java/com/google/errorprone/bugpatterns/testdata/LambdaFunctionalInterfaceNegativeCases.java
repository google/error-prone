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

package com.google.errorprone.bugpatterns.testdata;

import java.util.function.Function;
import java.util.function.IntToDoubleFunction;

public class LambdaFunctionalInterfaceNegativeCases {

  public double fooIntToDoubleFunction(int x, Function<Integer, Double> fn) {
    return fn.apply(x).doubleValue();
  }

  public void fooIntToDoubleUtil(int y, IntToDoubleFunction fn) {
    fn.applyAsDouble(y);
  }

  public long fooIntToLongFunction(int x, Function<Integer, Long> fn) {
    return fn.apply(x);
  }

  public long fooIntToIntFunction(int x, Function<Integer, Long> fn) {
    return fn.apply(x);
  }

  public double fooDoubleToDoubleFunction(double x, Function<Double, Double> fn) {
    return fn.apply(x);
  }

  public int fooDoubleToIntFunction(double x, Function<Double, Integer> fn) {
    return fn.apply(x);
  }

  public String add(String string, Function<String, String> func) {
    return func.apply(string);
  }

  public void fooInterface(String str, Function<Integer, Double> func) {}

  public double fooDouble(double x, Function<Double, Integer> fn) {
    return fn.apply(x);
  }

  public static class WithCallSiteExplicitFunction {

    public static double generateDataSeries(Function<Double, Double> curveFunction) {
      final double scale = 100;
      final double modX = 2.0;
      return modX / curveFunction.apply(scale);
    }

    // call site
    private static double generateSpendCurveForMetric(double curved) {
      // explicit Function variable creation
      Function<Double, Double> curveFunction = x -> Math.pow(x, 1 / curved) * 100;
      return generateDataSeries(curveFunction);
    }
  }

  public static class WithCallSiteAnonymousFunction {

    public static double findOptimalMu(Function<Double, Long> costFunc, double mid) {
      return costFunc.apply(mid);
    }

    // call site: anonymous Function
    public Double getMu() {
      return findOptimalMu(
          new Function<Double, Long>() {
            @Override
            public Long apply(Double mu) {
              return 0L;
            }
          },
          3.0);
    }
  }

  public static class WithCallSiteLambdaFunction {

    public static double findOptimalMuLambda(Function<Double, Long> costFunc, double mid) {
      return costFunc.apply(mid);
    }

    // call site: anonymous Function
    public Double getMu() {
      return findOptimalMuLambda(mu -> 0L, 3.0);
    }
  }
}
