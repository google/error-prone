/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.threadsafety.GuardedByExpression.Kind;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree;

import javax.lang.model.element.Name;

/**
 * Binds the value of a {@code @GuardedBy} annotation to a {@link GuardedByExpression}.
 *
 * @author cushon@google.com (Liam Miller-Cushon)
 */
public class GuardedByBinder {

  /**
   * Create a {@link GuardedByExpression} from a bound AST node.
   */
  public static GuardedByExpression bindExpression(
      JCTree.JCExpression exp, VisitorState visitorState) {
    return bind(
        exp,
        BinderContext.of(
            ALREADY_BOUND_RESOLVER,
            ASTHelpers.getSymbol(visitorState.findEnclosing(ClassTree.class)),
            visitorState.getTypes()));
  }

  /**
   * Create a {@link GuardedByExpression} from a string, given the resolution context.
   */
  static GuardedByExpression bindString(String string, GuardedBySymbolResolver resolver) {
    return bind(
        GuardedByUtils.parseString(string, resolver.context()),
        BinderContext.of(
            resolver,
            resolver.enclosingClass(),
            Types.instance(resolver.context())));
  }

  private static class BinderContext {
    final Resolver resolver;
    final ClassSymbol thisClass;
    final Types types;

    public BinderContext(Resolver resolver, ClassSymbol thisClass, Types types) {
     this.resolver = resolver;
     this.thisClass = thisClass;
     this.types = types;
    }

    public static BinderContext of(Resolver resolver, ClassSymbol thisClass, Types types) {
      return new BinderContext(resolver, thisClass, types);
    }
  }

  private static GuardedByExpression bind(JCTree.JCExpression exp, BinderContext context) {
    GuardedByExpression expr = BINDER.visit(exp, context);
    if (expr == null) {
      throw new IllegalGuardedBy(exp.toString());
    }
    if (expr.kind() == Kind.TYPE_LITERAL) {
      throw new IllegalGuardedBy("Raw type literal: " + exp);
    }
    return expr;
  }

  /**
   * The logic for resolving the {@link com.sun.tools.javac.code.Symbol} for an AST node.
   *
   * Guard expressions can be bound from the string value of an {@code @GuardedBy} annotation, or
   * from an actual java expression. In the first case, the string is parsed into an AST which will
   * not have any semantic information attached.
   */
  public interface Resolver {
    Symbol resolveIdentifier(IdentifierTree node);

    Symbol.MethodSymbol resolveMethod(MethodInvocationTree node, Name name);

    Symbol.MethodSymbol resolveMethod(MethodInvocationTree node, GuardedByExpression base,
        Name identifier);

    Symbol resolveSelect(GuardedByExpression base, MemberSelectTree node);

    Symbol resolveTypeLiteral(ExpressionTree expression);

    Symbol resolveEnclosingClass(ExpressionTree expression);
  }

  /**
   * If javac has already bound the AST, there isn't a lot of work to do here:
   */
  static final Resolver ALREADY_BOUND_RESOLVER = new Resolver() {
    @Override
    public Symbol resolveIdentifier(IdentifierTree node) {
      return ASTHelpers.getSymbol(node);
    }

    @Override
    public Symbol.MethodSymbol resolveMethod(MethodInvocationTree node, Name name) {
      return ASTHelpers.getSymbol(node);
    }

    @Override
    public Symbol.MethodSymbol resolveMethod(MethodInvocationTree node, GuardedByExpression base,
        Name identifier) {
      return ASTHelpers.getSymbol(node);
    }

    @Override
    public Symbol resolveSelect(GuardedByExpression base, MemberSelectTree node) {
      return ASTHelpers.getSymbol(node);
    }

    @Override
    public Symbol resolveTypeLiteral(ExpressionTree expression) {
      return ASTHelpers.getSymbol(expression);
    }

    @Override
    public Symbol resolveEnclosingClass(ExpressionTree expression) {
      return ASTHelpers.getSymbol(expression);
    }
  };

  private static final GuardedByExpression.Factory F = new GuardedByExpression.Factory();

  private static final SimpleTreeVisitor<GuardedByExpression, BinderContext> BINDER =
      new SimpleTreeVisitor<GuardedByExpression, BinderContext>() {

        @Override
        public GuardedByExpression visitMethodInvocation(MethodInvocationTree node,
            BinderContext context) {
          if (!node.getArguments().isEmpty() || !node.getTypeArguments().isEmpty()) {
            throw new IllegalGuardedBy("Only nullary methods are allowed.");
          }
          ExpressionTree methodSelect = node.getMethodSelect();
          switch (methodSelect.getKind()) {
            case IDENTIFIER: {
              IdentifierTree identifier = (IdentifierTree) methodSelect;
              Symbol.MethodSymbol method =
                  context.resolver.resolveMethod(node, identifier.getName());
              return bindSelect(computeBase(context, method), method);
            }
            case MEMBER_SELECT: {
              MemberSelectTree select = (MemberSelectTree) methodSelect;
              GuardedByExpression base = visit(select.getExpression(), context);
              if (base == null) {
                throw new IllegalGuardedBy(select.getExpression().toString());
              }
              Symbol.MethodSymbol method =
                  context.resolver.resolveMethod(node, base, select.getIdentifier());
              return bindSelect(base, method);
            }
            default:
              throw new IllegalGuardedBy(methodSelect.getKind().toString());
          }
        }

        @Override
        public GuardedByExpression visitMemberSelect(
            MemberSelectTree node,
            BinderContext context) {

          String name = node.getIdentifier().toString();

          if (name.equals("this")) {
            Symbol base = context.resolver.resolveEnclosingClass(node.getExpression());
            return F.qualifiedThis(base);
          }

          if (name.equals("class")) {
            Symbol base = context.resolver.resolveTypeLiteral(node.getExpression());
            return F.classLiteral(base);
          }

          GuardedByExpression base = visit(node.getExpression(), context);
          if (base == null) {
            throw new IllegalGuardedBy("Bad expression: " + node.getExpression());
          }
          Symbol sym = context.resolver.resolveSelect(base, node);
          if (sym == null) {
            throw new IllegalGuardedBy("Could not resolve: " + node);
          }
          if (sym instanceof Symbol.VarSymbol) {
            return bindSelect(base, sym);
          } else {
            throw new IllegalGuardedBy("Bad member symbol: " + sym.getClass());
          }
        }

        private GuardedByExpression bindSelect(GuardedByExpression base, Symbol sym) {
          if (base.kind().equals(Kind.TYPE_LITERAL) && !sym.isStatic()) {
            throw new IllegalGuardedBy("Instance access on static: " + base + ", " + sym);
          }

          // TODO(user) - forbid static access on instance?

          return F.select(base, sym);
        }

        @Override
        public GuardedByExpression visitIdentifier(IdentifierTree node, BinderContext context) {
          Symbol symbol = context.resolver.resolveIdentifier(node);
          if (symbol instanceof Symbol.VarSymbol) {
            Symbol.VarSymbol varSymbol = (Symbol.VarSymbol) symbol;
            switch (varSymbol.getKind()) {
              case LOCAL_VARIABLE:
              case PARAMETER:
                return F.localVariable(varSymbol);
              case FIELD: {
                if (symbol.name.contentEquals("this")) {
                  return F.thisliteral();
                }
                return F.select(computeBase(context, varSymbol), varSymbol);
              }
              default:
                throw new IllegalGuardedBy(varSymbol.getKind().toString());
            }
          } else if (symbol instanceof Symbol.MethodSymbol) {
            Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol) symbol;
            return F.select(computeBase(context, symbol), methodSymbol);
          } else if (symbol instanceof Symbol.ClassSymbol) {
            if (node.getName().contentEquals("this")) {
              return F.thisliteral();
            } else {
              return F.typeLiteral(symbol);
            }
          }
          throw new IllegalGuardedBy(symbol.getClass().toString());
        }

        /**
         * Determine the implicit receiver of a select expression that accesses the given
         * symbol by simple name in the given resolution context.
         *
         * Returns a type name (for static accesses), a qualified this access (for members
         * of a lexically enclosing scope), or a 'simple' this access for members
         * of the current class.
         */
        private GuardedByExpression computeBase(BinderContext context, Symbol symbol) {
          if (symbol.isStatic()) {
            return F.typeLiteral(symbol.owner);
          }

          if (symbol.isMemberOf(context.thisClass.type.tsym, context.types)) {
            return F.thisliteral();
          }

          Symbol lexicalOwner = isEnclosedIn(context.thisClass, symbol, context.types);
          if (lexicalOwner != null) {
            return F.qualifiedThis(lexicalOwner);
          }

          throw new IllegalStateException();
        }

        /**
         * Returns the owner if the given member is declared in a lexically enclosing scope.
         */
        private Symbol isEnclosedIn(ClassSymbol startingClass, Symbol member, Types types) {
          for (Symbol scope = startingClass.owner; scope != null; scope = scope.owner) {
            if (member.isMemberOf(scope.type.tsym, types)) {
              return scope;
            }
          }
          return null;
        }
      };
}
