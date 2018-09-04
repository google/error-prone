/*
 * Copyright 2012 The Error Prone Authors.
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

package com.google.errorprone.scanner;

import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.ErrorProneOptions;
import com.google.errorprone.SuppressionHelper;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Suppressible;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Symbol;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * TODO(eaftan): I'm worried about this performance of this code, specifically the part that handles
 * SuppressWarnings. We should profile it and see where the hotspots are.
 *
 * @author alexeagle@google.com (Alex Eagle)
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class Scanner extends TreePathScanner<Void, VisitorState> {

  private Set<String> suppressions = new HashSet<>();
  private Set<Class<? extends Annotation>> customSuppressions = new HashSet<>();
  private boolean inGeneratedCode = false;
  // This must be lazily initialized, because the list of custom suppression annotations will
  // not be available until after the subclass's constructor has run.
  private SuppressionHelper suppressionHelper;

  private void initSuppressionHelper() {
    if (suppressionHelper == null) {
      suppressionHelper = new SuppressionHelper(getCustomSuppressionAnnotations());
    }
  }

  /** Scan a tree from a position identified by a TreePath. */
  @Override
  public Void scan(TreePath path, VisitorState state) {
    SuppressionHelper.SuppressionInfo prevSuppressionInfo =
        updateSuppressions(path.getLeaf(), state);

    try {
      return super.scan(path, state);
    } finally {
      // Restore old suppression state.
      suppressions = prevSuppressionInfo.suppressWarningsStrings;
      customSuppressions = prevSuppressionInfo.customSuppressions;
      inGeneratedCode = prevSuppressionInfo.inGeneratedCode;
    }
  }

  /** Scan a single node. The current path is updated for the duration of the scan. */
  @Override
  public Void scan(Tree tree, VisitorState state) {
    if (tree == null) {
      return null;
    }

    SuppressionHelper.SuppressionInfo prevSuppressionInfo = updateSuppressions(tree, state);
    try {
      return super.scan(tree, state);
    } finally {
      // Restore old suppression state.
      suppressions = prevSuppressionInfo.suppressWarningsStrings;
      customSuppressions = prevSuppressionInfo.customSuppressions;
      inGeneratedCode = prevSuppressionInfo.inGeneratedCode;
    }
  }

  /**
   * Updates current suppression state with information for the given {@code tree}. Returns the
   * previous suppression state so that it can be restored when going up the tree.
   */
  private SuppressionHelper.SuppressionInfo updateSuppressions(Tree tree, VisitorState state) {
    SuppressionHelper.SuppressionInfo prevSuppressionInfo =
        new SuppressionHelper.SuppressionInfo(suppressions, customSuppressions, inGeneratedCode);

    initSuppressionHelper();

    Symbol sym = ASTHelpers.getDeclaredSymbol(tree);
    if (sym != null) {
      SuppressionHelper.SuppressionInfo newSuppressions =
          suppressionHelper.extendSuppressionSets(
              sym,
              state.getSymtab().suppressWarningsType,
              suppressions,
              customSuppressions,
              inGeneratedCode,
              state);
      if (newSuppressions.suppressWarningsStrings != null) {
        suppressions = newSuppressions.suppressWarningsStrings;
      }
      if (newSuppressions.customSuppressions != null) {
        customSuppressions = newSuppressions.customSuppressions;
      }
      inGeneratedCode = newSuppressions.inGeneratedCode;
    }

    return prevSuppressionInfo;
  }

  /**
   * Returns true if this checker should be suppressed on the current tree path.
   *
   * @param suppressible holds information about the suppressibility of a checker
   */
  protected boolean isSuppressed(Suppressible suppressible, ErrorProneOptions errorProneOptions) {
    initSuppressionHelper();

    return SuppressionHelper.isSuppressed(
        suppressible,
        suppressions,
        customSuppressions,
        severityMap().get(suppressible.canonicalName()),
        inGeneratedCode,
        errorProneOptions.disableWarningsInGeneratedCode());
  }

  /**
   * Returns a set of all the custom suppression annotation types used by the {@code BugChecker}s in
   * this{@code Scanner}.
   */
  protected Set<Class<? extends Annotation>> getCustomSuppressionAnnotations() {
    return Collections.<Class<? extends Annotation>>emptySet();
  }

  protected void reportMatch(Description description, VisitorState state) {
    if (description == null || description == Description.NO_MATCH) {
      return;
    }
    state.reportMatch(description);
  }

  /** Handles an exception thrown by an individual check. */
  protected void handleError(Suppressible s, Throwable t) {}

  /** Returns a mapping between the canonical names of checks and their {@link SeverityLevel}. */
  public Map<String, SeverityLevel> severityMap() {
    return Collections.emptyMap();
  }
}
