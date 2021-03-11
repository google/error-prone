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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.ErrorProneOptions;
import com.google.errorprone.SuppressionInfo;
import com.google.errorprone.SuppressionInfo.SuppressedState;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.hubspot.HubSpotUtils;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Suppressible;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.Name;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * TODO(eaftan): I'm worried about this performance of this code, specifically the part that handles
 * SuppressWarnings. We should profile it and see where the hotspots are.
 *
 * @author alexeagle@google.com (Alex Eagle)
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@CheckReturnValue
public class Scanner extends TreePathScanner<Void, VisitorState> {

  private SuppressionInfo currentSuppressions = SuppressionInfo.EMPTY;

  /** Scan a tree from a position identified by a TreePath. */
  @Override
  public Void scan(TreePath path, VisitorState state) {
    SuppressionInfo prevSuppressionInfo = updateSuppressions(path.getLeaf(), state);
    try {
      return super.scan(path, state);
    } finally {
      // Restore old suppression state.
      currentSuppressions = prevSuppressionInfo;
    }
  }

  /** Scan a single node. The current path is updated for the duration of the scan. */
  @Override
  public Void scan(Tree tree, VisitorState state) {
    if (tree == null) {
      return null;
    }

    SuppressionInfo prevSuppressionInfo = updateSuppressions(tree, state);
    try {
      return super.scan(tree, state);
    } finally {
      // Restore old suppression state.
      currentSuppressions = prevSuppressionInfo;
    }
  }

  /**
   * Updates current suppression state with information for the given {@code tree}. Returns the
   * previous suppression state so that it can be restored when going up the tree.
   */
  private SuppressionInfo updateSuppressions(Tree tree, VisitorState state) {
    SuppressionInfo prevSuppressionInfo = currentSuppressions;
    if (tree instanceof CompilationUnitTree) {
      currentSuppressions =
          currentSuppressions.forCompilationUnit((CompilationUnitTree) tree, state);
    } else {
      Symbol sym = ASTHelpers.getDeclaredSymbol(tree);
      if (sym != null) {
        currentSuppressions =
            currentSuppressions.withExtendedSuppressions(
                sym, state, getCustomSuppressionAnnotations(state));
      }
    }
    return prevSuppressionInfo;
  }

  /**
   * Returns if this checker should be suppressed on the current tree path.
   *
   * @param suppressible holds information about the suppressibility of a checker
   * @param errorProneOptions Options object configuring whether or not to suppress non-errors in
   */
  protected SuppressedState isSuppressed(
      Suppressible suppressible, ErrorProneOptions errorProneOptions, VisitorState state) {

    final boolean suppressedInGeneratedCode;
    if (HubSpotUtils.isGeneratedCodeInspectionEnabled(state)) {
      suppressedInGeneratedCode = !suppressible.inspectGeneratedCode()
          || (errorProneOptions.disableWarningsInGeneratedCode()
          && severityMap().get(suppressible.canonicalName()) != SeverityLevel.ERROR);
    } else {
      suppressedInGeneratedCode =
          errorProneOptions.disableWarningsInGeneratedCode()
              && severityMap().get(suppressible.canonicalName()) != SeverityLevel.ERROR;
    }

    return currentSuppressions.suppressedState(suppressible, suppressedInGeneratedCode, state);
  }

  /**
   * Returns a set of all the custom suppression annotation types used by the {@code BugChecker}s in
   * this{@code Scanner}.
   */
  protected Set<? extends Name> getCustomSuppressionAnnotations(VisitorState state) {
    return ImmutableSet.of();
  }

  protected void reportMatch(Description description, VisitorState state) {
    checkNotNull(description, "Use Description.NO_MATCH to denote an absent finding.");
    state.reportMatch(description);
  }

  /** Handles an exception thrown by an individual check. */
  protected void handleError(Suppressible s, Throwable t) {}

  /** Returns a mapping between the canonical names of checks and their {@link SeverityLevel}. */
  public Map<String, SeverityLevel> severityMap() {
    return Collections.emptyMap();
  }
}
