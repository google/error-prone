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
import com.google.errorprone.BugPattern.SeverityLevel;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/** @author alexeagle@google.com (Alex Eagle) */
public class VisitorState {

  private final DescriptionListener descriptionListener;
  public final Context context;
  private final TreePath path;
  private final Map<String, SeverityLevel> severityMap;
  private final ErrorProneOptions errorProneOptions;
  private final LoadingCache<String, Optional<Type>> typeCache;

  // The default no-op implementation of DescriptionListener. We use this instead of null so callers
  // of getDescriptionListener() don't have to do null-checking.
  private static final DescriptionListener NULL_LISTENER =
      new DescriptionListener() {
        @Override
        public void onDescribed(Description description) {}
      };

  public VisitorState(Context context) {
    this(context, NULL_LISTENER);
  }

  public VisitorState(Context context, DescriptionListener listener) {
    this(
        context,
        listener,
        Collections.<String, SeverityLevel>emptyMap(),
        ErrorProneOptions.empty());
  }

  public VisitorState(
      Context context,
      DescriptionListener listener,
      Map<String, SeverityLevel> severityMap,
      ErrorProneOptions errorProneOptions) {
    this(context, null, listener, severityMap, errorProneOptions, null);
  }

  private VisitorState(
      Context context,
      TreePath path,
      DescriptionListener descriptionListener,
      Map<String, SeverityLevel> severityMap,
      ErrorProneOptions errorProneOptions,
      LoadingCache<String, Optional<Type>> typeCache) {
    this.context = context;
    this.path = path;
    this.descriptionListener = descriptionListener;
    this.severityMap = severityMap;
    this.errorProneOptions = errorProneOptions;
    if (typeCache != null) {
      this.typeCache = typeCache;
    } else {
      this.typeCache =
          CacheBuilder.newBuilder()
              .concurrencyLevel(1) // resolving symbols in javac is not is not thread-safe
              .build(
                  new CacheLoader<String, Optional<Type>>() {
                    @Override
                    public Optional<Type> load(String key) throws Exception {
                      return Optional.fromNullable(getTypeFromStringInternal(key));
                    }
                  });
    }
  }

  public VisitorState withPath(TreePath path) {
    return new VisitorState(
        context, path, descriptionListener, severityMap, errorProneOptions, typeCache);
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
    descriptionListener.onDescribed(description);
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
  public Type getTypeFromString(String typeStr) {
    try {
      return typeCache.get(typeStr).orNull();
    } catch (ExecutionException e) {
      return null;
    }
  }

  private Type getTypeFromStringInternal(String typeStr) {
    validateTypeStr(typeStr);
    if (isPrimitiveType(typeStr)) {
      return getPrimitiveType(typeStr);
    }
    if (isVoidType(typeStr)) {
      return getVoidType();
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
  public java.util.List<ErrorProneToken> getTokensForNode(Tree tree) {
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
          "Cannot convert array types, please build them using " + "getType()");
    }
    if (typeStr.contains("<") || typeStr.contains(">")) {
      throw new IllegalArgumentException(
          "Cannot convert generic types, please build them using getType()");
    }
  }

  /**
   * Given a string that represents a primitive type (e.g., "int"), return the corresponding Type.
   */
  private Type getPrimitiveType(String typeStr) {
    if (typeStr.equals("byte")) {
      return getSymtab().byteType;
    } else if (typeStr.equals("short")) {
      return getSymtab().shortType;
    } else if (typeStr.equals("int")) {
      return getSymtab().intType;
    } else if (typeStr.equals("long")) {
      return getSymtab().longType;
    } else if (typeStr.equals("float")) {
      return getSymtab().floatType;
    } else if (typeStr.equals("double")) {
      return getSymtab().doubleType;
    } else if (typeStr.equals("boolean")) {
      return getSymtab().booleanType;
    } else if (typeStr.equals("char")) {
      return getSymtab().charType;
    } else {
      throw new IllegalStateException("Type string " + typeStr + " expected to be primitive");
    }
  }

  private Type getVoidType() {
    return getSymtab().voidType;
  }

  private static boolean isPrimitiveType(String typeStr) {
    return typeStr.equals("byte")
        || typeStr.equals("short")
        || typeStr.equals("int")
        || typeStr.equals("long")
        || typeStr.equals("float")
        || typeStr.equals("double")
        || typeStr.equals("boolean")
        || typeStr.equals("char");
  }

  private static boolean isVoidType(String typeStr) {
    return typeStr.equals("void");
  }

  /** Returns true if the compilation is targeting Android. */
  public boolean isAndroidCompatible() {
    return Options.instance(context).getBoolean("androidCompatible");
  }
}
