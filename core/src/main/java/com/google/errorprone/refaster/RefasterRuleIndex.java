/*
 * Copyright 2026 The Error Prone Authors.
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

package com.google.errorprone.refaster;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.errorprone.util.ASTHelpers.stringContainsComments;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.errorprone.SuppressionInfo;
import com.google.errorprone.SuppressionInfo.SuppressedState;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Suppressible;
import com.google.errorprone.refaster.RefasterSuppressionHelper.RefasterSuppressible;
import com.google.errorprone.refaster.Unifier.MatchContext;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotatedType;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCMemberReference;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import com.sun.tools.javac.tree.JCTree.JCPackageDecl;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCTypeParameter;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.ListBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;

/**
 * Immutable, thread-safe index over a collection of {@link RefasterRule}s that executes all rules
 * in a single AST pass using per-compilation-unit AST symbol and type-closure pre-filtering,
 * two-level {@code (Tree.Kind, methodName)} dispatch, and hoisted lexical {@code @SuppressWarnings}
 * tracking.
 *
 * <p>Semantics match running {@link RefasterRule#apply} independently for each rule in order.
 *
 * @param <K> Caller-supplied key associated with each indexed rule entry.
 */
public final class RefasterRuleIndex<K> {

  /**
   * Associates a caller-defined key with a {@link RefasterRule} inside a {@link RefasterRuleIndex}.
   */
  public record RuleEntry<K>(K key, RefasterRule<?, ?> rule) {
    public RuleEntry {
      checkNotNull(key);
      checkNotNull(rule);
    }
  }

  private static final int[] EMPTY_INT_ARRAY = new int[0];
  private static final String[] EMPTY_STRING_ARRAY = new String[0];
  private static final ImmutableSet<Kind> NUMERIC_LITERAL_KINDS =
      Sets.immutableEnumSet(
          Kind.INT_LITERAL, Kind.LONG_LITERAL, Kind.FLOAT_LITERAL, Kind.DOUBLE_LITERAL);
  private static final ImmutableSet<Kind> SYMBOL_EXPR_KINDS =
      Sets.immutableEnumSet(
          Kind.IDENTIFIER,
          Kind.MEMBER_SELECT,
          Kind.METHOD_INVOCATION,
          Kind.NEW_CLASS,
          Kind.MEMBER_REFERENCE,
          Kind.ANNOTATED_TYPE,
          Kind.PARAMETERIZED_TYPE);

  private final ImmutableList<RuleEntry<K>> ruleEntries;
  private final ImmutableList<IndexedTemplate<K>> templates;
  private final Map<String, int[]> exprMethodInvByName;
  private final int[] exprMethodInvWildcard;
  private final EnumMap<Kind, int[]> exprByKind;
  private final int[] exprWildcard;
  private final Map<String, int[]> blockExprStmtMethodByName;
  private final EnumMap<Kind, int[]> blockByFirstStmtKind;
  private final int[] blockWildcard;

  private RefasterRuleIndex(
      ImmutableList<RuleEntry<K>> ruleEntries,
      ImmutableList<IndexedTemplate<K>> templates,
      Map<String, int[]> exprMethodInvByName,
      int[] exprMethodInvWildcard,
      EnumMap<Kind, int[]> exprByKind,
      int[] exprWildcard,
      Map<String, int[]> blockExprStmtMethodByName,
      EnumMap<Kind, int[]> blockByFirstStmtKind,
      int[] blockWildcard) {
    this.ruleEntries = ruleEntries;
    this.templates = templates;
    this.exprMethodInvByName = exprMethodInvByName;
    this.exprMethodInvWildcard = exprMethodInvWildcard;
    this.exprByKind = exprByKind;
    this.exprWildcard = exprWildcard;
    this.blockExprStmtMethodByName = blockExprStmtMethodByName;
    this.blockByFirstStmtKind = blockByFirstStmtKind;
    this.blockWildcard = blockWildcard;
  }

  /** Builds an immutable index over the given {@link RuleEntry} sequence. */
  public static <K> RefasterRuleIndex<K> create(Iterable<? extends RuleEntry<K>> entries) {
    ImmutableList<RuleEntry<K>> entryList = ImmutableList.copyOf(entries);
    ImmutableList.Builder<IndexedTemplate<K>> templatesBuilder = ImmutableList.builder();

    Map<String, List<Integer>> exprMethodInvMap = new HashMap<>();
    List<Integer> exprMethodInvWildcardList = new ArrayList<>();
    EnumMap<Kind, List<Integer>> exprByKindMap = new EnumMap<>(Kind.class);
    List<Integer> exprWildcardList = new ArrayList<>();

    Map<String, List<Integer>> blockExprStmtMethodMap = new HashMap<>();
    EnumMap<Kind, List<Integer>> blockByFirstStmtKindMap = new EnumMap<>(Kind.class);
    List<Integer> blockWildcardList = new ArrayList<>();

    int templateId = 0;
    for (int ruleOrdinal = 0; ruleOrdinal < entryList.size(); ruleOrdinal++) {
      RuleEntry<K> entry = entryList.get(ruleOrdinal);
      RefasterRule<?, ?> rule = entry.rule();
      Suppressible suppressible = new RefasterSuppressible(rule);
      for (Template<?> template : rule.beforeTemplates()) {
        TemplateRequirements reqs = extractRequirements(template);
        int minSimpleStmts = 0;
        EnumSet<Kind> requiredBlockStmtKinds = EnumSet.noneOf(Kind.class);
        if (template instanceof BlockTemplate blockTemplate) {
          for (UStatement stmt : blockTemplate.templateStatements()) {
            Kind reqKind = requiredStatementKind(stmt);
            if (reqKind != null) {
              minSimpleStmts++;
              requiredBlockStmtKinds.add(reqKind);
            }
          }
        }
        IndexedTemplate<K> indexed =
            new IndexedTemplate<>(
                ruleOrdinal,
                entry.key(),
                rule,
                suppressible,
                template,
                reqs.astRequiredClassIdents.isEmpty()
                    ? EMPTY_STRING_ARRAY
                    : reqs.astRequiredClassIdents.toArray(new String[0]),
                reqs.astRequiredNonNullArgClasses.isEmpty()
                    ? EMPTY_STRING_ARRAY
                    : reqs.astRequiredNonNullArgClasses.toArray(new String[0]),
                reqs.astRequiredMethodNames.isEmpty()
                    ? EMPTY_STRING_ARRAY
                    : reqs.astRequiredMethodNames.toArray(new String[0]),
                minSimpleStmts,
                requiredBlockStmtKinds);
        templatesBuilder.add(indexed);

        if (template instanceof ExpressionTemplate exprTemplate) {
          RootDispatch dispatch = classifyExpressionRoot(exprTemplate.expression());
          if (dispatch.wildcard) {
            exprWildcardList.add(templateId);
          } else {
            if (dispatch.methodInvWildcard) {
              exprMethodInvWildcardList.add(templateId);
            } else {
              for (String methodName : dispatch.methodInvNames) {
                exprMethodInvMap
                    .computeIfAbsent(methodName, k -> new ArrayList<>())
                    .add(templateId);
              }
            }
            for (Kind kind : dispatch.kinds) {
              if (kind != Kind.METHOD_INVOCATION) {
                exprByKindMap.computeIfAbsent(kind, k -> new ArrayList<>()).add(templateId);
              }
            }
          }
        } else if (template instanceof BlockTemplate blockTemplate) {
          ImmutableList<UStatement> stmts = blockTemplate.templateStatements();
          Kind firstKind = !stmts.isEmpty() ? requiredStatementKind(stmts.getFirst()) : null;
          if (firstKind != null) {
            UStatement firstStmt = stmts.getFirst();
            String firstMethodName =
                firstStmt instanceof UExpressionStatement exprStmt
                    ? extractDirectMethodName(exprStmt.getExpression())
                    : null;
            if (firstMethodName != null) {
              blockExprStmtMethodMap
                  .computeIfAbsent(firstMethodName, k -> new ArrayList<>())
                  .add(templateId);
            } else {
              blockByFirstStmtKindMap
                  .computeIfAbsent(firstKind, k -> new ArrayList<>())
                  .add(templateId);
            }
          } else {
            blockWildcardList.add(templateId);
          }
        }
        templateId++;
      }
    }

    return new RefasterRuleIndex<>(
        entryList,
        templatesBuilder.build(),
        toSortedArrayMap(exprMethodInvMap),
        toSortedUniqueArray(exprMethodInvWildcardList),
        toSortedEnumArrayMap(exprByKindMap),
        toSortedUniqueArray(exprWildcardList),
        toSortedArrayMap(blockExprStmtMethodMap),
        toSortedEnumArrayMap(blockByFirstStmtKindMap),
        toSortedUniqueArray(blockWildcardList));
  }

  /** Applies all indexed rules to {@code path}, forwarding matches to {@code listener}. */
  public void apply(TreePath path, Context context, BiConsumer<K, Description> listener) {
    applyAll(
        (JCCompilationUnit) path.getCompilationUnit(),
        ImmutableList.of(path),
        context,
        k -> true,
        listener,
        null);
  }

  /**
   * Applies all indexed rules accepted by {@code entryFilter} across {@code paths} in a single AST
   * pass per path.
   *
   * @param compilationUnit enclosing compilation unit for all {@code paths}
   * @param paths AST roots (typically the compilation unit or modified member subtrees) to scan
   * @param context javac compilation context
   * @param entryFilter predicate selecting which indexed rule keys are active for this run
   * @param listener receives {@code (key, description)} for every match
   * @param errorHandler optional handler for per-rule runtime exceptions/errors; if {@code null},
   *     exceptions propagate immediately
   */
  public void applyAll(
      JCCompilationUnit compilationUnit,
      Iterable<TreePath> paths,
      Context context,
      Predicate<K> entryFilter,
      BiConsumer<K, Description> listener,
      @Nullable BiConsumer<K, Throwable> errorHandler) {
    if (templates.isEmpty()) {
      return;
    }
    ImmutableList<TreePath> pathList = ImmutableList.copyOf(paths);
    if (pathList.isEmpty()) {
      return;
    }

    MatchContext matchContext = new MatchContext(context, compilationUnit);
    AstSignature signature = collectAstSignature(pathList, matchContext.types());

    boolean[] activeRuleFlags = new boolean[ruleEntries.size()];
    for (int i = 0; i < ruleEntries.size(); i++) {
      activeRuleFlags[i] = entryFilter.test(ruleEntries.get(i).key());
    }

    boolean[] activeTemplateFlags = new boolean[templates.size()];
    boolean anyActive = false;
    boolean anyActiveBlocks = false;
    for (int i = 0; i < templates.size(); i++) {
      IndexedTemplate<K> item = templates.get(i);
      if (!activeRuleFlags[item.ruleOrdinal]) {
        continue;
      }
      if (!isTemplatePotentiallyApplicable(item, signature)) {
        continue;
      }
      activeTemplateFlags[i] = true;
      anyActive = true;
      if (item.template instanceof BlockTemplate) {
        anyActiveBlocks = true;
      }
    }

    if (!anyActive) {
      return;
    }

    int[] activeMethodInvWildcards = filterActiveIds(exprMethodInvWildcard, activeTemplateFlags);
    int[] activeExprWildcards = filterActiveIds(exprWildcard, activeTemplateFlags);
    int[] activeBlockWildcards =
        anyActiveBlocks ? filterActiveIds(blockWildcard, activeTemplateFlags) : EMPTY_INT_ARRAY;

    IndexedRefasterScanner<K> scanner =
        new IndexedRefasterScanner<>(
            this,
            matchContext,
            activeTemplateFlags,
            activeMethodInvWildcards,
            activeExprWildcards,
            activeBlockWildcards,
            anyActiveBlocks,
            listener,
            errorHandler);

    for (TreePath path : pathList) {
      scanner.scanPath(path);
    }
  }

  private static boolean isTemplatePotentiallyApplicable(
      IndexedTemplate<?> item, AstSignature signature) {
    for (String methodName : item.astRequiredMethodNames) {
      if (!signature.presentMethodNames.contains(methodName)) {
        return false;
      }
    }
    for (String classIdent : item.astRequiredClassIdents) {
      if (!signature.presentClassFqcns.contains(classIdent)) {
        return false;
      }
    }
    if (!signature.hasNullOrErrorType) {
      for (String argClass : item.astRequiredNonNullArgClasses) {
        if (!signature.presentClassFqcns.contains(argClass)) {
          return false;
        }
      }
    }
    return true;
  }

  private static @Nullable Kind requiredStatementKind(UStatement stmt) {
    if (stmt instanceof UIf) {
      return Kind.IF;
    }
    if (stmt instanceof USimpleStatement && !(stmt instanceof UClassDecl)) {
      return stmt.getKind();
    }
    return null;
  }

  private static int[] filterActiveIds(int[] ids, boolean[] activeFlags) {
    if (ids.length == 0) {
      return EMPTY_INT_ARRAY;
    }
    int count = 0;
    for (int id : ids) {
      if (activeFlags[id]) {
        count++;
      }
    }
    if (count == 0) {
      return EMPTY_INT_ARRAY;
    }
    if (count == ids.length) {
      return ids;
    }
    int[] filtered = new int[count];
    int idx = 0;
    for (int id : ids) {
      if (activeFlags[id]) {
        filtered[idx++] = id;
      }
    }
    return filtered;
  }

  private static AstSignature collectAstSignature(List<TreePath> paths, Types types) {
    AstSignature signature = new AstSignature();
    Set<Type> visitedTypes = Collections.newSetFromMap(new IdentityHashMap<>());
    Set<Symbol> closedTypeSymbols = new HashSet<>();
    var collector =
        new TreeScanner<Void, Void>() {
          void addType(@Nullable Type type) {
            if (type == null || !visitedTypes.add(type)) {
              return;
            }
            if (type.hasTag(TypeTag.BOT) || type.hasTag(TypeTag.ERROR)) {
              signature.hasNullOrErrorType = true;
              return;
            }
            if (type instanceof Type.ArrayType arrayType) {
              // By JLS §4.10.3, every array type is a subtype of Serializable and Cloneable.
              signature.presentClassFqcns.add("java.io.Serializable");
              signature.presentClassFqcns.add("java.lang.Cloneable");
              addType(arrayType.elemtype);
              return;
            }
            if (type.isPrimitive()) {
              try {
                ClassSymbol boxed = types.boxedClass(type);
                if (boxed != null && boxed.type != null) {
                  addType(boxed.type);
                }
              } catch (RuntimeException ignored) {
                // Ignore unboxable pseudo-primitives (e.g. VOID/NONE).
              }
              return;
            }
            if (type.tsym != null) {
              String fqcn = type.tsym.getQualifiedName().toString();
              if (!fqcn.isEmpty()) {
                signature.presentClassFqcns.add(fqcn);
              }
              if (closedTypeSymbols.add(type.tsym)) {
                try {
                  for (Type sup : types.closure(type)) {
                    addType(sup);
                  }
                } catch (RuntimeException ignored) {
                  // Ignore broken/incomplete types during closure computation.
                }
              }
            }
            try {
              for (Type typeArg : type.getTypeArguments()) {
                addType(typeArg);
              }
            } catch (RuntimeException ignored) {
              // Ignore types without type arguments.
            }
            if (type instanceof Type.WildcardType wildcardType) {
              addType(wildcardType.type);
            } else if (type instanceof Type.MethodType methodType) {
              addType(methodType.restype);
              for (Type paramType : methodType.argtypes) {
                addType(paramType);
              }
            } else if (type instanceof Type.ForAll forAll) {
              addType(forAll.qtype);
            }
          }

          @Override
          public Void scan(@Nullable Tree node, Void unused) {
            if (node == null) {
              return null;
            }
            if (node.getKind() == Kind.NULL_LITERAL) {
              signature.hasNullOrErrorType = true;
            }
            if (node instanceof MethodInvocationTree mit) {
              String name = extractAstMethodName(mit);
              if (name != null) {
                signature.presentMethodNames.add(name);
              }
            }
            if (node instanceof JCTree jc) {
              addType(jc.type);
            }
            Symbol sym = safeGetSymbol(node);
            if (sym != null) {
              if (sym instanceof Symbol.MethodSymbol) {
                signature.presentMethodNames.add(sym.getSimpleName().toString());
              }
              String qn = sym.getQualifiedName().toString();
              if (!qn.isEmpty()) {
                signature.presentClassFqcns.add(qn);
              }
              addType(sym.type);
              Symbol enclosing = sym.getEnclosingElement();
              if (enclosing != null) {
                String encQn = enclosing.getQualifiedName().toString();
                if (!encQn.isEmpty()) {
                  signature.presentClassFqcns.add(encQn);
                }
                addType(enclosing.type);
              }
            } else if (node instanceof JCMethodInvocation
                || node instanceof JCNewClass
                || node instanceof JCMemberReference) {
              signature.hasNullOrErrorType = true;
            }
            return super.scan(node, null);
          }
        };

    for (TreePath path : paths) {
      collector.scan(path.getLeaf(), null);
      for (TreePath ancestor = path.getParentPath();
          ancestor != null;
          ancestor = ancestor.getParentPath()) {
        if (ancestor.getLeaf() instanceof ClassTree classTree) {
          Symbol sym = safeGetSymbol(classTree);
          if (sym != null) {
            String qn = sym.getQualifiedName().toString();
            if (!qn.isEmpty()) {
              signature.presentClassFqcns.add(qn);
            }
            collector.addType(sym.type);
          }
        }
      }
    }
    return signature;
  }

  private static @Nullable Symbol safeGetDeclaredSymbol(@Nullable Tree tree) {
    if (tree instanceof JCPackageDecl pkg) {
      return pkg.packge;
    }
    if (tree instanceof JCTypeParameter typeParam) {
      return typeParam.type != null ? typeParam.type.tsym : null;
    }
    if (tree instanceof JCClassDecl classDecl) {
      return classDecl.sym;
    }
    if (tree instanceof JCMethodDecl methodDecl) {
      return methodDecl.sym;
    }
    if (tree instanceof JCVariableDecl varDecl) {
      return varDecl.sym;
    }
    return null;
  }

  private static @Nullable Symbol safeGetSymbol(@Nullable Tree tree) {
    if (tree instanceof AnnotationTree annotationTree) {
      return safeGetSymbol(annotationTree.getAnnotationType());
    }
    if (tree instanceof JCFieldAccess fieldAccess) {
      return fieldAccess.sym;
    }
    if (tree instanceof JCIdent ident) {
      return ident.sym;
    }
    if (tree instanceof JCMethodInvocation methodInv) {
      Symbol sym = safeGetSymbol(methodInv.getMethodSelect());
      return sym instanceof Symbol.MethodSymbol ? sym.baseSymbol() : sym;
    }
    if (tree instanceof JCNewClass newClass) {
      return newClass.constructor;
    }
    if (tree instanceof JCMemberReference memberRef) {
      return memberRef.sym;
    }
    if (tree instanceof JCAnnotatedType annotatedType) {
      return safeGetSymbol(annotatedType.underlyingType);
    }
    if (tree instanceof ParameterizedTypeTree parameterizedType) {
      return safeGetSymbol(parameterizedType.getType());
    }
    return safeGetDeclaredSymbol(tree);
  }

  private static @Nullable String extractAstMethodName(MethodInvocationTree mit) {
    Tree select = mit.getMethodSelect();
    if (select instanceof MemberSelectTree mst) {
      return mst.getIdentifier().toString();
    }
    if (select instanceof IdentifierTree id) {
      return id.getName().toString();
    }
    Symbol sym = safeGetSymbol(mit);
    return sym != null ? sym.getSimpleName().toString() : null;
  }

  private static TemplateRequirements extractRequirements(Template<?> template) {
    TemplateRequirements reqs = new TemplateRequirements();
    ImmutableMap<String, UType> argTypes = template.expressionArgumentTypes();
    if (template instanceof ExpressionTemplate exprTemplate) {
      extractConjunctiveRequirements(exprTemplate.expression(), argTypes, reqs);
    } else if (template instanceof BlockTemplate blockTemplate) {
      for (UStatement stmt : blockTemplate.templateStatements()) {
        extractConjunctiveRequirements(stmt, argTypes, reqs);
      }
    }
    return reqs;
  }

  private static void extractConjunctiveRequirements(
      @Nullable Tree root, ImmutableMap<String, UType> argTypes, TemplateRequirements out) {
    if (root == null) {
      return;
    }
    new TreeScanner<Void, Void>() {
      @Override
      public Void scan(@Nullable Tree node, Void unused) {
        if (node == null) {
          return null;
        }
        // Skip optional/wildcard/non-unified subtrees so we never over-constrain.
        if (node instanceof URepeated
            || node instanceof UPlaceholderExpression
            || node instanceof UPlaceholderStatement
            || node instanceof UBlank
            || node instanceof UModifiers
            || node instanceof UAnnotation) {
          return null;
        }
        // UMatches and UOfKind delegate accept() directly to expression(), bypassing scan().
        if (node instanceof UMatches matches) {
          scan(matches.expression(), null);
          return null;
        }
        if (node instanceof UOfKind ofKind) {
          scan(ofKind.expression(), null);
          return null;
        }
        if (node instanceof UAnyOf anyOf) {
          ImmutableList<UExpression> branches = anyOf.expressions();
          if (!branches.isEmpty()) {
            TemplateRequirements intersection = new TemplateRequirements();
            extractConjunctiveRequirements(branches.getFirst(), argTypes, intersection);
            for (int i = 1; i < branches.size(); i++) {
              TemplateRequirements branchReqs = new TemplateRequirements();
              extractConjunctiveRequirements(branches.get(i), argTypes, branchReqs);
              intersection.retainAll(branchReqs);
              if (intersection.isEmpty()) {
                break;
              }
            }
            out.addAll(intersection);
          }
          return null;
        }
        if (node instanceof UClassIdent classIdent) {
          addRequiredClassIdent(classIdent.getName().contents(), out.astRequiredClassIdents);
          return null;
        }
        if (node instanceof UMethodIdent methodIdent) {
          addRequiredClassIdent(
              methodIdent.classIdent().getName().contents(), out.astRequiredClassIdents);
          out.astRequiredMethodNames.add(methodIdent.getName().contents());
          return null;
        }
        if (node instanceof UStaticIdent staticIdent) {
          addRequiredClassIdent(
              staticIdent.classIdent().getName().contents(), out.astRequiredClassIdents);
          return null;
        }
        if (node instanceof UMemberSelect memberSelect) {
          UFreeIdent receiverFree = unwrapToFreeIdent(memberSelect.getExpression());
          if (receiverFree != null) {
            UType argType = argTypes.get(receiverFree.getName().contents());
            if (argType instanceof UClassType classType) {
              addRequiredClassIdent(
                  classType.fullyQualifiedClass().contents(), out.astRequiredClassIdents);
            }
          }
        }
        if (node instanceof UFreeIdent freeIdent) {
          UType argType = argTypes.get(freeIdent.getName().contents());
          if (argType instanceof UClassType classType) {
            addRequiredClassIdent(
                classType.fullyQualifiedClass().contents(), out.astRequiredNonNullArgClasses);
          }
          return null;
        }
        if (node instanceof UMethodInvocation methodInv) {
          String methodName = extractDirectMethodName(methodInv);
          if (methodName != null) {
            out.astRequiredMethodNames.add(methodName);
          }
        }
        return super.scan(node, null);
      }
    }.scan(root, null);
  }

  private static @Nullable UFreeIdent unwrapToFreeIdent(UExpression expr) {
    while (true) {
      if (expr instanceof UFreeIdent freeIdent) {
        return freeIdent;
      }
      if (expr instanceof UParens parens) {
        expr = parens.getExpression();
      } else if (expr instanceof UMatches matches) {
        expr = matches.expression();
      } else if (expr instanceof UOfKind ofKind) {
        expr = ofKind.expression();
      } else {
        return null;
      }
    }
  }

  private static void addRequiredClassIdent(String fqcn, Set<String> target) {
    if (!fqcn.isEmpty() && !fqcn.equals("java.lang.Object")) {
      target.add(fqcn);
    }
  }

  private static @Nullable String extractDirectMethodName(UExpression expr) {
    if (!(expr instanceof UMethodInvocation methodInv)) {
      return null;
    }
    UExpression select = methodInv.getMethodSelect();
    if (select instanceof UMemberSelect memberSelect) {
      return memberSelect.getIdentifier().contents();
    }
    if (select instanceof UMethodIdent methodIdent) {
      return methodIdent.getName().contents();
    }
    if (select instanceof UStaticIdent staticIdent) {
      return staticIdent.getName().contents();
    }
    return null;
  }

  private static RootDispatch classifyExpressionRoot(UExpression root) {
    if (root instanceof UMethodInvocation methodInv) {
      String methodName = extractDirectMethodName(methodInv);
      RootDispatch d = new RootDispatch();
      if (methodName != null) {
        d.methodInvNames.add(methodName);
      } else {
        d.methodInvWildcard = true;
      }
      return d;
    }
    if (root instanceof UParens parens) {
      RootDispatch d = classifyExpressionRoot(parens.getExpression());
      if (!d.wildcard) {
        d.kinds.add(Kind.PARENTHESIZED);
      }
      return d;
    }
    if (root instanceof UMatches matches) {
      RootDispatch d = classifyExpressionRoot(matches.expression());
      if (!d.wildcard) {
        d.kinds.add(Kind.PARENTHESIZED);
      }
      return d;
    }
    if (root instanceof UAnyOf anyOf) {
      RootDispatch combined = new RootDispatch();
      for (UExpression branch : anyOf.expressions()) {
        RootDispatch bd = classifyExpressionRoot(branch);
        if (bd.wildcard) {
          return RootDispatch.wildcardDispatch();
        }
        combined.merge(bd);
      }
      combined.kinds.add(Kind.PARENTHESIZED);
      return combined;
    }
    if (root instanceof UOfKind ofKind) {
      RootDispatch inner = classifyExpressionRoot(ofKind.expression());
      if (!inner.wildcard) {
        // Intersect inner dispatch with ofKind.allowed(); conservatively keep inner's method info
        // only when METHOD_INVOCATION is allowed.
        RootDispatch restricted = new RootDispatch();
        for (Kind k : inner.kinds) {
          if (ofKind.allowed().contains(k)) {
            restricted.kinds.add(k);
          }
        }
        if (ofKind.allowed().contains(Kind.METHOD_INVOCATION)) {
          restricted.methodInvWildcard = inner.methodInvWildcard;
          restricted.methodInvNames.addAll(inner.methodInvNames);
        }
        return restricted;
      }
      RootDispatch d = new RootDispatch();
      for (Kind k : ofKind.allowed()) {
        if (k == Kind.METHOD_INVOCATION) {
          d.methodInvWildcard = true;
        } else {
          d.kinds.add(k);
        }
      }
      return d;
    }
    if (root instanceof URepeated repeated) {
      return classifyExpressionRoot(repeated.expression());
    }
    if (root instanceof UFreeIdent || root instanceof UPlaceholderExpression) {
      return RootDispatch.wildcardDispatch();
    }
    if (root instanceof ULiteral literal) {
      RootDispatch d = new RootDispatch();
      if (literal.getValue() instanceof Number) {
        d.kinds.addAll(NUMERIC_LITERAL_KINDS);
      } else {
        d.kinds.add(literal.getKind());
      }
      return d;
    }
    if (root instanceof UMemberSelect) {
      RootDispatch d = new RootDispatch();
      d.kinds.add(Kind.MEMBER_SELECT);
      d.kinds.add(Kind.IDENTIFIER);
      return d;
    }
    if (root instanceof UClassIdent
        || root instanceof UStaticIdent
        || root instanceof UMethodIdent) {
      RootDispatch d = new RootDispatch();
      d.kinds.addAll(SYMBOL_EXPR_KINDS);
      d.methodInvWildcard = true;
      return d;
    }
    if (root instanceof UBinary
        || root instanceof UUnary
        || root instanceof UAssign
        || root instanceof UAssignOp
        || root instanceof UConditional
        || root instanceof UNewClass
        || root instanceof UNewArray
        || root instanceof ULambda
        || root instanceof UMemberReference
        || root instanceof UInstanceOf
        || root instanceof UTypeCast
        || root instanceof UArrayAccess
        || root instanceof UAnnotatedType
        || root instanceof UArrayTypeTree
        || root instanceof UPrimitiveTypeTree
        || root instanceof UTypeApply
        || root instanceof UIntersectionType
        || root instanceof UUnionType
        || root instanceof UWildcard
        || root instanceof ULocalVarIdent) {
      RootDispatch d = new RootDispatch();
      d.kinds.add(root.getKind());
      return d;
    }
    return RootDispatch.wildcardDispatch();
  }

  private static Map<String, int[]> toSortedArrayMap(Map<String, List<Integer>> input) {
    Map<String, int[]> out = HashMap.newHashMap(input.size());
    for (Map.Entry<String, List<Integer>> entry : input.entrySet()) {
      out.put(entry.getKey(), toSortedUniqueArray(entry.getValue()));
    }
    return out;
  }

  private static EnumMap<Kind, int[]> toSortedEnumArrayMap(EnumMap<Kind, List<Integer>> input) {
    EnumMap<Kind, int[]> out = new EnumMap<>(Kind.class);
    for (Map.Entry<Kind, List<Integer>> entry : input.entrySet()) {
      out.put(entry.getKey(), toSortedUniqueArray(entry.getValue()));
    }
    return out;
  }

  private static int[] toSortedUniqueArray(List<Integer> values) {
    if (values.isEmpty()) {
      return EMPTY_INT_ARRAY;
    }
    int[] arr = new int[values.size()];
    for (int i = 0; i < values.size(); i++) {
      arr[i] = values.get(i);
    }
    Arrays.sort(arr);
    int unique = 1;
    for (int i = 1; i < arr.length; i++) {
      if (arr[i] != arr[unique - 1]) {
        arr[unique++] = arr[i];
      }
    }
    return unique == arr.length ? arr : Arrays.copyOf(arr, unique);
  }

  private static final class IndexedTemplate<K> {
    final int ruleOrdinal;
    final K key;
    final RefasterRule<?, ?> rule;
    final Suppressible suppressible;
    final Template<?> template;
    final String[] astRequiredClassIdents;
    final String[] astRequiredNonNullArgClasses;
    final String[] astRequiredMethodNames;
    final int minSimpleStatements;
    final EnumSet<Kind> requiredBlockStmtKinds;

    IndexedTemplate(
        int ruleOrdinal,
        K key,
        RefasterRule<?, ?> rule,
        Suppressible suppressible,
        Template<?> template,
        String[] astRequiredClassIdents,
        String[] astRequiredNonNullArgClasses,
        String[] astRequiredMethodNames,
        int minSimpleStatements,
        EnumSet<Kind> requiredBlockStmtKinds) {
      this.ruleOrdinal = ruleOrdinal;
      this.key = key;
      this.rule = rule;
      this.suppressible = suppressible;
      this.template = template;
      this.astRequiredClassIdents = astRequiredClassIdents;
      this.astRequiredNonNullArgClasses = astRequiredNonNullArgClasses;
      this.astRequiredMethodNames = astRequiredMethodNames;
      this.minSimpleStatements = minSimpleStatements;
      this.requiredBlockStmtKinds = requiredBlockStmtKinds;
    }
  }

  private static final class AstSignature {
    final Set<String> presentMethodNames = new HashSet<>();
    final Set<String> presentClassFqcns = new HashSet<>();
    boolean hasNullOrErrorType;
  }

  private static final class TemplateRequirements {
    final Set<String> astRequiredClassIdents = new HashSet<>();
    final Set<String> astRequiredNonNullArgClasses = new HashSet<>();
    final Set<String> astRequiredMethodNames = new HashSet<>();

    void addAll(TemplateRequirements other) {
      astRequiredClassIdents.addAll(other.astRequiredClassIdents);
      astRequiredNonNullArgClasses.addAll(other.astRequiredNonNullArgClasses);
      astRequiredMethodNames.addAll(other.astRequiredMethodNames);
    }

    void retainAll(TemplateRequirements other) {
      astRequiredClassIdents.retainAll(other.astRequiredClassIdents);
      astRequiredNonNullArgClasses.retainAll(other.astRequiredNonNullArgClasses);
      astRequiredMethodNames.retainAll(other.astRequiredMethodNames);
    }

    boolean isEmpty() {
      return astRequiredClassIdents.isEmpty()
          && astRequiredNonNullArgClasses.isEmpty()
          && astRequiredMethodNames.isEmpty();
    }
  }

  private static final class RootDispatch {
    boolean wildcard;
    boolean methodInvWildcard;
    final Set<String> methodInvNames = new HashSet<>();
    final EnumSet<Kind> kinds = EnumSet.noneOf(Kind.class);

    static RootDispatch wildcardDispatch() {
      RootDispatch d = new RootDispatch();
      d.wildcard = true;
      return d;
    }

    void merge(RootDispatch other) {
      if (other.wildcard) {
        wildcard = true;
        return;
      }
      methodInvWildcard |= other.methodInvWildcard;
      if (!methodInvWildcard) {
        methodInvNames.addAll(other.methodInvNames);
      } else {
        methodInvNames.clear();
      }
      kinds.addAll(other.kinds);
    }
  }

  /**
   * Single-pass AST scanner matching {@link RefasterScanner} traversal, paren-skipping, synthetic
   * class-member block scanning, and lexical {@code @SuppressWarnings} semantics across all active
   * templates simultaneously.
   */
  private static final class IndexedRefasterScanner<K> extends TreeScanner<Void, Context> {
    private final RefasterRuleIndex<K> index;
    private final MatchContext matchContext;
    private final boolean[] activeTemplates;
    private final int[] activeMethodInvWildcards;
    private final int[] activeExprWildcards;
    private final int[] activeBlockWildcards;
    private final boolean anyActiveBlocks;
    private final BiConsumer<K, Description> listener;
    private final @Nullable BiConsumer<K, Throwable> errorHandler;

    private final ArrayDeque<String> enclosingClassNames = new ArrayDeque<>();
    private SuppressionInfo currentSuppression = SuppressionInfo.EMPTY;
    private @Nullable TreePath currentPath;

    private final int[] blockSeenStamp;
    private int currentStamp = 0;
    private int[] candidateScratch = new int[64];
    private final EnumSet<Kind> blockStmtKindsScratch = EnumSet.noneOf(Kind.class);

    IndexedRefasterScanner(
        RefasterRuleIndex<K> index,
        MatchContext matchContext,
        boolean[] activeTemplates,
        int[] activeMethodInvWildcards,
        int[] activeExprWildcards,
        int[] activeBlockWildcards,
        boolean anyActiveBlocks,
        BiConsumer<K, Description> listener,
        @Nullable BiConsumer<K, Throwable> errorHandler) {
      this.index = index;
      this.matchContext = matchContext;
      this.activeTemplates = activeTemplates;
      this.activeMethodInvWildcards = activeMethodInvWildcards;
      this.activeExprWildcards = activeExprWildcards;
      this.activeBlockWildcards = activeBlockWildcards;
      this.anyActiveBlocks = anyActiveBlocks;
      this.listener = listener;
      this.errorHandler = errorHandler;
      this.blockSeenStamp = new int[index.templates.size()];
    }

    void scanPath(TreePath path) {
      SuppressionInfo savedSuppression = currentSuppression;
      int savedClassDepth = enclosingClassNames.size();
      TreePath savedPath = currentPath;
      try {
        // Apply ancestor suppressions & enclosing template class checks identically to
        // RefasterScanner.scan(TreePath, Context).
        VisitorState state = matchContext.visitorState();
        List<Tree> ancestors = new ArrayList<>();
        for (TreePath parent = path.getParentPath();
            parent != null;
            parent = parent.getParentPath()) {
          ancestors.add(parent.getLeaf());
        }
        for (int i = ancestors.size() - 1; i >= 0; i--) {
          Tree ancestor = ancestors.get(i);
          if (ancestor instanceof JCClassDecl classDecl && classDecl.sym != null) {
            enclosingClassNames.push(classDecl.sym.getQualifiedName().toString());
          }
          Symbol sym = safeGetDeclaredSymbol(ancestor);
          if (sym != null) {
            currentSuppression =
                currentSuppression.withExtendedSuppressions(sym, state, ImmutableSet.of());
          }
        }
        if (isAllSuppressed()) {
          return;
        }
        currentPath = path.getParentPath();
        scan(path.getLeaf(), matchContext.context());
      } finally {
        currentSuppression = savedSuppression;
        currentPath = savedPath;
        while (enclosingClassNames.size() > savedClassDepth) {
          enclosingClassNames.pop();
        }
      }
    }

    @Override
    public Void visitClass(ClassTree node, Context context) {
      SuppressionInfo prevSuppression = currentSuppression;
      boolean pushedClass = false;
      ClassSymbol classSym = node instanceof JCClassDecl jcClass ? jcClass.sym : null;
      if (classSym != null) {
        enclosingClassNames.push(classSym.getQualifiedName().toString());
        pushedClass = true;
      }
      Symbol declSym = safeGetDeclaredSymbol(node);
      if (declSym != null) {
        currentSuppression =
            currentSuppression.withExtendedSuppressions(
                declSym, matchContext.visitorState(), ImmutableSet.of());
      }
      try {
        if (isAllSuppressed()) {
          return null;
        }
        ListBuffer<JCStatement> statements = new ListBuffer<>();
        for (Tree tree : node.getMembers()) {
          if (tree instanceof JCStatement jcStatement) {
            statements.append(jcStatement);
          } else {
            TreePath prevPath = currentPath;
            currentPath = currentPath != null ? new TreePath(currentPath, tree) : null;
            try {
              tree.accept(this, context);
            } finally {
              currentPath = prevPath;
            }
          }
        }
        JCBlock syntheticBlock = matchContext.treeMaker().Block(0, statements.toList());
        // Synthetic class-member blocks are not real child nodes of ClassTree, so match against
        // them directly and scan their member statements with ClassTree as parent TreePath.
        if (anyActiveBlocks) {
          dispatchBlock(syntheticBlock, /* blockPath= */ null);
        }
        for (JCStatement stmt : syntheticBlock.getStatements()) {
          scan(stmt, context);
        }
        return null;
      } finally {
        currentSuppression = prevSuppression;
        if (pushedClass) {
          enclosingClassNames.pop();
        }
      }
    }

    @Override
    public Void visitMethod(MethodTree node, Context context) {
      SuppressionInfo prevSuppression = currentSuppression;
      Symbol declSym = safeGetDeclaredSymbol(node);
      if (declSym != null) {
        currentSuppression =
            currentSuppression.withExtendedSuppressions(
                declSym, matchContext.visitorState(), ImmutableSet.of());
      }
      try {
        if (isAllSuppressed()) {
          return null;
        }
        return super.visitMethod(node, context);
      } finally {
        currentSuppression = prevSuppression;
      }
    }

    @Override
    public Void visitVariable(VariableTree node, Context context) {
      SuppressionInfo prevSuppression = currentSuppression;
      Symbol declSym = safeGetDeclaredSymbol(node);
      if (declSym != null) {
        currentSuppression =
            currentSuppression.withExtendedSuppressions(
                declSym, matchContext.visitorState(), ImmutableSet.of());
      }
      try {
        if (isAllSuppressed()) {
          return null;
        }
        return super.visitVariable(node, context);
      } finally {
        currentSuppression = prevSuppression;
      }
    }

    @Override
    public Void scan(@Nullable Tree tree, Context context) {
      if (tree == null) {
        return null;
      }
      TreePath prevPath = currentPath;
      TreePath nodePath =
          prevPath != null
              ? new TreePath(prevPath, tree)
              : (tree instanceof JCCompilationUnit cu ? new TreePath(cu) : null);
      currentPath = nodePath;
      try {
        if (tree instanceof JCExpression expr) {
          dispatchExpression(expr, nodePath);
        } else if (anyActiveBlocks && tree instanceof JCBlock block) {
          dispatchBlock(block, nodePath);
        }
        return super.scan(tree, context);
      } finally {
        currentPath = prevPath;
      }
    }

    @Override
    public Void visitDoWhileLoop(DoWhileLoopTree node, Context context) {
      scan(node.getStatement(), context);
      scanSkippingParens(node.getCondition(), context);
      return null;
    }

    @Override
    public Void visitWhileLoop(WhileLoopTree node, Context context) {
      scanSkippingParens(node.getCondition(), context);
      scan(node.getStatement(), context);
      return null;
    }

    @Override
    public Void visitSynchronized(SynchronizedTree node, Context context) {
      scanSkippingParens(node.getExpression(), context);
      scan(node.getBlock(), context);
      return null;
    }

    @Override
    public Void visitIf(IfTree node, Context context) {
      scanSkippingParens(node.getCondition(), context);
      scan(node.getThenStatement(), context);
      scan(node.getElseStatement(), context);
      return null;
    }

    private void scanSkippingParens(@Nullable Tree expr, Context context) {
      TreePath prevPath = currentPath;
      Tree current = expr;
      while (current instanceof ParenthesizedTree paren) {
        currentPath = currentPath != null ? new TreePath(currentPath, paren) : null;
        current = paren.getExpression();
      }
      try {
        scan(current, context);
      } finally {
        currentPath = prevPath;
      }
    }

    private boolean isAllSuppressed() {
      return currentSuppression != SuppressionInfo.EMPTY
          && currentSuppression
              .suppressedState(
                  RefasterSuppressionHelper.SUPPRESS_ALL_PROBE,
                  /* suppressedInGeneratedCode= */ false,
                  matchContext.visitorState())
              .equals(SuppressedState.SUPPRESSED);
    }

    private boolean isRuleSuppressed(IndexedTemplate<K> item) {
      if (!enclosingClassNames.isEmpty()
          && enclosingClassNames.contains(item.rule.qualifiedTemplateClass())) {
        return true;
      }
      return currentSuppression != SuppressionInfo.EMPTY
          && currentSuppression
              .suppressedState(
                  item.suppressible,
                  /* suppressedInGeneratedCode= */ false,
                  matchContext.visitorState())
              .equals(SuppressedState.SUPPRESSED);
    }

    private void dispatchExpression(JCExpression expr, @Nullable TreePath nodePath) {
      Kind kind = expr.getKind();
      if (kind == Kind.METHOD_INVOCATION) {
        String methodName = extractAstMethodName((MethodInvocationTree) expr);
        int[] namedBucket = methodName != null ? index.exprMethodInvByName.get(methodName) : null;
        if (activeMethodInvWildcards.length == 0 && activeExprWildcards.length == 0) {
          if (namedBucket != null) {
            runActiveBucket(namedBucket, expr, nodePath);
          }
          return;
        }
        int count = 0;
        if (namedBucket != null) {
          count = appendActive(namedBucket, count);
        }
        count = appendAll(activeMethodInvWildcards, count);
        count = appendAll(activeExprWildcards, count);
        runSortedCandidates(count, expr, nodePath);
      } else {
        int[] kindBucket = index.exprByKind.get(kind);
        if (activeExprWildcards.length == 0) {
          if (kindBucket != null) {
            runActiveBucket(kindBucket, expr, nodePath);
          }
          return;
        }
        if (kindBucket == null) {
          runActiveBucket(activeExprWildcards, expr, nodePath);
          return;
        }
        int count = appendActive(kindBucket, 0);
        count = appendAll(activeExprWildcards, count);
        runSortedCandidates(count, expr, nodePath);
      }
    }

    private void dispatchBlock(JCBlock block, @Nullable TreePath blockPath) {
      com.sun.tools.javac.util.List<JCStatement> stmts = block.getStatements();
      int stmtCount = stmts.size();
      if (stmtCount == 0 && activeBlockWildcards.length == 0) {
        return;
      }
      blockStmtKindsScratch.clear();
      for (JCStatement stmt : stmts) {
        blockStmtKindsScratch.add(stmt.getKind());
      }
      int stamp = ++currentStamp;
      int count = 0;
      for (JCStatement stmt : stmts) {
        Kind stmtKind = stmt.getKind();
        if (stmtKind == Kind.EXPRESSION_STATEMENT) {
          ExpressionStatementTree est = (ExpressionStatementTree) stmt;
          if (est.getExpression() instanceof MethodInvocationTree mit) {
            String methodName = extractAstMethodName(mit);
            if (methodName != null) {
              int[] methodBucket = index.blockExprStmtMethodByName.get(methodName);
              if (methodBucket != null) {
                count = appendActiveBlockBucket(methodBucket, stmtCount, stamp, count);
              }
            }
          }
        }
        int[] kindBucket = index.blockByFirstStmtKind.get(stmtKind);
        if (kindBucket != null) {
          count = appendActiveBlockBucket(kindBucket, stmtCount, stamp, count);
        }
      }
      if (activeBlockWildcards.length > 0) {
        count = appendActiveBlockBucket(activeBlockWildcards, stmtCount, stamp, count);
      }
      runSortedCandidates(count, block, blockPath);
    }

    private int appendActive(int[] bucket, int count) {
      for (int id : bucket) {
        if (activeTemplates[id]) {
          count = appendCandidate(id, count);
        }
      }
      return count;
    }

    private int appendAll(int[] bucket, int count) {
      for (int id : bucket) {
        count = appendCandidate(id, count);
      }
      return count;
    }

    private int appendActiveBlockBucket(int[] bucket, int stmtCount, int stamp, int count) {
      for (int id : bucket) {
        if (activeTemplates[id] && blockSeenStamp[id] != stamp) {
          IndexedTemplate<K> item = index.templates.get(id);
          if (item.minSimpleStatements <= stmtCount
              && blockStmtKindsScratch.containsAll(item.requiredBlockStmtKinds)) {
            blockSeenStamp[id] = stamp;
            count = appendCandidate(id, count);
          }
        }
      }
      return count;
    }

    private int appendCandidate(int id, int count) {
      if (count == candidateScratch.length) {
        candidateScratch = Arrays.copyOf(candidateScratch, candidateScratch.length * 2);
      }
      candidateScratch[count++] = id;
      return count;
    }

    private void runActiveBucket(int[] bucket, JCTree tree, @Nullable TreePath nodePath) {
      for (int id : bucket) {
        if (activeTemplates[id]) {
          runTemplate(index.templates.get(id), tree, nodePath);
        }
      }
    }

    private void runSortedCandidates(int count, JCTree tree, @Nullable TreePath nodePath) {
      if (count == 0) {
        return;
      }
      if (count > 1) {
        Arrays.sort(candidateScratch, 0, count);
      }
      int prev = -1;
      for (int i = 0; i < count; i++) {
        int id = candidateScratch[i];
        if (id != prev) {
          prev = id;
          runTemplate(index.templates.get(id), tree, nodePath);
        }
      }
    }

    // RefasterRule<M, T> and Template<M> share a correlated match type M per rule entry, so every
    // TemplateMatch yielded by item.template.match(...) is accepted by
    // RefasterScanner.buildDescription(item.rule, match).
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void runTemplate(IndexedTemplate<K> item, JCTree tree, @Nullable TreePath nodePath) {
      if (isRuleSuppressed(item)) {
        return;
      }
      try {
        JCCompilationUnit compilationUnit = matchContext.compilationUnit();
        Context context = matchContext.context();
        RefasterRule rule = item.rule;
        Template template = item.template;
        matchLoop:
        for (Object matchObj : template.match(tree, matchContext, nodePath)) {
          TemplateMatch match = (TemplateMatch) matchObj;
          if (rule.rejectMatchesWithComments()) {
            String matchContents = match.getRange(compilationUnit);
            if (stringContainsComments(matchContents, context)) {
              continue matchLoop;
            }
          }
          listener.accept(item.key, RefasterScanner.buildDescription(rule, match));
        }
      } catch (RuntimeException | Error e) {
        if (errorHandler != null) {
          errorHandler.accept(item.key, e);
        } else {
          throw e;
        }
      }
    }
  }
}
