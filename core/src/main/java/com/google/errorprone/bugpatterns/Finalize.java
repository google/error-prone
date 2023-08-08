/*
 * Copyright 2022 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isVoidType;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.MethodTree;
import java.util.Set;
import javax.lang.model.element.Modifier;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(summary = "Do not override finalize", severity = WARNING, documentSuppression = false)
public class Finalize extends BugChecker implements MethodTreeMatcher {

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    if (!tree.getName().contentEquals("finalize")) {
      return NO_MATCH;
    }
    if (!tree.getParameters().isEmpty()) {
      return NO_MATCH;
    }
    if (!isVoidType(getType(tree.getReturnType()), state)) {
      return NO_MATCH;
    }
    Set<Modifier> modifiers = getSymbol(tree).getModifiers();
    if (!modifiers.contains(Modifier.PROTECTED) && !modifiers.contains(Modifier.PUBLIC)) {
      return NO_MATCH;
    }
    return describeMatch(tree);
  }
}
