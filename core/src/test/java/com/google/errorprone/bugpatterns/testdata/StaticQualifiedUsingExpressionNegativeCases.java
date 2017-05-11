/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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

/** @author eaftan@google.com (Eddie Aftandilian) */
public class StaticQualifiedUsingExpressionNegativeCases {

  public static int staticVar1 = 1;

  public static void staticTestMethod() {}

  public void test1() {
    Integer i = Integer.MAX_VALUE;
    i = Integer.valueOf(10);
  }

  public void test2() {
    int i = staticVar1;
    i = StaticQualifiedUsingExpressionNegativeCases.staticVar1;
  }

  public void test3() {
    test1();
    this.test1();
    new StaticQualifiedUsingExpressionNegativeCases().test1();
    staticTestMethod();
  }

  public void test4() {
    Class<?> klass = String[].class;
  }

  @SuppressWarnings("static")
  public void testJavacAltname() {
    this.staticTestMethod();
  }

  @SuppressWarnings("static-access")
  public void testEclipseAltname() {
    this.staticTestMethod();
  }
}
