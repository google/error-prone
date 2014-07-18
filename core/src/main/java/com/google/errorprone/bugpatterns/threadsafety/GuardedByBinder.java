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

import com.google.errorprone.bugpatterns.threadsafety.GuardedByExpression.Kind;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;

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
  public static GuardedByExpression bindExpression(JCTree.JCExpression exp) {
    return bind(exp, ALREADY_BOUND_RESOLVER);
  }

  /**
   * Create a {@link GuardedByExpression} from a string, given the resolution context.
   */
  public static GuardedByExpression bindString(String exp, Resolver resolver, Context context) {
    return bind(GuardedByUtils.parseString(exp, context), resolver);
  }

  private static GuardedByExpression bind(JCTree.JCExpression exp, Resolver resolver) {
    GuardedByExpression expr = BINDER.visit(exp, resolver);
    if (expr == null) {
      throw new IllegalGuardedBy(exp.toString());
    }
    if (expr.kind() == Kind.TYPE_LITERAL) {
      throw new IllegalGuardedBy("Raw type literal: " + exp.toString());
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
  };

  private static final GuardedByExpression.Factory F = new GuardedByExpression.Factory();

  private static final SimpleTreeVisitor<GuardedByExpression, Resolver> BINDER =
      new SimpleTreeVisitor<GuardedByExpression, Resolver>() {

        @Override
        public GuardedByExpression visitMethodInvocation(MethodInvocationTree node,
            Resolver resolver) {
          if (!node.getArguments().isEmpty() || !node.getTypeArguments().isEmpty()) {
            throw new IllegalGuardedBy("Only nullary methods are allowed.");
          }
          ExpressionTree methodSelect = node.getMethodSelect();
          switch (methodSelect.getKind()) {
            case IDENTIFIER: {
              IdentifierTree identifier = (IdentifierTree) methodSelect;
              Symbol.MethodSymbol method = resolver.resolveMethod(node, identifier.getName());
              GuardedByExpression base =
                  method.isStatic() ? F.typeLiteral(method.owner) : F.thisliteral(method.owner);
              return bindSelect(base, method);
            }
            case MEMBER_SELECT: {
              MemberSelectTree select = (MemberSelectTree) methodSelect;
              GuardedByExpression base = visit(select.getExpression(), resolver);
              if (base == null) {
                throw new IllegalGuardedBy(select.getExpression().toString());
              }
              Symbol.MethodSymbol method =
                  resolver.resolveMethod(node, base, select.getIdentifier());
              return bindSelect(base, method);
            }
            default:
              throw new IllegalGuardedBy(methodSelect.getKind().toString());
          }
        }

        @Override
        public GuardedByExpression visitMemberSelect(MemberSelectTree node, Resolver resolver) {
          String name = node.getIdentifier().toString();

          if (name.equals("this")) {
            Symbol base = resolver.resolveTypeLiteral(node.getExpression());
            return F.thisliteral(base);
          }

          if (name.equals("class")) {
            Symbol base = resolver.resolveTypeLiteral(node.getExpression());
            return F.classLiteral(base);
          }

          GuardedByExpression base = visit(node.getExpression(), resolver);
          if (base == null) {
            throw new IllegalGuardedBy("Bad expression: " + node.getExpression());
          }
          Symbol sym = resolver.resolveSelect(base, node);
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

          // TODO(cushon) - forbid static access on instance?

          return F.select(base, sym);
        }

        @Override
        public GuardedByExpression visitIdentifier(IdentifierTree node, Resolver resolver) {
          Symbol symbol = resolver.resolveIdentifier(node);
          if (symbol instanceof Symbol.VarSymbol) {
            Symbol.VarSymbol varSymbol = (Symbol.VarSymbol) symbol;
            switch (varSymbol.getKind()) {
              case LOCAL_VARIABLE:
              case PARAMETER:
                return F.localVariable(varSymbol);
              case FIELD: {
                if (symbol.name.contentEquals("this")) {
                  return F.thisliteral(symbol.owner);
                }
                GuardedByExpression base = symbol.isStatic()
                    ? F.typeLiteral(varSymbol.owner)
                    : F.thisliteral(varSymbol.owner);
                return F.select(base, varSymbol);
              }
              default:
                throw new IllegalGuardedBy(varSymbol.getKind().toString());
            }
          } else if (symbol instanceof Symbol.MethodSymbol) {
            Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol) symbol;
            GuardedByExpression base = symbol.isStatic()
                ? F.classLiteral(methodSymbol.owner)
                : F.thisliteral(methodSymbol.owner);
            return F.select(base, methodSymbol);
          } else if (symbol instanceof Symbol.ClassSymbol) {
            if (node.getName().contentEquals("this")) {
              return F.thisliteral(symbol);
            } else {
              return F.typeLiteral(symbol);
            }
          }
          throw new IllegalGuardedBy(symbol.getClass().toString());
        }
      };
}
