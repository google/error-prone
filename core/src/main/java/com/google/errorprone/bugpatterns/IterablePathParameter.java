/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.isSameType;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.WildcardType;
import com.sun.tools.javac.code.TypeTag;
import java.nio.file.Path;
import javax.lang.model.element.ElementKind;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "IterablePathParameter",
  category = JDK,
  summary = "Path implements Iterable<Path>; prefer Collection<Path> for clarity",
  severity = ERROR,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class IterablePathParameter extends BugChecker implements VariableTreeMatcher {
  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    Type type = ASTHelpers.getType(tree);
    VarSymbol symbol = ASTHelpers.getSymbol(tree);
    if (type == null || symbol == null) {
      return NO_MATCH;
    }
    if (symbol.getKind() != ElementKind.PARAMETER) {
      return NO_MATCH;
    }
    if (!isSameType(type, state.getSymtab().iterableType, state)) {
      return NO_MATCH;
    }
    if (type.getTypeArguments().isEmpty()) {
      return NO_MATCH;
    }
    if (!isSameType(
        wildBound(getOnlyElement(type.getTypeArguments())),
        state.getTypeFromString(Path.class.getName()),
        state)) {
      return NO_MATCH;
    }
    Description.Builder description = buildDescription(tree);
    if (tree.getType() instanceof ParameterizedTypeTree) {
      description.addFix(
          SuggestedFix.builder()
              .addImport("java.util.Collection")
              .replace(((ParameterizedTypeTree) tree.getType()).getType(), "Collection")
              .build());
    }
    return description.build();
  }

  static Type wildBound(Type type) {
    return type.hasTag(TypeTag.WILDCARD) ? ((WildcardType) type).type : type;
  }
}
