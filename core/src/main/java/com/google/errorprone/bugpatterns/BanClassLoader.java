/*
 * Copyright 2023 The Error Prone Authors.
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

import static com.google.errorprone.matchers.Matchers.anyMethod;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.constructor;
import static com.google.errorprone.matchers.Matchers.isExtensionOf;
import static com.google.errorprone.predicates.TypePredicates.allOf;
import static com.google.errorprone.predicates.TypePredicates.isDescendantOf;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;

/** A {@link BugChecker} that detects use of the unsafe JNDI API system. */
@BugPattern(
    summary =
        "Using dangerous ClassLoader APIs may deserialize untrusted user input into bytecode,"
            + " leading to remote code execution vulnerabilities",
    severity = SeverityLevel.ERROR)
public final class BanClassLoader extends AbstractBanUnsafeAPIChecker
    implements MethodInvocationTreeMatcher, NewClassTreeMatcher, ClassTreeMatcher {

  private static final Matcher<MethodInvocationTree> METHOD_MATCHER =
      anyOf(
          anyMethod().onDescendantOf("java.lang.ClassLoader").named("defineClass"),
          anyMethod().onDescendantOf("java.lang.invoke.MethodHandles.Lookup").named("defineClass"),
          anyMethod()
              .onDescendantOf("java.rmi.server.RMIClassLoader")
              .namedAnyOf("loadClass", "loadProxyClass"),
          anyMethod()
              .onDescendantOf("java.rmi.server.RMIClassLoaderSpi")
              .namedAnyOf("loadClass", "loadProxyClass"));

  private static final Matcher<ExpressionTree> CONSTRUCTOR_MATCHER =
      constructor().forClass(allOf(isDescendantOf("java.net.URLClassLoader")));

  private static final Matcher<ClassTree> EXTEND_CLASS_MATCHER =
      isExtensionOf("java.net.URLClassLoader");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    return matchHelper(tree, state, METHOD_MATCHER);
  }

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    return matchHelper(tree, state, CONSTRUCTOR_MATCHER);
  }

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    return matchHelper(tree, state, EXTEND_CLASS_MATCHER);
  }
}
