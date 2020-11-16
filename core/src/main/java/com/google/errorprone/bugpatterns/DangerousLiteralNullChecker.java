/*
 * Copyright 2020 The Error Prone Authors.
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

import static com.google.errorprone.matchers.Description.NO_MATCH;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableTable;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.LiteralTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.util.Name;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    name = "DangerousLiteralNull",
    summary = "This method is null-hostile: passing a null literal to it is always wrong",
    severity = SeverityLevel.ERROR)
public class DangerousLiteralNullChecker extends BugChecker implements LiteralTreeMatcher {

  @AutoValue
  abstract static class NullReplacementRule {
    abstract Name klass();

    abstract Name method();

    abstract String replacementBody();

    private static Builder builder() {
      return new AutoValue_DangerousLiteralNullChecker_NullReplacementRule.Builder();
    }

    @AutoValue.Builder
    abstract static class Builder {
      abstract Builder setKlass(Name klass);

      abstract Builder setMethod(Name method);

      abstract Builder setReplacementBody(String body);

      abstract NullReplacementRule build();
    }
  }

  private static final Supplier<ImmutableList<NullReplacementRule>> RULES =
      VisitorState.memoize(
          state ->
              ImmutableList.of(
                  NullReplacementRule.builder()
                      .setKlass(state.getName("java.util.Optional"))
                      .setMethod(state.getName("orElseGet"))
                      .setReplacementBody("orElse(null)")
                      .build(),
                  NullReplacementRule.builder()
                      .setKlass(state.getName("java.util.Optional"))
                      .setMethod(state.getName("orElseThrow"))
                      .setReplacementBody("orElseThrow(NullPointerException::new)")
                      .build()));

  private static final Supplier<ImmutableTable<Name, Name, String>> RULE_LOOKUP =
      VisitorState.memoize(
          state -> {
            ImmutableTable.Builder<Name, Name, String> builder = ImmutableTable.builder();
            for (NullReplacementRule rule : RULES.get(state)) {
              builder.put(rule.klass(), rule.method(), rule.replacementBody());
            }
            return builder.build();
          });

  @Override
  public Description matchLiteral(LiteralTree tree, VisitorState state) {
    if (!Matchers.nullLiteral().matches(tree, state)) {
      return NO_MATCH;
    }
    Tree parent = state.getPath().getParentPath().getLeaf();
    if (!(parent instanceof MethodInvocationTree)) {
      return NO_MATCH;
    }
    MethodInvocationTree invocation = (MethodInvocationTree) parent;
    if (invocation.getArguments().size() != 1) {
      return NO_MATCH;
    }
    MethodSymbol sym = ASTHelpers.getSymbol(invocation);
    if (sym == null) {
      return NO_MATCH;
    }
    String newBody = RULE_LOOKUP.get(state).get(sym.owner.getQualifiedName(), sym.name);
    if (newBody == null) {
      return NO_MATCH;
    }
    return describeMatch(
        invocation,
        SuggestedFix.replace(
            invocation,
            String.format(
                "%s.%s", state.getSourceForNode(ASTHelpers.getReceiver(invocation)), newBody)));
  }
}
