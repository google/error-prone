package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.auto.value.AutoValue;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.dataflow.nullnesspropagation.Nullness;
import com.google.errorprone.dataflow.nullnesspropagation.NullnessAnalysis;
import com.google.errorprone.dataflow.nullnesspropagation.NullnessAnalysis.MethodInfo;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.meta.TypeQualifierDefault;
import javax.annotation.meta.TypeQualifierNickname;
import javax.annotation.meta.When;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;

@BugPattern(
    name = "NonnullParameter",
    summary = "Passing a null value to a nonnull parameter",
    explanation = "The JSR 305 @Nonnull marks types which should never be null. This error is"
        + " triggered on method invocations where a null value is passed to a parameter annotated"
        + " with @Nonnull. This checker also takes into account @TypeQualifierDefaults such as"
        + " @ParametersAreNonnullByDefault.",
    category = JDK, maturity = EXPERIMENTAL, severity = ERROR)
@ParametersAreNonnullByDefault
public class NonnullParameter extends BugChecker implements MethodInvocationTreeMatcher,
    MethodTreeMatcher, ClassTreeMatcher {
  private static final String NULL_VALUES = "You cannot pass a null value to a nonnull parameter"
      + " at position %s.";
  private static final String MULTIPLE_NULLNESS = "More than one nullness annotation %s cannot"
      + " be applied to the same %s.";
  private static final String NONNULL_VOID = "%s cannot be applied to Void types.";

  private static final Predicate<MethodInfo> NONNULL_METHOD_PREDICATE = new Predicate<MethodInfo>() {
    @Override
    public boolean apply(MethodInfo input) {
      return input.annotations().contains(Nonnull.class.getName());
    }
  };
  private final NullnessAnalysis nullnessAnalysis = new NullnessAnalysis(NONNULL_METHOD_PREDICATE);

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    MethodSymbol method = ASTHelpers.getSymbol(tree);
    List<VarSymbol> parameters = method.getParameters();
    List<? extends ExpressionTree> arguments = tree.getArguments();

    Preconditions.checkState(arguments.size() >= parameters.size() - 1);

    Optional<Boolean> defaultNonnull = areParametersNonnullByDefault(method);
    boolean nonnullByDefault = defaultNonnull.isPresent() && defaultNonnull.get();

    List<Integer> mismatchingPositions = new ArrayList<>();
    for (int i = 0; i < parameters.size() - 1; i++) {
      if (matchParameterAndArgument(parameters.get(i), arguments.get(i), state, nonnullByDefault)) {
        mismatchingPositions.add(i);
      }
    }

    VarSymbol lastParameter = parameters.get(parameters.size() - 1);
    for (int i = parameters.size() - 1; i < arguments.size(); i++) {
      if (matchParameterAndArgument(lastParameter, arguments.get(i), state, nonnullByDefault)) {
        mismatchingPositions.add(i);
      }
    }

    if (mismatchingPositions.isEmpty()) {
      return Description.NO_MATCH;
    } else {
      return buildDescription(tree).setMessage(String.format(NULL_VALUES, mismatchingPositions))
          .build();
    }
  }

  /*
   * Returns true if the parameter should be nonnull but the argument is null
   */
  private boolean matchParameterAndArgument(VarSymbol parameter, ExpressionTree argument,
      VisitorState state, boolean nonnullByDefault) {
    Optional<Boolean> paramIsNonnull = isParameterNonnull(parameter);
    if (paramIsNonnull.isPresent()) {
      if (paramIsNonnull.get() && isArgumentNull(argument, state)) {
        return true;
      }
    } else if (nonnullByDefault && !ASTHelpers.isVoidType(parameter.asType(), state)
        && isArgumentNull(argument, state)) {
      return true;
    }

    return false;
  }

  private Optional<Boolean> isParameterNonnull(VarSymbol parameter) {
    return isParameterNonnull(getTypeQualifiers(parameter, Nonnull.class));
  }

  private Optional<Boolean> isParameterNonnull(Set<TypeQualifierAnnotation<Nonnull>> annotations) {
    if (annotations.size() != 1) {
      // This should only happen if annotations is empty.
      // If there are more than one nullness annotations, then an error
      // should be generated anyways. Let's just ignore it.
      return Optional.absent();
    } else {
      return Optional.of(annotations.iterator().next().getAnnotation().when() == When.ALWAYS);
    }
  }

  private <A extends Annotation> Set<TypeQualifierAnnotation<A>> getTypeQualifiers(Element element,
      Class<A> annotationType) {
    return getTypeQualifiers(element, null, annotationType);
  }

  private <A extends Annotation> Set<TypeQualifierAnnotation<A>> getTypeQualifiers(
      AnnotationMirror original, Class<A> annotationType) {
    return getTypeQualifiers(original.getAnnotationType().asElement(), original, annotationType);
  }

  private <A extends Annotation> Set<TypeQualifierAnnotation<A>> getTypeQualifiers(Element element,
      @Nullable final AnnotationMirror original, Class<A> annotationType) {
    Set<TypeQualifierAnnotation<A>> annotations = new LinkedHashSet<>();
    // Check for a direct annotation
    A annotation = element.getAnnotation(annotationType);
    if (annotation != null) {
      AnnotationMirror directOriginal = original == null ? findAnnotationMirror(
          element.getAnnotationMirrors(), annotation) : original;
      annotations.add(TypeQualifierAnnotation.create(annotation, directOriginal));
    }

    // Check TypeQualifierNicknames
    for (AnnotationMirror mirror : getAnnotatedAnnotations(element, TypeQualifierNickname.class)) {
      Element mirrorType = mirror.getAnnotationType().asElement();
      // Recursively check for TypeQualifierNicknames to catch indirect annotations
      // Ex:
      // @Nonnull
      // @TypeQualifierNickname
      // @interface Foo {}
      //
      // @Foo
      // @TypeQualifierNickname
      // @interface Bar{}
      //
      // void baz(@Bar Object param) {}
      //
      // @Bar inherits @Foo's @Nonnull annotation
      annotations.addAll(getTypeQualifiers(mirrorType, original == null ? mirror : original,
          annotationType));
    }

    return annotations;
  }

  private AnnotationMirror findAnnotationMirror(List<? extends AnnotationMirror> mirrors,
      Annotation annotation) {
    for (AnnotationMirror mirror : mirrors) {
      if (annotation.annotationType()
          .getCanonicalName()
          .equals(ASTHelpers.getQualifiedName(mirror.getAnnotationType().asElement()))) {
        return mirror;
      }
    }

    return null;
  }

  public Set<? extends AnnotationMirror> getAnnotatedAnnotations(Element element,
      Class<? extends Annotation> metaAnnotationType) {
    return getAnnotatedAnnotations(element, metaAnnotationType, Predicates.alwaysTrue());
  }

  /*
   * Returns a list of annotations on {@code element} which are themselves annotated with {@code
   * metaAnnotationType}. The {@code metaAnnotations} must pass the given predicate.
   *
   * Example:
   *
   * @TypeQualifierNickname public @interface Foo {}
   *
   * @Foo public void bar() {}
   *
   * findMetaAnnotatedAnnotations(bar, @TypeQualifierNickname) = [@Foo]
   */
  private <A extends Annotation> Set<? extends AnnotationMirror> getAnnotatedAnnotations(
      Element element, Class<A> metaAnnotationType, Predicate<? super A> metaAnnotationPredicate) {
    Target target = metaAnnotationType.getAnnotation(Target.class);
    Preconditions.checkState(
        target == null || Arrays.asList(target.value()).contains(ElementType.ANNOTATION_TYPE),
        "metaAnnotationType must be able to target annotations");

    Set<AnnotationMirror> annotations = new LinkedHashSet<>();
    for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
      Element type = mirror.getAnnotationType().asElement();

      A metaAnnotation = type.getAnnotation(metaAnnotationType);
      if (metaAnnotation != null && metaAnnotationPredicate.apply(metaAnnotation)) {
        annotations.add(mirror);
      }
    }

    return annotations;
  }

  private Optional<Boolean> areParametersNonnullByDefault(MethodSymbol method) {
    return isParameterNonnull(getTypeQualifierDefaults(method, ElementType.PARAMETER, Nonnull.class));
  }

  /*
   * Returns a list of annotations that are present as {@link TypeQualifierDefault}s on the closest
   * enclosing scope.
   */
  private <A extends Annotation> Set<TypeQualifierAnnotation<A>> getTypeQualifierDefaults(
      Element element, final ElementType elementType, Class<A> annotationType) {
    while (element != null) {
      Set<TypeQualifierAnnotation<A>> defaults = getImmediateTypeQualifierDefaults(element,
          elementType, annotationType);

      if (!defaults.isEmpty()) {
        return defaults;
      }

      element = element.getEnclosingElement();
    }

    return Collections.emptySet();
  }

  /*
   * Get the TypeQualifierDefaults on the element itself but none of its parents.
   */
  private <A extends Annotation> Set<TypeQualifierAnnotation<A>> getImmediateTypeQualifierDefaults(
      Element element, final ElementType elementType, Class<A> annotationType) {
    Predicate<TypeQualifierDefault> typeQualifierPredicate = new Predicate<TypeQualifierDefault>() {
      @Override
      public boolean apply(TypeQualifierDefault input) {
        return Arrays.asList(input.value()).contains(elementType);
      }
    };

    Set<TypeQualifierAnnotation<A>> defaults = new LinkedHashSet<>();
    for (AnnotationMirror mirror : getAnnotatedAnnotations(element, TypeQualifierDefault.class,
        typeQualifierPredicate)) {
      defaults.addAll(getTypeQualifiers(mirror, annotationType));
    }

    return defaults;
  }

  private boolean isArgumentNull(ExpressionTree argument, VisitorState state) {
    return ASTHelpers.getNullnessValue(argument, state, nullnessAnalysis) == Nullness.NULL;
  }

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    Optional<Description> desc = checkMultipleNullnessAnnotations(tree, tree.getModifiers(), state,
        "method");
    if (desc.isPresent()) {
      return desc.get();
    }

    for (VariableTree variableTree : tree.getParameters()) {
      desc = checkMultipleNullnessAnnotations(variableTree, variableTree.getModifiers(), state,
          "parameter");
      if (desc.isPresent()) {
        return desc.get();
      }

      desc = checkNonnullVoidType(variableTree, state);
      if (desc.isPresent()) {
        return desc.get();
      }
    }

    return Description.NO_MATCH;
  }

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    return checkMultipleNullnessAnnotations(tree, tree.getModifiers(), state, "class").or(
        Description.NO_MATCH);
  }

  private Optional<Description> checkMultipleNullnessAnnotations(Tree tree,
      ModifiersTree modifiers, VisitorState state, String typeOfTree) {
    Symbol symbol = ASTHelpers.getSymbol(tree);
    Set<TypeQualifierAnnotation<Nonnull>> nullnessAnnotations = getTypeQualifiers(symbol,
        Nonnull.class);
    if (getAnnotations(nullnessAnnotations).size() > 1) {
      return Optional.of(createMultipleNullnessDescription(tree, modifiers, nullnessAnnotations,
          typeOfTree));
    }

    nullnessAnnotations = getImmediateTypeQualifierDefaults(symbol, ElementType.PARAMETER,
        Nonnull.class);

    if (getAnnotations(nullnessAnnotations).size() > 1) {
      return Optional.of(createMultipleNullnessDescription(tree, modifiers, nullnessAnnotations,
          typeOfTree));
    }

    return Optional.absent();
  }

  private <A extends Annotation> Set<A> getAnnotations(
      Set<TypeQualifierAnnotation<A>> typeQualifiers) {
    Set<A> annotations = new LinkedHashSet<>(typeQualifiers.size());
    for (TypeQualifierAnnotation<? extends A> qualifier : typeQualifiers) {
      annotations.add(qualifier.getAnnotation());
    }
    return annotations;
  }

  private Description createMultipleNullnessDescription(Tree tree, ModifiersTree modifiers,
      Set<TypeQualifierAnnotation<Nonnull>> nullnessAnnotations, String typeOfTree) {
    List<AnnotationMirror> originals = new ArrayList<>(nullnessAnnotations.size());
    for (TypeQualifierAnnotation<Nonnull> nullnessAnnotation : nullnessAnnotations) {
      originals.add(nullnessAnnotation.getOriginalAnnotation());
    }

    Description.Builder desc = buildDescription(tree).setMessage(
        String.format(MULTIPLE_NULLNESS, originals, typeOfTree));
    SuggestedFix.Builder fix = SuggestedFix.builder();
    List<? extends AnnotationTree> annotations = modifiers.getAnnotations();
    for (int i = 1; i < nullnessAnnotations.size(); i++) {
      AnnotationTree annotationTree = findAnnotationTree(annotations, originals.get(i));
      fix.delete(annotationTree);
    }

    return desc.addFix(fix.build()).build();
  }

  private Optional<Description> checkNonnullVoidType(VariableTree paramTree, VisitorState state) {
    VarSymbol param = ASTHelpers.getSymbol(paramTree);
    if (ASTHelpers.isVoidType(param.asType(), state)) {
      Set<TypeQualifierAnnotation<Nonnull>> nullnessAnnotations = getTypeQualifiers(param,
          Nonnull.class);
      Optional<Boolean> nonnull = isParameterNonnull(nullnessAnnotations);
      if (nonnull.isPresent() && nonnull.get()) {
        AnnotationMirror original = nullnessAnnotations.iterator().next().getOriginalAnnotation();
        AnnotationTree annotationTree = findAnnotationTree(paramTree.getModifiers()
            .getAnnotations(), original);
        return Optional.of(buildDescription(paramTree).setMessage(
            String.format(NONNULL_VOID, original))
            .addFix(SuggestedFix.delete(annotationTree))
            .build());
      }
    }

    return Optional.absent();
  }

  private AnnotationTree findAnnotationTree(List<? extends AnnotationTree> annotations,
      AnnotationMirror annotation) {
    for (AnnotationTree tree : annotations) {
      if (ASTHelpers.getAnnotationMirror(tree).equals(annotation)) {
        return tree;
      }
    }

    return null;
  }

  @AutoValue
  abstract static class TypeQualifierAnnotation<A extends Annotation> {
    public abstract A getAnnotation();

    public abstract AnnotationMirror getOriginalAnnotation();

    public static <A extends Annotation> TypeQualifierAnnotation<A> create(A annotation,
        AnnotationMirror original) {
      return new AutoValue_NonnullParameter_TypeQualifierAnnotation<>(annotation, original);
    }
  }
}
