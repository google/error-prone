/*
 * Copyright 2024 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.fixes.SuggestedFixes.prettyType;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.isEffectivelyPrivate;
import static com.google.errorprone.util.ASTHelpers.isStatic;
import static java.util.stream.Collectors.joining;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.google.common.graph.Traverser;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import java.util.regex.Pattern;
import javax.lang.model.element.ElementKind;

/** See the summary. */
@BugPattern(summary = "Possible class initialization deadlock", severity = WARNING)
public class ClassInitializationDeadlock extends BugChecker implements BugChecker.ClassTreeMatcher {

  private static final Pattern AUTO_VALUE_PREFIX = Pattern.compile("\\$*AutoValue_.*");

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    ClassSymbol classSymbol = getSymbol(tree);
    if (classSymbol.isInterface()
        && !ASTHelpers.scope(classSymbol.members())
            .anyMatch(ClassInitializationDeadlock::defaultMethod)) {
      // Interfaces are only recursively initialized by their subtypes if they declare any
      // non-abstract, non-static (i.e. default) methods, see JVMS 5.5.
      // This heuristic ignores interfaces that declare default methods directly, to be fully
      // correct we should consider arbitrary paths between the subtype and the interface that
      // pass through an intermediate interface with default methods.
      return NO_MATCH;
    }
    new SuppressibleTreePathScanner<Void, Void>(state) {

      @Override
      public Void visitClass(ClassTree node, Void unused) {
        for (Tree member : node.getMembers()) {
          if (member instanceof ClassTree) {
            continue;
          }
          scan(member, null);
        }
        return null;
      }

      @Override
      public Void visitBlock(BlockTree tree, Void unused) {
        if (tree.isStatic()) {
          scanForSubtypes(getCurrentPath(), classSymbol, state);
        }
        return null;
      }

      @Override
      public Void visitVariable(VariableTree tree, Void unused) {
        ExpressionTree initializer = tree.getInitializer();
        if (getSymbol(tree).isStatic() && initializer != null) {
          scanForSubtypes(new TreePath(getCurrentPath(), initializer), classSymbol, state);
        }
        return null;
      }
    }.scan(state.getPath(), null);
    return NO_MATCH;
  }

  private void scanForSubtypes(TreePath path, ClassSymbol classSymbol, VisitorState state) {
    new TreePathScanner<Void, Void>() {

      @Override
      public Void visitClass(ClassTree node, Void unused) {
        return null;
      }

      @Override
      public Void visitMethod(MethodTree node, Void unused) {
        return null;
      }

      @Override
      public Void visitMemberReference(MemberReferenceTree node, Void unused) {
        return null;
      }

      @Override
      public Void visitLambdaExpression(LambdaExpressionTree node, Void unused) {
        return null;
      }

      @Override
      public Void visitMemberSelect(MemberSelectTree tree, Void unused) {
        if (ASTHelpers.constValue(tree) != null) {
          return null;
        }
        if (tree.getIdentifier().contentEquals("class")) {
          return null;
        }
        handle(tree);
        return super.visitMemberSelect(tree, null);
      }

      @Override
      public Void visitIdentifier(IdentifierTree tree, Void unused) {
        if (ASTHelpers.constValue(tree) != null) {
          return null;
        }
        handle(tree);
        return null;
      }

      private void handle(ExpressionTree tree) {
        Symbol use = getSymbol(tree);
        if (!(use instanceof ClassSymbol)) {
          return;
        }
        if (use.equals(classSymbol)) {
          return;
        }
        if (!use.isSubClass(classSymbol, state.getTypes())) {
          return;
        }
        if (use.isEnclosedBy(classSymbol) && !isStatic(use)) {
          // Nested inner classes implicitly take the enclosing instance as a constructor parameter,
          // and can't be initialized without first initializing their containing class.
          return;
        }
        ImmutableSet<ClassSymbol> nonPrivateInstantiators =
            nonPrivateInstantiators((ClassSymbol) use, classSymbol, state.getTypes());
        if (nonPrivateInstantiators.isEmpty()) {
          return;
        }
        StringBuilder message = new StringBuilder();
        message.append(
            String.format(
                "Possible class initialization deadlock: %s is a subclass of the"
                    + " containing class %s",
                prettyType(use.asType(), state), prettyType(classSymbol.asType(), state)));
        if (!nonPrivateInstantiators.contains(use)) {
          message.append(
              String.format(
                  " (via %s, which can be initialized from outside the current file)",
                  nonPrivateInstantiators.stream()
                      .map(s -> prettyType(s.asType(), state))
                      .collect(joining(", "))));
        }
        state.reportMatch(buildDescription(tree).setMessage(message.toString()).build());
      }
    }.scan(path, null);
  }

  // Cycles involving classes that can't be instantiated outside the current file are usually
  // benign, since accessing them typically requires first accessing the enclosing class.
  // See also discussion of `private` classes in
  // https://errorprone.info/bugpattern/ClassInitializationDeadlock
  private ImmutableSet<ClassSymbol> nonPrivateInstantiators(
      ClassSymbol from, ClassSymbol to, Types types) {
    MutableGraph<ClassSymbol> superTypes = GraphBuilder.directed().build();
    superTypeClosure(from, to, types, superTypes);
    return Streams.stream(Traverser.forGraph(superTypes::predecessors).depthFirstPreOrder(to))
        .filter(current -> current != to && nonPrivateInstantiator(current))
        .collect(toImmutableSet());
  }

  private static void superTypeClosure(
      ClassSymbol current, ClassSymbol stop, Types types, MutableGraph<ClassSymbol> superTypes) {
    if (current == stop) {
      return;
    }
    for (Type t : types.directSupertypes(current.type)) {
      ClassSymbol s = (ClassSymbol) t.tsym;
      superTypes.putEdge(current, s);
      superTypeClosure(s, stop, types, superTypes);
    }
  }

  // Returns true if the class can't be instantiated outside the current compilation unit.
  boolean nonPrivateInstantiator(ClassSymbol use) {
    if (isEffectivelyPrivate(use)) {
      // If the class (or a containing class) is private, it can't be instantiated outside the
      // current unit.
      return false;
    }
    if (!ASTHelpers.scope(use.members())
        .anyMatch(ClassInitializationDeadlock::nonPrivateConstructorOrFactory)) {
      // If the class has no non-private constructors or static methods (which could be factory
      // methods), it can't be directly instantiated outside the current file.
      return false;
    }
    if (AUTO_VALUE_PREFIX.matcher(use.getSimpleName().toString()).matches()) {
      // AutoValue generated code is necessarily package-private, but should only be  accessed
      // within the declaration of the corresponding base class. See also the discussion of
      // AutoValue in
      // https://errorprone.info/bugpattern/ClassInitializationDeadlock
      return false;
    }
    return true;
  }

  private static boolean nonPrivateConstructorOrFactory(Symbol symbol) {
    if (symbol.isPrivate()) {
      return false;
    }
    return symbol.isConstructor() || isStatic(symbol);
  }

  private static boolean defaultMethod(Symbol s) {
    return s.getKind().equals(ElementKind.METHOD) && ((Symbol.MethodSymbol) s).isDefault();
  }
}
