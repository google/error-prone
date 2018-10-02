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
import static com.google.common.collect.Iterables.getLast;
import static com.google.errorprone.util.ASTHelpers.getAnnotation;
import static com.google.errorprone.util.ASTHelpers.getAnnotationWithSimpleName;
import static com.google.errorprone.util.ASTHelpers.getModifiers;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.sun.source.tree.Tree.Kind.ASSIGNMENT;
import static com.sun.source.tree.Tree.Kind.NEW_ARRAY;
import static com.sun.tools.javac.code.TypeTag.CLASS;
import static java.util.stream.Collectors.joining;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicates;
import com.google.common.base.Verify;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.CharStreams;
import com.google.errorprone.VisitorState;
import com.google.errorprone.apply.DescriptionBasedDiff;
import com.google.errorprone.apply.ImportOrganizer;
import com.google.errorprone.apply.SourceFile;
import com.google.errorprone.fixes.SuggestedFix.Builder;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.ErrorProneToken;
import com.google.errorprone.util.ErrorProneTokens;
import com.google.errorprone.util.FindIdentifiers;
import com.sun.source.doctree.DocTree;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.DocTreePath;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.BoundKind;
import com.sun.tools.javac.code.Kinds.KindSelector;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types.DefaultTypeVisitor;
import com.sun.tools.javac.main.Arguments;
import com.sun.tools.javac.parser.Tokens;
import com.sun.tools.javac.parser.Tokens.TokenKind;
import com.sun.tools.javac.tree.DCTree;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.TreeScanner;
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
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
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
    Set<Modifier> toAdd =
        Sets.difference(new TreeSet<>(Arrays.asList(modifiers)), originalModifiers.getFlags());
    SuggestedFix.Builder fix = SuggestedFix.builder();
    List<Modifier> modifiersToWrite = new ArrayList<>();
    if (!originalModifiers.getFlags().isEmpty()) {
      // a map from modifiers to modifier position (or -1 if the modifier is being added)
      // modifiers are sorted in Google Java Style order
      Map<Modifier, Integer> modifierPositions = new TreeMap<>();
      for (Modifier mod : toAdd) {
        modifierPositions.put(mod, -1);
      }
      List<ErrorProneToken> tokens = state.getTokensForNode(originalModifiers);
      int base = ((JCTree) originalModifiers).getStartPosition();
      for (ErrorProneToken tok : tokens) {
        Modifier mod = getTokModifierKind(tok);
        if (mod != null) {
          modifierPositions.put(mod, base + tok.pos());
        }
      }
      // walk the map of all modifiers, and accumulate a list of new modifiers to insert
      // beside an existing modifier
      for (Modifier mod : modifierPositions.keySet()) {
        int p = modifierPositions.get(mod);
        if (p == -1) {
          modifiersToWrite.add(mod);
        } else if (!modifiersToWrite.isEmpty()) {
          fix.replace(p, p, Joiner.on(' ').join(modifiersToWrite) + " ");
          modifiersToWrite.clear();
        }
      }
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
    int pos =
        state.getEndPosition(originalModifiers) != Position.NOPOS
            ? state.getEndPosition(originalModifiers) + 1
            : ((JCTree) tree).getStartPosition();
    int base = ((JCTree) tree).getStartPosition();
    Optional<Integer> insert =
        state.getTokensForNode(tree).stream()
            .map(token -> token.pos() + base)
            .filter(thisPos -> thisPos >= pos)
            .findFirst();
    int insertPos = insert.orElse(pos); // shouldn't ever be able to get to the else
    fix.replace(insertPos, insertPos, Joiner.on(' ').join(toAdd) + " ");
  }

  /** Remove modifiers from the given class, method, or field declaration. */
  public static Optional<SuggestedFix> removeModifiers(
      Tree tree, VisitorState state, Modifier... modifiers) {
    Set<Modifier> toRemove = ImmutableSet.copyOf(modifiers);
    ModifiersTree originalModifiers = getModifiers(tree);
    if (originalModifiers == null) {
      return Optional.empty();
    }
    SuggestedFix.Builder fix = SuggestedFix.builder();
    List<ErrorProneToken> tokens = state.getTokensForNode(originalModifiers);
    int basePos = ((JCTree) originalModifiers).getStartPosition();
    boolean empty = true;
    for (ErrorProneToken tok : tokens) {
      Modifier mod = getTokModifierKind(tok);
      if (toRemove.contains(mod)) {
        empty = false;
        fix.replace(basePos + tok.pos(), basePos + tok.endPos() + 1, "");
      }
    }
    if (empty) {
      return Optional.empty();
    }
    return Optional.of(fix.build());
  }

  /**
   * Returns a human-friendly name of the given {@link Symbol.TypeSymbol} for use in fixes.
   *
   * <ul>
   *   <li>If the type is already imported, its simple name is used.
   *   <li>If an enclosing type is imported, that enclosing type is used as a qualified.
   *   <li>Otherwise the outermost enclosing type is imported and used as a qualifier.
   * </ul>
   */
  public static String qualifyType(VisitorState state, SuggestedFix.Builder fix, Symbol sym) {
    if (sym.getKind() == ElementKind.TYPE_PARAMETER) {
      return sym.getSimpleName().toString();
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

  /**
   * Returns a human-friendly name of the given {@code typeName} for use in fixes.
   *
   * <p>This should be used if the type may not be loaded.
   */
  public static String qualifyType(VisitorState state, SuggestedFix.Builder fix, String typeName) {
    for (int startOfClass = typeName.indexOf('.');
        startOfClass > 0;
        startOfClass = typeName.indexOf('.', startOfClass + 1)) {
      int endOfClass = typeName.indexOf('.', startOfClass + 1);
      if (endOfClass < 0) {
        endOfClass = typeName.length();
      }
      if (!Character.isUpperCase(typeName.charAt(startOfClass + 1))) {
        continue;
      }
      String className = typeName.substring(startOfClass + 1);
      Symbol found = FindIdentifiers.findIdent(className, state, KindSelector.VAL_TYP);
      // No clashing name: import it and return.
      if (found == null) {
        fix.addImport(typeName.substring(0, endOfClass));
        return className;
      }
      // Type already imported.
      if (found.getQualifiedName().contentEquals(typeName)) {
        return className;
      }
    }
    return typeName;
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
   * Returns a {@link Fix} that adds members defined by {@code firstMember} (and optionally {@code
   * otherMembers}) to the end of the class referenced by {@code classTree}. This method should only
   * be called once per {@link ClassTree} as the suggestions will otherwise collide.
   */
  public static Fix addMembers(
      ClassTree classTree, VisitorState state, String firstMember, String... otherMembers) {
    checkNotNull(classTree);
    int classEndPosition = state.getEndPosition(classTree);
    StringBuilder stringBuilder = new StringBuilder();
    for (String memberSnippet : Lists.asList(firstMember, otherMembers)) {
      stringBuilder.append("\n\n").append(memberSnippet);
    }
    stringBuilder.append('\n');

    return SuggestedFix.replace(
        classEndPosition - 1, classEndPosition - 1, stringBuilder.toString());
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
    int searchOffset = typeEndPos == -1 ? 0 : (typeEndPos - ((JCTree) tree).getStartPosition());
    int pos =
        ((JCTree) tree).getStartPosition()
            + state.getSourceForNode(tree).indexOf(name, searchOffset);
    final SuggestedFix.Builder fix =
        SuggestedFix.builder().replace(pos, pos + name.length(), replacement);
    final Symbol.VarSymbol sym = getSymbol(tree);
    ((JCTree) state.getPath().getCompilationUnit())
        .accept(
            new TreeScanner() {
              @Override
              public void visitIdent(JCTree.JCIdent tree) {
                if (sym.equals(getSymbol(tree))) {
                  fix.replace(tree, replacement);
                }
              }
            });
    return fix.build();
  }

  /** Be warned, only changes method name at the declaration. */
  public static SuggestedFix renameMethod(
      MethodTree tree, final String replacement, VisitorState state) {
    // Search tokens from beginning of method tree to beginning of method body.
    int basePos = ((JCTree) tree).getStartPosition();
    int endPos =
        tree.getBody() != null
            ? ((JCTree) tree.getBody()).getStartPosition()
            : state.getEndPosition(tree);
    List<ErrorProneToken> methodTokens =
        ErrorProneTokens.getTokens(
            state.getSourceCode().subSequence(basePos, endPos).toString(), state.context);
    for (ErrorProneToken token : methodTokens) {
      if (token.kind() == TokenKind.IDENTIFIER && token.name().equals(tree.getName())) {
        int nameStartPosition = basePos + token.pos();
        int nameEndPosition = basePos + token.endPos();
        return SuggestedFix.builder()
            .replace(nameStartPosition, nameEndPosition, replacement)
            .build();
      }
    }
    // Method name not found.
    throw new AssertionError();
  }

  /** Replaces the name of the method being invoked in {@code tree} with {@code replacement}. */
  public static SuggestedFix renameMethodInvocation(
      MethodInvocationTree tree, String replacement, VisitorState state) {
    Tree methodSelect = tree.getMethodSelect();
    int startPos;
    String extra = "";
    if (methodSelect instanceof MemberSelectTree) {
      startPos = state.getEndPosition(((MemberSelectTree) methodSelect).getExpression());
      extra = ".";
    } else if (methodSelect instanceof IdentifierTree) {
      startPos = ((JCTree) tree).getStartPosition();
    } else {
      return SuggestedFix.builder().build();
    }
    int endPos = state.getEndPosition(methodSelect);
    return SuggestedFix.replace(startPos, endPos, extra + replacement);
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
        ((JCTree) tree.getThrows().get(0)).getStartPosition(),
        state.getEndPosition(getLast(tree.getThrows())),
        replacement);
  }

  private static int getThrowsPosition(MethodTree tree, VisitorState state) {
    for (ErrorProneToken token : state.getTokensForNode(tree)) {
      if (token.kind() == Tokens.TokenKind.THROWS) {
        return ((JCTree) tree).getStartPosition() + token.pos();
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
   * @param fixBuilder
   * @param state
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
          formattedLineComment.orElse("") + "@SuppressWarnings(" + suppression + ") ";

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
                    || (tree instanceof ClassTree
                        && ((ClassTree) tree).getSimpleName().length() != 0)
                    || tree instanceof VariableTree)
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
    if (fix.isEmpty()) {
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
    for (int i = 0; i < fileObjects.size(); i++) {
      final JavaFileObject oldFile = fileObjects.get(i);
      if (modifiedFile.toUri().equals(oldFile.toUri())) {
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
    Options.instance(context).putAll(Options.instance(javacTask.getContext()));
    context.put(Arguments.class, arguments);
    JavacTask newTask =
        JavacTool.create()
            .getTask(
                CharStreams.nullWriter(),
                state.context.get(JavaFileManager.class),
                diagnosticListener,
                ImmutableList.of(),
                arguments.getClassNames(),
                fileObjects,
                context);
    try {
      newTask.analyze();
    } catch (Throwable e) {
      return false; // ¯\_(ツ)_/¯
    }
    return countErrors(diagnosticListener) == 0;
  }

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

  private static long countErrors(DiagnosticCollector<JavaFileObject> diagnosticCollector) {
    return diagnosticCollector.getDiagnostics().stream()
        .filter(d -> d.getKind() == Diagnostic.Kind.ERROR)
        .count();
  }

  /**
   * Pretty-prints a Type for use in fixes, qualifying any enclosed type names using {@link
   * #qualifyType}}.
   */
  public static String prettyType(
      @Nullable VisitorState state, @Nullable SuggestedFix.Builder fix, Type type) {
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
            if (state == null || fix == null) {
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
  public static Optional<SuggestedFix> suggestWhitelistAnnotation(
      String whitelistAnnotation, TreePath where, VisitorState state) {
    // TODO(bangert): Support annotations that do not have @Target(CLASS).
    if (whitelistAnnotation.equals("com.google.errorprone.annotations.DontSuggestFixes")) {
      return Optional.empty();
    }
    SuggestedFix.Builder builder = SuggestedFix.builder();
    Type whitelistAnnotationType = state.getTypeFromString(whitelistAnnotation);
    ImmutableSet<Tree.Kind> supportedWhitelistLocationKinds;
    String annotationName;

    if (whitelistAnnotationType != null) {
      supportedWhitelistLocationKinds = supportedTreeTypes(whitelistAnnotationType.asElement());
      annotationName = qualifyType(state, builder, whitelistAnnotationType);
    } else {
      // If we can't resolve the type, fall back to an approximation.
      int idx = whitelistAnnotation.lastIndexOf('.');
      Verify.verify(idx > 0 && idx + 1 < whitelistAnnotation.length());
      supportedWhitelistLocationKinds = TREE_TYPE_UNKNOWN_ANNOTATION;
      annotationName = whitelistAnnotation.substring(idx + 1);
      builder.addImport(whitelistAnnotation);
    }
    Optional<Tree> whitelistLocation =
        StreamSupport.stream(where.spliterator(), false)
            .filter(tree -> supportedWhitelistLocationKinds.contains(tree.getKind()))
            .filter(Predicates.not(SuggestedFixes::isAnonymousClassTree))
            .findFirst();

    return whitelistLocation.map(
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
   * <p>These are reasonable for whitelist-type annotations which annotate a block of code, e.g.
   * they don't usually make sense on a variable declaration.
   */
  private static final ImmutableSet<Tree.Kind> TREE_TYPE_UNKNOWN_ANNOTATION =
      ImmutableSet.of(
          Tree.Kind.CLASS,
          Tree.Kind.ENUM,
          Tree.Kind.INTERFACE,
          Tree.Kind.ANNOTATION_TYPE,
          Tree.Kind.METHOD);

  /** Returns true iff {@code suggestWhitelistAnnotation()} supports this annotation. */
  public static boolean suggestedWhitelistAnnotationSupported(Element whitelistAnnotation) {
    return !supportedTreeTypes(whitelistAnnotation).isEmpty();
  }

  private static ImmutableSet<Tree.Kind> supportedTreeTypes(Element whitelistAnnotation) {
    Target targetAnnotation = whitelistAnnotation.getAnnotation(Target.class);
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
}
