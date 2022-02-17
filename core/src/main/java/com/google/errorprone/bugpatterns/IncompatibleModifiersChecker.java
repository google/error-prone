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
import static com.google.errorprone.util.MoreAnnotations.getValue;

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
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.SimpleAnnotationValueVisitor8;

/**
 * @author sgoldfeder@google.com (Steven Goldfeder)
 */
@BugPattern(
    name = "IncompatibleModifiers",
    summary =
        "This annotation has incompatible modifiers as specified by its "
            + "@IncompatibleModifiers annotation",
    linkType = NONE,
    severity = ERROR)
public class IncompatibleModifiersChecker extends BugChecker implements AnnotationTreeMatcher {

  private static final String INCOMPATIBLE_MODIFIERS =
      "com.google.errorprone.annotations.IncompatibleModifiers";

  @Override
  public Description matchAnnotation(AnnotationTree tree, VisitorState state) {
    Symbol sym = ASTHelpers.getSymbol(tree);
    if (sym == null) {
      return NO_MATCH;
    }
    Attribute.Compound annotation =
        sym.getRawAttributes().stream()
            .filter(a -> a.type.tsym.getQualifiedName().contentEquals(INCOMPATIBLE_MODIFIERS))
            .findAny()
            .orElse(null);
    if (annotation == null) {
      return NO_MATCH;
    }
    Set<Modifier> incompatibleModifiers = new LinkedHashSet<>();
    getValue(annotation, "value").ifPresent(a -> getModifiers(incompatibleModifiers, a));
    getValue(annotation, "modifier").ifPresent(a -> getModifiers(incompatibleModifiers, a));
    if (incompatibleModifiers.isEmpty()) {
      return NO_MATCH;
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
        .addFix(
            SuggestedFixes.removeModifiers((ModifiersTree) parent, state, incompatible)
                .orElse(SuggestedFix.emptyFix()))
        .setMessage(message)
        .build();
  }

  private static void getModifiers(Collection<Modifier> modifiers, Attribute attribute) {
    class Visitor extends SimpleAnnotationValueVisitor8<Void, Void> {
      @Override
      public Void visitEnumConstant(VariableElement c, Void unused) {
        modifiers.add(Modifier.valueOf(c.getSimpleName().toString()));
        return null;
      }

      @Override
      public Void visitArray(List<? extends AnnotationValue> vals, Void unused) {
        vals.forEach(val -> val.accept(this, null));
        return null;
      }
    }
    attribute.accept(new Visitor(), null);
  }
}
