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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.MaturityLevel.MATURE;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.tree.JCTree;

/** @author avenet@google.com (Arnaud J. Venet) */
@BugPattern(
  name = "InsecureCipherMode",
  summary = "Cipher.getInstance() is invoked using either the default settings or ECB mode",
  category = JDK,
  severity = ERROR,
  documentSuppression = false,
  maturity = MATURE
)
public class InsecureCipherMode extends BugChecker implements MethodInvocationTreeMatcher {
  private static final String MESSAGE_BASE = "Insecure usage of Cipher.getInstance(): ";

  private static final Matcher<ExpressionTree> GETINSTANCE_MATCHER =
      staticMethod().onClass("javax.crypto.Cipher").named("getInstance");


  private Description buildErrorMessage(MethodInvocationTree tree, String explanation) {
    Description.Builder description = buildDescription(tree);
    description.setMessage(MESSAGE_BASE + explanation + ".");
    return description.build();
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!GETINSTANCE_MATCHER.matches(tree, state)) {
      return Description.NO_MATCH;
    }


    // We analyze the first argument of all the overloads of Cipher.getInstance().
    Object argument = ASTHelpers.constValue((JCTree) tree.getArguments().get(0));
    if (argument == null) {
      // We flag call sites where the transformation string is dynamically computed.
      return buildErrorMessage(
          tree, "the transformation is not a compile-time constant expression");
    }
    // Otherwise, we know that the transformation is specified by a string literal.
    String transformation = (String) argument;

    // We exclude stream ciphers (this check only makes sense for block ciphers), i.e., the RC4
    // cipher. The name of this algorithm is "ARCFOUR" in the SunJce and "ARC4" in Conscrypt.
    // Some other providers like JCraft also seem to use the name "RC4".
    if (transformation.matches("ARCFOUR.*")
        || transformation.matches("ARC4.*")
        || transformation.matches("RC4.*")) {
      return Description.NO_MATCH;
    }

    if (!transformation.matches(".*/.*/.*")) {
      // The mode and padding shall be explicitly specified. We don't allow default settings to be
      // used, regardless of the algorithm and provider.
      return buildErrorMessage(tree, "the mode and padding must be explicitly specified");
    }

    if (transformation.matches(".*/ECB/.*")
        && !transformation.matches("RSA/.*")
        && !transformation.matches("AESWrap/.*")) {
      // Otherwise, ECB mode should be explicitly specified in order to trigger the check. RSA
      // is an exception, as this transformation doesn't actually implement a block cipher
      // encryption mode (the input is limited by the size of the key). AESWrap is another
      // exception, because this algorithm is only used to encrypt encryption keys.
      return buildErrorMessage(tree, "ECB mode must not be used");
    }

    if (transformation.matches("ECIES.*") || transformation.matches("DHIES.*")) {
      // Existing implementations of IES-based algorithms use ECB under the hood and must also be
      // flagged as vulnerable. See b/30424901 for a more detailed rationale.
      return buildErrorMessage(tree, "IES-based algorithms use ECB mode and are insecure");
    }

    return Description.NO_MATCH;
  }
}
