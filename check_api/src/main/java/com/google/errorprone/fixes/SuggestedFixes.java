/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Predicates;
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
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.DocTreePath;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Kinds.KindSelector;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
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

  /** Add modifiers to the given class, method, or field declaration. */
  @Nullable
  public static SuggestedFix addModifiers(Tree tree, VisitorState state, Modifier... modifiers) {
    ModifiersTree originalModifiers = getModifiers(tree);
    if (originalModifiers == null) {
      return null;
    }
    Set<Modifier> toAdd =
        Sets.difference(new TreeSet<>(Arrays.asList(modifiers)), originalModifiers.getFlags());
    if (originalModifiers.getFlags().isEmpty()) {
      int pos =
          state.getEndPosition(originalModifiers) != Position.NOPOS
              ? state.getEndPosition(originalModifiers) + 1
              : ((JCTree) tree).getStartPosition();
      int base = ((JCTree) tree).getStartPosition();
      java.util.Optional<Integer> insert =
          state
              .getTokensForNode(tree)
              .stream()
              .map(token -> token.pos() + base)
              .filter(thisPos -> thisPos >= pos)
              .findFirst();
      int insertPos = insert.orElse(pos); // shouldn't ever be able to get to the else
      return SuggestedFix.replace(insertPos, insertPos, Joiner.on(' ').join(toAdd) + " ");
    }
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
    SuggestedFix.Builder fix = SuggestedFix.builder();
    // walk the map of all modifiers, and accumulate a list of new modifiers to insert
    // beside an existing modifier
    List<Modifier> modifiersToWrite = new ArrayList<>();
    for (Modifier mod : modifierPositions.keySet()) {
      int p = modifierPositions.get(mod);
      if (p == -1) {
        modifiersToWrite.add(mod);
      } else if (!modifiersToWrite.isEmpty()) {
        fix.replace(p, p, Joiner.on(' ').join(modifiersToWrite) + " ");
        modifiersToWrite.clear();
      }
    }
    if (!modifiersToWrite.isEmpty()) {
      fix.postfixWith(originalModifiers, " " + Joiner.on(' ').join(modifiersToWrite));
    }
    return fix.build();
  }

  /** Remove modifiers from the given class, method, or field declaration. */
  @Nullable
  public static SuggestedFix removeModifiers(Tree tree, VisitorState state, Modifier... modifiers) {
    Set<Modifier> toRemove = ImmutableSet.copyOf(modifiers);
    ModifiersTree originalModifiers = getModifiers(tree);
    if (originalModifiers == null) {
      return null;
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
        break;
      }
    }
    if (empty) {
      return null;
    }
    return fix.build();
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
        if (importClash(state, curr)) {
          names.addFirst(curr.owner.getQualifiedName().toString());
          break;
        } else {
          fix.addImport(curr.getQualifiedName().toString());
          break;
        }
      }
    }
    return Joiner.on('.').join(names);
  }

  private static boolean importClash(VisitorState state, Symbol sym) {
    for (ImportTree importTree : state.getPath().getCompilationUnit().getImports()) {
      if (((MemberSelectTree) importTree.getQualifiedIdentifier())
              .getIdentifier()
              .contentEquals(sym.getSimpleName())
          && !sym.equals(ASTHelpers.getSymbol(importTree.getQualifiedIdentifier()))) {
        return true;
      }
    }
    return false;
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
    int typeLength = state.getSourceForNode(tree.getType()).length();
    int pos =
        ((JCTree) tree).getStartPosition() + state.getSourceForNode(tree).indexOf(name, typeLength);
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
   * <p>If the closest suppressible element already has a @SuppressWarning annotation,
   * warningToSuppress will be added to the value in {@code @SuppressWarnings} instead.
   *
   * <p>In the event that a suppressible element couldn't be found (e.g.: the state is pointing at a
   * CompilationUnit, or some other internal inconsistency has occurred), or the enclosing
   * suppressible element already has a {@code @SuppressWarnings} annotation with {@code
   * warningToSuppress}, this method will return null.
   */
  @Nullable
  public static Fix addSuppressWarnings(VisitorState state, String warningToSuppress) {
    Builder fixBuilder = SuggestedFix.builder();
    addSuppressWarnings(fixBuilder, state, warningToSuppress);
    return fixBuilder.isEmpty() ? null : fixBuilder.build();
  }

  /**
   * Modifies {@code fixBuilder} to either create a new {@code @SuppressWarnings} element on the
   * closest suppressible node, or add {@code warningToSuppress} to that node if there's already a
   * {@code SuppressWarnings} annotation there.
   *
   * @see #addSuppressWarnings(VisitorState, String)
   */
  public static void addSuppressWarnings(
      Builder fixBuilder, VisitorState state, String warningToSuppress) {
    // Find the nearest tree to add @SuppressWarnings to.
    Tree suppressibleNode = suppressibleNode(state.getPath());
    if (suppressibleNode == null) {
      return;
    }

    SuppressWarnings existingAnnotation = getAnnotation(suppressibleNode, SuppressWarnings.class);
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
              suppressAnnotationTree,
              "value",
              ImmutableList.of(state.getTreeMaker().Literal(CLASS, warningToSuppress).toString()),
              state));
    } else {
      // Otherwise, add a suppress annotation to the element
      fixBuilder.prefixWith(suppressibleNode, "@SuppressWarnings(\"" + warningToSuppress + "\")\n");
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
    return Optional.absent();
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
    JavacTaskImpl javacTask = (JavacTaskImpl) state.context.get(JavacTask.class);
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
            new SimpleJavaFileObject(modifiedFile.toUri(), Kind.SOURCE) {
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
      e.printStackTrace();
    }
    return countErrors(diagnosticListener) == 0;
  }

  private static long countErrors(DiagnosticCollector<JavaFileObject> diagnosticCollector) {
    return diagnosticCollector
        .getDiagnostics()
        .stream()
        .filter(d -> d.getKind() == Diagnostic.Kind.ERROR)
        .count();
  }
}
