package com.google.errorprone.bugpatterns;
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

/**
 * Negative cases for {@link UnneededConditionalOperator}.
 *
 * @author sjnickerson@google.com (Simon Nickerson)
 */
public class UnneededConditionalOperatorNegativeCases {

  public static void negativeCase1() {
    boolean t = (4 > 5) ? true : isFoo();
  }

  public static void negativeCase2() {
    boolean t = (4 > 5) ? isFoo() : true;
  }

  public static void negativeCase3() {
    String t = isFoo() ? "true" : "false";
  }

  private static boolean isFoo() {
    return true;
  }
}
