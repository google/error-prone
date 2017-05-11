/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;

import com.sun.source.tree.MethodInvocationTree;
import java.util.regex.Pattern;

/** Matchers for method invocations related to Object.wait() and Condition.await(); */
public class WaitMatchers {

  private static final String OBJECT_FQN = "java.lang.Object";
  private static final String CONDITION_FQN = "java.util.concurrent.locks.Condition";

  /** Matches any wait/await method. */
  public static final Matcher<MethodInvocationTree> waitMethod =
      anyOf(
          instanceMethod().onExactClass(OBJECT_FQN).named("wait"),
          instanceMethod()
              .onDescendantOf(CONDITION_FQN)
              .withNameMatching(Pattern.compile("await.*")));

  /** Matches wait/await methods that have a timeout. */
  public static final Matcher<MethodInvocationTree> waitMethodWithTimeout =
      anyOf(
          instanceMethod().onExactClass(OBJECT_FQN).withSignature("wait(long)"),
          instanceMethod().onExactClass(OBJECT_FQN).withSignature("wait(long,int)"),
          instanceMethod()
              .onDescendantOf(CONDITION_FQN)
              .withSignature("await(long,java.util.concurrent.TimeUnit)"),
          instanceMethod().onDescendantOf(CONDITION_FQN).named("awaitNanos"),
          instanceMethod().onDescendantOf(CONDITION_FQN).named("awaitUntil"));
}
