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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.RequiredModifiers;
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
    name = "RequiredModifiers",
    summary =
        "This annotation is missing required modifiers as specified by its "
            + "@RequiredModifiers annotation",
    linkType = NONE,
    severity = ERROR)
public class RequiredModifiersChecker extends BugChecker implements AnnotationTreeMatcher {

  private static final String MESSAGE_TEMPLATE =
      "%s has specified that it must be used together with the following modifiers: %s";

  @Override
  public Description matchAnnotation(AnnotationTree tree, VisitorState state) {
    RequiredModifiers annotation = ASTHelpers.getAnnotation(tree, RequiredModifiers.class);
    if (annotation == null) {
      return Description.NO_MATCH;
    }
    Set<Modifier> requiredModifiers = ImmutableSet.copyOf(annotation.value());
    if (requiredModifiers.isEmpty()) {
      return Description.NO_MATCH;
    }

    Tree parent = state.getPath().getParentPath().getLeaf();
    if (!(parent instanceof ModifiersTree)) {
      // e.g. An annotated package name
      return Description.NO_MATCH;
    }

    Set<Modifier> missing = Sets.difference(requiredModifiers, ((ModifiersTree) parent).getFlags());

    if (missing.isEmpty()) {
      return Description.NO_MATCH;
    }

    String annotationName = ASTHelpers.getAnnotationName(tree);
    String nameString =
        annotationName != null
            ? String.format("The annotation '@%s'", annotationName)
            : "This annotation";
    String customMessage = String.format(MESSAGE_TEMPLATE, nameString, missing);
    return buildDescription(tree)
        .addFix(
            SuggestedFixes.addModifiers(
                state.getPath().getParentPath().getParentPath().getLeaf(),
                (ModifiersTree) parent,
                state,
                missing))
        .setMessage(customMessage)
        .build();
  }
}
