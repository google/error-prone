/*
 * Copyright 2020 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.flogger;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.fixes.SuggestedFixes.addModifiers;
import static com.google.errorprone.fixes.SuggestedFixes.removeModifiers;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PROTECTED;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import com.google.common.base.Joiner;
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.LinkType;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.IdentifierTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MemberSelectTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.fixes.SuggestedFixes.AdditionPosition;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.suppliers.Suppliers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;

/** Ensures that class-level FluentLogger objects are private static final. */
@BugPattern(
    name = "FloggerRequiredModifiers",
    summary =
        "FluentLogger.forEnclosingClass should always be saved to a private static final field.",
    link = "https://google.github.io/flogger/best_practice#modifiers",
    linkType = LinkType.CUSTOM,
    severity = WARNING)
public final class FloggerRequiredModifiers extends BugChecker
    implements MethodInvocationTreeMatcher,
        IdentifierTreeMatcher,
        MemberSelectTreeMatcher,
        VariableTreeMatcher {

  private static final String GOOGLE_LOGGER = "com.google.common.flogger.FluentLogger";
  private static final Supplier<Type> LOGGER_TYPE = Suppliers.typeFromString(GOOGLE_LOGGER);

  private static final Matcher<ExpressionTree> INIT_LOGGER =
      staticMethod().onClass(LOGGER_TYPE).named("forEnclosingClass").withParameters();

  private static final ImmutableList<Modifier> EXPECTED_MODIFIERS =
      ImmutableList.of(Modifier.PRIVATE, STATIC, FINAL);

  /** A list of names we could give to new fields of type Logger, in descending preference order. */
  private static final ImmutableList<String> REASONABLE_LOGGER_NAMES =
      ImmutableList.of("logger", "flogger", "googleLogger", "myLogger");

  private static final String NESTED_LOGGER_CLASSNAME = "Private";
  private static final String NESTED_LOGGER_FIELDNAME = "logger";
  private static final String NESTED_LOGGER_DEFINITION =
      Joiner.on('\n')
          .join(
              "/** Do not use. Exists only to hide implementation details of this interface. */",
              String.format("public static final class %s {", NESTED_LOGGER_CLASSNAME),
              String.format("  private %s() {}", NESTED_LOGGER_CLASSNAME),
              String.format(
                  "  private static final FluentLogger %s = FluentLogger.forEnclosingClass();",
                  NESTED_LOGGER_FIELDNAME),
              "}");

  private final Map<String, LocalLogger> localLogger = new HashMap<>();

  private static final Matcher<Tree> CONTAINS_INIT_LOGGER =
      Matchers.contains(ExpressionTree.class, INIT_LOGGER);

  /**
   * Constructs a checker configured by {@code flags}. The only flag this constructor looks at is
   * {@code "FloggerRequiredModifiers:Goal"}. It expects that flag to have one of the following
   * values:
   *
   * <p>
   *
   * <ul>
   *   <li>{@code REHOME_FOREIGN_LOGGERS}
   *   <li>{@code HIDE_LOGGERS_IN_INTERFACES}
   *   <li>{@code HOIST_CONSTANT_EXPRESSIONS}
   *   <li>{@code ADD_FINAL}
   *   <li>{@code ADD_STATIC}
   *   <li>{@code MAKE_PRIVATE}
   *   <li>{@code DEFAULT_ALL_GOALS}
   * </ul>
   *
   * <p>The default value is {@code DEFAULT_ALL_GOALS}, which enables all features; other legal
   * values check only a subset of the Flogger best practices, and are designed to generate a more
   * fine grained adjustments.
   */
  public FloggerRequiredModifiers(ErrorProneFlags flags) {
    this(flags.getEnum("FloggerRequiredModifiers:Goal", Goal.class).orElse(Goal.DEFAULT_ALL_GOALS));
  }

  FloggerRequiredModifiers(Goal goal) {
    this.goal = goal;
  }

  enum Goal {
    REHOME_FOREIGN_LOGGERS,
    HIDE_LOGGERS_IN_INTERFACES,
    HOIST_CONSTANT_EXPRESSIONS,
    ADD_FINAL,
    ADD_STATIC,
    MAKE_PRIVATE,
    /** Combines all other goals to be maximally picky and do a full rewrite at once. */
    DEFAULT_ALL_GOALS,
  }

  private final Goal goal;

  private boolean shouldRehomeForeignLoggers() {
    return goal.equals(Goal.REHOME_FOREIGN_LOGGERS);
  }

  private boolean shouldHideInterfaceLoggers() {
    switch (goal) {
      case HIDE_LOGGERS_IN_INTERFACES:
      case DEFAULT_ALL_GOALS:
        return true;
      default:
        return false;
    }
  }

  private boolean shouldHoistConstantExpressions() {
    switch (goal) {
      case DEFAULT_ALL_GOALS:
      case HOIST_CONSTANT_EXPRESSIONS:
        return true;
      default:
        return false;
    }
  }

  private boolean shouldStandardizeModifiers() {
    switch (goal) {
      case DEFAULT_ALL_GOALS:
      case ADD_FINAL:
      case ADD_STATIC:
      case MAKE_PRIVATE:
        return true;
      default:
        return false;
    }
  }

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    if (!shouldStandardizeModifiers()) {
      return NO_MATCH;
    }
    Type loggerType = LOGGER_TYPE.get(state);
    if (!ASTHelpers.isSameType(loggerType, ASTHelpers.getType(tree), state)) {
      return NO_MATCH;
    }
    VarSymbol sym = ASTHelpers.getSymbol(tree);
    if (!(sym.owner instanceof ClassSymbol)) {
      return NO_MATCH;
    }
    ExpressionTree initializer = tree.getInitializer();
    // Static fields with no initializer, or fields initialized to a constant FluentLogger
    if (initializer == null
        ? sym.isStatic()
        : isConstantLogger(initializer, (ClassSymbol) sym.owner, state)) {
      return fixModifier(tree, (ClassTree) state.getPath().getParentPath().getLeaf(), state);
    }

    return NO_MATCH;
  }

  private static boolean isConstantLogger(
      ExpressionTree initializer, ClassSymbol owner, VisitorState state) {
    if (initializer instanceof MethodInvocationTree) {
      Type loggerType = LOGGER_TYPE.get(state);
      MethodInvocationTree method = (MethodInvocationTree) initializer;
      MethodSymbol methodSym = ASTHelpers.getSymbol(method);
      if (methodSym == null) {
        return false; // Something is broken, just give up
      }
      if (methodSym.isStatic()
          && methodSym.owner.equals(owner)
          && ASTHelpers.isSameType(loggerType, methodSym.getReturnType(), state)) {
        return true;
      }
      // Fall through to search for FluentLogger.forEnclosingClass()
    }
    return CONTAINS_INIT_LOGGER.matches(initializer, state);
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!shouldHoistConstantExpressions()) {
      return NO_MATCH;
    }
    if (!INIT_LOGGER.matches(tree, state)) {
      return NO_MATCH;
    }
    Type loggerType = LOGGER_TYPE.get(state);
    if (loggerType == null) {
      return NO_MATCH;
    }

    // An expression should always be inside of a method or a variable (initializer blocks count as
    // a method), but just in case we use class as a final backstop.
    TreePath owner =
        state.findPathToEnclosing(ClassTree.class, MethodTree.class, VariableTree.class);
    Tree parent = owner.getLeaf();
    Tree grandparent = owner.getParentPath().getLeaf();
    boolean isLoggerField =
        parent instanceof VariableTree
            && grandparent instanceof ClassTree
            && ASTHelpers.isSameType(loggerType, ASTHelpers.getType(parent), state);
    if (isLoggerField) {
      // Declared as a class member - matchVariable will already have fixed the modifiers, and we
      // allow calls to forEnclosingClass() in initializer context, so we can just quit here
      return NO_MATCH;
    }

    // See if they're defining a static method that produces a FluentLogger. If so, we assume they
    // are using it to initialize their FluentLogger instance, and we don't stop them from calling
    // FluentLogger.forEnclosingClass() inside the method.
    MethodTree owningMethod = state.findEnclosing(MethodTree.class);
    if (owningMethod != null) {
      MethodSymbol methodSym = ASTHelpers.getSymbol(owningMethod);
      if (methodSym != null) {
        Type returnType = methodSym.getReturnType();
        // Could be null for initializer blocks
        if (ASTHelpers.isSameType(loggerType, returnType, state)) {
          return NO_MATCH;
        }
      }
    }

    // They're using forEnclosingClass inside a method that doesn't produce a logger, or in the
    // initializer for some variable that's not a logger. We'll replace this with a reference to a
    // class-level logger.
    state.incrementCounter(this, parent instanceof VariableTree ? "local-variable" : "inline");
    return replaceWithFieldLookup(tree, state);
  }

  private boolean modifierChangesInclude(Goal g) {
    return goal == Goal.DEFAULT_ALL_GOALS || goal == g;
  }

  private Description fixModifier(VariableTree field, ClassTree owningClass, VisitorState state) {
    ModifiersTree modifiers = field.getModifiers();
    Set<Modifier> flags = modifiers.getFlags();
    if (flags.containsAll(EXPECTED_MODIFIERS)) {
      return NO_MATCH;
    }
    updateModifierCounters(state, flags);

    // We have to add all the modifiers as a single SuggestedFix or they conflict
    ImmutableSet.Builder<Modifier> toAdd = ImmutableSet.builder();
    SuggestedFix.Builder fix = SuggestedFix.builder();
    if (modifierChangesInclude(Goal.MAKE_PRIVATE)) {
      removeModifiers(field, state, PUBLIC, PROTECTED).ifPresent(fix::merge);
      toAdd.add(PRIVATE);
    }
    if (modifierChangesInclude(Goal.ADD_FINAL)) {
      toAdd.add(Modifier.FINAL);
    }
    if (modifierChangesInclude(Goal.ADD_STATIC)
        && (flags.contains(FINAL) || modifierChangesInclude(Goal.ADD_FINAL))
        && canHaveStaticFields(ASTHelpers.getSymbol(owningClass))) {
      // We only add static to fields which are already final, or that we're also making final.
      // It's a bit dangerous to quietly make something static if it might be getting reassigned.
      toAdd.add(STATIC);
    }
    ImmutableSet<Modifier> newModifiers = toAdd.build();
    if (!newModifiers.isEmpty()) {
      addModifiers(field, modifiers, state, newModifiers).ifPresent(fix::merge);
    }
    if (fix.isEmpty()) {
      // They're missing some modifiers, but not the ones we've been instructed to change today.
      // This is instead of blindly using fix.build(), which would insert a comment if empty.
      return NO_MATCH;
    }
    return describeMatch(field, fix.build());
  }

  private Description replaceWithFieldLookup(ExpressionTree expr, VisitorState state) {
    SuggestedFix.Builder fix = SuggestedFix.builder();
    LocalLogger logger = findOrDefineLogger(state, fix);
    /*
    Sometimes people assign to the logger in, e.g., a static initializer. We don't want to
    rewrite this to { logger = logger; }, so we search up the tree to see if we are part
    of any assignment to the logger that we're planning to replace this with a reference
    to.
    */
    if (logger.provenance == LocalLogger.Provenance.ALREADY_PRESENT && logger.sym.isPresent()) {
      // There should always be a symbol for ALREADY_PRESENT, but we check just in case.
      Symbol target = logger.sym.get();
      Tree e = expr;
      TreePath path = state.getPath();

      do {
        if (e instanceof AssignmentTree) {
          AssignmentTree assignment = (AssignmentTree) e;
          if (ASTHelpers.getSymbol(assignment.getVariable()).equals(target)) {
            state.incrementCounter(this, "skip-self-assignment");
            return NO_MATCH;
          }
        }
        path = path.getParentPath();
        e = path.getLeaf();
      } while (e instanceof ExpressionTree);
    }
    String loggerName = logger.name;
    Tree parent = state.getPath().getParentPath().getLeaf();
    if (parent instanceof VariableTree
        && ((VariableTree) parent).getName().contentEquals(loggerName)) {
      // Instead of making a local shadow of a class member, just use the class member.
      return describeMatch(expr, fix.delete(parent).build());
    }
    if (loggerName.equals(state.getSourceForNode(expr))) {
      // Don't rewrite `logger` to `logger`, which runs the formatter over an "unchanged"
      // line.
      if (fix.isEmpty()) {
        // The logger field we want to refer to already exists, and we don't need to
        // change this
        // line, so there's actually no work to do.
        return NO_MATCH;
      }
      // Produce a fix that inserts the new logger field without changing this line.
      return describeMatch(expr, fix.build());
    }
    return describeMatch(expr, fix.replace(expr, loggerName).build());
  }

  private void updateModifierCounters(VisitorState state, Set<Modifier> flags) {
    // We expect to see all of these, so note when we don't
    for (Modifier modifier : ImmutableList.of(STATIC, FINAL)) {
      if (!flags.contains(modifier)) {
        state.incrementCounter(this, "missing-" + modifier);
      }
    }
    // These we expect to see at most one of, so log whichever is there (or package otherwise)
    boolean explicitVisibility = false;
    for (Modifier visibility : ImmutableList.of(PUBLIC, Modifier.PRIVATE, PROTECTED)) {
      if (flags.contains(visibility)) {
        state.incrementCounter(this, "visibility-" + visibility);
        explicitVisibility = true;
        break;
      }
    }
    if (!explicitVisibility) {
      state.incrementCounter(this, "visibility-package");
    }
  }

  private static final class LocalLogger {

    LocalLogger(Provenance provenance, Optional<Symbol> sym, String name) {
      this.provenance = provenance;
      this.sym = sym;
      this.name = name;
    }

    enum Provenance {
      ALREADY_PRESENT,
      DEFINED_BY_FIX
    }

    final Provenance provenance;
    final Optional<Symbol> sym;
    final String name;
  }

  private LocalLogger findOrDefineLogger(VisitorState state, SuggestedFix.Builder fix) {
    return localLogger.computeIfAbsent(
        ASTHelpers.getFileName(state.getPath().getCompilationUnit()),
        s -> computeLocalLogger(state, fix));
  }

  private LocalLogger computeLocalLogger(VisitorState state, SuggestedFix.Builder fix) {
    Type loggerType = LOGGER_TYPE.get(state);
    if (loggerType == null) {
      throw new AssertionError("Attempting to define new logger in a file without loggers");
    }
    ImmutableSet<Symbol> ignoredFields = fieldsToIgnore(state);

    ClassTree topLevelClassInFile = outermostClassTree(state.getPath());
    ClassSymbol targetClassSym = ASTHelpers.getSymbol(topLevelClassInFile);
    for (Tree member : topLevelClassInFile.getMembers()) {
      Symbol memberSym = ASTHelpers.getSymbol(member);
      if (memberSym instanceof VarSymbol
          && ASTHelpers.isSubtype(memberSym.type, loggerType, state)
          && !ignoredFields.contains(memberSym)) {
        // Found some logger defined, let's just use that, unless it needs to be moved.
        if (!targetClassSym.isInterface()) {
          state.incrementCounter(this, "found-existing");
          return new LocalLogger(
              LocalLogger.Provenance.ALREADY_PRESENT,
              Optional.of(memberSym),
              memberSym.getSimpleName().toString());
        }
        /* This logger belongs to an interface, which means it is public. We'll create a new static
        nested class to hold the logger, and make the logger a private member of that class.
        */
        fix.delete(member);
        return defineNestedClassWithLogger(topLevelClassInFile, state, fix);
      }
    }

    fix.addImport(GOOGLE_LOGGER);
    if (targetClassSym.isInterface()) {
      // Interfaces should get a nested class, not a normal field.
      return defineNestedClassWithLogger(topLevelClassInFile, state, fix);
    }

    String name =
        REASONABLE_LOGGER_NAMES.stream()
            .filter(
                candidate ->
                    Iterables.isEmpty(
                        targetClassSym.members().getSymbolsByName(state.getName(candidate))))
            .findFirst()
            .orElseThrow(IllegalStateException::new);

    String newMember =
        String.format(
            "private static final FluentLogger %s =" + " FluentLogger.forEnclosingClass();", name);
    fix.merge(
        SuggestedFixes.addMembers(topLevelClassInFile, state, AdditionPosition.FIRST, newMember));
    return new LocalLogger(LocalLogger.Provenance.DEFINED_BY_FIX, Optional.empty(), name);
  }

  private static LocalLogger defineNestedClassWithLogger(
      ClassTree topLevelClassInFile, VisitorState state, SuggestedFix.Builder fix) {
    fix.merge(SuggestedFixes.addMembers(topLevelClassInFile, state, NESTED_LOGGER_DEFINITION));
    return new LocalLogger(
        LocalLogger.Provenance.DEFINED_BY_FIX,
        Optional.empty(),
        String.format("%s.%s", NESTED_LOGGER_CLASSNAME, NESTED_LOGGER_FIELDNAME));
  }

  private static ClassTree outermostClassTree(TreePath path) {
    ClassTree result = null;
    while (path != null) {
      Tree leaf = path.getLeaf();
      if (leaf instanceof ClassTree) {
        result = (ClassTree) leaf;
      }
      path = path.getParentPath();
    }

    Verify.verifyNotNull(result, "No enclosing class");

    return result;
  }

  private static boolean canHaveStaticFields(ClassSymbol enclosingClassSym) {
    return enclosingClassSym.getNestingKind() == NestingKind.TOP_LEVEL
        || enclosingClassSym.getNestingKind() == NestingKind.MEMBER
            && ((enclosingClassSym.flags() & Flags.STATIC) != 0);
  }

  @Override
  public Description matchIdentifier(IdentifierTree tree, VisitorState state) {
    return rehomeLogger(tree, state);
  }

  @Override
  public Description matchMemberSelect(MemberSelectTree tree, VisitorState state) {
    return rehomeLogger(tree, state);
  }

  /**
   * If the expression refers to a FluentLogger owned by a class in another file, rewrites it to
   * refer to a logger in this file, defining one if necessary.
   */
  private Description rehomeLogger(ExpressionTree tree, VisitorState state) {
    if (!shouldRehomeForeignLoggers() && !shouldHideInterfaceLoggers()) {
      return NO_MATCH;
    }
    Symbol sym = ASTHelpers.getSymbol(tree);
    if (sym == null) {
      return NO_MATCH;
    }
    Type type = sym.type;
    if (!ASTHelpers.isSameType(type, LOGGER_TYPE.get(state), state)) {
      return NO_MATCH;
    }
    Symbol owner = sym.owner;
    if (!(owner instanceof ClassSymbol)) {
      // This may be looking up FluentLogger itself, as a member of its package, or reading a local.
      return NO_MATCH;
    }
    if (!(sym.isStatic() || tree instanceof IdentifierTree)) {
      // We can only be referring to another class's logger statically, or implicitly through a
      // superclass as an identifier. This early exit avoids flagging instance field lookups.
      return NO_MATCH;
    }
    /* Loggers owned by public interfaces should be moved regardless of whether they're defined in
    the current file, because fields in interfaces must be public, but loggers should be private.
     */
    boolean needsMoveFromInterface = shouldHideInterfaceLoggers() && owner.isInterface();
    boolean local = false;
    Symbol outermostClassOfLogger = findUltimateOwningClass(owner);
    ClassTree outermostClassOfFile = null;
    for (Tree parent : state.getPath()) {
      Symbol ownerSym = ASTHelpers.getSymbol(parent);
      if (outermostClassOfLogger.equals(ownerSym)) {
        /* Seems to be a logger owned by a class in this file. */
        if (!needsMoveFromInterface) {
          return NO_MATCH;
        }
        local = true;
      }
      if (parent instanceof ClassTree) {
        outermostClassOfFile = (ClassTree) parent;
      }
    }

    if (outermostClassOfFile == null) {
      // Impossible, I think?
      state.incrementCounter(this, "error-no-outermost-class");
      return NO_MATCH;
    }

    if (!local && !shouldRehomeForeignLoggers()) {
      return NO_MATCH;
    }

    return replaceWithFieldLookup(tree, state);
  }

  private static Symbol findUltimateOwningClass(Symbol sym) {
    Symbol result = sym;
    while (sym instanceof ClassSymbol) {
      result = sym;
      sym = sym.owner;
    }
    return result;
  }

  /**
   * Fields which we should not use to replace the current expression, because we are in the middle
   * of defining them.
   */
  private static ImmutableSet<Symbol> fieldsToIgnore(VisitorState state) {
    Tree t = state.findEnclosing(VariableTree.class, ClassTree.class);
    return t instanceof VariableTree ? ImmutableSet.of(ASTHelpers.getSymbol(t)) : ImmutableSet.of();
  }
}
