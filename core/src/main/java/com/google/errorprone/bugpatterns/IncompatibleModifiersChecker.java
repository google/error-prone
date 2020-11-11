/*
 * Copyright 2013 The Error Prone Authors.
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
import static com.google.errorprone.matchers.Description.NO_MATCH;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.IncompatibleModifiers;
import com.google.errorprone.bugpatterns.BugChecker.AnnotationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import java.util.Set;
import javax.lang.model.element.Modifier;

/** @author sgoldfeder@google.com (Steven Goldfeder) */
@BugPattern(
    name = "IncompatibleModifiers",
    summary =
        "This annotation has incompatible modifiers as specified by its "
            + "@IncompatibleModifiers annotation",
    linkType = NONE,
    severity = ERROR)
public class IncompatibleModifiersChecker extends BugChecker implements AnnotationTreeMatcher {

  @Override
  public Description matchAnnotation(AnnotationTree tree, VisitorState state) {
    IncompatibleModifiers annotation = ASTHelpers.getAnnotation(tree, IncompatibleModifiers.class);
    if (annotation == null) {
      return NO_MATCH;
    }
    ImmutableSet<Modifier> incompatibleModifiers = ImmutableSet.copyOf(annotation.value());
    if (incompatibleModifiers.isEmpty()) {
      return Description.NO_MATCH;
    }

    Tree parent = state.getPath().getParentPath().getLeaf();
    if (!(parent instanceof ModifiersTree)) {
      // e.g. An annotated package name
      return NO_MATCH;
    }

    Set<Modifier> incompatible =
        Sets.intersection(incompatibleModifiers, ((ModifiersTree) parent).getFlags());

    if (incompatible.isEmpty()) {
      return NO_MATCH;
    }

    String annotationName = ASTHelpers.getAnnotationName(tree);
    String nameString =
        annotationName != null
            ? String.format("The annotation '@%s'", annotationName)
            : "This annotation";
    String message =
        String.format(
            "%s has specified that it should not be used together with the following modifiers: %s",
            nameString, incompatible);
    return buildDescription(tree)
        .addFix(SuggestedFixes.removeModifiers((ModifiersTree) parent, state, incompatible))
        .setMessage(message)
        .build();
  }
}
