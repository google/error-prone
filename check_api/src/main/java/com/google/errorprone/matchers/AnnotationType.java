/*
 * Copyright 2011 The Error Prone Authors.
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

package com.google.errorprone.matchers;

import static com.google.errorprone.util.ASTHelpers.hasClassName;
import static java.util.Objects.requireNonNull;

import com.google.errorprone.ClassName;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;

/**
 * @author eaftan@google.com (Eddie Aftandilian)
 * @author pepstein@google.com (Peter Epstein)
 */
public class AnnotationType implements Matcher<AnnotationTree> {

  private final ClassName annotationClass;

  /** Matches an {@link AnnotationTree} with the given annotation class name. */
  public AnnotationType(String annotationClassName) {
    this(ClassName.of(annotationClassName));
  }

  /** Matches an {@link AnnotationTree} with the given annotation {@link ClassName}. */
  public AnnotationType(ClassName annotationClass) {
    this.annotationClass = requireNonNull(annotationClass);
  }

  @Override
  public boolean matches(AnnotationTree annotationTree, VisitorState state) {
    Tree type = annotationTree.getAnnotationType();
    Symbol sym =
        switch (type) {
          case JCTree.JCIdent jcIdent -> jcIdent.sym;
          case JCTree.JCFieldAccess jcFieldAccess -> jcFieldAccess.sym;
          default -> null;
        };
    return hasClassName(sym, annotationClass, state);
  }
}
