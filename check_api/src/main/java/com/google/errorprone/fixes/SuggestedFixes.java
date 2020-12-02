/*
 * Copyright 2016 The Error Prone Authors.
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

package com.google.errorprone.fixes;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.collect.Iterables.getLast;
import static com.google.errorprone.util.ASTHelpers.getAnnotation;
import static com.google.errorprone.util.ASTHelpers.getAnnotationWithSimpleName;
import static com.google.errorprone.util.ASTHelpers.getModifiers;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.sun.source.tree.Tree.Kind.ASSIGNMENT;
import static com.sun.source.tree.Tree.Kind.CONDITIONAL_EXPRESSION;
import static com.sun.source.tree.Tree.Kind.NEW_ARRAY;
import static com.sun.tools.javac.code.TypeTag.CLASS;
import static com.sun.tools.javac.util.Position.NOPOS;
import static java.util.stream.Collectors.joining;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.base.Verify;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.Streams;
import com.google.common.io.CharStreams;
import com.google.errorprone.VisitorState;
import com.google.errorprone.apply.DescriptionBasedDiff;
import com.google.errorprone.apply.ImportOrganizer;
import com.google.errorprone.apply.SourceFile;
import com.google.errorprone.fixes.SuggestedFix.Builder;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.ErrorProneToken;
import com.google.errorprone.util.FindIdentifiers;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.ParamTree;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.DocSourcePositions;
import com.sun.source.util.DocTreePath;
import com.sun.source.util.DocTreeScanner;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.BoundKind;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Kinds.KindSelector;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types.DefaultTypeVisitor;
import com.sun.tools.javac.main.Arguments;
import com.sun.tools.javac.parser.Tokens;
import com.sun.tools.javac.parser.Tokens.Comment;
import com.sun.tools.javac.parser.Tokens.TokenKind;
import com.sun.tools.javac.tree.DCTree;
import com.sun.tools.javac.tree.DCTree.DCDocComment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Options;
import com.sun.tools.javac.util.Position;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.net.JarURLConnection;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleTypeVisitor8;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;

/** Factories for constructing {@link Fix}es. */
public class SuggestedFixes {

  /** Parse a modifier token into a {@link Modifier}. */
  @Nullable
  private static Modifier getTokModifierKind(ErrorProneToken tok) {
    switch (tok.kind()) {
      case PUBLIC:
        return Modifier.PUBLIC;
      case PROTECTED:
        return Modifier.PROTECTED;
      case PRIVATE:
        return Modifier.PRIVATE;
      case ABSTRACT:
        return Modifier.ABSTRACT;
      case STATIC:
        return Modifier.STATIC;
      case FINAL:
        return Modifier.FINAL;
      case TRANSIENT:
        return Modifier.TRANSIENT;
      case VOLATILE:
        return Modifier.VOLATILE;
      case SYNCHRONIZED:
        return Modifier.SYNCHRONIZED;
      case NATIVE:
        return Modifier.NATIVE;
      case STRICTFP:
        return Modifier.STRICTFP;
      case DEFAULT:
        return Modifier.DEFAULT;
      default:
        return null;
    }
  }

  /** Adds modifiers to the given class, method, or field declaration. */
  public static Optional<SuggestedFix> addModifiers(
      Tree tree, VisitorState state, Modifier... modifiers) {
    ModifiersTree originalModifiers = getModifiers(tree);
    if (originalModifiers == null) {
      return Optional.empty();
    }
    return addModifiers(tree, originalModifiers, state, new TreeSet<>(Arrays.asList(modifiers)));
  }

  /** Adds modifiers to the given declaration and corresponding modifiers tree. */
  public static Optional<SuggestedFix> addModifiers(
      Tree tree, ModifiersTree originalModifiers, VisitorState state, Set<Modifier> modifiers) {
    Set<Modifier> toAdd = Sets.difference(modifiers, originalModifiers.getFlags());
    SuggestedFix.Builder fix = SuggestedFix.builder();
    List<Modifier> modifiersToWrite = new ArrayList<>();
    if (!originalModifiers.getFlags().isEmpty()) {
      // a map from modifiers to modifier position (or -1 if the modifier is being added)
      // modifiers are sorted in Google Java Style order
      Map<Modifier, Integer> modifierPositions = new TreeMap<>();
      for (Modifier mod : toAdd) {
        modifierPositions.put(mod, -1);
      }
      List<ErrorProneToken> tokens = state.getOffsetTokensForNode(originalModifiers);
      for (ErrorProneToken tok : tokens) {
        Modifier mod = getTokModifierKind(tok);
        if (mod != null) {
          modifierPositions.put(mod, tok.pos());
        }
      }
      // walk the map of all modifiers, and accumulate a list of new modifiers to insert
      // beside an existing modifier
      modifierPositions.forEach(
          (mod, p) -> {
            if (p == -1) {
              modifiersToWrite.add(mod);
            } else if (!modifiersToWrite.isEmpty()) {
              fix.replace(p, p, Joiner.on(' ').join(modifiersToWrite) + " ");
              modifiersToWrite.clear();
            }
          });
    } else {
      modifiersToWrite.addAll(toAdd);
    }
    addRemainingModifiers(tree, state, originalModifiers, modifiersToWrite, fix);
    return Optional.of(fix.build());
  }

  private static void addRemainingModifiers(
      Tree tree,
      VisitorState state,
      ModifiersTree originalModifiers,
      Collection<Modifier> toAdd,
      SuggestedFix.Builder fix) {
    if (toAdd.isEmpty()) {
      return;
    }
    int insertPos;
    if (tree.getKind() == Tree.Kind.ANNOTATION_TYPE) {
      // For annotation types, the modifiers tree include the '@' of @interface. And all modifiers
      // must appear before @interface.
      int pos =
          Streams.findLast(
                  state.getOffsetTokensForNode(originalModifiers).stream()
                      .filter(tok -> tok.kind().equals(TokenKind.MONKEYS_AT)))
              .get()
              .pos();
      insertPos =
          state.getOffsetTokensForNode(tree).stream()
              .mapToInt(ErrorProneToken::pos)
              .filter(thisPos -> thisPos >= pos)
              .findFirst()
              .orElse(pos); // shouldn't ever be able to get to the else
    } else {
      int pos =
          state.getEndPosition(originalModifiers) == NOPOS
              ? getStartPosition(tree)
              : state.getEndPosition(originalModifiers) + 1;
      insertPos =
          state.getOffsetTokensForNode(originalModifiers).stream()
              .filter(t -> getTokModifierKind(t) != null)
              .mapToInt(t -> t.endPos() + 1)
              .max()
              .orElse(pos);
    }

    fix.replace(insertPos, insertPos, Joiner.on(' ').join(toAdd) + " ");
  }

  /** Removes modifiers from the given class, method, or field declaration. */
  public static Optional<SuggestedFix> removeModifiers(
      Tree tree, VisitorState state, Modifier... modifiers) {
    Set<Modifier> toRemove = ImmutableSet.copyOf(modifiers);
    ModifiersTree originalModifiers = getModifiers(tree);
    if (originalModifiers == null) {
      return Optional.empty();
    }
    return removeModifiers(originalModifiers, state, toRemove);
  }

  /** Removes modifiers to the given declaration and corresponding modifiers tree. */
  public static Optional<SuggestedFix> removeModifiers(
      ModifiersTree originalModifiers, VisitorState state, Set<Modifier> toRemove) {
    SuggestedFix.Builder fix = SuggestedFix.builder();
    List<ErrorProneToken> tokens = state.getOffsetTokensForNode(originalModifiers);
    boolean empty = true;
    for (ErrorProneToken tok : tokens) {
      Modifier mod = getTokModifierKind(tok);
      if (toRemove.contains(mod)) {
        empty = false;
        fix.replace(tok.pos(), tok.endPos() + 1, "");
      }
    }
    if (empty) {
      return Optional.empty();
    }
    return Optional.of(fix.build());
  }

  /**
   * Returns a human-friendly name of the given {@link Symbol} for use in fixes.
   *
   * <ul>
   *   <li>If the symbol is already in scope, its simple name is used.
   *   <li>If the symbol is a {@link Symbol.TypeSymbol} and an enclosing type is imported, that
   *       enclosing type is used as a qualified.
   *   <li>Otherwise the outermost enclosing type is imported and used as a qualifier.
   * </ul>
   */
  public static String qualifyType(VisitorState state, SuggestedFix.Builder fix, Symbol sym) {
    if (sym.getKind() == ElementKind.TYPE_PARAMETER) {
      return sym.getSimpleName().toString();
    }
    if (sym.getKind() == ElementKind.CLASS) {
      if (sym.isLocal()) {
        if (!sym.isAnonymous()) {
          return sym.getSimpleName().toString();
        }
        sym = ((ClassSymbol) sym).getSuperclass().tsym;
      }
    }
    if (variableClashInScope(state, sym)) {
      return qualifyType(state, fix, sym.owner) + "." + sym.getSimpleName();
    }
    Deque<String> names = new ArrayDeque<>();
    for (Symbol curr = sym; curr != null; curr = curr.owner) {
      names.addFirst(curr.getSimpleName().toString());
      Symbol found =
          FindIdentifiers.findIdent(curr.getSimpleName().toString(), state, KindSelector.VAL_TYP);
      if (found == curr) {
        break;
      }
      if (curr.owner != null && curr.owner.getKind() == ElementKind.PACKAGE) {
        // If the owner of curr is a package, we can't do anything except import or fully-qualify
        // the type name.
        if (found != null) {
          names.addFirst(curr.owner.getQualifiedName().toString());
        } else {
          fix.addImport(curr.getQualifiedName().toString());
        }
        break;
      }
    }
    return Joiner.on('.').join(names);
  }

  private static boolean variableClashInScope(VisitorState state, Symbol sym) {
    if (!sym.getKind().isField()) {
      return false;
    }
    MethodTree method = state.findEnclosing(MethodTree.class);
    if (method == null) {
      return false;
    }
    boolean[] result = {false};
    new TreeScanner<Void, Void>() {
      @Override
      public Void visitVariable(VariableTree tree, Void unused) {
        if (tree.getName().contentEquals(sym.getSimpleName())) {
          result[0] = true;
        }
        return super.visitVariable(tree, null);
      }
    }.scan(method, null);
    return result[0];
  }

  /** Returns a human-friendly name of the given type for use in fixes. */
  public static String qualifyType(VisitorState state, SuggestedFix.Builder fix, TypeMirror type) {
    return type.accept(
        new SimpleTypeVisitor8<String, SuggestedFix.Builder>() {
          @Override
          protected String defaultAction(TypeMirror e, Builder builder) {
            return e.toString();
          }

          @Override
          public String visitArray(ArrayType t, Builder builder) {
            return t.getComponentType().accept(this, builder) + "[]";
          }

          @Override
          public String visitDeclared(DeclaredType t, Builder builder) {
            String baseType = qualifyType(state, builder, ((Type) t).tsym);
            if (t.getTypeArguments().isEmpty()) {
              return baseType;
            }
            StringBuilder b = new StringBuilder(baseType);
            b.append('<');
            boolean started = false;
            for (TypeMirror arg : t.getTypeArguments()) {
              if (started) {
                b.append(',');
              }
              b.append(arg.accept(this, builder));
              started = true;
            }
            b.append('>');
            return b.toString();
          }
        },
        fix);
  }

  private static final Splitter COMPONENT_SPLITTER = Splitter.on('.');

  /**
   * Returns a human-friendly name of the given {@code typeName} for use in fixes.
   *
   * <p>This should be used if the type may not be loaded.
   *
   * @param typeName a qualified canonical type name, e.g. {@code java.util.Map.Entry}.
   */
  public static String qualifyType(VisitorState state, SuggestedFix.Builder fix, String typeName) {
    List<String> components = COMPONENT_SPLITTER.splitToList(typeName);
    // Check if the simple name is already visible.
    String simpleName = Iterables.getLast(components);
    Symbol simpleNameSymbol = FindIdentifiers.findIdent(simpleName, state, KindSelector.VAL_TYP);
    if (simpleNameSymbol != null
        && !simpleNameSymbol.getKind().equals(ElementKind.OTHER)
        && simpleNameSymbol.getQualifiedName().contentEquals(typeName)) {
      return simpleName;
    }

    for (int i = 0; i < components.size(); ++i) {
      String component = components.get(i);
      // If it's lowercase, probably a package name.
      if (!Character.isUpperCase(component.charAt(0))) {
        continue;
      }
      // The qualified name up to (and including) the component we're currently dealing with.
      String qualifiedName = components.subList(0, i + 1).stream().collect(joining("."));

      Symbol found = FindIdentifiers.findIdent(component, state, KindSelector.VAL_TYP);
      // No clashing name: import it and return.
      if (found == null) {
        fix.addImport(qualifiedName);
        return components.subList(i, components.size()).stream().collect(joining("."));
      }
      // Type already imported or otherwise visible.
      if (found.getQualifiedName().contentEquals(qualifiedName)) {
        return components.subList(i, components.size()).stream().collect(joining("."));
      }
    }
    return typeName;
  }

  /**
   * Provides a name to use for the (fully qualified) method provided in {@code qualifiedName},
   * trying to static import it if possible. Adds imports to {@code fix} as appropriate.
   */
  public static String qualifyStaticImport(
      String qualifiedName, SuggestedFix.Builder fix, VisitorState state) {
    String name = qualifiedName.substring(qualifiedName.lastIndexOf(".") + 1);
    AtomicBoolean foundConflict = new AtomicBoolean(false);
    new TreeScanner<Void, Void>() {

      @Override
      public Void visitIdentifier(IdentifierTree ident, Void unused) {
        process(ident);
        return super.visitIdentifier(ident, null);
      }

      private void process(IdentifierTree ident) {
        if (!ident.getName().contentEquals(name)) {
          return;
        }
        Symbol symbol = getSymbol(ident);
        if (symbol == null) {
          return;
        }
        String identifierQualifiedName =
            symbol.owner.getQualifiedName() + "." + symbol.getSimpleName();
        if (!qualifiedName.equals(identifierQualifiedName)) {
          foundConflict.set(true);
        }
      }
    }.scan(state.getPath().getCompilationUnit(), null);
    if (foundConflict.get()) {
      String className = qualifiedName.substring(0, qualifiedName.lastIndexOf("."));
      return qualifyType(state, fix, className) + "." + name;
    }
    fix.addStaticImport(qualifiedName);
    return name;
  }

  /** Replaces the leaf doctree in the given path with {@code replacement}. */
  public static void replaceDocTree(
      SuggestedFix.Builder fix, DocTreePath docPath, String replacement) {
    DocTree leaf = docPath.getLeaf();
    checkArgument(
        leaf instanceof DCTree.DCEndPosTree, "no end position information for %s", leaf.getKind());
    DCTree.DCEndPosTree<?> node = (DCTree.DCEndPosTree<?>) leaf;
    DCTree.DCDocComment comment = (DCTree.DCDocComment) docPath.getDocComment();
    fix.replace((int) node.getSourcePosition(comment), node.getEndPos(comment), replacement);
  }

  /**
   * Fully qualifies a javadoc reference, e.g. for replacing {@code {@link List}} with {@code {@link
   * java.util.List}}
   *
   * @param fix the fix builder to add to
   * @param docPath the path to a {@link DCTree.DCReference} element
   */
  public static void qualifyDocReference(
      SuggestedFix.Builder fix, DocTreePath docPath, VisitorState state) {

    DocTree leaf = docPath.getLeaf();
    checkArgument(
        leaf.getKind() == DocTree.Kind.REFERENCE,
        "expected a path to a reference, got %s instead",
        leaf.getKind());
    DCTree.DCReference reference = (DCTree.DCReference) leaf;

    Symbol sym = (Symbol) JavacTrees.instance(state.context).getElement(docPath);
    if (sym == null) {
      return;
    }
    String refString = reference.toString();
    String qualifiedName;
    int idx = refString.indexOf('#');
    if (idx >= 0) {
      qualifiedName = sym.owner.getQualifiedName() + refString.substring(idx, refString.length());
    } else {
      qualifiedName = sym.getQualifiedName().toString();
    }

    replaceDocTree(fix, docPath, qualifiedName);
  }

  /**
   * Instructs {@link #addMembers(ClassTree, VisitorState, AdditionPosition, String, String...)}
   * whether to add the new member(s) at the beginning of the class, or at the end.
   */
  public enum AdditionPosition {
    FIRST {

      @Override
      int pos(ClassTree tree, VisitorState state) {
        // We scan backwards from the first member, looking for the class's opening { token.
        int classStart = getStartPosition(tree);
        List<? extends Tree> members =
            tree.getMembers().stream()
                /* Throw away generated members, which may be synthetic, or whose start
                position may be the same as the class's. We only want to look at members defined
                in the source, so we can find a source position which is after the opening {.*/
                .filter(AdditionPosition::definedInSourceFile)
                .filter(member -> getStartPosition(member) > classStart)
                .collect(toImmutableList());
        if (members.isEmpty()) {
          // The approach for inserting first only works if there's a member, but if not then LAST
          // is just as good.
          return LAST.pos(tree, state);
        }
        JCTree firstMember = (JCTree) members.get(0);
        int firstMemberStart = firstMember.getStartPosition();
        List<ErrorProneToken> methodTokens = state.getOffsetTokens(classStart, firstMemberStart);
        ListIterator<ErrorProneToken> iter = methodTokens.listIterator(methodTokens.size());
        while (iter.hasPrevious()) {
          ErrorProneToken token = iter.previous();
          if (token.kind() == TokenKind.LBRACE) {
            return token.pos() + 1;
          }
        }
        throw new AssertionError("Found no open brace for class " + tree);
      }
    },
    LAST {
      @Override
      int pos(ClassTree tree, VisitorState state) {
        return state.getEndPosition(tree) - 1;
      }
    };

    /** The position at which to make changes */
    abstract int pos(ClassTree tree, VisitorState state);

    /**
     * An imprecise guess at whether the member is actually defined in a .java file, as opposed to
     * being generated by javac (e.g. a synthetic member or a generated constructor).
     */
    private static boolean definedInSourceFile(Tree member) {
      Symbol sym = getSymbol(member);
      if (sym == null) {
        return false;
      }
      if (member instanceof MethodTree && ASTHelpers.isGeneratedConstructor((MethodTree) member)) {
        return false;
      }

      return (sym.flags() & Flags.SYNTHETIC) == 0;
    }
  }

  /**
   * Returns a {@link Fix} that adds members defined by {@code firstMember} (and optionally {@code
   * otherMembers}) to the end of the class referenced by {@code classTree}. This method should only
   * be called once per {@link ClassTree} as the suggestions will otherwise collide.
   */
  public static SuggestedFix addMembers(
      ClassTree classTree, VisitorState state, String firstMember, String... otherMembers) {
    return addMembers(classTree, state, AdditionPosition.LAST, firstMember, otherMembers);
  }

  /**
   * Returns a {@link Fix} that adds members defined by {@code firstMember} (and optionally {@code
   * otherMembers}) to the class referenced by {@code classTree}. This method should only be called
   * once per {@link ClassTree} as the suggestions will otherwise collide.
   */
  public static SuggestedFix addMembers(
      ClassTree classTree,
      VisitorState state,
      AdditionPosition where,
      String firstMember,
      String... otherMembers) {
    checkNotNull(classTree);
    List<String> members = Lists.asList(firstMember, otherMembers);
    return addMembers(classTree, state, where, members).get();
  }

  /**
   * Returns a {@link Fix} that adds members defined by {@code members} to the class referenced by
   * {@code classTree}. This method should only be called once per {@link ClassTree} as the
   * suggestions will otherwise collide. It will return {@code Optional.empty()} if and only if
   * {@code members} is empty.
   */
  public static Optional<SuggestedFix> addMembers(
      ClassTree classTree, VisitorState state, AdditionPosition where, Iterable<String> members) {
    // Manually desugaring a foreach over members so that we can behave differently if it's empty.
    Iterator<String> items = members.iterator();
    if (!items.hasNext()) {
      return Optional.empty();
    }
    StringBuilder stringBuilder = new StringBuilder();
    do {
      String item = items.next();
      stringBuilder.append("\n\n").append(item);
    } while (items.hasNext());
    stringBuilder.append('\n');
    int pos = where.pos(classTree, state);
    return Optional.of(SuggestedFix.replace(pos, pos, stringBuilder.toString()));
  }

  /**
   * Renames the given {@link VariableTree} and its usages in the current compilation unit to {@code
   * replacement}.
   */
  public static SuggestedFix renameVariable(
      VariableTree tree, final String replacement, VisitorState state) {
    String name = tree.getName().toString();
    int typeEndPos = state.getEndPosition(tree.getType());
    // handle implicit lambda parameter types
    int searchOffset = typeEndPos == -1 ? 0 : (typeEndPos - getStartPosition(tree));
    int pos = getStartPosition(tree) + state.getSourceForNode(tree).indexOf(name, searchOffset);
    return SuggestedFix.builder()
        .replace(pos, pos + name.length(), replacement)
        .merge(renameVariableUsages(tree, replacement, state))
        .build();
  }

  /**
   * Renames usage of the given {@link VariableTree} in the current compilation unit to {@code
   * replacement}.
   */
  public static SuggestedFix renameVariableUsages(
      VariableTree tree, final String replacement, VisitorState state) {
    final SuggestedFix.Builder fix = SuggestedFix.builder();
    final Symbol.VarSymbol sym = getSymbol(tree);
    new TreeScanner<Void, Void>() {
      @Override
      public Void visitIdentifier(IdentifierTree tree, Void unused) {
        if (sym.equals(getSymbol(tree))) {
          fix.replace(tree, replacement);
        }
        return super.visitIdentifier(tree, null);
      }

      @Override
      public Void visitMemberSelect(MemberSelectTree tree, Void unused) {
        if (sym.equals(getSymbol(tree))) {
          fix.replace(
              state.getEndPosition(tree.getExpression()),
              state.getEndPosition(tree),
              "." + replacement);
        }
        return super.visitMemberSelect(tree, null);
      }
    }.scan(state.getPath().getCompilationUnit(), null);
    return fix.build();
  }

  /** Be warned, only changes method name at the declaration. */
  public static SuggestedFix renameMethod(
      MethodTree tree, final String replacement, VisitorState state) {
    // Search tokens from beginning of method tree to beginning of method body.
    int basePos = getStartPosition(tree);
    int endPos =
        tree.getBody() != null ? getStartPosition(tree.getBody()) : state.getEndPosition(tree);
    List<ErrorProneToken> methodTokens = state.getOffsetTokens(basePos, endPos);
    for (ErrorProneToken token : methodTokens) {
      if (token.kind() == TokenKind.IDENTIFIER && token.name().equals(tree.getName())) {
        return SuggestedFix.replace(token.pos(), token.endPos(), replacement);
      }
    }
    // Method name not found.
    throw new AssertionError();
  }

  /**
   * Renames the given {@link MethodTree} and its usages in the current compilation unit to {@code
   * replacement}.
   */
  public static SuggestedFix renameMethodWithInvocations(
      MethodTree tree, final String replacement, VisitorState state) {
    SuggestedFix.Builder fix = SuggestedFix.builder().merge(renameMethod(tree, replacement, state));
    MethodSymbol sym = getSymbol(tree);
    new TreeScanner<Void, Void>() {
      @Override
      public Void visitIdentifier(IdentifierTree tree, Void unused) {
        if (sym.equals(getSymbol(tree))) {
          fix.replace(tree, replacement);
        }
        return super.visitIdentifier(tree, null);
      }

      @Override
      public Void visitMemberSelect(MemberSelectTree tree, Void unused) {
        if (sym.equals(getSymbol(tree))) {
          fix.replace(
              state.getEndPosition(tree.getExpression()),
              state.getEndPosition(tree),
              "." + replacement);
        }
        return super.visitMemberSelect(tree, null);
      }
    }.scan(state.getPath().getCompilationUnit(), null);
    return fix.build();
  }

  /** Replaces the name of the method being invoked in {@code tree} with {@code replacement}. */
  public static SuggestedFix renameMethodInvocation(
      MethodInvocationTree tree, String replacement, VisitorState state) {
    Tree methodSelect = tree.getMethodSelect();
    Name identifier;
    int startPos;
    if (methodSelect instanceof MemberSelectTree) {
      identifier = ((MemberSelectTree) methodSelect).getIdentifier();
      startPos = state.getEndPosition(((MemberSelectTree) methodSelect).getExpression());
    } else if (methodSelect instanceof IdentifierTree) {
      identifier = ((IdentifierTree) methodSelect).getName();
      startPos = getStartPosition(tree);
    } else {
      throw malformedMethodInvocationTree(tree);
    }
    int endPos =
        tree.getArguments().isEmpty()
            ? state.getEndPosition(tree)
            : getStartPosition(tree.getArguments().get(0));
    List<ErrorProneToken> tokens = state.getOffsetTokens(startPos, endPos);
    for (ErrorProneToken token : Lists.reverse(tokens)) {
      if (token.kind() == TokenKind.IDENTIFIER && token.name().equals(identifier)) {
        return SuggestedFix.replace(token.pos(), token.endPos(), replacement);
      }
    }
    throw malformedMethodInvocationTree(tree);
  }

  private static IllegalStateException malformedMethodInvocationTree(MethodInvocationTree tree) {
    return new IllegalStateException(
        String.format("Couldn't replace the method name in %s.", tree));
  }

  /**
   * Renames a type parameter {@code typeParameter} owned by {@code owningTree} to {@code
   * typeVarReplacement}. Renames occurrences in Javadoc as well.
   */
  public static SuggestedFix renameTypeParameter(
      TypeParameterTree typeParameter,
      Tree owningTree,
      String typeVarReplacement,
      VisitorState state) {
    Symbol typeParameterSymbol = getSymbol(typeParameter);

    // replace only the type parameter name (and not any upper bounds)
    String name = typeParameter.getName().toString();
    int pos = getStartPosition(typeParameter);
    Builder fixBuilder =
        SuggestedFix.builder().replace(pos, pos + name.length(), typeVarReplacement);

    new TreeScanner<Void, Void>() {
      @Override
      public Void visitIdentifier(IdentifierTree tree, Void unused) {
        Symbol identSym = getSymbol(tree);
        if (Objects.equal(identSym, typeParameterSymbol)) {
          // Lambda parameters can be desugared early, so we need to make sure the source
          // is there. In the example below, we would try to suggest replacing the node 't'
          // with T2, since the compiler desugars to g((T t) -> false). The extra condition
          // prevents us from doing that.

          // Foo<T> {
          //   <G> void g(Predicate<G> p) {},
          //   <T> void blah() {
          //     g(t -> false);
          //   }
          // }
          if (Objects.equal(state.getSourceForNode(tree), name)) {
            fixBuilder.replace(tree, typeVarReplacement);
          }
        }
        return super.visitIdentifier(tree, null);
      }
    }.scan(owningTree, null);
    DCDocComment docCommentTree =
        (DCDocComment) JavacTrees.instance(state.context).getDocCommentTree(state.getPath());
    if (docCommentTree != null) {
      docCommentTree.accept(
          new DocTreeScanner<Void, Void>() {
            @Override
            public Void visitParam(ParamTree paramTree, Void unused) {
              if (paramTree.isTypeParameter()
                  && paramTree.getName().getName().contentEquals(name)) {
                DocSourcePositions positions =
                    JavacTrees.instance(state.context).getSourcePositions();
                CompilationUnitTree compilationUnitTree = state.getPath().getCompilationUnit();
                int startPos =
                    (int)
                        positions.getStartPosition(
                            compilationUnitTree, docCommentTree, paramTree.getName());
                int endPos =
                    (int)
                        positions.getEndPosition(
                            compilationUnitTree, docCommentTree, paramTree.getName());
                fixBuilder.replace(startPos, endPos, typeVarReplacement);
              }
              return super.visitParam(paramTree, null);
            }
          },
          null);
    }
    return fixBuilder.build();
  }

  /** Deletes the given exceptions from a method's throws clause. */
  public static Fix deleteExceptions(
      MethodTree tree, final VisitorState state, List<ExpressionTree> toDelete) {
    List<? extends ExpressionTree> trees = tree.getThrows();
    if (toDelete.size() == trees.size()) {
      return SuggestedFix.replace(
          getThrowsPosition(tree, state) - 1, state.getEndPosition(getLast(trees)), "");
    }
    String replacement =
        FluentIterable.from(tree.getThrows())
            .filter(Predicates.not(Predicates.in(toDelete)))
            .transform(
                new Function<ExpressionTree, String>() {
                  @Override
                  @Nullable
                  public String apply(ExpressionTree input) {
                    return state.getSourceForNode(input);
                  }
                })
            .join(Joiner.on(", "));
    return SuggestedFix.replace(
        getStartPosition(tree.getThrows().get(0)),
        state.getEndPosition(getLast(tree.getThrows())),
        replacement);
  }

  private static int getThrowsPosition(MethodTree tree, VisitorState state) {
    for (ErrorProneToken token : state.getOffsetTokensForNode(tree)) {
      if (token.kind() == Tokens.TokenKind.THROWS) {
        return token.pos();
      }
    }
    throw new AssertionError();
  }

  /**
   * Returns a fix that adds a {@code @SuppressWarnings(warningToSuppress)} to the closest
   * suppressible element to the node pointed at by {@code state.getPath()}.
   *
   * @see #addSuppressWarnings(VisitorState, String, String)
   */
  @Nullable
  public static Fix addSuppressWarnings(VisitorState state, String warningToSuppress) {
    return addSuppressWarnings(state, warningToSuppress, null);
  }

  /**
   * Returns a fix that adds a {@code @SuppressWarnings(warningToSuppress)} to the closest
   * suppressible element to the node pointed at by {@code state.getPath()}, optionally suffixing
   * the suppression with a comment suffix (e.g. a reason for the suppression).
   *
   * <p>If the closest suppressible element already has a @SuppressWarning annotation,
   * warningToSuppress will be added to the value in {@code @SuppressWarnings} instead.
   *
   * <p>In the event that a suppressible element couldn't be found (e.g.: the state is pointing at a
   * CompilationUnit, or some other internal inconsistency has occurred), or the enclosing
   * suppressible element already has a {@code @SuppressWarnings} annotation with {@code
   * warningToSuppress}, this method will return null.
   */
  @Nullable
  public static Fix addSuppressWarnings(
      VisitorState state, String warningToSuppress, @Nullable String lineComment) {
    Builder fixBuilder = SuggestedFix.builder();
    addSuppressWarnings(fixBuilder, state, warningToSuppress, lineComment);
    return fixBuilder.isEmpty() ? null : fixBuilder.build();
  }

  /**
   * Modifies {@code fixBuilder} to either create a new {@code @SuppressWarnings} element on the
   * closest suppressible node, or add {@code warningToSuppress} to that node if there's already a
   * {@code SuppressWarnings} annotation there.
   *
   * @see #addSuppressWarnings(VisitorState, String, String)
   */
  public static void addSuppressWarnings(
      Builder fixBuilder, VisitorState state, String warningToSuppress) {
    addSuppressWarnings(fixBuilder, state, warningToSuppress, null);
  }

  /**
   * Modifies {@code fixBuilder} to either create a new {@code @SuppressWarnings} element on the
   * closest suppressible node, or add {@code warningToSuppress} to that node if there's already a
   * {@code SuppressWarnings} annotation there.
   *
   * @param warningToSuppress the warning to be suppressed, without the surrounding annotation. For
   *     example, to produce {@code @SuppressWarnings("Foo")}, pass {@code Foo}.
   * @param lineComment if non-null, the {@code @SuppressWarnings} will be prefixed by a line
   *     comment containing this text. Do not pass leading {@code //} or include any line breaks.
   * @see #addSuppressWarnings(VisitorState, String, String)
   */
  public static void addSuppressWarnings(
      Builder fixBuilder,
      VisitorState state,
      String warningToSuppress,
      @Nullable String lineComment) {
    addSuppressWarnings(fixBuilder, state, warningToSuppress, lineComment, true);
  }
  /**
   * Modifies {@code fixBuilder} to either remove a {@code warningToRemove} warning from the closest
   * {@code SuppressWarning} node or remove the entire {@code SuppressWarning} node if {@code
   * warningToRemove} is the only warning in that node.
   */
  public static void removeSuppressWarnings(
      SuggestedFix.Builder fixBuilder, VisitorState state, String warningToRemove) {
    // Find the nearest tree to remove @SuppressWarnings from.
    Tree suppressibleNode = suppressibleNode(state.getPath());
    if (suppressibleNode == null) {
      return;
    }

    AnnotationTree suppressAnnotationTree =
        getAnnotationWithSimpleName(
            findAnnotationsTree(suppressibleNode), SuppressWarnings.class.getSimpleName());
    if (suppressAnnotationTree == null) {
      return;
    }

    SuppressWarnings annotation = getAnnotation(suppressibleNode, SuppressWarnings.class);
    ImmutableSet<String> warningsSuppressed = ImmutableSet.copyOf(annotation.value());
    ImmutableSet<String> newWarningSet =
        warningsSuppressed.stream()
            .filter(warning -> !warning.equals(warningToRemove))
            .map(warningToKeep -> state.getElements().getConstantExpression(warningToKeep))
            .collect(toImmutableSet());
    if (newWarningSet.size() == warningsSuppressed.size()) {
      // no matches found. Nothing to delete.
      return;
    }
    if (newWarningSet.isEmpty()) {
      // No warning left to suppress. Delete entire annotation.
      fixBuilder.delete(suppressAnnotationTree);
      return;
    }
    fixBuilder.merge(
        updateAnnotationArgumentValues(suppressAnnotationTree, "value", newWarningSet));
  }

  /**
   * Modifies {@code fixBuilder} to either create a new {@code @SuppressWarnings} element on the
   * closest suppressible node, or add {@code warningToSuppress} to that node if there's already a
   * {@code SuppressWarnings} annotation there.
   *
   * @param warningToSuppress the warning to be suppressed, without the surrounding annotation. For
   *     example, to produce {@code @SuppressWarnings("Foo")}, pass {@code Foo}.
   * @param lineComment if non-null, the {@code @SuppressWarnings} will have this comment associated
   *     with it. Do not pass leading {@code //} or include any line breaks.
   * @param commentOnNewLine if false, and this suppression results in a new annotation, the line
   *     comment will be added on the same line as the {@code @SuppressWarnings} annotation. In
   *     other cases, the line comment will be on its own line.
   * @see #addSuppressWarnings(VisitorState, String, String)
   */
  public static void addSuppressWarnings(
      Builder fixBuilder,
      VisitorState state,
      String warningToSuppress,
      @Nullable String lineComment,
      boolean commentOnNewLine) {
    // Find the nearest tree to add @SuppressWarnings to.
    Tree suppressibleNode = suppressibleNode(state.getPath());
    if (suppressibleNode == null) {
      return;
    }

    SuppressWarnings existingAnnotation = getAnnotation(suppressibleNode, SuppressWarnings.class);
    String suppression = state.getTreeMaker().Literal(CLASS, warningToSuppress).toString();

    // Line comment to add, if it is present.
    Optional<String> formattedLineComment =
        Optional.ofNullable(lineComment).map(s -> "// " + s + "\n");

    // If we have an existing @SuppressWarnings on the element, extend its value
    if (existingAnnotation != null) {
      // Add warning to the existing annotation
      String[] values = existingAnnotation.value();
      if (Arrays.asList(values).contains(warningToSuppress)) {
        // The nearest suppress warnings already contains this thing, so we can't add another thing
        return;
      }
      AnnotationTree suppressAnnotationTree =
          getAnnotationWithSimpleName(
              findAnnotationsTree(suppressibleNode), SuppressWarnings.class.getSimpleName());
      if (suppressAnnotationTree == null) {
        // This is weird, bail out
        return;
      }

      fixBuilder.merge(
          addValuesToAnnotationArgument(
              suppressAnnotationTree, "value", ImmutableList.of(suppression), state));
      formattedLineComment.ifPresent(lc -> fixBuilder.prefixWith(suppressAnnotationTree, lc));
    } else {
      // Otherwise, add a suppress annotation to the element
      String replacement =
          commentOnNewLine
              ? formattedLineComment.orElse("") + "@SuppressWarnings(" + suppression + ") "
              : "@SuppressWarnings(" + suppression + ") " + formattedLineComment.orElse("");

      fixBuilder.prefixWith(suppressibleNode, replacement);
    }
  }

  private static List<? extends AnnotationTree> findAnnotationsTree(Tree tree) {
    ModifiersTree maybeModifiers = getModifiers(tree);
    return maybeModifiers == null ? ImmutableList.of() : maybeModifiers.getAnnotations();
  }

  @Nullable
  private static Tree suppressibleNode(TreePath path) {
    return StreamSupport.stream(path.spliterator(), false)
        .filter(
            tree ->
                tree instanceof MethodTree
                    // Anonymous classes can't be suppressed
                    || (tree instanceof ClassTree
                        && ((ClassTree) tree).getSimpleName().length() != 0)
                    // Lambda parameters can't be suppressed unless they have Type decls
                    || (tree instanceof VariableTree
                        && getStartPosition(((VariableTree) tree).getType()) != -1))
        .findFirst()
        .orElse(null);
  }

  /**
   * Returns a fix that appends {@code newValues} to the {@code parameterName} argument for {@code
   * annotation}, regardless of whether there is already an argument.
   *
   * <p>N.B.: {@code newValues} are source-code strings, not string literal values.
   */
  public static Builder addValuesToAnnotationArgument(
      AnnotationTree annotation,
      String parameterName,
      Collection<String> newValues,
      VisitorState state) {
    if (annotation.getArguments().isEmpty()) {
      String parameterPrefix = parameterName.equals("value") ? "" : (parameterName + " = ");
      return SuggestedFix.builder()
          .replace(
              annotation,
              annotation
                  .toString()
                  .replaceFirst("\\(\\)", "(" + parameterPrefix + newArgument(newValues) + ")"));
    }
    Optional<ExpressionTree> maybeExistingArgument = findArgument(annotation, parameterName);
    if (!maybeExistingArgument.isPresent()) {
      return SuggestedFix.builder()
          .prefixWith(
              annotation.getArguments().get(0),
              parameterName + " = " + newArgument(newValues) + ", ");
    }

    ExpressionTree existingArgument = maybeExistingArgument.get();
    if (!existingArgument.getKind().equals(NEW_ARRAY)) {
      return SuggestedFix.builder()
          .replace(
              existingArgument, newArgument(state.getSourceForNode(existingArgument), newValues));
    }

    NewArrayTree newArray = (NewArrayTree) existingArgument;
    if (newArray.getInitializers().isEmpty()) {
      return SuggestedFix.builder().replace(newArray, newArgument(newValues));
    } else {
      return SuggestedFix.builder()
          .postfixWith(getLast(newArray.getInitializers()), ", " + Joiner.on(", ").join(newValues));
    }
  }

  /**
   * Returns a fix that updates {@code newValues} to the {@code parameterName} argument for {@code
   * annotation}, regardless of whether there is already an argument.
   *
   * <p>N.B.: {@code newValues} are source-code strings, not string literal values.
   */
  public static Builder updateAnnotationArgumentValues(
      AnnotationTree annotation, String parameterName, Collection<String> newValues) {
    if (annotation.getArguments().isEmpty()) {
      String parameterPrefix = parameterName.equals("value") ? "" : (parameterName + " = ");
      return SuggestedFix.builder()
          .replace(
              annotation,
              annotation
                  .toString()
                  .replaceFirst("\\(\\)", "(" + parameterPrefix + newArgument(newValues) + ")"));
    }
    Optional<ExpressionTree> maybeExistingArgument = findArgument(annotation, parameterName);
    if (!maybeExistingArgument.isPresent()) {
      return SuggestedFix.builder()
          .prefixWith(
              annotation.getArguments().get(0),
              parameterName + " = " + newArgument(newValues) + ", ");
    }

    ExpressionTree existingArgument = maybeExistingArgument.get();
    return SuggestedFix.builder().replace(existingArgument, newArgument(newValues));
  }

  private static String newArgument(String existingParameters, Collection<String> initializers) {
    return newArgument(
        ImmutableList.<String>builder().add(existingParameters).addAll(initializers).build());
  }

  private static String newArgument(Collection<String> initializers) {
    StringBuilder expression = new StringBuilder();
    if (initializers.isEmpty()) {
      return "{}";
    }
    if (initializers.size() > 1) {
      expression.append('{');
    }
    Joiner.on(", ").appendTo(expression, initializers);
    if (initializers.size() > 1) {
      expression.append('}');
    }
    return expression.toString();
  }

  private static Optional<ExpressionTree> findArgument(
      AnnotationTree annotation, String parameter) {
    for (ExpressionTree argument : annotation.getArguments()) {
      if (argument.getKind().equals(ASSIGNMENT)) {
        AssignmentTree assignment = (AssignmentTree) argument;
        if (assignment.getVariable().toString().equals(parameter)) {
          return Optional.of(ASTHelpers.stripParentheses(assignment.getExpression()));
        }
      }
    }
    return Optional.empty();
  }

  /**
   * Returns true if the current compilation would succeed with the given fix applied. Note that
   * calling this method is very expensive as it requires rerunning the entire compile, so it should
   * be used with restraint.
   */
  public static boolean compilesWithFix(Fix fix, VisitorState state) {
    return compilesWithFix(fix, state, ImmutableList.of(), false);
  }

  /**
   * Returns true if the current compilation would succeed with the given fix applied, using the
   * given additional compiler options, optionally limiting the checking of compilation failures to
   * the compilation unit in which the fix is applied. Note that calling this method is very
   * expensive as it requires rerunning the entire compile, so it should be used with restraint.
   */
  public static boolean compilesWithFix(
      Fix fix,
      VisitorState state,
      ImmutableList<String> extraOptions,
      boolean onlyInSameCompilationUnit) {
    ImmutableList.Builder<String> extraOptionsBuilder =
        ImmutableList.<String>builder().addAll(extraOptions);
    int maxErrors = findOptionOrAppend(extraOptionsBuilder, extraOptions, "-Xmaxerrs", 100);
    int maxWarnings = findOptionOrAppend(extraOptionsBuilder, extraOptions, "-Xmaxwarns", 100);
    return compilesWithFix(
        fix, state, extraOptionsBuilder.build(), onlyInSameCompilationUnit, maxErrors, maxWarnings);
  }

  private static int findOptionOrAppend(
      ImmutableList.Builder<String> newOptions,
      ImmutableList<String> extraOptions,
      String key,
      int defaultValue) {
    int pos = extraOptions.lastIndexOf(key);
    int value;
    if (pos >= 0) {
      // The maximum number of errors was explicitly set.
      value = Integer.parseInt(extraOptions.get(pos + 1));
    } else {
      // The maximum number of errors was not set - pick a default value.
      value = defaultValue;
      newOptions.add(key).add("" + defaultValue);
    }
    return value;
  }

  private static boolean compilesWithFix(
      Fix fix,
      VisitorState state,
      ImmutableList<String> extraOptions,
      boolean onlyInSameCompilationUnit,
      int maxErrors,
      int maxWarnings) {
    if (fix.isEmpty() && extraOptions.isEmpty()) {
      return true;
    }

    JCCompilationUnit compilationUnit = (JCCompilationUnit) state.getPath().getCompilationUnit();
    JavaFileObject modifiedFile = compilationUnit.getSourceFile();
    BasicJavacTask javacTask = (BasicJavacTask) state.context.get(JavacTask.class);
    if (javacTask == null) {
      throw new IllegalArgumentException("No JavacTask in context.");
    }
    Arguments arguments = Arguments.instance(javacTask.getContext());
    List<JavaFileObject> fileObjects = new ArrayList<>(arguments.getFileObjects());
    URI modifiedFileUri = modifiedFile.toUri();
    for (int i = 0; i < fileObjects.size(); i++) {
      final JavaFileObject oldFile = fileObjects.get(i);
      if (modifiedFileUri.equals(oldFile.toUri())) {
        DescriptionBasedDiff diff =
            DescriptionBasedDiff.create(compilationUnit, ImportOrganizer.STATIC_FIRST_ORGANIZER);
        diff.handleFix(fix);
        SourceFile fixSource;
        try {
          fixSource =
              new SourceFile(
                  modifiedFile.getName(),
                  modifiedFile.getCharContent(false /*ignoreEncodingErrors*/));
        } catch (IOException e) {
          return false;
        }
        diff.applyDifferences(fixSource);
        fileObjects.set(
            i,
            new SimpleJavaFileObject(sourceURI(modifiedFile.toUri()), Kind.SOURCE) {
              @Override
              public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
                return fixSource.getAsSequence();
              }
            });
        break;
      }
    }
    DiagnosticCollector<JavaFileObject> diagnosticListener = new DiagnosticCollector<>();
    Context context = new Context();
    Options options = Options.instance(context);
    Options originalOptions = Options.instance(javacTask.getContext());
    for (String key : originalOptions.keySet()) {
      String value = originalOptions.get(key);
      if (key.equals("-Xplugin:") && value.startsWith("ErrorProne")) {
        // When using the -Xplugin Error Prone integration, disable Error Prone for speculative
        // recompiles to avoid infinite recursion.
        continue;
      }
      if (SOURCE_TARGET_OPTIONS.contains(key) && originalOptions.isSet("--release")) {
        // javac does not allow -source and -target to be specified explicitly when --release is,
        // but does add them in response to passing --release. Here we invert that operation.
        continue;
      }
      options.put(key, value);
    }
    JavacTask newTask =
        JavacTool.create()
            .getTask(
                CharStreams.nullWriter(),
                state.context.get(JavaFileManager.class),
                diagnosticListener,
                extraOptions,
                arguments.getClassNames(),
                fileObjects,
                context);
    try {
      newTask.analyze();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    // If we reached the maximum number of diagnostics of a given kind without finding one in the
    // modified compilation unit, we won't find any more diagnostics, but we can't be sure that
    // there isn't an diagnostic, as the diagnostic may simply be the (max+1)-th diagnostic, and
    // thus was dropped.
    int countErrors = 0;
    int countWarnings = 0;
    boolean warningIsError = false;
    boolean warningInSameCompilationUnit = false;
    for (Diagnostic<? extends JavaFileObject> diagnostic : diagnosticListener.getDiagnostics()) {
      warningIsError |= diagnostic.getCode().equals("compiler.err.warnings.and.werror");
      boolean diagnosticInSameCompilationUnit =
          diagnostic.getSource().toUri().equals(modifiedFileUri);
      switch (diagnostic.getKind()) {
        case ERROR:
          ++countErrors;
          if (!onlyInSameCompilationUnit || diagnosticInSameCompilationUnit) {
            return false;
          }
          break;
        case WARNING:
          ++countWarnings;
          warningInSameCompilationUnit |= diagnosticInSameCompilationUnit;
          break;
        default:
          continue;
      }

      if ((warningIsError && warningInSameCompilationUnit)
          || (countErrors >= maxErrors)
          || (countWarnings >= maxWarnings)) {
        return false;
      }
    }
    return true;
  }

  private static final ImmutableSet<String> SOURCE_TARGET_OPTIONS =
      ImmutableSet.of("-source", "--source", "-target", "--target");

  /** Create a plausible URI to use in {@link #compilesWithFix}. */
  @VisibleForTesting
  static URI sourceURI(URI uri) {
    if (!uri.getScheme().equals("jar")) {
      return uri;
    }
    try {
      return URI.create(
          "file:/" + ((JarURLConnection) uri.toURL().openConnection()).getEntryName());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Pretty-prints a Type for use in diagnostic messages, qualifying any enclosed type names using
   * {@link #qualifyType}}.
   */
  public static String prettyType(Type type, @Nullable VisitorState state) {
    return prettyType(state, /* existingFix= */ null, type);
  }

  /**
   * Pretty-prints a Type for use in fixes, qualifying any enclosed type names using {@link
   * #qualifyType}}.
   */
  public static String prettyType(
      @Nullable VisitorState state, @Nullable SuggestedFix.Builder existingFix, Type type) {
    SuggestedFix.Builder fix = existingFix == null ? SuggestedFix.builder() : existingFix;
    return type.accept(
        new DefaultTypeVisitor<String, Void>() {
          @Override
          public String visitWildcardType(Type.WildcardType t, Void unused) {
            StringBuilder sb = new StringBuilder();
            sb.append(t.kind);
            if (t.kind != BoundKind.UNBOUND) {
              sb.append(t.type.accept(this, null));
            }
            return sb.toString();
          }

          @Override
          public String visitClassType(Type.ClassType t, Void unused) {
            StringBuilder sb = new StringBuilder();
            if (state == null) {
              sb.append(t.tsym.getSimpleName());
            } else {
              sb.append(qualifyType(state, fix, t.tsym));
            }
            if (t.getTypeArguments().nonEmpty()) {
              sb.append('<');
              sb.append(
                  t.getTypeArguments().stream()
                      .map(a -> a.accept(this, null))
                      .collect(joining(", ")));
              sb.append(">");
            }
            return sb.toString();
          }

          @Override
          public String visitCapturedType(Type.CapturedType t, Void unused) {
            return t.wildcard.accept(this, null);
          }

          @Override
          public String visitArrayType(Type.ArrayType t, Void unused) {
            return t.elemtype.accept(this, null) + "[]";
          }

          @Override
          public String visitType(Type t, Void unused) {
            return t.toString();
          }
        },
        null);
  }

  /**
   * Create a fix to add a suppression annotation on the surrounding class.
   *
   * <p>No suggested fix is produced if the suppression annotation cannot be used on classes, i.e.
   * the annotation has a {@code @Target} but does not include {@code @Target(TYPE)}.
   *
   * <p>If the suggested annotation is {@code DontSuggestFixes}, return empty.
   */
  public static Optional<SuggestedFix> suggestExemptingAnnotation(
      String exemptingAnnotation, TreePath where, VisitorState state) {
    // TODO(bangert): Support annotations that do not have @Target(CLASS).
    if (exemptingAnnotation.equals("com.google.errorprone.annotations.DontSuggestFixes")) {
      return Optional.empty();
    }
    SuggestedFix.Builder builder = SuggestedFix.builder();
    Type exemptingAnnotationType = state.getTypeFromString(exemptingAnnotation);
    ImmutableSet<Tree.Kind> supportedExemptingAnnotationLocationKinds;
    String annotationName;

    if (exemptingAnnotationType != null) {
      supportedExemptingAnnotationLocationKinds =
          supportedTreeTypes(exemptingAnnotationType.asElement());
      annotationName = qualifyType(state, builder, exemptingAnnotationType);
    } else {
      // If we can't resolve the type, fall back to an approximation.
      int idx = exemptingAnnotation.lastIndexOf('.');
      Verify.verify(idx > 0 && idx + 1 < exemptingAnnotation.length());
      supportedExemptingAnnotationLocationKinds = TREE_TYPE_UNKNOWN_ANNOTATION;
      annotationName = exemptingAnnotation.substring(idx + 1);
      builder.addImport(exemptingAnnotation);
    }
    Optional<Tree> exemptingAnnotationLocation =
        StreamSupport.stream(where.spliterator(), false)
            .filter(tree -> supportedExemptingAnnotationLocationKinds.contains(tree.getKind()))
            .filter(Predicates.not(SuggestedFixes::isAnonymousClassTree))
            .findFirst();

    return exemptingAnnotationLocation.map(
        location -> builder.prefixWith(location, "@" + annotationName + " ").build());
  }

  private static boolean isAnonymousClassTree(Tree t) {
    if (t instanceof ClassTree) {
      ClassTree classTree = (ClassTree) t;
      return classTree.getSimpleName().contentEquals("");
    }
    return false;
  }

  /**
   * We assume annotations with an unknown type can be used on these Tree kinds.
   *
   * <p>These are reasonable for exempting annotations which annotate a block of code, e.g. they
   * don't usually make sense on a variable declaration.
   */
  private static final ImmutableSet<Tree.Kind> TREE_TYPE_UNKNOWN_ANNOTATION =
      ImmutableSet.of(
          Tree.Kind.CLASS,
          Tree.Kind.ENUM,
          Tree.Kind.INTERFACE,
          Tree.Kind.ANNOTATION_TYPE,
          Tree.Kind.METHOD);

  /** Returns true iff {@code suggestExemptingAnnotation()} supports this annotation. */
  public static boolean suggestedExemptingAnnotationSupported(Element exemptingAnnotation) {
    return !supportedTreeTypes(exemptingAnnotation).isEmpty();
  }

  private static ImmutableSet<Tree.Kind> supportedTreeTypes(Element exemptingAnnotation) {
    Target targetAnnotation = exemptingAnnotation.getAnnotation(Target.class);
    if (targetAnnotation == null) {
      // in the absence of further information, we assume the annotation is supported on classes and
      // methods.
      return TREE_TYPE_UNKNOWN_ANNOTATION;
    }
    ImmutableSet.Builder<Tree.Kind> types = ImmutableSet.builder();
    for (ElementType t : targetAnnotation.value()) {
      switch (t) {
        case TYPE:
          types.add(
              Tree.Kind.CLASS, Tree.Kind.ENUM, Tree.Kind.INTERFACE, Tree.Kind.ANNOTATION_TYPE);
          break;
        case METHOD:
          types.add(Tree.Kind.METHOD);
          break;
        default:
          break;
      }
    }
    return types.build();
  }

  /**
   * Replaces the tree at {@code path} along with any Javadocs/associated single-line comments.
   *
   * <p>This is the same as just deleting the tree for non-class members. For class members, we
   * tokenize and scan backwards to try to work out which prior comments are associated with this
   * node.
   */
  public static SuggestedFix replaceIncludingComments(
      TreePath path, String replacement, VisitorState state) {
    Tree tree = path.getLeaf();
    Tree parent = path.getParentPath().getLeaf();
    if (!(parent instanceof ClassTree)) {
      return SuggestedFix.replace(tree, replacement);
    }
    Tree previousMember = null;
    ClassTree classTree = (ClassTree) parent;
    int startTokenization;
    for (Tree member : classTree.getMembers()) {
      if (member instanceof MethodTree && ASTHelpers.isGeneratedConstructor((MethodTree) member)) {
        continue;
      }
      if (member.equals(tree)) {
        break;
      }
      previousMember = member;
    }
    if (previousMember != null) {
      startTokenization = state.getEndPosition(previousMember);
    } else if (state.getEndPosition(classTree.getModifiers()) == Position.NOPOS) {
      startTokenization = getStartPosition(classTree);
    } else {
      startTokenization = state.getEndPosition(classTree.getModifiers());
    }
    List<ErrorProneToken> tokens =
        state.getOffsetTokens(startTokenization, state.getEndPosition(tree));
    if (previousMember == null) {
      tokens = getTokensAfterOpeningBrace(tokens);
    }
    if (tokens.isEmpty()) {
      return SuggestedFix.replace(tree, replacement);
    }
    if (tokens.get(0).comments().isEmpty()) {
      return SuggestedFix.replace(tokens.get(0).pos(), state.getEndPosition(tree), replacement);
    }
    ImmutableList<Comment> comments =
        ImmutableList.sortedCopyOf(
            Comparator.<Comment>comparingInt(c -> c.getSourcePos(0)).reversed(),
            tokens.get(0).comments());
    int startPos = getStartPosition(tree);
    // This can happen for desugared expressions like `int a, b;`.
    if (startPos < startTokenization) {
      return SuggestedFix.emptyFix();
    }
    // Delete backwards for comments which are not separated from our target by a blank line.
    CharSequence sourceCode = state.getSourceCode();
    for (Comment comment : comments) {
      int endOfCommentPos = comment.getSourcePos(comment.getText().length() - 1);
      CharSequence stringBetweenComments = sourceCode.subSequence(endOfCommentPos, startPos);
      if (stringBetweenComments.chars().filter(c -> c == '\n').count() > 1) {
        break;
      }
      startPos = comment.getSourcePos(0);
    }
    return SuggestedFix.replace(startPos, state.getEndPosition(tree), replacement);
  }

  private static List<ErrorProneToken> getTokensAfterOpeningBrace(List<ErrorProneToken> tokens) {
    for (int i = 0; i < tokens.size() - 1; ++i) {
      if (tokens.get(i).kind() == TokenKind.LBRACE) {
        return tokens.subList(i + 1, tokens.size());
      }
    }
    return ImmutableList.of();
  }

  /** Casts the given {@code expressionTree} to {@code toType}, adding parentheses if necessary. */
  public static String castTree(ExpressionTree expressionTree, String toType, VisitorState state) {
    boolean needsParentheses =
        expressionTree instanceof BinaryTree
            || expressionTree instanceof AssignmentTree
            || expressionTree instanceof CompoundAssignmentTree
            || expressionTree instanceof InstanceOfTree
            || expressionTree.getKind() == CONDITIONAL_EXPRESSION;

    return "("
        + toType
        + ") "
        + (needsParentheses ? "(" : "")
        + state.getSourceForNode(expressionTree)
        + (needsParentheses ? ")" : "");
  }
}
