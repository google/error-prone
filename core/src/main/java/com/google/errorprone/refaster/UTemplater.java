/*
 * Copyright 2013 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.refaster;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;
import com.google.errorprone.SubContext;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.refaster.annotation.Matches;
import com.google.errorprone.refaster.annotation.NotMatches;
import com.google.errorprone.refaster.annotation.OfKind;
import com.google.errorprone.refaster.annotation.Repeated;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ArrayTypeTree;
import com.sun.source.tree.AssertTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.BreakTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ContinueTree;
import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.EmptyStatementTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.IntersectionTypeTree;
import com.sun.source.tree.LabeledStatementTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.UnionTypeTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.tree.WildcardTree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.tools.javac.code.Attribute.Compound;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ArrayType;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.code.Type.ForAll;
import com.sun.tools.javac.code.Type.IntersectionClassType;
import com.sun.tools.javac.code.Type.MethodType;
import com.sun.tools.javac.code.Type.TypeVar;
import com.sun.tools.javac.code.Type.WildcardType;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.model.AnnotationProxyMaker;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCLambda;
import com.sun.tools.javac.tree.JCTree.JCModifiers;
import com.sun.tools.javac.tree.JCTree.JCPrimitiveTypeTree;
import com.sun.tools.javac.util.Context;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;

/**
 * Converts a type-checked syntax tree to a portable {@code UTree} template.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
public class UTemplater extends SimpleTreeVisitor<Tree, Void> {
  /**
   * Context key to indicate that templates should be treated as BlockTemplates, regardless of their
   * structure.
   */
  public static final Context.Key<Boolean> REQUIRE_BLOCK_KEY = new Context.Key<>();

  /**
   * Returns a template based on a method. One-line methods starting with a {@code return} statement
   * are guessed to be expression templates, and all other methods are guessed to be block
   * templates.
   */
  public static Template<?> createTemplate(Context context, MethodTree decl) {
    MethodSymbol declSym = ASTHelpers.getSymbol(decl);
    ImmutableClassToInstanceMap<Annotation> annotations = UTemplater.annotationMap(declSym);
    ImmutableMap<String, VarSymbol> freeExpressionVars = freeExpressionVariables(decl);
    Context subContext = new SubContext(context);
    final UTemplater templater = new UTemplater(freeExpressionVars, subContext);
    ImmutableMap<String, UType> expressionVarTypes =
        ImmutableMap.copyOf(
            Maps.transformValues(
                freeExpressionVars, (VarSymbol sym) -> templater.template(sym.type)));

    UType genericType = templater.template(declSym.type);
    List<UTypeVar> typeParameters;
    UMethodType methodType;
    if (genericType instanceof UForAll) {
      UForAll forAllType = (UForAll) genericType;
      typeParameters = forAllType.getTypeVars();
      methodType = (UMethodType) forAllType.getQuantifiedType();
    } else if (genericType instanceof UMethodType) {
      typeParameters = ImmutableList.of();
      methodType = (UMethodType) genericType;
    } else {
      throw new IllegalArgumentException(
          "Expected genericType to be either a ForAll or a UMethodType, but was " + genericType);
    }

    List<? extends StatementTree> bodyStatements = decl.getBody().getStatements();
    if (bodyStatements.size() == 1
        && Iterables.getOnlyElement(bodyStatements).getKind() == Kind.RETURN
        && context.get(REQUIRE_BLOCK_KEY) == null) {
      ExpressionTree expression =
          ((ReturnTree) Iterables.getOnlyElement(bodyStatements)).getExpression();
      return ExpressionTemplate.create(
          annotations,
          typeParameters,
          expressionVarTypes,
          templater.template(expression),
          methodType.getReturnType());
    } else {
      List<UStatement> templateStatements = new ArrayList<>();
      for (StatementTree statement : bodyStatements) {
        templateStatements.add(templater.template(statement));
      }
      return BlockTemplate.create(
          annotations, typeParameters, expressionVarTypes, templateStatements);
    }
  }

  public static ImmutableMap<String, VarSymbol> freeExpressionVariables(
      MethodTree templateMethodDecl) {
    ImmutableMap.Builder<String, VarSymbol> builder = ImmutableMap.builder();
    for (VariableTree param : templateMethodDecl.getParameters()) {
      builder.put(param.getName().toString(), ASTHelpers.getSymbol(param));
    }
    return builder.build();
  }

  private final ImmutableMap<String, VarSymbol> freeVariables;
  private final Context context;

  public UTemplater(Map<String, VarSymbol> freeVariables, Context context) {
    this.freeVariables = ImmutableMap.copyOf(freeVariables);
    this.context = context;
  }

  UTemplater(Context context) {
    this(ImmutableMap.<String, VarSymbol>of(), context);
  }

  public Tree template(Tree tree) {
    return tree.accept(this, null);
  }

  @Nullable
  private List<Tree> templateTrees(@Nullable Iterable<? extends Tree> trees) {
    if (trees == null) {
      return null;
    }
    ImmutableList.Builder<Tree> builder = ImmutableList.builder();
    for (Tree tree : trees) {
      builder.add(template(tree));
    }
    return builder.build();
  }

  private static <T> ImmutableList<T> cast(Iterable<?> elements, Class<T> clazz) {
    ImmutableList.Builder<T> builder = ImmutableList.builder();
    for (Object element : elements) {
      builder.add(clazz.cast(element));
    }
    return builder.build();
  }

  @Override
  public UMethodDecl visitMethod(MethodTree decl, Void v) {
    return UMethodDecl.create(
        visitModifiers(decl.getModifiers(), null),
        decl.getName(),
        templateType(decl.getReturnType()),
        cast(templateStatements(decl.getParameters()), UVariableDecl.class),
        templateExpressions(decl.getThrows()),
        (UBlock) template(decl.getBody()));
  }

  @Override
  public UModifiers visitModifiers(ModifiersTree modifiers, Void v) {
    return UModifiers.create(
        ((JCModifiers) modifiers).flags,
        cast(templateExpressions(modifiers.getAnnotations()), UAnnotation.class));
  }

  public UExpression template(ExpressionTree tree) {
    return (UExpression) tree.accept(this, null);
  }

  @Nullable
  private List<UExpression> templateExpressions(
      @Nullable Iterable<? extends ExpressionTree> expressions) {
    if (expressions == null) {
      return null;
    }
    ImmutableList.Builder<UExpression> builder = ImmutableList.builder();
    for (ExpressionTree expression : expressions) {
      builder.add(template(expression));
    }
    return builder.build();
  }

  public UExpression templateType(Tree tree) {
    checkArgument(
        tree instanceof ExpressionTree,
        "Trees representing types are expected to implement ExpressionTree, but %s does not",
        tree);
    return template((ExpressionTree) tree);
  }

  @Nullable
  private List<UExpression> templateTypeExpressions(@Nullable Iterable<? extends Tree> types) {
    if (types == null) {
      return null;
    }
    ImmutableList.Builder<UExpression> builder = ImmutableList.builder();
    for (Tree type : types) {
      builder.add(templateType(type));
    }
    return builder.build();
  }

  @Override
  public UInstanceOf visitInstanceOf(InstanceOfTree tree, Void v) {
    return UInstanceOf.create(template(tree.getExpression()), templateType(tree.getType()));
  }

  @Override
  public UPrimitiveTypeTree visitPrimitiveType(PrimitiveTypeTree tree, Void v) {
    return UPrimitiveTypeTree.create(((JCPrimitiveTypeTree) tree).typetag);
  }

  @Override
  public ULiteral visitLiteral(LiteralTree tree, Void v) {
    return ULiteral.create(tree.getKind(), tree.getValue());
  }

  @Override
  public UParens visitParenthesized(ParenthesizedTree tree, Void v) {
    return UParens.create(template(tree.getExpression()));
  }

  @Override
  public UAssign visitAssignment(AssignmentTree tree, Void v) {
    return UAssign.create(template(tree.getVariable()), template(tree.getExpression()));
  }

  @Override
  public UArrayAccess visitArrayAccess(ArrayAccessTree tree, Void v) {
    return UArrayAccess.create(template(tree.getExpression()), template(tree.getIndex()));
  }

  @Override
  public UAnnotation visitAnnotation(AnnotationTree tree, Void v) {
    return UAnnotation.create(
        templateType(tree.getAnnotationType()), templateExpressions(tree.getArguments()));
  }

  @Override
  public UAnnotatedType visitAnnotatedType(AnnotatedTypeTree tree, Void v) {
    return UAnnotatedType.create(
        cast(templateExpressions(tree.getAnnotations()), UAnnotation.class),
        template(tree.getUnderlyingType()));
  }

  @Override
  public UExpression visitMemberSelect(MemberSelectTree tree, Void v) {
    Symbol sym = ASTHelpers.getSymbol(tree);
    if (sym instanceof ClassSymbol) {
      return UClassIdent.create((ClassSymbol) sym);
    } else if (sym.isStatic()) {
      ExpressionTree selected = tree.getExpression();
      checkState(
          ASTHelpers.getSymbol(selected) instanceof ClassSymbol,
          "Refaster cannot match static methods used on instances");
      return staticMember(sym);
    }
    return UMemberSelect.create(
        template(tree.getExpression()), tree.getIdentifier(), template(sym.type));
  }

  private UStaticIdent staticMember(Symbol symbol) {
    return UStaticIdent.create(
        (ClassSymbol) symbol.getEnclosingElement(),
        symbol.getSimpleName(),
        template(symbol.asType()));
  }

  private static final UStaticIdent ANY_OF;
  private static final UStaticIdent IS_INSTANCE;
  private static final UStaticIdent CLAZZ;
  private static final UStaticIdent NEW_ARRAY;
  private static final UStaticIdent ENUM_VALUE_OF;
  private static final UStaticIdent AS_VARARGS;

  static {
    UTypeVar tVar = UTypeVar.create("T");
    ANY_OF =
        UStaticIdent.create(
            Refaster.class.getCanonicalName(),
            "anyOf",
            UForAll.create(
                ImmutableList.of(tVar), UMethodType.create(tVar, UArrayType.create(tVar))));
    IS_INSTANCE =
        UStaticIdent.create(
            Refaster.class.getCanonicalName(),
            "isInstance",
            UForAll.create(
                ImmutableList.of(tVar),
                UMethodType.create(
                    UPrimitiveType.BOOLEAN, UClassType.create(Object.class.getCanonicalName()))));
    CLAZZ =
        UStaticIdent.create(
            Refaster.class.getCanonicalName(),
            "clazz",
            UForAll.create(
                ImmutableList.of(tVar),
                UMethodType.create(UClassType.create(Class.class.getCanonicalName(), tVar))));
    NEW_ARRAY =
        UStaticIdent.create(
            Refaster.class.getCanonicalName(),
            "newArray",
            UForAll.create(
                ImmutableList.of(tVar),
                UMethodType.create(UArrayType.create(tVar), UPrimitiveType.INT)));
    UTypeVar eVar =
        UTypeVar.create(
            "E", UClassType.create(Enum.class.getCanonicalName(), UTypeVar.create("E")));
    ENUM_VALUE_OF =
        UStaticIdent.create(
            Refaster.class.getCanonicalName(),
            "enumValueOf",
            UForAll.create(
                ImmutableList.of(eVar),
                UMethodType.create(eVar, UClassType.create(String.class.getCanonicalName()))));
    AS_VARARGS =
        UStaticIdent.create(
            Refaster.class.getCanonicalName(),
            "asVarargs",
            UForAll.create(
                ImmutableList.of(tVar), UMethodType.create(UArrayType.create(tVar), tVar)));
  }

  private static Tree getSingleExplicitTypeArgument(MethodInvocationTree tree) {
    if (tree.getTypeArguments().isEmpty()) {
      throw new IllegalArgumentException(
          "Methods in the Refaster class must be invoked with "
              + "an explicit type parameter; for example, 'Refaster.<T>isInstance(o)'.");
    }
    return Iterables.getOnlyElement(tree.getTypeArguments());
  }

  static <T, U extends Unifiable<? super T>> boolean anyMatch(
      U toUnify, T target, Unifier unifier) {
    return toUnify.unify(target, unifier).first().isPresent();
  }

  @Override
  public UExpression visitMethodInvocation(MethodInvocationTree tree, Void v) {
    if (anyMatch(ANY_OF, tree.getMethodSelect(), new Unifier(context))) {
      return UAnyOf.create(templateExpressions(tree.getArguments()));
    } else if (anyMatch(IS_INSTANCE, tree.getMethodSelect(), new Unifier(context))) {
      return UInstanceOf.create(
          template(Iterables.getOnlyElement(tree.getArguments())),
          templateType(getSingleExplicitTypeArgument(tree)));
    } else if (anyMatch(CLAZZ, tree.getMethodSelect(), new Unifier(context))) {
      Tree typeArg = getSingleExplicitTypeArgument(tree);
      return UMemberSelect.create(
          templateType(typeArg),
          "class",
          UClassType.create("java.lang.Class", template(((JCTree) typeArg).type)));
    } else if (anyMatch(NEW_ARRAY, tree.getMethodSelect(), new Unifier(context))) {
      Tree typeArg = getSingleExplicitTypeArgument(tree);
      ExpressionTree lengthArg = Iterables.getOnlyElement(tree.getArguments());
      return UNewArray.create(templateType(typeArg), ImmutableList.of(template(lengthArg)), null);
    } else if (anyMatch(ENUM_VALUE_OF, tree.getMethodSelect(), new Unifier(context))) {
      Tree typeArg = getSingleExplicitTypeArgument(tree);
      ExpressionTree strArg = Iterables.getOnlyElement(tree.getArguments());
      return UMethodInvocation.create(
          UMemberSelect.create(
              templateType(typeArg),
              "valueOf",
              UMethodType.create(
                  template(((JCTree) typeArg).type), UClassType.create("java.lang.String"))),
          template(strArg));
    } else if (anyMatch(AS_VARARGS, tree.getMethodSelect(), new Unifier(context))) {
      ExpressionTree arg = Iterables.getOnlyElement(tree.getArguments());
      checkArgument(
          ASTHelpers.hasAnnotation(
              ASTHelpers.getSymbol(arg), Repeated.class, new VisitorState(context)));
      return template(arg);
    }
    Map<MethodSymbol, PlaceholderMethod> placeholderMethods =
        context.get(RefasterRuleBuilderScanner.PLACEHOLDER_METHODS_KEY);
    if (placeholderMethods != null && placeholderMethods.containsKey(ASTHelpers.getSymbol(tree))) {
      return UPlaceholderExpression.create(
          placeholderMethods.get(ASTHelpers.getSymbol(tree)),
          templateExpressions(tree.getArguments()));
    } else {
      return UMethodInvocation.create(
          template(tree.getMethodSelect()), templateExpressions(tree.getArguments()));
    }
  }

  @Override
  public UBinary visitBinary(BinaryTree tree, Void v) {
    return UBinary.create(
        tree.getKind(), template(tree.getLeftOperand()), template(tree.getRightOperand()));
  }

  @Override
  public UAssignOp visitCompoundAssignment(CompoundAssignmentTree tree, Void v) {
    return UAssignOp.create(
        template(tree.getVariable()), tree.getKind(), template(tree.getExpression()));
  }

  @Override
  public UUnary visitUnary(UnaryTree tree, Void v) {
    return UUnary.create(tree.getKind(), template(tree.getExpression()));
  }

  @Override
  public UExpression visitConditionalExpression(ConditionalExpressionTree tree, Void v) {
    return UConditional.create(
        template(tree.getCondition()),
        template(tree.getTrueExpression()),
        template(tree.getFalseExpression()));
  }

  @Override
  public UNewArray visitNewArray(NewArrayTree tree, Void v) {
    return UNewArray.create(
        (UExpression) template(tree.getType()),
        templateExpressions(tree.getDimensions()),
        templateExpressions(tree.getInitializers()));
  }

  @Override
  public UNewClass visitNewClass(NewClassTree tree, Void v) {
    return UNewClass.create(
        tree.getEnclosingExpression() == null ? null : template(tree.getEnclosingExpression()),
        templateTypeExpressions(tree.getTypeArguments()),
        template(tree.getIdentifier()),
        templateExpressions(tree.getArguments()),
        (tree.getClassBody() == null) ? null : visitClass(tree.getClassBody(), null));
  }

  @Override
  public UClassDecl visitClass(ClassTree tree, Void v) {
    ImmutableList.Builder<UMethodDecl> decls = ImmutableList.builder();
    for (MethodTree decl : Iterables.filter(tree.getMembers(), MethodTree.class)) {
      if (decl.getReturnType() != null) {
        decls.add(visitMethod(decl, null));
      }
    }
    return UClassDecl.create(decls.build());
  }

  @Override
  public UArrayTypeTree visitArrayType(ArrayTypeTree tree, Void v) {
    return UArrayTypeTree.create(templateType(tree.getType()));
  }

  @Override
  public UTypeApply visitParameterizedType(ParameterizedTypeTree tree, Void v) {
    return UTypeApply.create(
        templateType(tree.getType()), templateTypeExpressions(tree.getTypeArguments()));
  }

  @Override
  public UUnionType visitUnionType(UnionTypeTree tree, Void v) {
    return UUnionType.create(templateTypeExpressions(tree.getTypeAlternatives()));
  }

  @Override
  public UWildcard visitWildcard(WildcardTree tree, Void v) {
    return UWildcard.create(
        tree.getKind(), (tree.getBound() == null) ? null : templateType(tree.getBound()));
  }

  @Override
  public UIntersectionType visitIntersectionType(IntersectionTypeTree tree, Void v) {
    return UIntersectionType.create(templateTypeExpressions(tree.getBounds()));
  }

  @Override
  public UTypeParameter visitTypeParameter(TypeParameterTree tree, Void v) {
    return UTypeParameter.create(
        tree.getName(),
        templateTypeExpressions(tree.getBounds()),
        cast(templateExpressions(tree.getAnnotations()), UAnnotation.class));
  }

  @Override
  public UTypeCast visitTypeCast(TypeCastTree tree, Void v) {
    return UTypeCast.create(templateType(tree.getType()), template(tree.getExpression()));
  }

  @Override
  public ULambda visitLambdaExpression(LambdaExpressionTree tree, Void v) {
    return ULambda.create(
        ((JCLambda) tree).paramKind,
        cast(templateStatements(tree.getParameters()), UVariableDecl.class),
        (UTree<?>) template(tree.getBody()));
  }

  @Override
  public UMemberReference visitMemberReference(MemberReferenceTree tree, Void v) {
    return UMemberReference.create(
        tree.getMode(),
        template(tree.getQualifierExpression()),
        tree.getName(),
        (tree.getTypeArguments() == null) ? null : templateExpressions(tree.getTypeArguments()));
  }

  @Override
  public UExpression visitIdentifier(IdentifierTree tree, Void v) {
    Symbol sym = ASTHelpers.getSymbol(tree);
    if (sym instanceof ClassSymbol) {
      return UClassIdent.create((ClassSymbol) sym);
    } else if (sym != null && sym.isStatic()) {
      return staticMember(sym);
    } else if (freeVariables.containsKey(tree.getName().toString())) {
      VarSymbol symbol = freeVariables.get(tree.getName().toString());
      checkState(symbol == sym);
      UExpression ident = UFreeIdent.create(tree.getName());
      Matches matches = ASTHelpers.getAnnotation(symbol, Matches.class);
      if (matches != null) {
        ident = UMatches.create(getValue(matches), /* positive= */ true, ident);
      }
      NotMatches notMatches = ASTHelpers.getAnnotation(symbol, NotMatches.class);
      if (notMatches != null) {
        ident = UMatches.create(getValue(notMatches), /* positive= */ false, ident);
      }
      OfKind hasKind = ASTHelpers.getAnnotation(symbol, OfKind.class);
      if (hasKind != null) {
        EnumSet<Kind> allowed = EnumSet.copyOf(Arrays.asList(hasKind.value()));
        ident = UOfKind.create(ident, ImmutableSet.copyOf(allowed));
      }
      // @Repeated annotations need to be checked last.
      Repeated repeated = ASTHelpers.getAnnotation(symbol, Repeated.class);
      if (repeated != null) {
        ident = URepeated.create(tree.getName(), ident);
      }
      return ident;
    }
    if (sym == null) {
      return UTypeVarIdent.create(tree.getName());
    }
    switch (sym.getKind()) {
      case TYPE_PARAMETER:
        return UTypeVarIdent.create(tree.getName());
      default:
        return ULocalVarIdent.create(tree.getName());
    }
  }

  /**
   * Returns the {@link Class} instance for the {@link Matcher} associated with the provided {@link
   * Matches} annotation. This roundabout solution is recommended and explained by {@link
   * Element#getAnnotation(Class)}.
   */
  static Class<? extends Matcher<? super ExpressionTree>> getValue(Matches matches) {
    String name;
    try {
      matches.value();
      throw new RuntimeException("unreachable");
    } catch (MirroredTypeException e) {
      DeclaredType type = (DeclaredType) e.getTypeMirror();
      name = ((TypeElement) type.asElement()).getQualifiedName().toString();
    }
    try {
      return asSubclass(Class.forName(name), new TypeToken<Matcher<? super ExpressionTree>>() {});
    } catch (ClassNotFoundException | ClassCastException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns the {@link Class} instance for the {@link Matcher} associated with the provided {@link
   * NotMatches} annotation. This roundabout solution is recommended and explained by {@link
   * Element#getAnnotation(Class)}.
   */
  static Class<? extends Matcher<? super ExpressionTree>> getValue(NotMatches matches) {
    String name;
    try {
      matches.value();
      throw new RuntimeException("unreachable");
    } catch (MirroredTypeException e) {
      DeclaredType type = (DeclaredType) e.getTypeMirror();
      name = ((TypeElement) type.asElement()).getQualifiedName().toString();
    }
    try {
      return asSubclass(Class.forName(name), new TypeToken<Matcher<? super ExpressionTree>>() {});
    } catch (ClassNotFoundException | ClassCastException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Similar to {@link Class#asSubclass(Class)}, but it accepts a {@link TypeToken} so it handles
   * generics better.
   */
  @SuppressWarnings("unchecked")
  private static <T> Class<? extends T> asSubclass(Class<?> klass, TypeToken<T> token)
      throws ClassCastException {
    if (!token.isSupertypeOf(klass)) {
      throw new ClassCastException(klass + " is not assignable to " + token);
    }
    return (Class<? extends T>) klass;
  }

  public UStatement template(StatementTree tree) {
    return (UStatement) tree.accept(this, null);
  }

  @Nullable
  private List<UStatement> templateStatements(@Nullable List<? extends StatementTree> statements) {
    if (statements == null) {
      return null;
    }
    ImmutableList.Builder<UStatement> builder = ImmutableList.builder();
    for (StatementTree statement : statements) {
      builder.add(template(statement));
    }
    return builder.build();
  }

  @Override
  public UTry visitTry(TryTree tree, Void v) {
    @SuppressWarnings({"unchecked", "rawtypes"})
    List<UTree<?>> resources =
        cast(templateTrees(tree.getResources()), (Class<UTree<?>>) (Class) UTree.class);
    UBlock block = visitBlock(tree.getBlock(), null);
    ImmutableList.Builder<UCatch> catchesBuilder = ImmutableList.builder();
    for (CatchTree catchTree : tree.getCatches()) {
      catchesBuilder.add(visitCatch(catchTree, null));
    }
    UBlock finallyBlock =
        (tree.getFinallyBlock() == null) ? null : visitBlock(tree.getFinallyBlock(), null);
    return UTry.create(resources, block, catchesBuilder.build(), finallyBlock);
  }

  @Override
  public UCatch visitCatch(CatchTree tree, Void v) {
    return UCatch.create(
        visitVariable(tree.getParameter(), null), visitBlock(tree.getBlock(), null));
  }

  @Nullable
  private PlaceholderMethod placeholder(@Nullable ExpressionTree expr) {
    Map<MethodSymbol, PlaceholderMethod> placeholderMethods =
        context.get(RefasterRuleBuilderScanner.PLACEHOLDER_METHODS_KEY);
    return (placeholderMethods != null && expr != null)
        ? placeholderMethods.get(ASTHelpers.getSymbol(expr))
        : null;
  }

  @Override
  public UStatement visitExpressionStatement(ExpressionStatementTree tree, Void v) {
    PlaceholderMethod placeholderMethod = placeholder(tree.getExpression());
    if (placeholderMethod != null && placeholderMethod.returnType().equals(UPrimitiveType.VOID)) {
      MethodInvocationTree invocation = (MethodInvocationTree) tree.getExpression();
      return UPlaceholderStatement.create(
          placeholderMethod,
          templateExpressions(invocation.getArguments()),
          ControlFlowVisitor.Result.NEVER_EXITS);
    }
    return UExpressionStatement.create(template(tree.getExpression()));
  }

  @Override
  public UStatement visitReturn(ReturnTree tree, Void v) {
    PlaceholderMethod placeholderMethod = placeholder(tree.getExpression());
    if (placeholderMethod != null) {
      MethodInvocationTree invocation = (MethodInvocationTree) tree.getExpression();
      return UPlaceholderStatement.create(
          placeholderMethod,
          templateExpressions(invocation.getArguments()),
          ControlFlowVisitor.Result.ALWAYS_RETURNS);
    }
    return UReturn.create((tree.getExpression() == null) ? null : template(tree.getExpression()));
  }

  @Override
  public UWhileLoop visitWhileLoop(WhileLoopTree tree, Void v) {
    return UWhileLoop.create(template(tree.getCondition()), template(tree.getStatement()));
  }

  @Override
  public UVariableDecl visitVariable(VariableTree tree, Void v) {
    return UVariableDecl.create(
        tree.getName(),
        templateType(tree.getType()),
        (tree.getInitializer() == null) ? null : template(tree.getInitializer()));
  }

  @Override
  public USkip visitEmptyStatement(EmptyStatementTree tree, Void v) {
    return USkip.INSTANCE;
  }

  @Override
  public UForLoop visitForLoop(ForLoopTree tree, Void v) {
    return UForLoop.create(
        templateStatements(tree.getInitializer()),
        (tree.getCondition() == null) ? null : template(tree.getCondition()),
        cast(templateStatements(tree.getUpdate()), UExpressionStatement.class),
        template(tree.getStatement()));
  }

  @Override
  public ULabeledStatement visitLabeledStatement(LabeledStatementTree tree, Void v) {
    return ULabeledStatement.create(tree.getLabel(), template(tree.getStatement()));
  }

  @Override
  public UBreak visitBreak(BreakTree tree, Void v) {
    return UBreak.create(tree.getLabel());
  }

  @Override
  public UContinue visitContinue(ContinueTree tree, Void v) {
    return UContinue.create(tree.getLabel());
  }

  @Override
  public UBlock visitBlock(BlockTree tree, Void v) {
    return UBlock.create(templateStatements(tree.getStatements()));
  }

  @Override
  public UThrow visitThrow(ThrowTree tree, Void v) {
    return UThrow.create(template(tree.getExpression()));
  }

  @Override
  public UDoWhileLoop visitDoWhileLoop(DoWhileLoopTree tree, Void v) {
    return UDoWhileLoop.create(template(tree.getStatement()), template(tree.getCondition()));
  }

  @Override
  public UEnhancedForLoop visitEnhancedForLoop(EnhancedForLoopTree tree, Void v) {
    return UEnhancedForLoop.create(
        visitVariable(tree.getVariable(), null),
        template(tree.getExpression()),
        template(tree.getStatement()));
  }

  @Override
  public USynchronized visitSynchronized(SynchronizedTree tree, Void v) {
    return USynchronized.create(template(tree.getExpression()), visitBlock(tree.getBlock(), null));
  }

  @Override
  public UIf visitIf(IfTree tree, Void v) {
    return UIf.create(
        template(tree.getCondition()),
        template(tree.getThenStatement()),
        (tree.getElseStatement() == null) ? null : template(tree.getElseStatement()));
  }

  @Override
  public UAssert visitAssert(AssertTree tree, Void v) {
    return UAssert.create(
        template(tree.getCondition()),
        (tree.getDetail() == null) ? null : template(tree.getDetail()));
  }

  @Override
  protected UTree<?> defaultAction(Tree tree, Void v) {
    throw new IllegalArgumentException(
        "Refaster does not currently support syntax " + tree.getClass());
  }

  public UType template(Type type) {
    return type.accept(typeTemplater, null);
  }

  List<UType> templateTypes(Iterable<? extends Type> types) {
    ImmutableList.Builder<UType> builder = ImmutableList.builder();
    for (Type ty : types) {
      builder.add(template(ty));
    }
    return builder.build();
  }

  private final Type.Visitor<UType, Void> typeTemplater =
      new Types.SimpleVisitor<UType, Void>() {
        private final Map<TypeSymbol, UTypeVar> typeVariables = new HashMap<>();

        @Override
        public UType visitType(Type type, Void v) {
          if (UPrimitiveType.isDeFactoPrimitive(type.getKind())) {
            return UPrimitiveType.create(type.getKind());
          } else {
            throw new IllegalArgumentException(
                "Refaster does not currently support syntax " + type.getKind());
          }
        }

        @Override
        public UArrayType visitArrayType(ArrayType type, Void v) {
          return UArrayType.create(type.getComponentType().accept(this, null));
        }

        @Override
        public UMethodType visitMethodType(MethodType type, Void v) {
          return UMethodType.create(
              type.getReturnType().accept(this, null), templateTypes(type.getParameterTypes()));
        }

        @Override
        public UType visitClassType(ClassType type, Void v) {
          if (type instanceof IntersectionClassType) {
            return UIntersectionClassType.create(
                templateTypes(((IntersectionClassType) type).getComponents()));
          }
          return UClassType.create(
              type.tsym.getQualifiedName().toString(), templateTypes(type.getTypeArguments()));
        }

        @Override
        public UWildcardType visitWildcardType(WildcardType type, Void v) {
          return UWildcardType.create(type.kind, type.type.accept(this, null));
        }

        @Override
        public UTypeVar visitTypeVar(TypeVar type, Void v) {
          /*
           * In order to handle recursively bounded type variables without a stack overflow, we first
           * cache a type var with no bounds, then we template the bounds.
           */
          TypeSymbol tsym = type.asElement();
          if (typeVariables.containsKey(tsym)) {
            return typeVariables.get(tsym);
          }
          UTypeVar var = UTypeVar.create(tsym.getSimpleName().toString());
          typeVariables.put(
              tsym, var); // so the type variable can be used recursively in the bounds
          var.setLowerBound(type.getLowerBound().accept(this, null));
          var.setUpperBound(type.getUpperBound().accept(this, null));
          return var;
        }

        @Override
        public UForAll visitForAll(ForAll type, Void v) {
          List<UTypeVar> vars = cast(templateTypes(type.getTypeVariables()), UTypeVar.class);
          return UForAll.create(vars, type.qtype.accept(this, null));
        }
      };

  @SuppressWarnings("unchecked")
  public static ImmutableClassToInstanceMap<Annotation> annotationMap(Symbol symbol) {
    ImmutableClassToInstanceMap.Builder<Annotation> builder = ImmutableClassToInstanceMap.builder();
    for (Compound compound : symbol.getAnnotationMirrors()) {
      Name qualifiedAnnotationType =
          ((TypeElement) compound.getAnnotationType().asElement()).getQualifiedName();
      try {
        Class<? extends Annotation> annotationClazz =
            Class.forName(qualifiedAnnotationType.toString()).asSubclass(Annotation.class);
        builder.put(
            (Class) annotationClazz,
            AnnotationProxyMaker.generateAnnotation(compound, annotationClazz));
      } catch (ClassNotFoundException e) {
        throw new IllegalArgumentException("Unrecognized annotation type", e);
      }
    }
    return builder.build();
  }
}
