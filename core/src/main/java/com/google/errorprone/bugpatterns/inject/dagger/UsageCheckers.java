package com.google.errorprone.bugpatterns.inject.dagger;

import com.google.auto.service.AutoService;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.BugPattern;

import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.TreePathScanner;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.HashMap;

import static com.google.errorprone.BugPattern.Category.DAGGER;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

/** @authors Murali Krishna Ramanathan, Manu Sridharan */
/** Class that analyzes the use of variables to determine unused variables. */
@AutoService(BugChecker.class)
@BugPattern(
  name = "UsageCheckers",
  altNames = {"UnusedProviderParam", "UnusedMemberInjection"},
  summary = "Unused field with @Inject annotation.",
  category = DAGGER,
  severity = WARNING
)
public class UsageCheckers extends BugChecker implements BugChecker.ClassTreeMatcher {

  private static final String unusedInjectCheckerName = "UnusedMemberInjection";
  private static final String unusedInjectCheckerMessage = "Unused field with @Inject annotation.";
  private static final String unusedParamCheckerName = "UnusedProviderParam";
  private static final String unusedParamCheckerMessage =
      "Unused parameter in method with @Provides annotation.";

  private boolean symbolHasSuppressInitalizationWarningsAnnotation(
      Symbol symbol, String checkerName) {
    SuppressWarnings annotation = symbol.getAnnotation(SuppressWarnings.class);
    if (annotation != null) {
      for (String s : annotation.value()) {
        if (s.equals(checkerName)) return true;
      }
    }
    return false;
  }

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    UsageCheckers.CallScanner callScanner = new UsageCheckers.CallScanner(state);
    callScanner.scan(state.getPath(), null);
    for (VariableTree decl : callScanner.declaredInjectVars) {
      Symbol s = ASTHelpers.getSymbol(decl);
      if (!callScanner.usedVars.contains(s)
          && !symbolHasSuppressInitalizationWarningsAnnotation(s, unusedInjectCheckerName)) {
        Description.Builder b = buildDescription(decl).setMessage(unusedInjectCheckerMessage);
        b.addFix(SuggestedFix.delete(decl));
        state.reportMatch(b.build());
      }
    }

    for (VariableTree decl : callScanner.declaredParamVars.keySet()) {
      Symbol s = ASTHelpers.getSymbol(decl);
      Symbol.MethodSymbol mSym = callScanner.declaredParamVars.get(decl);
      if (!callScanner.usedVars.contains(s)
          && !symbolHasSuppressInitalizationWarningsAnnotation(mSym, unusedParamCheckerName)) {
        Description.Builder b = buildDescription(decl).setMessage(unusedParamCheckerMessage);
        b.addFix(SuggestedFix.delete(decl));
        state.reportMatch(b.build());
      }
    }
    return Description.NO_MATCH;
  }

  static class CallScanner extends TreePathScanner<Void, Void> {
    final Set<VariableTree> declaredInjectVars = new LinkedHashSet<>();
    final HashMap<VariableTree, Symbol.MethodSymbol> declaredParamVars =
        new HashMap<VariableTree, Symbol.MethodSymbol>();
    final Set<Symbol> usedVars = new LinkedHashSet<>();
    final VisitorState state;

    CallScanner(VisitorState state) {
      this.state = state;
    }

    @Override
    public Void visitMemberSelect(MemberSelectTree tree, Void unused) {
      usedVars.add(ASTHelpers.getSymbol(tree));
      return super.visitMemberSelect(tree, null);
    }

    @Override
    public Void visitIdentifier(IdentifierTree tree, Void unused) {
      usedVars.add(ASTHelpers.getSymbol(tree));
      return super.visitIdentifier(tree, null);
    }

    @Override
    public Void visitMethod(MethodTree tree, Void unused) {
      Symbol.MethodSymbol mSym = ASTHelpers.getSymbol(tree);
      if (ASTHelpers.hasAnnotation(mSym, "dagger.Provides", state)) {
        for (VariableTree vt : tree.getParameters()) {
          declaredParamVars.put(vt, mSym);
        }
      }
      return super.visitMethod(tree, null);
    }

    @Override
    public Void visitVariable(VariableTree tree, Void unused) {
      Symbol.VarSymbol vSym = ASTHelpers.getSymbol(tree);
      if (ASTHelpers.hasAnnotation(vSym, "javax.inject.Inject", state)) {
        declaredInjectVars.add(tree);
      }
      return super.visitVariable(tree, null);
    }
  }
}
