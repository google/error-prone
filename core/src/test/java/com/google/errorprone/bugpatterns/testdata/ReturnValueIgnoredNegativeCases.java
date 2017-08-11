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

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/** @author alexeagle@google.com (Alex Eagle) */
public class ReturnValueIgnoredNegativeCases {

  private String a = "thing";

  {
    String b = a.trim();
    System.out.println(a.trim());
    new String(new BigInteger(new byte[] {0x01}).add(BigInteger.ONE).toString());
  }

  String run() {
    a.trim().hashCode();
    return a.trim();
  }

  public void methodDoesntMatch() {
    Map<String, Integer> map = new HashMap<String, Integer>();
    map.put("test", 1);
  }

  public void methodDoesntMatch2() {
    final String b = a.toString().trim();
  }

  public void acceptFunctionOfVoid(Function<Integer, Void> arg) {
    arg.apply(5);
  }

  public void passReturnValueCheckedMethodReferenceToFunctionVoid() {
    Function<Integer, Void> fn = (i -> null);
    acceptFunctionOfVoid(fn::apply);
  }
}
