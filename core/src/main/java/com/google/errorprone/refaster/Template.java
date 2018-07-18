/*
 * Copyright 2013 The Error Prone Authors.
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

import static java.util.logging.Level.FINE;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.refaster.PlaceholderMethod.PlaceholderExpressionKey;
import com.google.errorprone.refaster.UTypeVar.TypeWithExpression;
import com.google.errorprone.refaster.annotation.NoAutoboxing;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Kinds.KindSelector;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ForAll;
import com.sun.tools.javac.code.Type.MethodType;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.comp.Attr;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Enter;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.comp.Resolve;
import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCCatch;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCExpressionStatement;
import com.sun.tools.javac.tree.JCTree.JCLambda;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCTry;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.Pretty;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Position;
import com.sun.tools.javac.util.Warner;
import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;
import javax.annotation.Nullable;

/**
 * Abstract superclass for templates that can be used to search and replace in a Java syntax tree.
 *
 * @author lowasser@google.com (Louis Wasserman)
 * @param <M> Type of a match for this template.
 */
public abstract class Template<M extends TemplateMatch> implements Serializable {
  private static final Logger logger = Logger.getLogger(Template.class.toString());

  public static final boolean AUTOBOXING_DEFAULT = true;

  public abstract ImmutableClassToInstanceMap<Annotation> annotations();

  public abstract ImmutableList<UTypeVar> templateTypeVariables();

  public abstract ImmutableMap<String, UType> expressionArgumentTypes();

  public abstract Iterable<M> match(JCTree tree, Context context);

  public abstract Fix replace(M match);

  Iterable<UTypeVar> typeVariables(Context context) {
    ImmutableList<UTypeVar> ruleTypeVars = context.get(RefasterRule.RULE_TYPE_VARS);
    return Iterables.concat(
        (ruleTypeVars == null) ? ImmutableList.<UTypeVar>of() : ruleTypeVars,
        templateTypeVariables());
  }

  boolean autoboxing() {
    return !annotations().containsKey(NoAutoboxing.class);
  }

  /**
   * Returns a list of the expected types to be matched. This consists of the argument types from
   * the @BeforeTemplate method, concatenated with the return types of expression placeholders,
   * sorted by the name of the placeholder method.
   *
   * @throws CouldNotResolveImportException if a referenced type could not be resolved
   */
  protected List<Type> expectedTypes(Inliner inliner) throws CouldNotResolveImportException {
    ArrayList<Type> result = new ArrayList<>();
    ImmutableList<UType> types = expressionArgumentTypes().values().asList();
    ImmutableList<String> argNames = expressionArgumentTypes().keySet().asList();
    for (int i = 0; i < argNames.size(); i++) {
      String argName = argNames.get(i);
      Optional<JCExpression> singleBinding =
          inliner.getOptionalBinding(new UFreeIdent.Key(argName));
      if (!singleBinding.isPresent()) {
        Optional<java.util.List<JCExpression>> exprs =
            inliner.getOptionalBinding(new URepeated.Key(argName));
        if (!exprs.isPresent() || exprs.get().isEmpty()) {
          // It is a repeated template variable and matches no expressions.
          continue;
        }
      }
      result.add(types.get(i).inline(inliner));
    }
    for (PlaceholderExpressionKey key :
        Ordering.natural()
            .immutableSortedCopy(
                Iterables.filter(inliner.bindings.keySet(), PlaceholderExpressionKey.class))) {
      result.add(key.method.returnType().inline(inliner));
    }
    return List.from(result);
  }

  /**
   * Returns a list of the actual types to be matched. This consists of the types of the expressions
   * bound to the @BeforeTemplate method parameters, concatenated with the types of the expressions
   * bound to expression placeholders, sorted by the name of the placeholder method.
   */
  protected List<Type> actualTypes(Inliner inliner) {
    ArrayList<Type> result = new ArrayList<>();
    ImmutableList<String> argNames = expressionArgumentTypes().keySet().asList();
    for (int i = 0; i < expressionArgumentTypes().size(); i++) {
      String argName = argNames.get(i);
      Optional<JCExpression> singleBinding =
          inliner.getOptionalBinding(new UFreeIdent.Key(argName));
      if (singleBinding.isPresent()) {
        result.add(singleBinding.get().type);
      } else {
        Optional<java.util.List<JCExpression>> exprs =
            inliner.getOptionalBinding(new URepeated.Key(argName));
        if (exprs.isPresent() && !exprs.get().isEmpty()) {
          Type[] exprTys = new Type[exprs.get().size()];
          for (int j = 0; j < exprs.get().size(); j++) {
            exprTys[j] = exprs.get().get(j).type;
          }
          // Get the least upper bound of the types of all expressions that the argument matches.
          // In the special case where exprs is empty, returns the "bottom" type, which is a
          // subtype of everything.
          result.add(inliner.types().lub(List.from(exprTys)));
        }
      }
    }
    for (PlaceholderExpressionKey key :
        Ordering.natural()
            .immutableSortedCopy(
                Iterables.filter(inliner.bindings.keySet(), PlaceholderExpressionKey.class))) {
      result.add(inliner.getBinding(key).type);
    }
    return List.from(result);
  }

  @Nullable
  protected Optional<Unifier> typecheck(
      Unifier unifier,
      Inliner inliner,
      Warner warner,
      List<Type> expectedTypes,
      List<Type> actualTypes) {
    try {
      ImmutableList<UTypeVar> freeTypeVars = freeTypeVars(unifier);
      infer(
          warner,
          inliner,
          inliner.<Type>inlineList(freeTypeVars),
          expectedTypes,
          inliner.symtab().voidType,
          actualTypes);

      for (UTypeVar var : freeTypeVars) {
        Type instantiationForVar =
            infer(
                warner,
                inliner,
                inliner.<Type>inlineList(freeTypeVars),
                expectedTypes,
                var.inline(inliner),
                actualTypes);
        unifier.putBinding(
            var.key(), TypeWithExpression.create(instantiationForVar.getReturnType()));
      }

      if (!checkBounds(unifier, inliner, warner)) {
        return Optional.absent();
      }
      return Optional.of(unifier);
    } catch (CouldNotResolveImportException e) {
      logger.log(FINE, "Failure to resolve an import", e);
      return Optional.absent();
    } catch (InferException e) {
      logger.log(FINE, "No valid instantiation found: " + e.getMessage());
      return Optional.absent();
    }
  }

  private boolean checkBounds(Unifier unifier, Inliner inliner, Warner warner)
      throws CouldNotResolveImportException {
    Types types = unifier.types();
    ListBuffer<Type> varsBuffer = new ListBuffer<>();
    ListBuffer<Type> bindingsBuffer = new ListBuffer<>();
    for (UTypeVar typeVar : typeVariables(unifier.getContext())) {
      varsBuffer.add(inliner.inlineAsVar(typeVar));
      bindingsBuffer.add(unifier.getBinding(typeVar.key()).type());
    }
    List<Type> vars = varsBuffer.toList();
    List<Type> bindings = bindingsBuffer.toList();
    for (UTypeVar typeVar : typeVariables(unifier.getContext())) {
      List<Type> bounds = types.getBounds(inliner.inlineAsVar(typeVar));
      bounds = types.subst(bounds, vars, bindings);
      if (!types.isSubtypeUnchecked(unifier.getBinding(typeVar.key()).type(), bounds, warner)) {
        logger.log(
            FINE,
            String.format("%s is not a subtype of %s", inliner.getBinding(typeVar.key()), bounds));
        return false;
      }
    }
    return true;
  }

  protected static Pretty pretty(Context context, final Writer writer) {
    final JCCompilationUnit unit = context.get(JCCompilationUnit.class);
    try {
      final String unitContents = unit.getSourceFile().getCharContent(false).toString();
      return new Pretty(writer, true) {
        {
          // Work-around for b/22196513
          width = 0;
        }

        @Override
        public void visitAnnotation(JCAnnotation anno) {
          if (anno.getArguments().isEmpty()) {
            try {
              print("@");
              printExpr(anno.annotationType);
            } catch (IOException e) {
              // the supertype swallows exceptions too
              throw new RuntimeException(e);
            }
          } else {
            super.visitAnnotation(anno);
          }
        }

        @Override
        public void printExpr(JCTree tree, int prec) throws IOException {
          EndPosTable endPositions = unit.endPositions;
          /*
           * Modifiers, and specifically flags like final, appear to just need weird special
           * handling.
           *
           * Note: we can't use {@code TreeInfo.getEndPos()} or {@code JCTree.getEndPosition()}
           * here, because they will return the end position of an enclosing AST node for trees
           * whose real end positions aren't stored.
           */
          int endPos = endPositions.getEndPos(tree);
          boolean hasRealEndPosition = endPos != Position.NOPOS;
          if (tree.getKind() != Kind.MODIFIERS && hasRealEndPosition) {
            writer.append(unitContents.substring(tree.getStartPosition(), endPos));
          } else {
            super.printExpr(tree, prec);
          }
        }

        @Override
        public void visitApply(JCMethodInvocation tree) {
          JCExpression select = tree.getMethodSelect();
          if (select != null && select.toString().equals("Refaster.emitCommentBefore")) {
            String commentLiteral = (String) ((JCLiteral) tree.getArguments().get(0)).getValue();
            JCExpression expr = tree.getArguments().get(1);
            try {
              print("/* " + commentLiteral + " */ ");
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
            expr.accept(this);
          } else {
            super.visitApply(tree);
          }
        }

        @Override
        public void printStat(JCTree tree) throws IOException {
          if (tree instanceof JCExpressionStatement
              && ((JCExpressionStatement) tree).getExpression() instanceof JCMethodInvocation) {
            JCMethodInvocation invocation =
                (JCMethodInvocation) ((JCExpressionStatement) tree).getExpression();
            JCExpression select = invocation.getMethodSelect();
            if (select != null && select.toString().equals("Refaster.emitComment")) {
              String commentLiteral =
                  (String) ((JCLiteral) invocation.getArguments().get(0)).getValue();
              print("// " + commentLiteral);
              return;
            }
          }
          super.printStat(tree);
        }

        // Don't print parentheses around single lambda parameters without an explicit type.
        @Override
        public void visitLambda(JCLambda lambda) {
          try {
            boolean exactlyOneParamWithNoType =
                lambda.params.size() == 1 && lambda.params.get(0).vartype == null;
            if (!exactlyOneParamWithNoType) {
              print("(");
            }
            boolean first = true;
            for (JCVariableDecl param : lambda.params) {
              if (!first) {
                print(",");
              }
              if (param.vartype != null) {
                if (!first) {
                  print(" ");
                }
                print(param.vartype + " ");
              }
              print(param.name);
              first = false;
            }
            if (!exactlyOneParamWithNoType) {
              print(")");
            }
            print("->");
            printExpr(lambda.body);
          } catch (IOException e) {
            throw new UncheckedIOException(e);
          }
        }

        @Override
        public void visitTry(JCTry tree) {
          if (tree.getResources().isEmpty()) {
            super.visitTry(tree);
            return;
          }
          try {
            print("try (");
            boolean first = true;
            for (JCTree resource : tree.getResources()) {
              if (!first) {
                print(";");
                println();
              }
              printExpr(resource);
              first = false;
            }
            print(")");
            printStat(tree.body);
            for (JCCatch catchStmt : tree.getCatches()) {
              printStat(catchStmt);
            }
            if (tree.getFinallyBlock() != null) {
              print(" finally ");
              printStat(tree.getFinallyBlock());
            }
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }
      };
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static class InferException extends Exception {
    final Collection<JCDiagnostic> diagnostics;

    public InferException(Collection<JCDiagnostic> diagnostics) {
      this.diagnostics = diagnostics;
    }

    @Override
    public String getMessage() {
      return "Inference failed with the following error(s): " + diagnostics.toString();
    }
  }

  /**
   * Returns the inferred method type of the template based on the given actual argument types.
   *
   * @throws InferException if no instances of the specified type variables would allow the {@code
   *     actualArgTypes} to match the {@code expectedArgTypes}
   */
  private Type infer(
      Warner warner,
      Inliner inliner,
      List<Type> freeTypeVariables,
      List<Type> expectedArgTypes,
      Type returnType,
      List<Type> actualArgTypes)
      throws InferException {
    Symtab symtab = inliner.symtab();

    Type methodType =
        new MethodType(expectedArgTypes, returnType, List.<Type>nil(), symtab.methodClass);
    if (!freeTypeVariables.isEmpty()) {
      methodType = new ForAll(freeTypeVariables, methodType);
    }

    Enter enter = inliner.enter();
    MethodSymbol methodSymbol =
        new MethodSymbol(0, inliner.asName("__m__"), methodType, symtab.unknownSymbol);

    Type site = symtab.methodClass.type;

    Env<AttrContext> env =
        enter.getTopLevelEnv(TreeMaker.instance(inliner.getContext()).TopLevel(List.<JCTree>nil()));

    // Set up the resolution phase:
    try {
      Field field = AttrContext.class.getDeclaredField("pendingResolutionPhase");
      field.setAccessible(true);
      field.set(env.info, newMethodResolutionPhase(autoboxing()));
    } catch (ReflectiveOperationException e) {
      throw new LinkageError(e.getMessage(), e);
    }

    Object resultInfo;
    try {
      Class<?> resultInfoClass = Class.forName("com.sun.tools.javac.comp.Attr$ResultInfo");
      Constructor<?> resultInfoCtor =
          resultInfoClass.getDeclaredConstructor(Attr.class, KindSelector.class, Type.class);
      resultInfoCtor.setAccessible(true);
      resultInfo =
          resultInfoCtor.newInstance(
              Attr.instance(inliner.getContext()), KindSelector.PCK, Type.noType);
    } catch (ReflectiveOperationException e) {
      throw new LinkageError(e.getMessage(), e);
    }

    // Type inference sometimes produces diagnostics, so we need to catch them to avoid interfering
    // with the enclosing compilation.
    Log.DeferredDiagnosticHandler handler =
        new Log.DeferredDiagnosticHandler(Log.instance(inliner.getContext()));
    try {
      MethodType result =
          callCheckMethod(warner, inliner, resultInfo, actualArgTypes, methodSymbol, site, env);
      if (!handler.getDiagnostics().isEmpty()) {
        throw new InferException(handler.getDiagnostics());
      }
      return result;
    } finally {
      Log.instance(inliner.getContext()).popDiagnosticHandler(handler);
    }
  }

  /** Reflectively instantiate the package-private {@code MethodResolutionPhase} enum. */
  private static Object newMethodResolutionPhase(boolean autoboxing) {
    for (Class<?> c : Resolve.class.getDeclaredClasses()) {
      if (!c.getName().equals("com.sun.tools.javac.comp.Resolve$MethodResolutionPhase")) {
        continue;
      }
      for (Object e : c.getEnumConstants()) {
        if (e.toString().equals(autoboxing ? "BOX" : "BASIC")) {
          return e;
        }
      }
    }
    return null;
  }

  /**
   * Reflectively invoke Resolve.checkMethod(), which despite being package-private is apparently
   * the only useful entry-point into javac8's type inference implementation.
   */
  private MethodType callCheckMethod(
      Warner warner,
      Inliner inliner,
      Object resultInfo,
      List<Type> actualArgTypes,
      MethodSymbol methodSymbol,
      Type site,
      Env<AttrContext> env)
      throws InferException {
    try {
      Method checkMethod;
      checkMethod =
          Resolve.class.getDeclaredMethod(
              "checkMethod",
              Env.class,
              Type.class,
              Symbol.class,
              Class.forName(
                  "com.sun.tools.javac.comp.Attr$ResultInfo"), // ResultInfo is package-private
              List.class,
              List.class,
              Warner.class);
      checkMethod.setAccessible(true);
      return (MethodType)
          checkMethod.invoke(
              Resolve.instance(inliner.getContext()),
              env,
              site,
              methodSymbol,
              resultInfo,
              actualArgTypes,
              /*freeTypeVariables=*/ List.<Type>nil(),
              warner);
    } catch (InvocationTargetException e) {
      if (e.getCause() instanceof Resolve.InapplicableMethodException) {
        throw new InferException(
            ImmutableList.of(
                ((Resolve.InapplicableMethodException) e.getTargetException()).getDiagnostic()));
      }
      throw new LinkageError(e.getMessage(), e.getCause());
    } catch (ReflectiveOperationException e) {
      throw new LinkageError(e.getMessage(), e);
    }
  }

  /**
   * Returns a list of the elements of {@code typeVariables} that are <em>not</em> bound in the
   * specified {@link Unifier}.
   */
  private ImmutableList<UTypeVar> freeTypeVars(Unifier unifier) {
    ImmutableList.Builder<UTypeVar> builder = ImmutableList.builder();
    for (UTypeVar var : typeVariables(unifier.getContext())) {
      if (unifier.getBinding(var.key()) == null) {
        builder.add(var);
      }
    }
    return builder.build();
  }

  protected static Fix addImports(Inliner inliner, SuggestedFix.Builder fix) {
    for (String importToAdd : inliner.getImportsToAdd()) {
      fix.addImport(importToAdd);
    }
    for (String staticImportToAdd : inliner.getStaticImportsToAdd()) {
      fix.addStaticImport(staticImportToAdd);
    }
    return fix.build();
  }
}
