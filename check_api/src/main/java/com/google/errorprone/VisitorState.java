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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultiset;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.SuppressionInfo.SuppressedState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.dataflow.nullnesspropagation.NullnessAnalysis;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Suppressible;
import com.google.errorprone.suppliers.Supplier;
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
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.parser.Tokens.Token;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import com.sun.tools.javac.util.Options;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.lang.model.util.Elements;

/** @author alexeagle@google.com (Alex Eagle) */
public class VisitorState {

  private final SharedState sharedState;
  public final Context context;
  private final TreePath path;
  private final SuppressedState suppressedState;

  // The default no-op implementation of DescriptionListener. We use this instead of null so callers
  // of getDescriptionListener() don't have to do null-checking.
  private static void nullListener(Description description) {}

  /**
   * Return a VisitorState that has no Error Prone configuration, and can't report results.
   *
   * <p>If using this method, consider moving to using utility methods not needing VisitorSate
   */
  public static VisitorState createForUtilityPurposes(Context context) {
    return new VisitorState(
        context,
        VisitorState::nullListener,
        ImmutableMap.of(),
        ErrorProneOptions.empty(),
        // Can't use this VisitorState to report results, so no-op collector.
        StatisticsCollector.createNoOpCollector(),
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
        VisitorState::nullListener,
        ImmutableMap.of(),
        ErrorProneOptions.empty(),
        // Can't use this VisitorState to report results, so no-op collector.
        StatisticsCollector.createNoOpCollector(),
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
        SuppressedState.UNSUPPRESSED);
  }

  /**
   * The constructor used for brand-new VisitorState objects from outside. It builds a new
   * SharedState.
   */
  private VisitorState(
      Context context,
      DescriptionListener descriptionListener,
      Map<String, SeverityLevel> severityMap,
      ErrorProneOptions errorProneOptions,
      StatisticsCollector statisticsCollector,
      TreePath path,
      SuppressedState suppressedState) {
    this.context = context;
    this.suppressedState = suppressedState;
    this.path = path;

    this.sharedState =
        new SharedState(
            context, descriptionListener, statisticsCollector, severityMap, errorProneOptions);
  }

  /**
   * The constructor used for basing a new VisitorState object on an older one. It accepts
   * parameters only for the things that can change, and reuses its SharedState.
   */
  private VisitorState(
      Context context, TreePath path, SuppressedState suppressedState, SharedState sharedState) {
    this.context = context;
    this.path = path;
    this.suppressedState = suppressedState;
    this.sharedState = sharedState;
  }

  public VisitorState withPath(TreePath path) {
    return new VisitorState(context, path, suppressedState, sharedState);
  }

  @Deprecated // TODO(amalloy): Delete after next error-prone release.
  public VisitorState withPathAndSuppression(TreePath path, SuppressedState suppressedState) {
    return new VisitorState(context, path, suppressedState, sharedState);
  }

  public VisitorState withSuppression(SuppressedState suppressedState) {
    if (suppressedState == this.suppressedState) {
      return this;
    }
    return new VisitorState(context, path, suppressedState, sharedState);
  }

  public TreePath getPath() {
    return path;
  }

  public TreeMaker getTreeMaker() {
    return sharedState.treeMaker;
  }

  public Types getTypes() {
    return sharedState.types;
  }

  public Elements getElements() {
    return JavacElements.instance(context);
  }

  public Symtab getSymtab() {
    return sharedState.symtab;
  }

  public Names getNames() {
    return sharedState.names;
  }

  public NullnessAnalysis getNullnessAnalysis() {
    return NullnessAnalysis.instance(context);
  }

  public ErrorProneOptions errorProneOptions() {
    return sharedState.errorProneOptions;
  }

  public void reportMatch(Description description) {
    checkNotNull(description, "Use Description.NO_MATCH to denote an absent finding.");
    if (description == Description.NO_MATCH) {
      return;
    }
    // TODO(cushon): creating Descriptions with the default severity and updating them here isn't
    // ideal (we could forget to do the update), so consider removing severity from Description.
    // Instead, there could be another method on the listener that took a description and a
    // (separate) SeverityLevel. Adding the method to the interface would require updating the
    // existing implementations, though. Wait for default methods?
    SeverityLevel override = sharedState.severityMap.get(description.checkName);
    if (override != null) {
      description = description.applySeverityOverride(override);
    }
    sharedState.statisticsCollector.incrementCounter(statsKey(description.checkName + "-findings"));

    // TODO(glorioso): I believe it is correct to still emit regular findings since the
    // Scanner configured the visitor state to explicitly scan suppressed nodes, but perhaps
    // we can add a 'suppressed' field to Description to allow the description listener to bucket
    // them out.
    sharedState.descriptionListener.onDescribed(description);
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
    sharedState.statisticsCollector.incrementCounter(
        statsKey(bugChecker.canonicalName() + "-" + key), count);
  }

  /**
   * Returns a copy of all of the counters previously added to this VisitorState with {@link
   * #incrementCounter}.
   */
  public ImmutableMultiset<String> counters() {
    return sharedState.statisticsCollector.counters();
  }

  public Name getName(String nameStr) {
    return getNames().fromString(nameStr);
  }

  /**
   * Given the binary name of a class, returns the {@link Type}.
   *
   * <p>Prefer not to use this method for constant strings, or strings otherwise known at compile
   * time. Instead, save the result of {@link
   * com.google.errorprone.suppliers.Suppliers#typeFromString} as a class constant, and use its
   * {@link Supplier#get} method to look up the Type when needed. This lookup will be faster,
   * improving Error Prone's analysis time.
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
    return sharedState
        .typeCache
        .computeIfAbsent(typeStr, key -> Optional.ofNullable(getTypeFromStringInternal(key)))
        .orElse(null);
  }

  @Nullable
  private Type getTypeFromStringInternal(String typeStr) {
    validateTypeStr(typeStr);
    Type primitiveOrVoidType = getPrimitiveOrVoidType(typeStr);
    if (primitiveOrVoidType != null) {
      return primitiveOrVoidType;
    }
    ClassSymbol classSymbol = (ClassSymbol) getSymbolFromString(typeStr);
    if (classSymbol != null) {
      return classSymbol.asType();
    }
    // It's possible for the type to exist on the classpath and still for getSymbolFromString to
    // return null if the type is not referenced in any source file (or by any of the referenced
    // types' supertypes). Checking for this, however, is prohibitively slow. See b/138753468
    return null;
  }

  /**
   * @param symStr the string representation of a symbol
   * @return the Symbol object, or null if it cannot be found
   */
  // TODO(cushon): deal with binary compat issues and return ClassSymbol
  @Nullable
  public Symbol getSymbolFromString(String symStr) {
    return getSymbolFromName(binaryNameFromClassname(symStr));
  }

  /**
   * Returns the Name object corresponding to the named class, converting it to binary form along
   * the way if necessary (i.e., replacing Foo.Bar with Foo$Bar). To get the Name corresponding to
   * some string that is not a class name, see the more general {@link #getName(String)}.
   */
  public Name binaryNameFromClassname(String className) {
    return getName(inferBinaryName(className));
  }

  /**
   * Look up the class symbol for a given Name.
   *
   * @param name the name to look up, which must be in binary form (i.e. with $ for nested classes).
   */
  @Nullable
  public ClassSymbol getSymbolFromName(Name name) {
    boolean modular = sharedState.modules.getDefaultModule() != getSymtab().noModule;
    if (!modular) {
      return getSymbolFromString(getSymtab().noModule, name);
    }
    for (ModuleSymbol msym : sharedState.modules.allModules()) {
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
  @VisibleForTesting
  static String inferBinaryName(String classname) {
    int len = classname.length();
    checkArgument(!classname.isEmpty(), "class name must be non-empty");
    checkArgument(classname.charAt(len - 1) != '.', "invalid class name: %s", classname);

    int lastPeriod = classname.lastIndexOf('.');
    if (lastPeriod == -1) {
      return classname; // top level class in default package
    }
    int secondToLastPeriod = classname.lastIndexOf('.', lastPeriod - 1);
    if (secondToLastPeriod != -1
        && !Character.isUpperCase(classname.charAt(secondToLastPeriod + 1))) {
      return classname; // top level class
    }

    StringBuilder sb = new StringBuilder(len);
    boolean foundUppercase = false;
    for (int i = 0; i < len; i++) {
      char c = classname.charAt(i);
      foundUppercase = foundUppercase || Character.isUpperCase(c);
      if (c == '.') {
        sb.append(foundUppercase ? '$' : '.');
      } else {
        sb.append(c);
      }
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

  /**
   * Returns the list of {@link Token}s for the given {@link JCTree}, offset by the start position
   * of the tree within the overall source.
   *
   * <p>This is moderately expensive (the source of the node has to be re-lexed), so it should only
   * be used if a fix is already going to be emitted.
   */
  public List<ErrorProneToken> getOffsetTokensForNode(Tree tree) {
    int start = getStartPosition(tree);
    return ErrorProneTokens.getTokens(getSourceForNode(tree), start, context);
  }

  /**
   * Returns the list of {@link Token}s for source code between the given positions, offset by the
   * start position.
   *
   * <p>This is moderately expensive (the source of the node has to be re-lexed), so it should only
   * be used if a fix is already going to be emitted.
   */
  public List<ErrorProneToken> getOffsetTokens(int start, int end) {
    return ErrorProneTokens.getTokens(
        getSourceCode().subSequence(start, end).toString(), start, context);
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

  /** Returns a timing span for the given {@link Suppressible}. */
  public AutoCloseable timingSpan(Suppressible suppressible) {
    return sharedState.timings.span(suppressible);
  }

  private static class Cache<T> implements Supplier<T> {
    private final Supplier<T> impl;
    /* Uses T instead of Optional<T> because we don't want to cache null results
    (b/138753468). These inline caches persist between compilation units, and a type that fails to
    resolve in one may become available in the next; we want to keep looking it up
    (relying on the per-file cache in typeCache) if we don't have a result. If you want to cache a
    computation which can return null, wrap it in an Optional at the call site.*/

    private WeakReference<T> cache = new WeakReference<>(null);
    private JavacInvocationInstance provenance;

    private Cache(Supplier<T> impl) {
      this.impl = impl;
      // provenance intentionally left null-initialized
    }

    @Override
    public synchronized T get(VisitorState state) {
      /* javac is single-threaded, so in principle we don't really need to lock.
      But in practice it's cheap enough to be worth getting peace of mind that this is
      always correct. */
      T value = cache.get();
      if (value == null) {
        value = impl.get(state);
        if (value != null) {
          cache = new WeakReference<>(value);
          provenance = state.sharedState.javacInvocationInstance;
        }
      } else {
        JavacInvocationInstance current = state.sharedState.javacInvocationInstance;
        if (provenance != current) {
          value = impl.get(state);
          cache = new WeakReference<>(value);
          provenance = current;
        }
      }
      return value;
    }
  }

  /**
   * Produces a cache for a function that is expected to return the same result throughout a
   * compilation, but requires a VisitorState to compute that result. Do not use this method for a
   * function that depends on the varying state of a VisitorState (e.g. {@link #getPath()}.
   */
  public static <T> Supplier<T> memoize(Supplier<T> f) {
    return new Cache<>(f);
  }

  /**
   * Instances that every {@link VisitorState} instance can share.
   *
   * <p>For the types that are typically stored in {@link Context}, caching the references over
   * {@code SomeClass.instance(context)} has sizable performance improvements in aggregate.
   */
  private static final class SharedState {
    private final Modules modules;
    private final Names names;
    private final Symtab symtab;
    private final ErrorProneTimings timings;
    private final Types types;
    private final TreeMaker treeMaker;
    private final JavacInvocationInstance javacInvocationInstance;

    private final DescriptionListener descriptionListener;
    private final StatisticsCollector statisticsCollector;
    private final Map<String, SeverityLevel> severityMap;
    private final ErrorProneOptions errorProneOptions;

    // TODO(ronshapiro): should we presize this with a reasonable size? We can check for the
    // smallest build and see how many types are loaded and use that. Or perhaps a heuristic
    // based on number of files?
    private final Map<String, Optional<Type>> typeCache = new HashMap<>();

    SharedState(
        Context context,
        DescriptionListener descriptionListener,
        StatisticsCollector statisticsCollector,
        Map<String, SeverityLevel> severityMap,
        ErrorProneOptions errorProneOptions) {
      this.modules = Modules.instance(context);
      this.names = Names.instance(context);
      this.symtab = Symtab.instance(context);
      this.timings = ErrorProneTimings.instance(context);
      this.types = Types.instance(context);
      this.treeMaker = TreeMaker.instance(context);
      this.javacInvocationInstance = JavacInvocationInstance.instance(context);

      this.descriptionListener = descriptionListener;
      this.statisticsCollector = statisticsCollector;
      this.severityMap = severityMap;
      this.errorProneOptions = errorProneOptions;
    }
  }
}
