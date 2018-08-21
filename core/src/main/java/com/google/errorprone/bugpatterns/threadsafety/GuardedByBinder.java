/*
 * Copyright 2014 The Error Prone Authors.
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

import static com.google.errorprone.bugpatterns.threadsafety.IllegalGuardedBy.checkGuardedBy;

import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.threadsafety.GuardedByExpression.Kind;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Names;
import java.util.Optional;
import javax.lang.model.element.Name;

/**
 * A binder from {@code @GuardedBy} annotations to {@link GuardedByExpression}s.
 *
 * @author cushon@google.com (Liam Miller-Cushon)
 */
public class GuardedByBinder {

  /**
   * Creates a {@link GuardedByExpression} from a bound AST node, or returns {@code
   * Optional.empty()} if the AST node doesn't correspond to a 'simple' lock expression.
   */
  public static Optional<GuardedByExpression> bindExpression(
      JCTree.JCExpression exp, VisitorState visitorState) {
    try {
      return Optional.of(
          bind(
              exp,
              BinderContext.of(
                  ALREADY_BOUND_RESOLVER,
                  ASTHelpers.getSymbol(visitorState.findEnclosing(ClassTree.class)),
                  visitorState.getTypes(),
                  Names.instance(visitorState.context))));
    } catch (IllegalGuardedBy expected) {
      return Optional.empty();
    }
  }

  /** Creates a {@link GuardedByExpression} from a string, given the resolution context. */
  static Optional<GuardedByExpression> bindString(String string, GuardedBySymbolResolver resolver) {
    try {
      return Optional.of(
          bind(
              GuardedByUtils.parseString(string, resolver.context()),
              BinderContext.of(
                  resolver,
                  resolver.enclosingClass(),
                  Types.instance(resolver.context()),
                  Names.instance(resolver.context()))));
    } catch (IllegalGuardedBy expected) {
      return Optional.empty();
    }
  }

  private static class BinderContext {
    final Resolver resolver;
    final ClassSymbol thisClass;
    final Types types;
    final Names names;

    public BinderContext(Resolver resolver, ClassSymbol thisClass, Types types, Names names) {
      this.resolver = resolver;
      this.thisClass = thisClass;
      this.types = types;
      this.names = names;
    }

    public static BinderContext of(
        Resolver resolver, ClassSymbol thisClass, Types types, Names names) {
      return new BinderContext(resolver, thisClass, types, names);
    }
  }

  private static GuardedByExpression bind(JCTree.JCExpression exp, BinderContext context) {
    GuardedByExpression expr = BINDER.visit(exp, context);
    checkGuardedBy(expr != null, String.valueOf(exp));
    checkGuardedBy(expr.kind() != Kind.TYPE_LITERAL, "Raw type literal: %s", exp);
    return expr;
  }

  /**
   * A context containing the information necessary to resolve a {@link
   * com.sun.tools.javac.code.Symbol} from an AST node.
   *
   * <p>Guard expressions can be bound from the string value of an {@code @GuardedBy} annotation, or
   * from an actual java expression. In the first case, the string is parsed into an AST which will
   * not have any semantic information attached.
   */
  public interface Resolver {
    Symbol resolveIdentifier(IdentifierTree node);

    Symbol.MethodSymbol resolveMethod(MethodInvocationTree node, Name name);

    Symbol.MethodSymbol resolveMethod(
        MethodInvocationTree node, GuardedByExpression base, Name identifier);

    Symbol resolveSelect(GuardedByExpression base, MemberSelectTree node);

    Symbol resolveTypeLiteral(ExpressionTree expression);

    Symbol resolveEnclosingClass(ExpressionTree expression);
  }

  /** A resolver for AST nodes that have already been bound by javac. */
  static final Resolver ALREADY_BOUND_RESOLVER =
      new Resolver() {
        @Override
        public Symbol resolveIdentifier(IdentifierTree node) {
          return ASTHelpers.getSymbol(node);
        }

        @Override
        public Symbol.MethodSymbol resolveMethod(MethodInvocationTree node, Name name) {
          return ASTHelpers.getSymbol(node);
        }

        @Override
        public Symbol.MethodSymbol resolveMethod(
            MethodInvocationTree node, GuardedByExpression base, Name identifier) {
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
        public GuardedByExpression visitMethodInvocation(
            MethodInvocationTree node, BinderContext context) {
          checkGuardedBy(
              node.getArguments().isEmpty() && node.getTypeArguments().isEmpty(),
              "Only nullary methods are allowed.");
          ExpressionTree methodSelect = node.getMethodSelect();
          switch (methodSelect.getKind()) {
            case IDENTIFIER:
              {
                IdentifierTree identifier = (IdentifierTree) methodSelect;
                Symbol.MethodSymbol method =
                    context.resolver.resolveMethod(node, identifier.getName());
                checkGuardedBy(method != null, identifier.toString());
                return bindSelect(computeBase(context, method), method);
              }
            case MEMBER_SELECT:
              {
                MemberSelectTree select = (MemberSelectTree) methodSelect;
                GuardedByExpression base = visit(select.getExpression(), context);
                checkGuardedBy(base != null, select.getExpression().toString());
                Symbol.MethodSymbol method =
                    context.resolver.resolveMethod(node, base, select.getIdentifier());
                checkGuardedBy(method != null, select.toString());
                return bindSelect(normalizeBase(context, method, base), method);
              }
            default:
              throw new IllegalGuardedBy(methodSelect.getKind().toString());
          }
        }

        @Override
        public GuardedByExpression visitMemberSelect(MemberSelectTree node, BinderContext context) {

          String name = node.getIdentifier().toString();

          if (name.equals("this")) {
            Symbol base = context.resolver.resolveEnclosingClass(node.getExpression());
            if (context.thisClass == base) {
              return F.thisliteral();
            }
            return F.qualifiedThis(context.names, context.thisClass, base);
          }

          if (name.equals("class")) {
            Symbol base = context.resolver.resolveTypeLiteral(node.getExpression());
            return F.classLiteral(base);
          }

          GuardedByExpression base = visit(node.getExpression(), context);
          checkGuardedBy(base != null, "Bad expression: %s", node.getExpression());
          Symbol sym = context.resolver.resolveSelect(base, node);
          checkGuardedBy(sym != null, "Could not resolve: %s", node);
          // TODO(cushon): allow MethodSymbol here once clean-up is done
          checkGuardedBy(
              sym instanceof Symbol.VarSymbol /* || sym instanceof Symbol.MethodSymbol*/,
              "Bad member symbol: %s",
              sym.getClass());
          return bindSelect(normalizeBase(context, sym, base), sym);
        }

        private GuardedByExpression bindSelect(GuardedByExpression base, Symbol sym) {
          if (base.kind().equals(Kind.TYPE_LITERAL) && !sym.isStatic()) {
            throw new IllegalGuardedBy("Instance access on static: " + base + ", " + sym);
          }

          // TODO(cushon) - forbid static access on instance?

          return F.select(base, sym);
        }

        @Override
        public GuardedByExpression visitIdentifier(IdentifierTree node, BinderContext context) {
          Symbol symbol = context.resolver.resolveIdentifier(node);
          checkGuardedBy(symbol != null, "Could not resolve %s", node);
          if (symbol instanceof Symbol.VarSymbol) {
            Symbol.VarSymbol varSymbol = (Symbol.VarSymbol) symbol;
            switch (varSymbol.getKind()) {
              case LOCAL_VARIABLE:
              case PARAMETER:
                return F.localVariable(varSymbol);
              case FIELD:
                {
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

        @Override
        public GuardedByExpression visitParenthesized(
            ParenthesizedTree node, BinderContext context) {
          return node.getExpression().accept(this, context);
        }

        /**
         * Determines the implicit receiver of a select expression that accesses the given symbol by
         * simple name in the given resolution context.
         */
        private GuardedByExpression computeBase(BinderContext context, Symbol symbol) {
          return normalizeBase(context, symbol, null);
        }

        /**
         * Normalizes the receiver of a select expression so that accesses on 'this' are divided
         * into type names (for static accesses), qualified this accesses (for members of a
         * lexically enclosing scope), or simple this accesses for members of the current class.
         */
        private GuardedByExpression normalizeBase(
            BinderContext context, Symbol symbol, GuardedByExpression base) {
          if (symbol.isStatic()) {
            return F.typeLiteral(symbol.owner.enclClass());
          }

          if (base != null && base.kind() != GuardedByExpression.Kind.THIS) {
            return base;
          }

          if (symbol.isMemberOf(context.thisClass.type.tsym, context.types)) {
            return F.thisliteral();
          }

          Symbol lexicalOwner = isEnclosedIn(context.thisClass, symbol, context.types);
          if (lexicalOwner != null) {
            return F.qualifiedThis(context.names, context.thisClass, lexicalOwner);
          }

          throw new IllegalGuardedBy("Could not find the implicit receiver.");
        }

        /**
         * Returns the owner if the given member is declared in a lexically enclosing scope, and
         * {@code null} otherwise.
         */
        private ClassSymbol isEnclosedIn(ClassSymbol startingClass, Symbol member, Types types) {
          for (ClassSymbol scope = startingClass.owner.enclClass();
              scope != null;
              scope = scope.owner.enclClass()) {
            if (member.isMemberOf(scope.type.tsym, types)) {
              return scope;
            }
          }
          return null;
        }
      };
}
