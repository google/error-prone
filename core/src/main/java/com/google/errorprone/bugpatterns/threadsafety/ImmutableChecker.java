/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.Immutable;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.threadsafety.ImmutableAnalysis.Violation;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.TypeVariableSymbol;
import com.sun.tools.javac.code.Type;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "Immutable",
  summary = "Type declaration annotated with @Immutable is not immutable",
  category = JDK,
  severity = ERROR,
  documentSuppression = false
)
public class ImmutableChecker extends BugChecker implements BugChecker.ClassTreeMatcher {

  private final WellKnownMutability wellKnownMutability;

  @Deprecated // Used reflectively, but you should pass in ErrorProneFlags to get custom mutability
  public ImmutableChecker() {
    this(ErrorProneFlags.empty());
  }

  public ImmutableChecker(ErrorProneFlags flags) {
    this.wellKnownMutability = WellKnownMutability.fromFlags(flags);
  }

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    ImmutableAnalysis analysis =
        new ImmutableAnalysis(
            this,
            state,
            wellKnownMutability,
            "@Immutable classes cannot have non-final fields",
            "@Immutable class has mutable field");
    if (tree.getSimpleName().length() == 0) {
      // anonymous classes have empty names
      // TODO(cushon): once Java 8 happens, require @Immutable on anonymous classes
      return handleAnonymousClass(tree, state, analysis);
    }

    AnnotationInfo annotation = analysis.getImmutableAnnotation(tree, state);
    if (annotation == null) {
      // If the type isn't annotated we don't check for immutability, but we do
      // report an error if it extends/implements any @Immutable-annotated types.
      return checkSubtype(tree, state);
    }

    // Special-case visiting declarations of known-immutable types; these uses
    // of the annotation are "trusted".
    if (wellKnownMutability.getKnownImmutableClasses().containsValue(annotation)) {
      return Description.NO_MATCH;
    }

    // Check that the types in containerOf actually exist
    Set<String> typarams = new HashSet<>();
    for (TypeParameterTree typaram : tree.getTypeParameters()) {
      typarams.add(typaram.getName().toString());
    }
    SetView<String> difference = Sets.difference(annotation.containerOf(), typarams);
    if (!difference.isEmpty()) {
      String message =
          String.format(
              "could not find type(s) referenced by containerOf: %s",
              Joiner.on("', '").join(difference));
      return buildDescription(tree).setMessage(message).build();
    }

    // Main path for @Immutable-annotated types:
    //
    // Check that the fields (including inherited fields) are immutable, and
    // validate the type hierarchy superclass.

    Violation info =
        analysis.checkForImmutability(
            Optional.of(tree),
            immutableTypeParametersInScope(ASTHelpers.getSymbol(tree), state, analysis),
            ASTHelpers.getType(tree));

    if (!info.isPresent()) {
      return Description.NO_MATCH;
    }

    String message =
        "type annotated with @Immutable could not be proven immutable: " + info.message();
    return buildDescription(tree).setMessage(message).build();
  }

  // Anonymous classes

  /** Check anonymous implementations of {@code @Immutable} types. */
  private Description handleAnonymousClass(
      ClassTree tree, VisitorState state, ImmutableAnalysis analysis) {
    ClassSymbol sym = ASTHelpers.getSymbol(tree);
    if (sym == null) {
      return Description.NO_MATCH;
    }
    Type superType = immutableSupertype(sym, state);
    if (superType == null) {
      return Description.NO_MATCH;
    }
    // We don't need to check that the superclass has an immutable instantiation.
    // The anonymous instance can only be referred to using a superclass type, so
    // the type arguments will be validated at any type use site where we care about
    // the instance's immutability.
    //
    // Also, we have no way to express something like:
    //
    // public static <@Immutable T> ImmutableBox<T> create(T t) {
    //   return new ImmutableBox<>(t);
    // }
    ImmutableSet<String> typarams = immutableTypeParametersInScope(sym, state, analysis);
    Violation info =
        analysis.areFieldsImmutable(Optional.of(tree), typarams, ASTHelpers.getType(tree));
    if (!info.isPresent()) {
      return Description.NO_MATCH;
    }
    String reason = Joiner.on(", ").join(info.path());
    String message =
        String.format(
            "Class extends @Immutable type %s, but is not immutable: %s", superType, reason);
    return buildDescription(tree).setMessage(message).build();
  }

  // Strong behavioural subtyping

  /** Check for classes without {@code @Immutable} that have immutable supertypes. */
  private Description checkSubtype(ClassTree tree, VisitorState state) {
    ClassSymbol sym = ASTHelpers.getSymbol(tree);
    if (sym == null) {
      return Description.NO_MATCH;
    }
    Type superType = immutableSupertype(sym, state);
    if (superType == null) {
      return Description.NO_MATCH;
    }
    String message =
        String.format(
            "Class extends @Immutable type %s, but is not annotated as immutable", superType);
    Fix fix =
        SuggestedFix.builder()
            .prefixWith(tree, "@Immutable ")
            .addImport(Immutable.class.getName())
            .build();
    return buildDescription(tree).setMessage(message).addFix(fix).build();
  }

  /**
   * Returns the type of the first superclass or superinterface in the hierarchy annotated with
   * {@code @Immutable}, or {@code null} if no such super type exists.
   */
  private static Type immutableSupertype(Symbol sym, VisitorState state) {
    for (Type superType : state.getTypes().closure(sym.type)) {
      if (superType.equals(sym.type)) {
        continue;
      }
      // Don't use getImmutableAnnotation here: subtypes of trusted types are
      // also trusted, only check for explicitly annotated supertypes.
      if (ASTHelpers.hasAnnotation(superType.tsym, Immutable.class, state)) {
        return superType;
      }
      // We currently trust that @interface annotations are immutable, but don't enforce that
      // custom interface implementations are also immutable. That means the check can be
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

  /**
   * Gets the set of in-scope immutable type parameters from the containerOf specs on
   * {@code @Immutable} annotations.
   *
   * <p>Usually only the immediately enclosing declaration is searched, but it's possible to have
   * cases like:
   *
   * <pre>
   * @Immutable(containerOf="T") class C<T> {
   *   class Inner extends ImmutableCollection<T> {}
   * }
   * </pre>
   */
  private static ImmutableSet<String> immutableTypeParametersInScope(
      Symbol sym, VisitorState state, ImmutableAnalysis analysis) {
    if (sym == null) {
      return ImmutableSet.of();
    }
    ImmutableSet.Builder<String> result = ImmutableSet.builder();
    OUTER:
    for (Symbol s = sym; s.owner != null; s = s.owner) {
      switch (s.getKind()) {
        case INSTANCE_INIT:
          continue;
        case PACKAGE:
          break OUTER;
        default:
          break;
      }
      AnnotationInfo annotation = analysis.getImmutableAnnotation(s, state);
      if (annotation == null) {
        continue;
      }
      for (TypeVariableSymbol typaram : s.getTypeParameters()) {
        String name = typaram.getSimpleName().toString();
        if (annotation.containerOf().contains(name)) {
          result.add(name);
        }
      }
      if (s.isStatic()) {
        break;
      }
    }
    return result.build();
  }
}
