/*
 * Copyright 2018 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.nullness;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MemberSelectTreeMatcher;
import com.google.errorprone.dataflow.nullnesspropagation.Nullness;
import com.google.errorprone.dataflow.nullnesspropagation.TrustingNullnessAnalysis;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import javax.lang.model.type.TypeKind;

/**
 * Warns when a dereference has a possibly-null receiver.
 *
 * <p>Nullability information is drawn from the trusting nullness analysis, which assumes that
 * fields and method returns are non-null unless otherwise annotated or inferred.
 *
 * @author bennostein@google.com (Benno Stein)
 */
@BugPattern(
    name = "NullableDereference",
    summary = "Dereference of possibly-null value",
    category = JDK,
    severity = WARNING,
    providesFix = ProvidesFix.NO_FIX)
public class NullableDereference extends BugChecker implements MemberSelectTreeMatcher {

  @Override
  public Description matchMemberSelect(MemberSelectTree tree, VisitorState state) {
    JCExpression receiverTree = (JCExpression) tree.getExpression();

    if (receiverTree == null
        || receiverTree.type == null
        || receiverTree.type.getKind() == TypeKind.PACKAGE) {
      return Description.NO_MATCH;
    }

    if (((tree instanceof JCIdent) && ((JCIdent) tree).sym.isStatic())
        || ((tree instanceof JCFieldAccess) && ((JCFieldAccess) tree).sym.isStatic())) {
      return Description.NO_MATCH;
    }

    Nullness nullness =
        TrustingNullnessAnalysis.instance(state.context)
            .getNullness(new TreePath(state.getPath(), receiverTree), state.context);

    Description.Builder descBuilder = buildDescription(tree);
    switch (nullness) {
      case NONNULL:
      case BOTTOM:
        return Description.NO_MATCH;
      case NULL:
        descBuilder.setMessage(
            String.format(
                "Dereferencing method/field \"%s\" of definitely null receiver %s",
                tree.getIdentifier(), receiverTree));
        break;
      case NULLABLE:
        descBuilder.setMessage(
            String.format(
                "Dereferencing method/field \"%s\" of possibly null receiver %s",
                tree.getIdentifier(), receiverTree));
    }
    return descBuilder.build();
  }
}
