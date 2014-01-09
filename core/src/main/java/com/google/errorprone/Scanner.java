/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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

package com.google.errorprone;

import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Suppressible;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Pair;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * TODO(eaftan): I'm worried about this performance of this code,
 * specifically the part that handles SuppressWarnings.  We should
 * profile it and see where the hotspots are.
 *
 * @author alexeagle@google.com (Alex Eagle)
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class Scanner extends TreePathScanner<Void, VisitorState> {

  private Set<String> suppressions = new HashSet<String>();
  private Set<Class<? extends Annotation>> customSuppressions =
      new HashSet<Class<? extends Annotation>>();

  /**
   * Scan a tree from a position identified by a TreePath.
   */
  @Override
  public Void scan(TreePath path, VisitorState state) {
    // Record previous suppression info so we can restore it when going up the tree.
    Set<String> prevSuppressions = suppressions;
    Set<Class<? extends Annotation>> prevCustomSuppressions = customSuppressions;

    Symbol sym = ASTHelpers.getSymbol(path.getLeaf());
    if (sym != null) {
      handleSuppressions(sym, state.getSymtab().suppressWarningsType);
    }

    try {
      return super.scan(path, state);
    } finally {
      // Restore old suppression state.
      suppressions = prevSuppressions;
      customSuppressions = prevCustomSuppressions;
    }
  }

  /**
   * Scan a single node.
   * The current path is updated for the duration of the scan.
   */
  @Override
  public Void scan(Tree tree, VisitorState state) {
    if (tree == null) {
      return null;
    }

    // Record previous suppression info so we can restore it when going up the tree.
    Set<String> prevSuppressions = suppressions;
    Set<Class<? extends Annotation>> prevCustomSuppressions = customSuppressions;

    Symbol sym = ASTHelpers.getSymbol(tree);
    if (sym != null) {
      handleSuppressions(sym, state.getSymtab().suppressWarningsType);
    }

    try {
      return super.scan(tree, state);
    } finally {
      // Restore old suppression state.
      suppressions = prevSuppressions;
      customSuppressions = prevCustomSuppressions;
    }
  }

  /**
   * Do all work necessary to support suppressions, both via {@code @SuppressWarnings} and via
   * custom suppressions. To do this we have to maintains 2 sets of suppression info:
   * 1) A set of the suppression types in all {@code @Suppresswarnings} annotations down this path
   *    of the AST.
   * 2) A set of all custom suppression types down this path of the AST.
   *
   * When we explore a new node, we have to extend the suppression sets with any new
   * suppressed warnings or custom suppression annotations.  We also have to retain the previous
   * suppression set so that we can reinstate it when we move up the tree.
   *
   * We do not modify the existing suppression sets, so they can be restored when moving up the
   * tree.  We also avoid copying the suppression sets if the next node to explore does not have
   * any suppressed warnings or custom suppression annotations.  This is the common case.
   *
   * @param sym The {@code Symbol} for the AST node currently being scanned
   * @param suppressWarningsType The {@code Type} for {@code @SuppressWarnings}, as given by
   *        javac's symbol table
   */
  private void handleSuppressions(Symbol sym, Type suppressWarningsType) {
    /**
     * Handle custom suppression annotations.
     */
    Set<Class<? extends Annotation>> newCustomSuppressions = null;
    for (Class<? extends Annotation> annotationType : getCustomAnnotationTypes()) {
      Annotation annotation = JavacElements.getAnnotation(sym, annotationType);
      if (annotation != null) {
        newCustomSuppressions = new HashSet<Class<? extends Annotation>>(customSuppressions);
        newCustomSuppressions.add(annotation.getClass());
      }
    }
    if (newCustomSuppressions != null) {
      customSuppressions = newCustomSuppressions;
    }

    /**
     * Handle @SuppressWarnings.
     */
    // FIXME: is copied necessary?  can this be simplified?
    Set<String> newSuppressions = null;
    boolean copied = false;

    // Iterate over annotations on this symbol, looking for SuppressWarnings
    for (Attribute.Compound attr : sym.getAnnotationMirrors()) {
      // FIXME: use JavacElements.getAnnotation instead
      if (attr.type.tsym == suppressWarningsType.tsym) {
        for (List<Pair<MethodSymbol,Attribute>> v = attr.values;
            v.nonEmpty(); v = v.tail) {
          Pair<MethodSymbol,Attribute> value = v.head;
          if (value.fst.name.toString().equals("value"))
            if (value.snd instanceof Attribute.Array) {  // SuppressWarnings takes an array
              for (Attribute suppress : ((Attribute.Array) value.snd).values) {
                if (!copied) {
                  newSuppressions = new HashSet<String>(suppressions);
                  copied = true;
                }
                // TODO(eaftan): check return value to see if this was a new warning?
                newSuppressions.add((String) suppress.getValue());
              }
            } else {
              throw new RuntimeException("Expected SuppressWarnings annotation to take array type");
            }
        }
      }
    }
    if (newSuppressions != null) {
      suppressions = newSuppressions;
    }

  }

  /**
   * Returns true if any of the warning IDs in the collection are in the set of current
   * suppressions from scanning down the AST.
   *
   * @param suppressible holds a collection of warning IDs
   */
  protected boolean isSuppressed(Suppressible suppressible) {
    return suppressible.isSuppressible() && !Collections.disjoint(
        suppressible.getAllNames(), suppressions);
  }

  /**
   * Returns a set of all the custom annotation types used by the {@code BugChecker}s in this
   * {@code Scanner}.
   *
   * FIXME(eaftan): better name?
   */
  protected Set<Class<? extends Annotation>> getCustomAnnotationTypes() {
    return Collections.<Class<? extends Annotation>>emptySet();
  }

  protected <T extends Tree> void reportMatch(Description description, T match, VisitorState state)
  {
    if (description == null || description == Description.NO_MATCH) {
      return;
    }
    state.getMatchListener().onMatch(match);
    state.getDescriptionListener().onDescribed(description);
  }
}
