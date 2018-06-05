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

package com.google.errorprone.bugpatterns.apidiff;

import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.IdentifierTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MemberSelectTreeMatcher;
import com.google.errorprone.bugpatterns.apidiff.ApiDiff.ClassMemberKey;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.Signatures;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import java.lang.annotation.Annotation;
import java.util.Optional;

/** A base Error Prone check implementation to enforce compliance with a given API diff. */
public abstract class ApiDiffChecker extends BugChecker
    implements IdentifierTreeMatcher, MemberSelectTreeMatcher {

  private final ApiDiff apiDiff;
  private final Optional<Class<? extends Annotation>> alsoForbidApisAnnotated;

  protected ApiDiffChecker(ApiDiff apiDiff) {
    this.apiDiff = apiDiff;
    this.alsoForbidApisAnnotated = Optional.empty();
  }

  protected ApiDiffChecker(ApiDiff apiDiff, Class<? extends Annotation> alsoForbidApisAnnotated) {
    this.apiDiff = apiDiff;
    this.alsoForbidApisAnnotated = Optional.of(alsoForbidApisAnnotated);
  }

  @Override
  public Description matchIdentifier(IdentifierTree tree, VisitorState state) {
    return check(tree, state);
  }

  @Override
  public Description matchMemberSelect(MemberSelectTree tree, VisitorState state) {
    return check(tree, state);
  }

  private Description check(ExpressionTree tree, VisitorState state) {
    if (state.findEnclosing(ImportTree.class) != null) {
      return Description.NO_MATCH;
    }
    Symbol sym = getSymbol(tree);
    if (sym == null) {
      return Description.NO_MATCH;
    }
    ClassSymbol receiver = getReceiver(tree, sym);
    if (receiver == null) {
      // e.g. package symbols
      return Description.NO_MATCH;
    }
    Types types = state.getTypes();
    // check for information associated with the class
    if (apiDiff.isClassUnsupported(Signatures.classDescriptor(receiver.type, types))
        || classOrEnclosingClassIsForbiddenByAnnotation(receiver, state)) {
      return buildDescription(tree)
          .setMessage(String.format("%s is not available", receiver))
          .build();
    }
    // check for fields and methods that are not present in the old API
    if (!(sym instanceof VarSymbol || sym instanceof MethodSymbol)) {
      return Description.NO_MATCH;
    }
    ClassMemberKey memberKey =
        ClassMemberKey.create(
            sym.getSimpleName().toString(), Signatures.descriptor(sym.type, types));
    ClassSymbol owner = sym.owner.enclClass();
    if (apiDiff.isMemberUnsupported(Signatures.classDescriptor(owner.type, types), memberKey)
        || hasAnnotationForbiddingUse(sym, state)) {
      return buildDescription(tree)
          .setMessage(String.format("%s#%s is not available in %s", owner, sym, receiver))
          .build();
    }
    return Description.NO_MATCH;
  }

  private boolean classOrEnclosingClassIsForbiddenByAnnotation(Symbol clazz, VisitorState state) {
    if (!alsoForbidApisAnnotated.isPresent()) {
      return false;
    }
    for (; clazz instanceof ClassSymbol; clazz = clazz.owner) {
      if (hasAnnotationForbiddingUse(clazz, state)) {
        return true;
      }
    }
    return false;
  }

  private boolean hasAnnotationForbiddingUse(Symbol sym, VisitorState state) {
    return alsoForbidApisAnnotated.isPresent()
        && ASTHelpers.hasAnnotation(sym, alsoForbidApisAnnotated.get(), state);
  }

  /**
   * Finds the class of the expression's receiver: the declaring class of a static member access, or
   * the type that an instance member is accessed on.
   */
  private ClassSymbol getReceiver(ExpressionTree tree, Symbol sym) {
    if (sym.isStatic() || sym instanceof ClassSymbol) {
      return sym.enclClass();
    }
    switch (tree.getKind()) {
      case MEMBER_SELECT:
      case METHOD_INVOCATION:
        Type receiver = ASTHelpers.getType(ASTHelpers.getReceiver(tree));
        if (receiver == null) {
          return null;
        }
        return receiver.tsym.enclClass();
      case IDENTIFIER:
        // Simple names are implicitly qualified by an enclosing instance, so if we get here
        // we're inside the compilation unit that declares the receiver, and the diff doesn't
        // contain accurate information.
        return null;
      default:
        return null;
    }
  }
}
