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

package com.google.errorprone.bugpatterns.android;

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MemberSelectTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.MemberSelectTree;
import com.sun.tools.javac.code.Symbol;
import javax.lang.model.element.ElementKind;

/**
 * Replacement of misleading <a
 * href="http://developer.android.com/reference/android/R.string.html">android.R.string</a>
 * constants with more intuitive ones.
 *
 * @author kmb@google.com (Kevin Bierhoff)
 */
@BugPattern(
    name = "MislabeledAndroidString",
    summary = "Certain resources in `android.R.string` have names that do not match their content",
    severity = ERROR)
public class MislabeledAndroidString extends BugChecker implements MemberSelectTreeMatcher {

  private static final String R_STRING_CLASSNAME = "android.R.string";
  /** Maps problematic resources defined in {@value #R_STRING_CLASSNAME} to their replacements. */
  @VisibleForTesting
  static final ImmutableMap<String, String> MISLEADING =
      ImmutableMap.of(
          "yes", "ok",
          "no", "cancel");
  /** Maps all resources appearing in {@link #MISLEADING} to an assumed meaning. */
  @VisibleForTesting
  static final ImmutableMap<String, String> ASSUMED_MEANINGS =
      ImmutableMap.of(
          "yes", "Yes", // assumed but not actual meaning
          "no", "No", // assumed but not actual meaning
          "ok", "OK", // assumed and actual meaning
          "cancel", "Cancel"); // assumed and actual meaning

  @Override
  public Description matchMemberSelect(MemberSelectTree tree, VisitorState state) {
    if (!state.isAndroidCompatible()) {
      return Description.NO_MATCH;
    }
    Symbol symbol = ASTHelpers.getSymbol(tree);
    // Match symbol's owner to android.R.string separately because couldn't get fully qualified
    // "android.R.string.yes" out of symbol, just "yes"
    if (symbol == null
        || symbol.owner == null
        || symbol.getKind() != ElementKind.FIELD
        || !symbol.isStatic()
        || !R_STRING_CLASSNAME.contentEquals(symbol.owner.getQualifiedName())) {
      return Description.NO_MATCH;
    }
    String misleading = symbol.getSimpleName().toString();
    String preferred = MISLEADING.get(misleading);
    if (preferred == null) {
      return Description.NO_MATCH;
    }
    return buildDescription(tree)
        .setMessage(
            String.format(
                "%s.%s is not \"%s\" but \"%s\"; prefer %s.%s for clarity",
                R_STRING_CLASSNAME,
                misleading,
                ASSUMED_MEANINGS.get(misleading),
                ASSUMED_MEANINGS.get(preferred),
                R_STRING_CLASSNAME,
                preferred))
        // Keep the way tree refers to android.R.string as it is but replace the identifier
        .addFix(
            SuggestedFix.replace(
                tree, state.getSourceForNode(tree.getExpression()) + "." + preferred))
        .build();
  }
}
