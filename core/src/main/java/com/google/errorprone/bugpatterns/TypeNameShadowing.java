/*
 * Copyright 2018 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.TypeParameterNaming.TypeParameterNamingClassification;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.PackageSymbol;
import com.sun.tools.javac.code.Symbol.TypeVariableSymbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Enter;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.tree.JCTree.Tag;
import com.sun.tools.javac.util.Names;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Warns when a type parameter shadows another type name in scope.
 *
 * @author bennostein@google.com (Benno Stein)
 */
@BugPattern(
    name = "TypeNameShadowing",
    summary = "Type parameter declaration shadows another named type",
    category = JDK,
    severity = WARNING,
    tags = StandardTags.STYLE,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public class TypeNameShadowing extends BugChecker implements MethodTreeMatcher, ClassTreeMatcher {

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    if (tree.getTypeParameters().isEmpty()) {
      return Description.NO_MATCH;
    }
    return findShadowedTypes(tree, tree.getTypeParameters(), state);
  }

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    if (tree.getTypeParameters().isEmpty()) {
      return Description.NO_MATCH;
    }
    return findShadowedTypes(tree, tree.getTypeParameters(), state);
  }

  /**
   * Returns a list of type symbols in the scope enclosing {@code env} guaranteeing that, when two
   * symbols share a simple name, the shadower precedes the shadow-ee
   */
  private static Iterable<Symbol> typesInEnclosingScope(
      Env<AttrContext> env, PackageSymbol javaLang) {
    // Collect all visible type names declared in this source file by ascending lexical scopes
    // and excluding TypeVariableSymbols (otherwise, every type parameter spuriously shadows itself)
    Iterable<Symbol> localSymbolsInScope =
        Streams.stream(env)
            .map(
                ctx ->
                    ctx.tree.getTag() == Tag.CLASSDEF
                        ? ((ClassSymbol) ASTHelpers.getSymbol(ctx.tree)).members().getSymbols()
                        : ctx.info.getLocalElements())
            .flatMap(
                symbols ->
                    Streams.stream(symbols).filter(sym -> !(sym instanceof TypeVariableSymbol)))
            .collect(ImmutableList.toImmutableList());

    // Concatenate with all visible type names declared in other source files:
    // Ignore wildcard imports here since we don't use them and they can cause issues (b/109867096)
    return Iterables.concat(
        localSymbolsInScope, // Local symbols
        env.toplevel.namedImportScope.getSymbols(), // Explicitly named imports
        javaLang.members().getSymbols(), // implicitly imported java.lang.* symbols
        env.toplevel.packge.members().getSymbols()); // Siblings in class hierarchy
  }

  /** Iterate through a list of type parameters, looking for type names shadowed by any of them. */
  private Description findShadowedTypes(
      Tree tree, List<? extends TypeParameterTree> typeParameters, VisitorState state) {

    Env<AttrContext> env =
        Enter.instance(state.context)
            .getEnv(ASTHelpers.getSymbol(state.findEnclosing(ClassTree.class)));

    Symtab symtab = state.getSymtab();
    PackageSymbol javaLang =
        symtab.enterPackage(symtab.java_base, Names.instance(state.context).java_lang);

    Iterable<Symbol> enclosingTypes = typesInEnclosingScope(env, javaLang);

    List<Symbol> shadowedTypes =
        typeParameters.stream()
            .map(
                param ->
                    Iterables.tryFind(
                            enclosingTypes,
                            sym ->
                                sym.getSimpleName()
                                    .equals(ASTHelpers.getType(param).tsym.getSimpleName()))
                        .orNull())
            .filter(Objects::nonNull)
            .collect(ImmutableList.toImmutableList());

    if (shadowedTypes.isEmpty()) {
      return Description.NO_MATCH;
    }

    Description.Builder descBuilder = buildDescription(tree);

    descBuilder.setMessage(buildMessage(shadowedTypes));

    Set<String> visibleNames =
        Streams.stream(Iterables.concat(env.info.getLocalElements(), enclosingTypes))
            .map(sym -> sym.getSimpleName().toString())
            .collect(ImmutableSet.toImmutableSet());

    SuggestedFix.Builder fixBuilder = SuggestedFix.builder();
    shadowedTypes.stream()
        .filter(tv -> TypeParameterNamingClassification.classify(tv.name.toString()).isValidName())
        .map(
            tv ->
                TypeParameterShadowing.renameTypeVariable(
                    TypeParameterShadowing.typeParameterInList(typeParameters, tv),
                    tree,
                    TypeParameterShadowing.replacementTypeVarName(tv.name, visibleNames),
                    state))
        .forEach(fixBuilder::merge);

    descBuilder.addFix(fixBuilder.build());
    return descBuilder.build();
  }

  private static String buildMessage(List<Symbol> shadowedTypes) {
    return "Found type parameters shadowing other declared types:\n\t"
        + shadowedTypes.stream()
            .map(
                sym ->
                    "Type parameter "
                        + sym.getSimpleName()
                        + " shadows visible type "
                        + sym.flatName())
            .collect(Collectors.joining("\n\t"));
  }
}
