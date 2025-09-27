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

package com.google.errorprone;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Immutable;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Suppressible;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Pair;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jspecify.annotations.Nullable;

/**
 * Immutable container of "suppression signals" - annotations or other information gathered from
 * source - that can be used to determine if a specific {@link Suppressible} object should be
 * considered suppressed.
 *
 * <p>SuppressionInfo instances are obtained by starting with the {@link #EMPTY} instance, then
 * getting new instances by calling {@link #withExtendedSuppressions} with symbols discovered as you
 * descend a program tree.
 */
@Immutable
@CheckReturnValue
public class SuppressionInfo {
  public static final SuppressionInfo EMPTY =
      new SuppressionInfo(ImmutableSet.of(), ImmutableSet.of(), false);

  private static final Supplier<Name> ANDROID_SUPPRESS_LINT =
      VisitorState.memoize(state -> state.getName("android.annotation.SuppressLint"));
  private static final Supplier<Name> VALUE = VisitorState.memoize(state -> state.getName("value"));
  private final ImmutableSet<String> suppressWarningsStrings;

  @SuppressWarnings("Immutable") /* Name is javac's interned version of a string. */
  private final ImmutableSet<Name> customSuppressions;

  private final boolean inGeneratedCode;

  @BugPattern(summary = "Warns when a @SuppressWarnings is not needed", severity = WARNING)
  private static final class UnusedSuppressionChecker extends BugChecker {}

  private static final UnusedSuppressionChecker UNUSED_SUPPRESSION_CHECKER =
      new UnusedSuppressionChecker();

  // for tracking unneeded suppressions
  static class SuppressionInfoForSymbol {
    private final @Nullable Symbol symbol;
    private final ImmutableSet<String> suppressWarningStringsForSymbol;
    private final Set<String> usedSuppressWarningStrings = new HashSet<>();
    private final ImmutableSet<Name> customSuppressionsForSymbol;
    private final @Nullable SuppressionInfoForSymbol parent;

    public SuppressionInfoForSymbol(
        @Nullable Symbol symbol,
        ImmutableSet<String> suppressWarningStringsForSymbol,
        ImmutableSet<Name> customSuppressionsForSymbol,
        @Nullable SuppressionInfoForSymbol parent) {
      this.symbol = symbol;
      this.suppressWarningStringsForSymbol = suppressWarningStringsForSymbol;
      this.customSuppressionsForSymbol = customSuppressionsForSymbol;
      this.parent = parent;
    }

    public void markSuppressionStringAsUsed(String suppressionName) {
      if (suppressWarningStringsForSymbol.contains(suppressionName)) {
        usedSuppressWarningStrings.add(suppressionName);
      } else if (parent != null) {
        parent.markSuppressionStringAsUsed(suppressionName);
      } else {
        throw new IllegalArgumentException("Suppression string not found: " + suppressionName);
      }
    }
  }

  private final @Nullable SuppressionInfoForSymbol infoForClosestSymbol;

  public void updatedUsedSuppressions(Suppressed suppressed) {
    String suppressionName = suppressed.getSuppressionName();
    if (suppressionName != null && suppressed.isUsed()) {
      infoForClosestSymbol.markSuppressionStringAsUsed(suppressionName);
    }
  }

  public void warnOnUnusedSuppressions(VisitorState state) {
    if (infoForClosestSymbol == null) {
      return;
    }
    for (String warn : infoForClosestSymbol.suppressWarningStringsForSymbol) {
      if (!infoForClosestSymbol.usedSuppressWarningStrings.contains(warn)) {
        Tree tree =
            Trees.instance(JavacProcessingEnvironment.instance(state.context))
                .getTree(infoForClosestSymbol.symbol);
        Description description =
            UNUSED_SUPPRESSION_CHECKER
                .buildDescription(tree)
                .setMessage("Unnecessary @SuppressWarnings(\"" + warn + "\")")
                .overrideSeverity(WARNING)
                .build();
        state.reportMatch(description);
      }
    }
  }

  private SuppressionInfo(
      Set<String> suppressWarningsStrings, Set<Name> customSuppressions, boolean inGeneratedCode) {
    this(suppressWarningsStrings, customSuppressions, inGeneratedCode, null);
  }

  private SuppressionInfo(
      Set<String> suppressWarningsStrings,
      Set<Name> customSuppressions,
      boolean inGeneratedCode,
      @Nullable SuppressionInfoForSymbol infoForClosestSymbol) {
    this.suppressWarningsStrings = ImmutableSet.copyOf(suppressWarningsStrings);
    this.customSuppressions = ImmutableSet.copyOf(customSuppressions);
    this.inGeneratedCode = inGeneratedCode;
    this.infoForClosestSymbol = infoForClosestSymbol;
  }

  private static boolean isGenerated(Symbol sym) {
    return !ASTHelpers.getGeneratedBy(sym).isEmpty();
  }

  /**
   * Returns true if this checker should be considered suppressed given the signals present in this
   * object.
   *
   * @param suppressible Holds information about the suppressibilty of a checker
   * @param suppressedInGeneratedCode true if this checker instance should be considered suppressed
   */
  public SuppressedState suppressedState(
      Suppressible suppressible, boolean suppressedInGeneratedCode, VisitorState state) {
    if (inGeneratedCode && suppressedInGeneratedCode) {
      return new Suppressed(null);
    }
    if (suppressible.supportsSuppressWarnings()) {
      Optional<String> warningName =
          suppressible.allNames().stream().filter(suppressWarningsStrings::contains).findAny();
      if (suppressWarningsStrings.contains("all") || warningName.isPresent()) {
        String name = warningName.orElse("all");
        return new Suppressed(name);
      }
    }
    if (suppressible.suppressedByAnyOf(customSuppressions, state)) {
      return new Suppressed(null);
    }

    return Unsuppressed.UNSUPPRESSED;
  }

  /**
   * Generates the {@link SuppressionInfo} for a {@link CompilationUnitTree}. This differs in that
   * {@code isGenerated} is determined by inspecting the annotations of the outermost class so that
   * matchers on {@link CompilationUnitTree} will also be suppressed.
   */
  public SuppressionInfo forCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    AtomicBoolean generated = new AtomicBoolean(false);
    new SimpleTreeVisitor<Void, Void>() {
      @Override
      public Void visitClass(ClassTree node, Void unused) {
        ClassSymbol symbol = ASTHelpers.getSymbol(node);
        generated.compareAndSet(false, symbol != null && isGenerated(symbol));
        return null;
      }
    }.visit(tree.getTypeDecls(), null);
    return new SuppressionInfo(suppressWarningsStrings, customSuppressions, generated.get());
  }

  /**
   * Returns an instance of {@code SuppressionInfo} that takes into account any suppression signals
   * present on {@code sym} as well as those already stored in {@code this}.
   *
   * <p>Checks suppressions for any {@code @SuppressWarnings}, Android's {@code SuppressLint}, and
   * custom suppression annotations described by {@code customSuppressionAnnosToLookFor}.
   *
   * <p>We do not modify the existing suppression sets, so they can be restored when moving up the
   * tree. We also avoid copying the suppression sets if the next node to explore does not have any
   * suppressed warnings or custom suppression annotations. This is the common case.
   *
   * @param sym The {@code Symbol} for the AST node currently being scanned
   * @param state VisitorState for checking the current tree, as well as for getting the {@code
   *     SuppressWarnings symbol type}.
   * @param warnOnUnneededSuppressWarningsStrings
   */
  public SuppressionInfo withExtendedSuppressions(
          Symbol sym,
          VisitorState state,
          Set<? extends Name> customSuppressionAnnosToLookFor,
          Set<String> warnOnUnneededSuppressWarningsStrings) {
    boolean newInGeneratedCode = inGeneratedCode || isGenerated(sym);
    boolean anyModification = newInGeneratedCode != inGeneratedCode;

    /* Handle custom suppression annotations. */
    Set<Name> lookingFor = new HashSet<>(customSuppressionAnnosToLookFor);
    // TODO what if we have nested suppressions with the same customSuppression annotation?
    //  Is the inner one unused?  Let's just say no for now.  We'll report on the outermost one.
    lookingFor.removeAll(customSuppressions);
    Set<Name> newlyPresent = ASTHelpers.annotationsAmong(sym, lookingFor, state);
    Set<Name> newCustomSuppressions;
    if (!newlyPresent.isEmpty()) {
      anyModification = true;
      newCustomSuppressions = newlyPresent;
      newCustomSuppressions.addAll(customSuppressions);
    } else {
      newCustomSuppressions = customSuppressions;
    }

    /* Handle {@code @SuppressWarnings} and {@code @SuppressLint}. */
    Name suppressLint = ANDROID_SUPPRESS_LINT.get(state);
    Name valueName = VALUE.get(state);
    Set<String> newSuppressions = null;
    Set<String> newWarnOnUnneededSuppressions = new HashSet<>();
    // Iterate over annotations on this symbol, looking for SuppressWarnings
    for (Attribute.Compound attr : sym.getAnnotationMirrors()) {
      if ((attr.type.tsym == state.getSymtab().suppressWarningsType.tsym)
          || attr.type.tsym.getQualifiedName().equals(suppressLint)) {
        for (Pair<MethodSymbol, Attribute> value : attr.values) {
          if (value.fst.name.equals(valueName)) {
            if (value.snd
                instanceof Attribute.Array array) { // SuppressWarnings/SuppressLint take an array
              for (Attribute suppress : array.values) {
                String suppressedWarning = (String) suppress.getValue();
                if (!suppressWarningsStrings.contains(suppressedWarning)) {
                  anyModification = true;
                  if (newSuppressions == null) {
                    newSuppressions = new HashSet<>(suppressWarningsStrings);
                  }
                  newSuppressions.add(suppressedWarning);
                  if (warnOnUnneededSuppressWarningsStrings.contains(suppressedWarning)) {
                    newWarnOnUnneededSuppressions.add(suppressedWarning);
                  }
                }
              }
            } else {
              throw new RuntimeException(
                  "Expected SuppressWarnings/SuppressLint annotation to take array type");
            }
          }
        }
      }
    }

    // Since this is invoked every time we descend into a new node, let's save some garbage
    // by returning the same instance if there were no changes.
    if (!anyModification) {
      return this;
    }

    if (newSuppressions == null) {
      newSuppressions = suppressWarningsStrings;
    }

    SuppressionInfoForSymbol newInfoForClosestSymbol =
        new SuppressionInfoForSymbol(
            sym,
            ImmutableSet.copyOf(newWarnOnUnneededSuppressions),
            ImmutableSet.copyOf(newlyPresent),
            infoForClosestSymbol);
    return new SuppressionInfo(
        newSuppressions, newCustomSuppressions, newInGeneratedCode, newInfoForClosestSymbol);
  }

  public sealed interface SuppressedState permits Suppressed, Unsuppressed {
    boolean isSuppressed();
  }

  public static final class Unsuppressed implements SuppressedState {
    public static final Unsuppressed UNSUPPRESSED = new Unsuppressed();

    private Unsuppressed() {}

    @Override
    public boolean isSuppressed() {
      return false;
    }
  }

  public static final class Suppressed implements SuppressedState {
    private final @Nullable String suppressionName;

    private boolean used = false;

    public Suppressed(@Nullable String suppressionName) {
      this.suppressionName = suppressionName;
    }

    public @Nullable String getSuppressionName() {
      return suppressionName;
    }

    public boolean isUsed() {
      return used;
    }

    public void setAsUsed() {
      this.used = true;
    }

    @Override
    public boolean isSuppressed() {
      return true;
    }
  }
}
