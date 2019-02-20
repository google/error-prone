/*
 * Copyright 2011 The Error Prone Authors.
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

package com.google.errorprone;

import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultiset;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.SuppressionInfo.SuppressedState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.dataflow.nullnesspropagation.NullnessAnalysis;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ErrorProneToken;
import com.google.errorprone.util.ErrorProneTokens;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Kinds.Kind;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.CompletionFailure;
import com.sun.tools.javac.code.Symbol.ModuleSymbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ArrayType;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.comp.Modules;
import com.sun.tools.javac.parser.Tokens.Token;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import com.sun.tools.javac.util.Options;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nullable;

/** @author alexeagle@google.com (Alex Eagle) */
public class VisitorState {

  private final DescriptionListener descriptionListener;
  private final StatisticsCollector statisticsCollector;
  private final Map<String, SeverityLevel> severityMap;
  private final ErrorProneOptions errorProneOptions;
  private final LoadingCache<String, Optional<Type>> typeCache;
  public final Context context;

  private final TreePath path;
  private final SuppressionInfo.SuppressedState suppressedState;

  // The default no-op implementation of DescriptionListener. We use this instead of null so callers
  // of getDescriptionListener() don't have to do null-checking.
  private static final DescriptionListener NULL_LISTENER = description -> {};

  /**
   * Return a VisitorState that has no Error Prone configuration, and can't report results.
   *
   * <p>If using this method, consider moving to using utility methods not needing VisitorSate
   */
  public static VisitorState createForUtilityPurposes(Context context) {
    return new VisitorState(
        context,
        NULL_LISTENER,
        ImmutableMap.of(),
        ErrorProneOptions.empty(),
        // Can't use this VisitorState to report results, so no-op collector.
        StatisticsCollector.createNoOpCollector(),
        null,
        null,
        SuppressedState.UNSUPPRESSED);
  }

  /**
   * Return a VisitorState that has no Error Prone configuration, but can report findings to {@code
   * listener}.
   */
  public static VisitorState createForCustomFindingCollection(
      Context context, DescriptionListener listener) {
    return new VisitorState(
        context,
        listener,
        ImmutableMap.of(),
        ErrorProneOptions.empty(),
        StatisticsCollector.createCollector(),
        null,
        null,
        SuppressedState.UNSUPPRESSED);
  }

  /**
   * Return a VisitorState configured for a new compilation, including Error Prone configuration.
   */
  public static VisitorState createConfiguredForCompilation(
      Context context,
      DescriptionListener listener,
      Map<String, SeverityLevel> severityMap,
      ErrorProneOptions errorProneOptions) {
    return new VisitorState(
        context,
        listener,
        severityMap,
        errorProneOptions,
        StatisticsCollector.createCollector(),
        null,
        null,
        SuppressedState.UNSUPPRESSED);
  }

  /**
   * Return a VisitorState that has no Error Prone configuration, and can't report results.
   *
   * @deprecated If VisitorState is needed, use {@link #createForUtilityPurposes}, otherwise just
   *     use utility methods in ASTHelpers that don't need VisitorSate.
   */
  @Deprecated
  public VisitorState(Context context) {
    this(
        context,
        NULL_LISTENER,
        ImmutableMap.of(),
        ErrorProneOptions.empty(),
        // Can't use this VisitorState to report results, so no-op collector.
        StatisticsCollector.createNoOpCollector(),
        null,
        null,
        SuppressedState.UNSUPPRESSED);
  }

  /**
   * Return a VisitorState that has no Error Prone configuration, but can report findings to {@code
   * listener}.
   *
   * @deprecated Use the equivalent factory method {@link #createForCustomFindingCollection}.
   */
  @Deprecated
  public VisitorState(Context context, DescriptionListener listener) {
    this(
        context,
        listener,
        ImmutableMap.of(),
        ErrorProneOptions.empty(),
        StatisticsCollector.createCollector(),
        null,
        null,
        SuppressedState.UNSUPPRESSED);
  }

  /**
   * Return a VisitorState configured for a new compilation, including Error Prone configuration.
   *
   * @deprecated Use the equivalent factory method {@link #createConfiguredForCompilation}.
   */
  @Deprecated
  public VisitorState(
      Context context,
      DescriptionListener listener,
      Map<String, SeverityLevel> severityMap,
      ErrorProneOptions errorProneOptions) {
    this(
        context,
        listener,
        severityMap,
        errorProneOptions,
        StatisticsCollector.createCollector(),
        null,
        null,
        SuppressedState.UNSUPPRESSED);
  }

  private VisitorState(
      Context context,
      DescriptionListener descriptionListener,
      Map<String, SeverityLevel> severityMap,
      ErrorProneOptions errorProneOptions,
      StatisticsCollector statisticsCollector,
      LoadingCache<String, Optional<Type>> typeCache,
      TreePath path,
      SuppressedState suppressedState) {
    this.context = context;
    this.descriptionListener = descriptionListener;
    this.severityMap = severityMap;
    this.errorProneOptions = errorProneOptions;
    this.statisticsCollector = statisticsCollector;

    this.suppressedState = suppressedState;
    this.path = path;
    this.typeCache =
        typeCache != null
            ? typeCache
            : CacheBuilder.newBuilder()
                .concurrencyLevel(1) // resolving symbols in javac is not thread-safe
                .build(
                    CacheLoader.from(key -> Optional.fromNullable(getTypeFromStringInternal(key))));
  }

  public VisitorState withPath(TreePath path) {
    return new VisitorState(
        context,
        descriptionListener,
        severityMap,
        errorProneOptions,
        statisticsCollector,
        typeCache,
        path,
        suppressedState);
  }

  public VisitorState withPathAndSuppression(TreePath path, SuppressedState suppressedState) {
    return new VisitorState(
        context,
        descriptionListener,
        severityMap,
        errorProneOptions,
        statisticsCollector,
        typeCache,
        path,
        suppressedState);
  }

  public TreePath getPath() {
    return path;
  }

  public TreeMaker getTreeMaker() {
    return TreeMaker.instance(context);
  }

  public Types getTypes() {
    return Types.instance(context);
  }

  public Symtab getSymtab() {
    return Symtab.instance(context);
  }

  public NullnessAnalysis getNullnessAnalysis() {
    return NullnessAnalysis.instance(context);
  }

  public ErrorProneOptions errorProneOptions() {
    return errorProneOptions;
  }

  public void reportMatch(Description description) {
    // TODO(cushon): creating Descriptions with the default severity and updating them here isn't
    // ideal (we could forget to do the update), so consider removing severity from Description.
    // Instead, there could be another method on the listener that took a description and a
    // (separate) SeverityLevel. Adding the method to the interface would require updating the
    // existing implementations, though. Wait for default methods?
    SeverityLevel override = severityMap.get(description.checkName);
    if (override != null) {
      description = description.applySeverityOverride(override);
    }
    statisticsCollector.incrementCounter(statsKey(description.checkName + "-findings"));

    // TODO(glorioso): I believe it is correct to still emit regular findings since the
    // Scanner configured the visitor state to explicitly scan suppressed nodes, but perhaps
    // we can add a 'suppressed' field to Description to allow the description listener to bucket
    // them out.
    descriptionListener.onDescribed(description);
  }

  private String statsKey(String key) {
    return suppressedState == SuppressedState.SUPPRESSED ? key + "-suppressed" : key;
  }

  /**
   * Increment the counter for a combination of {@code bugChecker}'s canonical name and {@code key}
   * by 1.
   *
   * <p>e.g.: a key of {@code foo} becomes {@code FooChecker-foo}.
   */
  public void incrementCounter(BugChecker bugChecker, String key) {
    incrementCounter(bugChecker, key, 1);
  }

  /**
   * Increment the counter for a combination of {@code bugChecker}'s canonical name and {@code key}
   * by {@code count}.
   *
   * <p>e.g.: a key of {@code foo} becomes {@code FooChecker-foo}.
   */
  public void incrementCounter(BugChecker bugChecker, String key, int count) {
    statisticsCollector.incrementCounter(statsKey(bugChecker.canonicalName() + "-" + key), count);
  }

  /**
   * Returns a copy of all of the counters previously added to this VisitorState with {@link
   * #incrementCounter}.
   */
  public ImmutableMultiset<String> counters() {
    return statisticsCollector.counters();
  }

  public Name getName(String nameStr) {
    return Names.instance(context).fromString(nameStr);
  }

  /**
   * Given the binary name of a class, returns the {@link Type}.
   *
   * <p>If this method returns null, the compiler doesn't have access to this type, which means that
   * if you are comparing other types to this for equality or the subtype relation, your result
   * would always be false even if it could create the type. Thus it might be best to bail out early
   * in your matcher if this method returns null on your type of interest.
   *
   * @param typeStr the JLS 13.1 binary name of the class, e.g. {@code "java.util.Map$Entry"}
   * @return the {@link Type}, or null if it cannot be found
   */
  @Nullable
  public Type getTypeFromString(String typeStr) {
    try {
      return typeCache.get(typeStr).orNull();
    } catch (ExecutionException e) {
      return null;
    }
  }

  @Nullable
  private Type getTypeFromStringInternal(String typeStr) {
    validateTypeStr(typeStr);
    Type primitiveOrVoidType = getPrimitiveOrVoidType(typeStr);
    if (primitiveOrVoidType != null) {
      return primitiveOrVoidType;
    }
    // Fast path if the type's symbol is available.
    ClassSymbol classSymbol = (ClassSymbol) getSymbolFromString(typeStr);
    if (classSymbol != null) {
      return classSymbol.asType();
    }
    return null;
  }

  /**
   * @param symStr the string representation of a symbol
   * @return the Symbol object, or null if it cannot be found
   */
  // TODO(cushon): deal with binary compat issues and return ClassSymbol
  @Nullable
  public Symbol getSymbolFromString(String symStr) {
    symStr = inferBinaryName(symStr);
    Name name = getName(symStr);
    Modules modules = Modules.instance(context);
    boolean modular = modules.getDefaultModule() != getSymtab().noModule;
    if (!modular) {
      return getSymbolFromString(getSymtab().noModule, name);
    }
    for (ModuleSymbol msym : Modules.instance(context).allModules()) {
      ClassSymbol result = getSymbolFromString(msym, name);
      if (result != null) {
        // TODO(cushon): the path where we iterate over all modules is probably slow.
        // Try to learn some lessons from JDK-8189747, and consider disallowing this case and
        // requiring users to call the getSymbolFromString(ModuleSymbol, Name) overload instead.
        return result;
      }
    }
    return null;
  }

  @Nullable
  public ClassSymbol getSymbolFromString(ModuleSymbol msym, Name name) {
    ClassSymbol result = getSymtab().getClass(msym, name);
    if (result == null || result.kind == Kind.ERR || !result.exists()) {
      return null;
    }
    try {
      result.complete();
    } catch (CompletionFailure failure) {
      // Ignoring completion error is problematic in general, but in this case we're ignoring a
      // completion error for a type that was directly requested, not one that was discovered
      // during the compilation.
      return null;
    }
    return result;
  }

  /**
   * Given a canonical class name, infers the binary class name using case conventions. For example,
   * give {@code com.example.Outer.Inner} returns {@code com.example.Outer$Inner}.
   */
  // TODO(cushon): consider migrating call sites to use binary names and removing this code.
  // (But then we'd probably want error handling for probably-incorrect canonical names,
  // so it may not end up being a performance win.)
  private static String inferBinaryName(String classname) {
    StringBuilder sb = new StringBuilder();
    boolean first = true;
    char sep = '.';
    for (String bit : Splitter.on('.').split(classname)) {
      if (!first) {
        sb.append(sep);
      }
      sb.append(bit);
      if (Character.isUpperCase(bit.charAt(0))) {
        sep = '$';
      }
      first = false;
    }
    return sb.toString();
  }

  /** Build an instance of a Type. */
  public Type getType(Type baseType, boolean isArray, List<Type> typeParams) {
    boolean isGeneric = typeParams != null && !typeParams.isEmpty();
    if (!isArray && !isGeneric) {
      // Simple type.
      return baseType;
    } else if (isArray && !isGeneric) {
      // Array type, not generic.
      ClassSymbol arraySymbol = getSymtab().arrayClass;
      return new ArrayType(baseType, arraySymbol);
    } else if (!isArray && isGeneric) {
      // Generic type, not array.
      com.sun.tools.javac.util.List<Type> typeParamsCopy =
          com.sun.tools.javac.util.List.from(typeParams);
      return new ClassType(Type.noType, typeParamsCopy, baseType.tsym);
    } else {
      throw new IllegalArgumentException("Unsupported arguments to getType");
    }
  }

  /** Build an Array Type from another Type */
  public Type arrayTypeForType(Type baseType) {
    return new ArrayType(baseType, getSymtab().arrayClass);
  }

  /**
   * Returns the {@link TreePath} to the nearest tree node of one of the given types. To instead
   * retrieve the element directly, use {@link #findEnclosing(Class...)}.
   *
   * @return the path, or {@code null} if there is no match
   */
  @Nullable
  @SafeVarargs
  public final TreePath findPathToEnclosing(Class<? extends Tree>... classes) {
    TreePath enclosingPath = getPath();
    while (enclosingPath != null) {
      for (Class<? extends Tree> clazz : classes) {
        if (clazz.isInstance(enclosingPath.getLeaf())) {
          return enclosingPath;
        }
      }
      enclosingPath = enclosingPath.getParentPath();
    }
    return null;
  }

  /**
   * Find the first enclosing tree node of one of the given types.
   *
   * @return the node, or {@code null} if there is no match
   */
  @Nullable
  @SuppressWarnings("unchecked") // findPathToEnclosing guarantees that the type is from |classes|
  @SafeVarargs
  public final <T extends Tree> T findEnclosing(Class<? extends T>... classes) {
    TreePath pathToEnclosing = findPathToEnclosing(classes);
    return (pathToEnclosing == null) ? null : (T) pathToEnclosing.getLeaf();
  }

  /**
   * Gets the current source file.
   *
   * @return the source file as a sequence of characters, or null if it is not available
   */
  @Nullable
  public CharSequence getSourceCode() {
    try {
      return getPath().getCompilationUnit().getSourceFile().getCharContent(false);
    } catch (IOException e) {
      return null;
    }
  }

  /**
   * Gets the original source code that represents the given node.
   *
   * <p>Note that this may be different from what is returned by calling .toString() on the node.
   * This returns exactly what is in the source code, whereas .toString() pretty-prints the node
   * from its AST representation.
   *
   * @return the source code that represents the node.
   */
  @Nullable
  public String getSourceForNode(Tree tree) {
    JCTree node = (JCTree) tree;
    int start = node.getStartPosition();
    int end = getEndPosition(node);
    if (end < 0) {
      return null;
    }
    return getSourceCode().subSequence(start, end).toString();
  }

  /**
   * Returns the list of {@link Token}s for the given {@link JCTree}.
   *
   * <p>This is moderately expensive (the source of the node has to be re-lexed), so it should only
   * be used if a fix is already going to be emitted.
   */
  public List<ErrorProneToken> getTokensForNode(Tree tree) {
    return ErrorProneTokens.getTokens(getSourceForNode(tree), context);
  }

  /** Returns the end position of the node, or -1 if it is not available. */
  public int getEndPosition(Tree node) {
    JCCompilationUnit compilationUnit = (JCCompilationUnit) getPath().getCompilationUnit();
    if (compilationUnit.endPositions == null) {
      return -1;
    }
    return ((JCTree) node).getEndPosition(compilationUnit.endPositions);
  }

  /** Validates a type string, ensuring it is not generic and not an array type. */
  private static void validateTypeStr(String typeStr) {
    if (typeStr.contains("[") || typeStr.contains("]")) {
      throw new IllegalArgumentException(
          String.format(
              "Cannot convert array types (%s), please build them using getType()", typeStr));
    }
    if (typeStr.contains("<") || typeStr.contains(">")) {
      throw new IllegalArgumentException(
          String.format(
              "Cannot convert generic types (%s), please build them using getType()", typeStr));
    }
  }

  /**
   * Given a string that represents a type, if it's a primitive type (e.g., "int") or "void", return
   * the corresponding Type, or null otherwise.
   */
  @Nullable
  private Type getPrimitiveOrVoidType(String typeStr) {
    switch (typeStr) {
      case "byte":
        return getSymtab().byteType;
      case "short":
        return getSymtab().shortType;
      case "int":
        return getSymtab().intType;
      case "long":
        return getSymtab().longType;
      case "float":
        return getSymtab().floatType;
      case "double":
        return getSymtab().doubleType;
      case "boolean":
        return getSymtab().booleanType;
      case "char":
        return getSymtab().charType;
      case "void":
        return getSymtab().voidType;
      default:
        return null;
    }
  }

  /** Returns true if the compilation is targeting Android. */
  public boolean isAndroidCompatible() {
    return Options.instance(context).getBoolean("androidCompatible");
  }
}
