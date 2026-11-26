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

import static com.google.common.truth.Truth.assertThat;
import static com.google.testing.compile.JavaFileObjects.forSourceLines;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.errorprone.CodeTransformer;
import com.google.errorprone.CompositeCodeTransformer;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.ErrorProneEndPosTable;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.refaster.RefasterRuleIndex.RuleEntry;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.TreeScanner;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.tools.JavaFileObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link RefasterRuleIndex}. */
@RunWith(JUnit4.class)
public final class RefasterRuleIndexTest extends CompilerBasedTest {

  public static final class CountingMatcher implements Matcher<ExpressionTree> {
    static final AtomicInteger INVOCATIONS = new AtomicInteger();

    @Override
    public boolean matches(ExpressionTree tree, VisitorState state) {
      INVOCATIONS.incrementAndGet();
      return true;
    }
  }

  public static final class ThrowingMatcher implements Matcher<ExpressionTree> {
    @Override
    public boolean matches(ExpressionTree tree, VisitorState state) {
      throw new IllegalStateException("intentional matcher failure");
    }
  }

  private record RecordedMatch(int ruleIndex, String checkName, int startPos, String fixSummary) {}

  private ImmutableList<RefasterRule<?, ?>> extractRules(JavaFileObject... templateFiles) {
    ImmutableList.Builder<RefasterRule<?, ?>> rules = ImmutableList.builder();
    for (JavaFileObject file : templateFiles) {
      List<JCCompilationUnit> units = new ArrayList<>();
      compile(
          new TreeScanner() {
            @Override
            public void visitTopLevel(JCCompilationUnit tree) {
              units.add(tree);
            }
          },
          file);
      for (JCCompilationUnit cu : units) {
        new com.sun.source.util.TreeScanner<Void, Void>() {
          @Override
          public Void visitClass(ClassTree classTree, Void unused) {
            for (CodeTransformer ct : RefasterRuleBuilderScanner.extractRules(classTree, context)) {
              collectRefasterRules(ct, rules);
            }
            return super.visitClass(classTree, null);
          }
        }.scan(cu, null);
      }
    }
    return rules.build();
  }

  private static void collectRefasterRules(
      CodeTransformer transformer, ImmutableList.Builder<RefasterRule<?, ?>> out) {
    if (transformer instanceof RefasterRule<?, ?> rule) {
      out.add(rule);
    } else if (transformer instanceof CompositeCodeTransformer composite) {
      for (CodeTransformer child : composite.transformers()) {
        collectRefasterRules(child, out);
      }
    }
  }

  private JCCompilationUnit compileSingleUnit(JavaFileObject source) {
    List<JCCompilationUnit> units = new ArrayList<>();
    compile(
        new TreeScanner() {
          @Override
          public void visitTopLevel(JCCompilationUnit tree) {
            units.add(tree);
          }
        },
        source);
    return Iterables.getOnlyElement(units);
  }

  private void assertIndexedMatchesBaseline(
      ImmutableList<RefasterRule<?, ?>> rules,
      JavaFileObject targetSource,
      List<TreePath> customPaths) {
    assertThat(rules).isNotEmpty();
    JCCompilationUnit cu = compileSingleUnit(targetSource);
    List<TreePath> paths = customPaths != null ? customPaths : ImmutableList.of(new TreePath(cu));

    List<RecordedMatch> baseline = new ArrayList<>();
    for (int i = 0; i < rules.size(); i++) {
      int ruleIdx = i;
      RefasterRule<?, ?> rule = rules.get(i);
      for (TreePath path : paths) {
        rule.apply(path, context, d -> baseline.add(toRecordedMatch(ruleIdx, d, cu)));
      }
    }

    List<RuleEntry<Integer>> entries = new ArrayList<>();
    for (int i = 0; i < rules.size(); i++) {
      entries.add(new RuleEntry<>(i, rules.get(i)));
    }
    RefasterRuleIndex<Integer> index = RefasterRuleIndex.create(entries);
    List<RecordedMatch> indexed = new ArrayList<>();
    index.applyAll(
        cu, paths, context, k -> true, (k, d) -> indexed.add(toRecordedMatch(k, d, cu)), null);
    indexed.sort(Comparator.comparingInt(RecordedMatch::ruleIndex));

    assertThat(indexed).containsExactlyElementsIn(baseline).inOrder();
  }

  private static RecordedMatch toRecordedMatch(int ruleIndex, Description d, JCCompilationUnit cu) {
    String fixSummary =
        d.fixes.isEmpty()
            ? ""
            : d.fixes.getFirst().getReplacements(ErrorProneEndPosTable.create(cu)).toString();
    return new RecordedMatch(ruleIndex, d.checkName, d.position.getStartPosition(), fixSummary);
  }

  @Test
  public void expressionTemplates_parityAcrossKindsNegationAnyOfLiteralAndRepeated() {
    ImmutableList<RefasterRule<?, ?>> rules =
        extractRules(
            forSourceLines(
                "Templates.java",
                """
                import com.google.errorprone.refaster.Refaster;
                import com.google.errorprone.refaster.annotation.AfterTemplate;
                import com.google.errorprone.refaster.annotation.AlsoNegation;
                import com.google.errorprone.refaster.annotation.BeforeTemplate;
                import com.google.errorprone.refaster.annotation.Repeated;
                import java.util.ArrayList;
                import java.util.List;
                public class Templates {
                  static class StringEqualsEmpty {
                    @BeforeTemplate boolean before(String s) { return s.equals(""); }
                    @AfterTemplate @AlsoNegation boolean after(String s) { return s.isEmpty(); }
                  }
                  static class AnyOfZeroLength {
                    @BeforeTemplate boolean before(String s) {
                      return Refaster.anyOf(s.length() == 0, s.equals(""));
                    }
                    @AfterTemplate boolean after(String s) { return s.isEmpty(); }
                  }
                  static class NewArrayListZero {
                    @BeforeTemplate List<String> before() { return new ArrayList<String>(0); }
                    @AfterTemplate List<String> after() { return new ArrayList<String>(); }
                  }
                  static class FormatRepeated {
                    @BeforeTemplate String before(String fmt, @Repeated Object args) {
                      return String.format(fmt, Refaster.asVarargs(args));
                    }
                    @AfterTemplate String after(String fmt, @Repeated Object args) {
                      return fmt.formatted(Refaster.asVarargs(args));
                    }
                  }
                  static class UnusedTypeRule {
                    @BeforeTemplate int before(java.util.regex.Pattern p) { return p.flags(); }
                    @AfterTemplate int after(java.util.regex.Pattern p) { return 0; }
                  }
                  static class LongZeroCompare {
                    @BeforeTemplate boolean before(long x) { return x == 0; }
                    @AfterTemplate boolean after(long x) { return x == 0L; }
                  }
                }
                """));

    JavaFileObject target =
        forSourceLines(
            "Target.java",
            """
            import java.util.ArrayList;
            import java.util.List;
            public class Target {
              boolean f(String s, long n) {
                if ((s.equals(""))) return true;
                if (!s.equals("")) return false;
                if (s.length() == 0) return true;
                if (s.equals(/* comment */ "")) return false;
                List<String> l = new ArrayList<String>(0);
                String formatted = String.format("hello");
                return n == 0L && l.isEmpty() && formatted.isEmpty();
              }
            }
            """);

    assertIndexedMatchesBaseline(rules, target, null);
  }

  @Test
  public void blockTemplatesAndClassMembers_parity() {
    ImmutableList<RefasterRule<?, ?>> rules =
        extractRules(
            forSourceLines(
                "BlockTemplates.java",
                """
                import com.google.errorprone.refaster.annotation.AfterTemplate;
                import com.google.errorprone.refaster.annotation.BeforeTemplate;
                import java.util.List;
                public class BlockTemplates {
                  static class AddTwice {
                    @BeforeTemplate void before(List<String> list, String a, String b) {
                      list.add(a);
                      list.add(b);
                    }
                    @AfterTemplate void after(List<String> list, String a, String b) {
                      list.addAll(List.of(a, b));
                    }
                  }
                  static class RedundantIf {
                    @BeforeTemplate boolean before(boolean cond) {
                      if (cond) { return true; } else { return false; }
                    }
                    @AfterTemplate boolean after(boolean cond) {
                      return cond;
                    }
                  }
                }
                """));

    JavaFileObject target =
        forSourceLines(
            "BlockTarget.java",
            """
            import java.util.List;
            public class BlockTarget {
              boolean run(List<String> list, boolean c) {
                list.add("x");
                list.add("y");
                if (c) { return true; } else { return false; }
              }
            }
            """);

    assertIndexedMatchesBaseline(rules, target, null);
  }

  @Test
  public void suppressWarningsAndSubtreePaths_parity() {
    ImmutableList<RefasterRule<?, ?>> rules =
        extractRules(
            forSourceLines(
                "SuppressedTemplates.java",
                """
                import com.google.errorprone.refaster.annotation.AfterTemplate;
                import com.google.errorprone.refaster.annotation.BeforeTemplate;
                public class SuppressedTemplates {
                  static class EqEmpty {
                    @BeforeTemplate boolean before(String s) { return s.equals(""); }
                    @AfterTemplate boolean after(String s) { return s.isEmpty(); }
                  }
                }
                """));

    JavaFileObject target =
        forSourceLines(
            "SuppressedTarget.java",
            """
            public class SuppressedTarget {
              boolean unsuppressed(String s) { return s.equals(""); }
              @SuppressWarnings("EqEmpty")
              boolean methodSuppressed(String s) { return s.equals(""); }
              @SuppressWarnings("all")
              boolean allSuppressed(String s) { return s.equals(""); }
              boolean varSuppressed(String s) {
                @SuppressWarnings("EqEmpty")
                boolean b = s.equals("");
                return b || s.equals("");
              }
              @SuppressWarnings("EqEmpty")
              static class SuppressedInner {
                boolean inside(String s) { return s.equals(""); }
              }
            }
            """);

    // Full CU parity
    assertIndexedMatchesBaseline(rules, target, null);

    // Subtree TreePath parity (verify ancestor @SuppressWarnings on SuppressedInner is respected!)
    JCCompilationUnit cu = compileSingleUnit(target);
    List<TreePath> methodPaths = new ArrayList<>();
    new TreePathScanner<Void, Void>() {
      @Override
      public Void visitMethod(MethodTree node, Void unused) {
        methodPaths.add(getCurrentPath());
        return super.visitMethod(node, null);
      }
    }.scan(cu, null);
    assertIndexedMatchesBaseline(rules, target, methodPaths);
  }

  @Test
  public void uMatches_skipsMatcherWhenParameterTypeIncompatible() {
    CountingMatcher.INVOCATIONS.set(0);
    RefasterRule<?, ?> rule =
        RefasterRule.create(
            "BigIntEq",
            ImmutableList.of(
                ExpressionTemplate.create(
                    ImmutableMap.of(
                        "a", UClassType.create("java.math.BigInteger"),
                        "b", UClassType.create("java.math.BigInteger")),
                    UBinary.create(
                        Kind.EQUAL_TO,
                        UMatches.create(CountingMatcher.class, true, UFreeIdent.create("a")),
                        UMatches.create(CountingMatcher.class, true, UFreeIdent.create("b"))),
                    UPrimitiveType.BOOLEAN)),
            ImmutableList.of());

    JavaFileObject target =
        forSourceLines(
            "MatcherTarget.java",
            """
            import java.math.BigInteger;
            public class MatcherTarget {
              boolean f(int x, int y, String s, String t, BigInteger b1, BigInteger b2) {
                return (x == y) || (s == t) || (b1 == b2);
              }
            }
            """);

    JCCompilationUnit cu = compileSingleUnit(target);
    RefasterRuleIndex<Integer> index =
        RefasterRuleIndex.create(ImmutableList.of(new RuleEntry<>(0, rule)));

    List<Description> descriptions = new ArrayList<>();
    index.apply(new TreePath(cu), context, (k, d) -> descriptions.add(d));

    assertThat(descriptions).hasSize(1);
    // CountingMatcher must only run on (b1, b2), NOT on (x, y) or (s, t)!
    assertThat(CountingMatcher.INVOCATIONS.get()).isEqualTo(2);
  }

  @Test
  public void errorHandler_isolatesCrashingRuleFromOtherRules() {
    RefasterRule<?, ?> crashingRule =
        RefasterRule.create(
            "CrashingRule",
            ImmutableList.of(
                ExpressionTemplate.create(
                    ImmutableMap.of("s", UClassType.create("java.lang.String")),
                    UMatches.create(ThrowingMatcher.class, true, UFreeIdent.create("s")),
                    UClassType.create("java.lang.String"))),
            ImmutableList.of());
    ImmutableList<RefasterRule<?, ?>> goodRules =
        extractRules(
            forSourceLines(
                "GoodRule.java",
                """
                import com.google.errorprone.refaster.annotation.AfterTemplate;
                import com.google.errorprone.refaster.annotation.BeforeTemplate;
                public class GoodRule {
                  @BeforeTemplate boolean before(String s) { return s.equals(""); }
                  @AfterTemplate boolean after(String s) { return s.isEmpty(); }
                }
                """));

    JavaFileObject target =
        forSourceLines(
            "ErrorIsolationTarget.java",
            """
            public class ErrorIsolationTarget {
              boolean f(String s) { return s.equals(""); }
            }
            """);

    JCCompilationUnit cu = compileSingleUnit(target);
    RefasterRuleIndex<String> index =
        RefasterRuleIndex.create(
            ImmutableList.of(
                new RuleEntry<>("crash", crashingRule),
                new RuleEntry<>("good", goodRules.getFirst())));

    List<String> hitKeys = new ArrayList<>();
    List<String> failedKeys = new ArrayList<>();
    index.applyAll(
        cu,
        ImmutableList.of(new TreePath(cu)),
        context,
        k -> true,
        (k, d) -> hitKeys.add(k),
        (k, t) -> failedKeys.add(k));

    assertThat(failedKeys).contains("crash");
    assertThat(hitKeys).containsExactly("good");
  }

  @Test
  public void entryFilter_filtersRulesByKey() {
    ImmutableList<RefasterRule<?, ?>> rules =
        extractRules(
            forSourceLines(
                "FilterTemplates.java",
                """
                import com.google.errorprone.refaster.annotation.AfterTemplate;
                import com.google.errorprone.refaster.annotation.BeforeTemplate;
                public class FilterTemplates {
                  static class RuleA {
                    @BeforeTemplate boolean before(String s) { return s.equals(""); }
                    @AfterTemplate boolean after(String s) { return s.isEmpty(); }
                  }
                  static class RuleB {
                    @BeforeTemplate int before(String s) { return s.length(); }
                    @AfterTemplate int after(String s) { return 0; }
                  }
                }
                """));

    JavaFileObject target =
        forSourceLines(
            "FilterTarget.java",
            """
            public class FilterTarget {
              boolean f(String s) { return s.equals("") && s.length() == 0; }
            }
            """);

    JCCompilationUnit cu = compileSingleUnit(target);
    RefasterRuleIndex<String> index =
        RefasterRuleIndex.create(
            ImmutableList.of(
                new RuleEntry<>("A", rules.get(0)), new RuleEntry<>("B", rules.get(1))));

    List<String> hitKeys = new ArrayList<>();
    index.applyAll(
        cu,
        ImmutableList.of(new TreePath(cu)),
        context,
        k -> k.equals("B"),
        (k, d) -> hitKeys.add(k),
        null);

    assertThat(hitKeys).containsExactly("B");
  }

  @Test
  public void typeVariablesWithSameNameAndAnonymousClass_matchesEquivalently() {
    ImmutableList<RefasterRule<?, ?>> rules =
        extractRules(
            forSourceLines(
                "TypeVarAnonTemplates.java",
                """
                import com.google.errorprone.refaster.annotation.AfterTemplate;
                import com.google.errorprone.refaster.annotation.BeforeTemplate;
                import java.util.Collection;
                public class TypeVarAnonTemplates {
                  static class CharSeqEmpty {
                    @BeforeTemplate boolean before(CharSequence cs) { return cs.length() == 0; }
                    @AfterTemplate boolean after(CharSequence cs) { return cs.isEmpty(); }
                  }
                  static class CollectionEmpty {
                    @BeforeTemplate boolean before(Collection<?> c) { return c.size() == 0; }
                    @AfterTemplate boolean after(Collection<?> c) { return c.isEmpty(); }
                  }
                }
                """));

    JavaFileObject target =
        forSourceLines(
            "TypeVarAnonTarget.java",
            """
            import java.util.ArrayList;
            import java.util.List;
            public class TypeVarAnonTarget {
              <T extends List<String>> int firstT(T a) { return a.hashCode(); }
              <T extends CharSequence> boolean secondT(T b) { return b.length() == 0; }
              boolean anon() {
                var x = new ArrayList<String>() {};
                return x.size() == 0;
              }
            }
            """);

    assertIndexedMatchesBaseline(rules, target, null);
  }

  @Test
  public void blockTemplatesWithUIfAndEarlyReturn_matchesEquivalently() {
    ImmutableList<RefasterRule<?, ?>> rules =
        extractRules(
            forSourceLines(
                "IfBlockTemplates.java",
                """
                import com.google.errorprone.refaster.annotation.AfterTemplate;
                import com.google.errorprone.refaster.annotation.BeforeTemplate;
                public class IfBlockTemplates {
                  static class IfElseReturn {
                    @BeforeTemplate
                    boolean before(boolean cond) {
                      if (cond) {
                        return true;
                      } else {
                        return false;
                      }
                    }
                    @AfterTemplate
                    boolean after(boolean cond) {
                      return cond;
                    }
                  }
                  static class VarThenIf {
                    @BeforeTemplate
                    int before(boolean cond, int a, int b) {
                      int x = a;
                      if (cond) {
                        x = b;
                      }
                      return x;
                    }
                    @AfterTemplate
                    int after(boolean cond, int a, int b) {
                      return cond ? b : a;
                    }
                  }
                }
                """));

    JavaFileObject target =
        forSourceLines(
            "IfBlockTarget.java",
            """
            public class IfBlockTarget {
              boolean earlyReturn(boolean c) {
                if (c) {
                  return true;
                }
                return false;
              }
              int varThenIf(boolean c, int a, int b) {
                int r = a;
                if (c) {
                  r = b;
                }
                return r;
              }
              int noIf(int a) {
                int r = a + 1;
                return r;
              }
            }
            """);

    assertIndexedMatchesBaseline(rules, target, null);
  }

  @Test
  public void matchesAndOfKindWrappingAnyOfAndFreeIdent_matchesEquivalently() {
    UExpression stringIsEmpty =
        UMethodInvocation.create(
            UMemberSelect.create(
                UFreeIdent.create("s"), "isEmpty", UMethodType.create(UPrimitiveType.BOOLEAN)));
    RefasterRule<?, ?> matchesAnyOfRule =
        RefasterRule.create(
            "MatchesAnyOf",
            ImmutableList.of(
                ExpressionTemplate.create(
                    ImmutableMap.of(
                        "s", UClassType.create("java.lang.String"), "b", UPrimitiveType.BOOLEAN),
                    UMatches.create(
                        CountingMatcher.class,
                        true,
                        UAnyOf.create(stringIsEmpty, UFreeIdent.create("b"))),
                    UPrimitiveType.BOOLEAN)),
            ImmutableList.of());

    UExpression stringLength =
        UMethodInvocation.create(
            UMemberSelect.create(
                UFreeIdent.create("s"), "length", UMethodType.create(UPrimitiveType.INT)));
    RefasterRule<?, ?> ofKindAnyOfRule =
        RefasterRule.create(
            "OfKindAnyOf",
            ImmutableList.of(
                ExpressionTemplate.create(
                    ImmutableMap.of(
                        "s", UClassType.create("java.lang.String"), "n", UPrimitiveType.INT),
                    UOfKind.create(
                        UAnyOf.create(stringLength, UFreeIdent.create("n")),
                        ImmutableSet.of(Kind.IDENTIFIER, Kind.METHOD_INVOCATION)),
                    UPrimitiveType.INT)),
            ImmutableList.of());

    // Target has neither java.lang.String nor isEmpty()/length(); both wrapped UAnyOf rules must
    // still match the primitive branch without being falsely pruned.
    JavaFileObject primitiveOnlyTarget =
        forSourceLines(
            "PrimitiveOnlyTarget.java",
            """
            public class PrimitiveOnlyTarget {
              boolean check(boolean flag) { return flag; }
              int count(int value) { return value; }
            }
            """);

    assertIndexedMatchesBaseline(
        ImmutableList.of(matchesAnyOfRule, ofKindAnyOfRule), primitiveOnlyTarget, null);

    // Also verify @OfKind wrapping a UFreeIdent receiver in a compiled template.
    ImmutableList<RefasterRule<?, ?>> ofKindReceiverRules =
        extractRules(
            forSourceLines(
                "OfKindReceiverTemplate.java",
                """
                import com.google.errorprone.refaster.annotation.AfterTemplate;
                import com.google.errorprone.refaster.annotation.BeforeTemplate;
                import com.google.errorprone.refaster.annotation.OfKind;
                import com.sun.source.tree.Tree.Kind;
                public class OfKindReceiverTemplate {
                  @BeforeTemplate
                  boolean before(@OfKind(Kind.IDENTIFIER) String s) {
                    return s.length() == 0;
                  }
                  @AfterTemplate
                  boolean after(String s) {
                    return s.isEmpty();
                  }
                }
                """));
    JavaFileObject receiverTarget =
        forSourceLines(
            "OfKindReceiverTarget.java",
            """
            public class OfKindReceiverTarget {
              boolean f(String s) { return s.length() == 0 && s.trim().length() == 0; }
            }
            """);

    assertIndexedMatchesBaseline(ofKindReceiverRules, receiverTarget, null);
  }

  @Test
  public void arrayTypesSatisfyingSerializableAndCloneable_matchesEquivalently() {
    ImmutableList<RefasterRule<?, ?>> rules =
        extractRules(
            forSourceLines(
                "ArraySupertypesTemplates.java",
                """
                import com.google.errorprone.refaster.annotation.AfterTemplate;
                import com.google.errorprone.refaster.annotation.BeforeTemplate;
                import java.io.Serializable;
                import java.util.Objects;
                public class ArraySupertypesTemplates {
                  static class SerializableNonNull {
                    @BeforeTemplate
                    Serializable before(Serializable s) {
                      return Objects.requireNonNull(s);
                    }
                    @AfterTemplate
                    Serializable after(Serializable s) {
                      return s;
                    }
                  }
                  static class CloneableNonNull {
                    @BeforeTemplate
                    Cloneable before(Cloneable c) {
                      return Objects.requireNonNull(c);
                    }
                    @AfterTemplate
                    Cloneable after(Cloneable c) {
                      return c;
                    }
                  }
                }
                """));

    // No null literals and no explicit Serializable/Cloneable mentions; only int[] satisfies both.
    JavaFileObject target =
        forSourceLines(
            "ArraySupertypesTarget.java",
            """
            import java.util.Objects;
            public class ArraySupertypesTarget {
              int[] check(int[] values) {
                return Objects.requireNonNull(values);
              }
            }
            """);

    assertIndexedMatchesBaseline(rules, target, null);
  }
}
