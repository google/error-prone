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
import static com.google.errorprone.util.AnnotationNames.REQUIRED_MODIFIERS_ANNOTATION;
import static com.google.errorprone.util.MoreAnnotations.getAnnotation;
import static com.google.errorprone.util.MoreAnnotations.getEnumValues;

import com.google.common.collect.Sets;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.AnnotationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.lang.model.element.Modifier;

/**
 * @author sgoldfeder@google.com (Steven Goldfeder)
 */
@BugPattern(
    name = "RequiredModifiers",
    summary =
        "This annotation is missing required modifiers as specified by its "
            + "@RequiredModifiers annotation",
    linkType = NONE,
    severity = ERROR)
public class RequiredModifiersChecker extends BugChecker implements AnnotationTreeMatcher {

  @Override
  public Description matchAnnotation(AnnotationTree tree, VisitorState state) {
    Symbol sym = ASTHelpers.getSymbol(tree);
    if (sym == null) {
      return NO_MATCH;
    }
    var anno = getAnnotation(sym, REQUIRED_MODIFIERS_ANNOTATION);
    if (anno.isEmpty()) {
      return NO_MATCH;
    }
    Set<Modifier> requiredModifiers = new LinkedHashSet<>();
    getEnumValues(anno.get(), "value").stream()
        .map(e -> Modifier.valueOf(e.getSimpleName().toString()))
        .forEach(requiredModifiers::add);
    getEnumValues(anno.get(), "modifier").stream()
        .map(e -> Modifier.valueOf(e.getSimpleName().toString()))
        .forEach(requiredModifiers::add);
    if (requiredModifiers.isEmpty()) {
      return NO_MATCH;
    }

    Tree parent = state.getPath().getParentPath().getLeaf();
    if (!(parent instanceof ModifiersTree modifiersTree)) {
      // e.g. An annotated package name
      return NO_MATCH;
    }

    Set<Modifier> missing = Sets.difference(requiredModifiers, modifiersTree.getFlags());

    if (missing.isEmpty()) {
      return NO_MATCH;
    }

    String annotationName = ASTHelpers.getAnnotationName(tree);
    String nameString =
        annotationName != null
            ? String.format("The annotation '@%s'", annotationName)
            : "This annotation";
    String customMessage =
        String.format(
            "%s has specified that it must be used together with the following modifiers: %s",
            nameString, missing);
    return buildDescription(tree)
        .addFix(
            SuggestedFixes.addModifiers(
                    state.getPath().getParentPath().getParentPath().getLeaf(),
                    modifiersTree,
                    state,
                    missing)
                .orElse(SuggestedFix.emptyFix()))
        .setMessage(customMessage)
        .build();
  }
}
