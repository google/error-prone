/*
 * Copyright 2015 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.method.MethodMatchers.constructor;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;

/** @author avenet@google.com (Arnaud J. Venet) */
@BugPattern(
    name = "InsecureCryptoUsage",
    altNames = {"InsecureCipherMode"},
    summary =
        "Usage of the Java Cryptography API is insecure because it is prone to vulnerabilities.",
    documentSuppression = false,
    severity = ERROR)
public class InsecureCipherMode extends BugChecker
    implements MethodInvocationTreeMatcher, NewClassTreeMatcher {
  private static final Matcher<ExpressionTree> BLOCKED_CRYPTO_EXPRESSIONS =
      anyOf(
          staticMethod().onClass("javax.crypto.Cipher").named("getInstance"),
          staticMethod().onClass("javax.crypto.KeyAgreement").named("getInstance"),
          staticMethod().onClass("javax.crypto.Mac").named("getInstance"),
          staticMethod().onClass("java.security.KeyPairGenerator").named("getInstance"),
          staticMethod().onClass("java.security.MessageDigest").named("getInstance"),
          staticMethod().onClass("java.security.Signature").named("getInstance"));

  private static final Matcher<NewClassTree> BLOCKED_CRYPTO_CLASSES =
      anyOf(
          constructor().forClass("javax.crypto.CipherInputStream"),
          constructor().forClass("javax.crypto.CipherOutputStream"));


  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    ImmutableList<Description> descriptionList = checkMethodInvocation(tree, state);


    for (Description description : descriptionList) {
      state.reportMatch(description);
    }
    return Description.NO_MATCH;
  }

  ImmutableList<Description> checkMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    ImmutableList.Builder<Description> descriptionList = ImmutableList.builder();
    if (BLOCKED_CRYPTO_EXPRESSIONS.matches(tree, state)) {
      String message = "Use of these APIs is considered insecure";
      descriptionList.add(buildDescription(tree).setMessage(message).build());
    }

    return descriptionList.build();
  }

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    Description description = checkClassInvocation(tree, state);


    return description;
  }

  Description checkClassInvocation(NewClassTree tree, VisitorState state) {
    if (BLOCKED_CRYPTO_CLASSES.matches(tree, state)) {
      String message = "Use of these APIs is considered insecure";
      return buildDescription(tree).setMessage(message).build();
    }

    return Description.NO_MATCH;
  }
}
