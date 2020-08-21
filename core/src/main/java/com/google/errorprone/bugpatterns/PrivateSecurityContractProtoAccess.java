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

import static com.google.errorprone.BugPattern.LinkType.NONE;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.not;
import static com.google.errorprone.matchers.Matchers.packageStartsWith;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.MethodInvocationTree;
import java.util.regex.Pattern;

/** Check for disallowed access to private_do_not_access_or_else proto fields. */
@BugPattern(
    name = "PrivateSecurityContractProtoAccess",
    summary =
        "Access to a private protocol buffer field is forbidden. This protocol buffer carries"
            + " a security contract, and can only be created using an approved library."
            + " Direct access to the fields is forbidden.",

    severity = ERROR,
    linkType = NONE)
public class PrivateSecurityContractProtoAccess extends BugChecker
    implements MethodInvocationTreeMatcher {

  private static final Pattern PRIVATE_DO_NOT_ACCESS_OR_ELSE =
      Pattern.compile(".*PrivateDoNotAccessOrElse.*");

  private static final Matcher<MethodInvocationTree> SAFEHTML_PRIVATE_FIELD_ACCESS =
      allOf(
          anyOf(
              createFieldMatcher("com.google.common.html.types.SafeHtmlProto"),
              createFieldMatcher("com.google.common.html.types.SafeUrlProto"),
              createFieldMatcher("com.google.common.html.types.TrustedResourceUrlProto"),
              createFieldMatcher("com.google.common.html.types.SafeScriptProto"),
              createFieldMatcher("com.google.common.html.types.SafeStyleProto"),
              createFieldMatcher("com.google.common.html.types.SafeStyleSheetProto")),
          not(packageStartsWith("com.google.common.html.types")));

  private static final String MESSAGE = "Forbidden access to a private proto field. See ";
  private static final String SAFEHTML_LINK = "https://github.com/google/safe-html-types/blob/master/doc/safehtml-types.md#protocol-buffer-conversion";

  // Matches instance methods with PrivateDoNotAccessOrElse in their names.
  private static Matcher<MethodInvocationTree> createFieldMatcher(String className) {
    String builderName = className + ".Builder";
    return anyOf(
        instanceMethod().onExactClass(className).withNameMatching(PRIVATE_DO_NOT_ACCESS_OR_ELSE),
        instanceMethod().onExactClass(builderName).withNameMatching(PRIVATE_DO_NOT_ACCESS_OR_ELSE));
  }

  private Description buildErrorMessage(MethodInvocationTree tree, String link) {
    Description.Builder description = buildDescription(tree);
    String message = MESSAGE + link + ".";
    description.setMessage(message);
    return description.build();
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (SAFEHTML_PRIVATE_FIELD_ACCESS.matches(tree, state)) {
      return buildErrorMessage(tree, SAFEHTML_LINK);
    }
    return Description.NO_MATCH;
  }
}
