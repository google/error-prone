/*
 * Copyright 2013 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.refaster;

import static com.google.errorprone.refaster.Unifier.unifications;

import com.google.auto.value.AutoValue;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.tree.JCTree.JCExpression;

/**
 * Identifier representing a static member (field, method, etc.) on a class.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
public abstract class UStaticIdent extends UIdent {
  public static UStaticIdent create(UClassIdent classIdent, CharSequence member, UType memberType) {
    return new AutoValue_UStaticIdent(classIdent, StringName.of(member), memberType);
  }

  public static UStaticIdent create(String qualifiedClass, CharSequence member, UType memberType) {
    return create(UClassIdent.create(qualifiedClass), member, memberType);
  }

  public static UStaticIdent create(ClassSymbol classSym, CharSequence member, UType memberType) {
    return create(UClassIdent.create(classSym), member, memberType);
  }

  abstract UClassIdent classIdent();

  @Override
  public abstract StringName getName();

  abstract UType memberType();

  @Override
  public JCExpression inline(Inliner inliner) throws CouldNotResolveImportException {
    return inliner
        .importPolicy()
        .staticReference(
            inliner, classIdent().getTopLevelClass(), classIdent().getName(), getName());
  }

  @Override
  protected Choice<Unifier> defaultAction(Tree node, Unifier unifier) {
    Symbol symbol = ASTHelpers.getSymbol(node);
    if (symbol != null) {
      return classIdent()
          .unify(symbol.getEnclosingElement(), unifier)
          .thenChoose(unifications(getName(), symbol.getSimpleName()))
          .thenChoose(unifications(memberType(), symbol.asType()));
    }
    return Choice.none();
  }
}
