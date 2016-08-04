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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.errorprone.VisitorState;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.ErrorProneToken;
import com.sun.source.doctree.DocTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.DocTreePath;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.DCTree;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.util.Position;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.annotation.Nullable;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;

/** Factories for constructing {@link Fix}es. */
public class SuggestedFixes {

  /** Parse a modifier token into a {@link Modifier}. */
  @Nullable
  private static Modifier getTokModifierKind(ErrorProneToken tok) {
    // TODO(cushon): handle DEFAULT
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
      default:
        return null;
    }
  }

  /** Add modifiers to the given class, method, or field declaration. */
  @Nullable
  public static Fix addModifiers(Tree tree, VisitorState state, Modifier... modifiers) {
    ModifiersTree originalModifiers = ASTHelpers.getModifiers(tree);
    if (originalModifiers == null) {
      return null;
    }
    Set<Modifier> toAdd =
        Sets.difference(new TreeSet<>(Arrays.asList(modifiers)), originalModifiers.getFlags());
    if (originalModifiers.getFlags().isEmpty()) {
      int pos =
          state.getEndPosition((JCTree) originalModifiers) != Position.NOPOS
              ? state.getEndPosition((JCTree) originalModifiers) + 1
              : ((JCTree) tree).getStartPosition();
      return SuggestedFix.replace(pos, pos, Joiner.on(' ').join(toAdd) + " ");
    }
    // a map from modifiers to modifier position (or -1 if the modifier is being added)
    // modifiers are sorted in Google Java Style order
    Map<Modifier, Integer> modifierPositions = new TreeMap<>();
    for (Modifier mod : toAdd) {
      modifierPositions.put(mod, -1);
    }
    ImmutableList<ErrorProneToken> tokens = state.getTokensForNode(originalModifiers);
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
  public static Fix removeModifiers(Tree tree, VisitorState state, Modifier... modifiers) {
    Set<Modifier> toRemove = ImmutableSet.copyOf(modifiers);
    ModifiersTree originalModifiers = ASTHelpers.getModifiers(tree);
    if (originalModifiers == null) {
      return null;
    }
    SuggestedFix.Builder fix = SuggestedFix.builder();
    ImmutableList<ErrorProneToken> tokens = state.getTokensForNode(originalModifiers);
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

  /** Returns a human-friendly name of the given {@link Symbol.TypeSymbol} for use in fixes. */
  public static String qualifyType(
      VisitorState state, SuggestedFix.Builder fix, Symbol.TypeSymbol sym) {
    if (sym.getKind() == ElementKind.TYPE_PARAMETER) {
      return sym.getSimpleName().toString();
    }
    TreeMaker make =
        TreeMaker.instance(state.context)
            .forToplevel((JCTree.JCCompilationUnit) state.getPath().getCompilationUnit());
    return qualifyType(make, fix, sym);
  }

  /**
   * Returns a human-friendly name of the given {@link Symbol.TypeSymbol} for use in fixes.
   *
   * <ul>
   * <li>If the type is already imported, its simple name is used.
   * <li>If an enclosing type is imported, that enclosing type is used as a qualified.
   * <li>Otherwise the outermost enclosing type is imported and used as a qualifier.
   * </ul>
   */
  public static String qualifyType(
      TreeMaker make, SuggestedFix.Builder fix, Symbol.TypeSymbol sym) {
    // let javac figure out whether the type is already imported
    JCTree.JCExpression qual = make.QualIdent(sym);
    if (!selectsPackage(qual)) {
      return qual.toString();
    }
    Deque<String> names = new ArrayDeque<>();
    Symbol curr = sym;
    while (true) {
      names.addFirst(curr.getSimpleName().toString());
      if (curr.owner == null || curr.owner.getKind() == ElementKind.PACKAGE) {
        break;
      }
      curr = curr.owner;
    }
    fix.addImport(curr.toString());
    return Joiner.on('.').join(names);
  }

  /** Returns true iff the given expression is qualified by a package. */
  private static boolean selectsPackage(JCTree.JCExpression qual) {
    JCTree.JCExpression curr = qual;
    while (true) {
      Symbol sym = ASTHelpers.getSymbol(curr);
      if (sym != null && sym.getKind() == ElementKind.PACKAGE) {
        return true;
      }
      if (!(curr instanceof JCTree.JCFieldAccess)) {
        break;
      }
      curr = ((JCTree.JCFieldAccess) curr).getExpression();
    }
    return false;
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
   * Fully qualifies a javadoc reference, e.g. for replacing {@code {@link List}} with
   * {@code {@link java.util.List}}
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
   * otherMembers}) to the end of the class referenced by {@code classTree}.  This method should
   * only be called once per {@link ClassTree} as the suggestions will otherwise collide.
   */
  public static Fix addMembers(
      ClassTree classTree, VisitorState state, String firstMember, String... otherMembers) {
    checkNotNull(classTree);
    int classEndPosition = state.getEndPosition((JCTree) classTree);
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
  public static Fix renameVariable(
      VariableTree tree, final String replacement, VisitorState state) {
    String name = tree.getName().toString();
    int pos = ((JCTree) tree).getStartPosition() + state.getSourceForNode(tree).indexOf(name);
    final SuggestedFix.Builder fix =
        SuggestedFix.builder().replace(pos, pos + name.length(), replacement);
    final Symbol.VarSymbol sym = ASTHelpers.getSymbol(tree);
    ((JCTree) state.getPath().getCompilationUnit())
        .accept(
            new TreeScanner() {
              @Override
              public void visitIdent(JCTree.JCIdent tree) {
                if (sym.equals(ASTHelpers.getSymbol(tree))) {
                  fix.replace(tree, replacement);
                }
              }
            });
    return fix.build();
  }
}
