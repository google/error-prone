/*
 * Copyright 2021 The Error Prone Authors.
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.findEnclosingNode;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import javax.lang.model.element.Modifier;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    name = "TooManyParameters",
    summary = "A large number of parameters on public APIs should be avoided.",
    severity = WARNING)
public class TooManyParameters extends BugChecker implements MethodTreeMatcher {
  // In 'Detecting Argument Selection Defects' by Rice et. al., the authors argue that methods
  // should have 5 of fewer parameters (see section 7.1):
  // https://static.googleusercontent.com/media/research.google.com/en//pubs/archive/46317.pdf
  // However, we have chosen a very conservative starting number, with hopes to decrease this in the
  // future.
  private static final int DEFAULT_LIMIT = 10;

  static final String TOO_MANY_PARAMETERS_FLAG_NAME = "TooManyParameters:ParameterLimit";

  private static final ImmutableSet<String> ANNOTATIONS_TO_IGNORE =
      ImmutableSet.of(
          "java.lang.Deprecated",
          "java.lang.Override",
          // dependency injection annotations
          "javax.inject.Inject",
          "com.google.inject.Inject",
          "com.google.inject.Provides",
          // dagger provider / producers
          "dagger.Provides",
          "dagger.producers.Produces",
          "com.google.auto.factory.AutoFactory");

  private final int limit;

  public TooManyParameters(ErrorProneFlags flags) {
    this.limit = flags.getInteger(TOO_MANY_PARAMETERS_FLAG_NAME).orElse(DEFAULT_LIMIT);
    checkArgument(limit > 0, "%s (%s) must be > 0", TOO_MANY_PARAMETERS_FLAG_NAME, limit);
  }

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    int paramCount = tree.getParameters().size();
    if (paramCount <= limit) {
      return NO_MATCH;
    }
    if (!shouldApplyApiChecks(tree, state)) {
      return NO_MATCH;
    }

    String message =
        String.format(
            "Consider using a builder pattern instead of a method with %s parameters. Data shows"
                + " that defining methods with > 5 parameters often leads to bugs. See also"
                + " Effective Java, Item 2.",
            paramCount);
    return buildDescription(tree).setMessage(message).build();
  }

  // Copied + modified from GoodTime API checker
  // TODO(kak): we should probably move this somewhere that future API checks can use
  private static boolean shouldApplyApiChecks(Tree tree, VisitorState state) {
    for (String annotation : ANNOTATIONS_TO_IGNORE) {
      if (hasAnnotation(tree, annotation, state)) {
        return false;
      }
    }
    // don't match non-public APIs
    if (!getSymbol(tree).getModifiers().contains(Modifier.PUBLIC)) {
      return false;
    }
    // don't match APIs in non-public classes
    ClassTree clazz = findEnclosingNode(state.getPath(), ClassTree.class);
    if (!getSymbol(clazz).getModifiers().contains(Modifier.PUBLIC)) {
      return false;
    }
    // don't match overrides (even "effective overrides")
    if (isEffectivelyOverride(getSymbol(tree), state.getTypes())) {
      return false;
    }
    return true;
  }

  // Copied from MissingOverride.java
  private static boolean isEffectivelyOverride(Symbol sym, Types types) {
    // static methods can't be overrides
    if (sym.isStatic()) {
      return false;
    }
    ClassSymbol owner = sym.enclClass();
    for (Type s : types.closure(owner.type)) {
      if (s.asElement().equals(owner)) {
        continue;
      }
      for (Symbol m : s.tsym.members().getSymbolsByName(sym.name)) {
        if (!(m instanceof MethodSymbol)) {
          continue;
        }
        MethodSymbol msym = (MethodSymbol) m;
        if (msym.isStatic()) {
          continue;
        }
        if (sym.overrides(msym, owner, types, /* checkResult= */ false)) {
          return true;
        }
      }
    }
    return false;
  }
}
