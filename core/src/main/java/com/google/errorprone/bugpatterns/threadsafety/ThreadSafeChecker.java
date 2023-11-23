/*
 * Copyright 2023 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.threadsafety;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.Immutable;
import com.google.errorprone.annotations.ThreadSafe;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MemberReferenceTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.TypeParameterTreeMatcher;
import com.google.errorprone.bugpatterns.threadsafety.ThreadSafety.Violation;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.TypeVariableSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree.JCMemberReference;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import javax.inject.Inject;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A {@link BugChecker}; see the associated {@link BugPattern} annotation for details.
 *
 * @author mboyington@google.com (Marcos Boyington)
 */
@BugPattern(
    name = "ThreadSafe",
    summary = "Type declaration annotated with @ThreadSafe is not thread safe",
    severity = ERROR)
public class ThreadSafeChecker extends BugChecker
    implements ClassTreeMatcher,
        NewClassTreeMatcher,
        TypeParameterTreeMatcher,
        MethodInvocationTreeMatcher,
        MemberReferenceTreeMatcher {

  private final WellKnownThreadSafety wellKnownThreadSafety;

  @Inject
  ThreadSafeChecker(WellKnownThreadSafety wellKnownThreadSafety) {
    this.wellKnownThreadSafety = wellKnownThreadSafety;
  }

  // check instantiations of `@ThreadSafe`s in method references
  @Override
  public Description matchMemberReference(MemberReferenceTree tree, VisitorState state) {
    return checkInvocation(
        tree, ((JCMemberReference) tree).referentType, state, ASTHelpers.getSymbol(tree));
  }

  // check instantiations of `@ThreadSafe`s in method invocations
  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    return checkInvocation(
        tree, ASTHelpers.getType(tree.getMethodSelect()), state, ASTHelpers.getSymbol(tree));
  }

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    // check instantiations of `@ThreadSafe.TypeParameter`s in generic constructor invocations
    checkInvocation(
        tree, ((JCNewClass) tree).constructorType, state, ((JCNewClass) tree).constructor);
    // check instantiations of `@ThreadSafe.TypeParameter`s in class constructor invocations
    ThreadSafeAnalysis analysis = new ThreadSafeAnalysis(this, state, wellKnownThreadSafety);
    Violation info =
        analysis.checkInstantiation(
            ASTHelpers.getSymbol(tree.getIdentifier()).getTypeParameters(),
            ASTHelpers.getType(tree).getTypeArguments());
    if (info.isPresent()) {
      state.reportMatch(buildDescription(tree).setMessage(info.message()).build());
    }
    return NO_MATCH;
  }

  private Description checkInvocation(
      Tree tree, Type methodType, VisitorState state, Symbol symbol) {
    ThreadSafeAnalysis analysis = new ThreadSafeAnalysis(this, state, wellKnownThreadSafety);
    Violation info = analysis.checkInvocation(methodType, symbol);
    if (info.isPresent()) {
      state.reportMatch(buildDescription(tree).setMessage(info.message()).build());
    }
    return NO_MATCH;
  }

  @Override
  public Description matchTypeParameter(TypeParameterTree tree, VisitorState state) {
    Symbol sym = ASTHelpers.getSymbol(tree);
    if (sym == null) {
      return NO_MATCH;
    }
    switch (sym.owner.getKind()) {
      case METHOD:
      case CONSTRUCTOR:
        return NO_MATCH;
      default: // fall out
    }
    ThreadSafeAnalysis analysis = new ThreadSafeAnalysis(this, state, wellKnownThreadSafety);
    if (analysis.hasThreadSafeTypeParameterAnnotation((TypeVariableSymbol) sym)) {
      if (analysis.getThreadSafeAnnotation(sym.owner, state) == null) {
        return buildDescription(tree)
            .setMessage("@ThreadSafe.TypeParameter is only supported on threadsafe classes")
            .build();
      }
    }
    if (analysis.hasThreadSafeElementAnnotation((TypeVariableSymbol) sym)) {
      if (analysis.getThreadSafeAnnotation(sym.owner, state) == null) {
        return buildDescription(tree)
            .setMessage("@ThreadSafe.Element is only supported on threadsafe classes")
            .build();
      }
    }
    return NO_MATCH;
  }

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    ThreadSafeAnalysis analysis = new ThreadSafeAnalysis(this, state, wellKnownThreadSafety);
    if (tree.getSimpleName().length() == 0) {
      // anonymous classes have empty names
      // TODO(cushon): once Java 8 happens, require @ThreadSafe on anonymous classes
      return handleAnonymousClass(tree, state, analysis);
    }

    AnnotationInfo annotation = analysis.getThreadSafeAnnotation(tree, state);
    if (annotation == null) {
      // If the type isn't annotated we don't check for thread safety, but we do
      // report an error if it extends/implements any @ThreadSafe-annotated types.
      return checkSubtype(tree, state);
    }

    // Special-case visiting declarations of known-threadsafe types; these uses
    // of the annotation are "trusted".
    if (wellKnownThreadSafety.getKnownThreadSafeClasses().containsValue(annotation)) {
      return NO_MATCH;
    }

    // Check that the types in containerOf actually exist
    Map<String, TypeVariableSymbol> typarams = new HashMap<>();
    for (TypeParameterTree typaram : tree.getTypeParameters()) {
      typarams.put(
          typaram.getName().toString(), (TypeVariableSymbol) ASTHelpers.getSymbol(typaram));
    }
    SetView<String> difference = Sets.difference(annotation.containerOf(), typarams.keySet());
    if (!difference.isEmpty()) {
      return buildDescription(tree)
          .setMessage(
              String.format(
                  "could not find type(s) referenced by containerOf: %s",
                  Joiner.on("', '").join(difference)))
          .build();
    }
    ImmutableSet<String> threadSafeAndContainer =
        typarams.entrySet().stream()
            .filter(
                e ->
                    annotation.containerOf().contains(e.getKey())
                        && analysis.hasThreadSafeTypeParameterAnnotation(e.getValue()))
            .map(Entry::getKey)
            .collect(toImmutableSet());
    if (!threadSafeAndContainer.isEmpty()) {
      return buildDescription(tree)
          .setMessage(
              String.format(
                  "using both @ThreadSafe.TypeParameter and @ThreadSafe.Element is redundant: %s",
                  Joiner.on("', '").join(threadSafeAndContainer)))
          .build();
    }

    // Main path for @ThreadSafe-annotated types:
    //
    // Check that the fields (including inherited fields) are threadsafe, and
    // validate the type hierarchy superclass.

    Violation info =
        analysis.checkForThreadSafety(
            Optional.of(tree),
            analysis.threadSafeTypeParametersInScope(ASTHelpers.getSymbol(tree)),
            ASTHelpers.getType(tree));

    if (!info.isPresent()) {
      return NO_MATCH;
    }

    String message =
        "type annotated with @ThreadSafe could not be proven threadsafe: " + info.message();
    return buildDescription(tree).setMessage(message).build();
  }

  // Anonymous classes

  /** Check anonymous implementations of {@code @ThreadSafe} types. */
  private Description handleAnonymousClass(
      ClassTree tree, VisitorState state, ThreadSafeAnalysis analysis) {
    ClassSymbol sym = ASTHelpers.getSymbol(tree);
    if (sym == null) {
      return NO_MATCH;
    }
    Type superType = threadSafeSupertype(sym, state);
    if (superType == null) {
      return NO_MATCH;
    }
    // We don't need to check that the superclass has a threadsafe instantiation.
    // The anonymous instance can only be referred to using a superclass type, so
    // the type arguments will be validated at any type use site where we care about
    // the instance's threadsafety.
    ImmutableSet<String> typarams = analysis.threadSafeTypeParametersInScope(sym);
    Violation info =
        analysis.areFieldsThreadSafe(Optional.of(tree), typarams, ASTHelpers.getType(tree));
    if (!info.isPresent()) {
      return NO_MATCH;
    }
    String reason = Joiner.on(", ").join(info.path());
    String message =
        String.format(
            "Class extends @ThreadSafe type %s, but is not threadsafe: %s", superType, reason);
    return buildDescription(tree).setMessage(message).build();
  }

  // Strong behavioural subtyping

  /** Check for classes without {@code @ThreadSafe} that have threadsafe supertypes. */
  private Description checkSubtype(ClassTree tree, VisitorState state) {
    ClassSymbol sym = ASTHelpers.getSymbol(tree);
    if (sym == null) {
      return NO_MATCH;
    }
    Type superType = threadSafeSupertype(sym, state);
    if (superType == null) {
      return NO_MATCH;
    }
    if (ASTHelpers.hasAnnotation(sym, Immutable.class, state)) {
      // If the superclass is @ThreadSafe and the subclass is @Immutable, then the subclass is
      // effectively also @ThreadSafe, and we defer to the @Immutable plugin.
      return NO_MATCH;
    }
    String message =
        String.format(
            "Class extends @ThreadSafe type %s, but is not annotated as threadsafe", superType);
    SuggestedFix.Builder fix = SuggestedFix.builder();
    String typeName = SuggestedFixes.qualifyType(state, fix, ThreadSafe.class.getName());
    fix.prefixWith(tree, "@" + typeName + " ");
    return buildDescription(tree).setMessage(message).addFix(fix.build()).build();
  }

  /**
   * Returns the type of the first superclass or superinterface in the hierarchy annotated with
   * {@code @ThreadSafe}, or {@code null} if no such super type exists.
   */
  private static @Nullable Type threadSafeSupertype(Symbol sym, VisitorState state) {
    for (Type superType : state.getTypes().closure(sym.type)) {
      if (superType.asElement().equals(sym)) {
        continue;
      }
      // Don't use getThreadSafeAnnotation here: subtypes of trusted types are
      // also trusted, only check for explicitly annotated supertypes.
      if (ASTHelpers.hasAnnotation(superType.tsym, ThreadSafe.class, state)) {
        return superType;
      }
      // We currently trust that @interface annotations are threadsafe, but don't enforce that
      // custom interface implementations are also threadsafe. That means the check can be
      // defeated by writing a custom mutable annotation implementation, and passing it around
      // using the superclass type.
      //
      // TODO(b/25630189): fix this
      //
      // if (superType.tsym.getKind() == ElementKind.ANNOTATION_TYPE) {
      //   return superType;
      // }
    }
    return null;
  }
}
