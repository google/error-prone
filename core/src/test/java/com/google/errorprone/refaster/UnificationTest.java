/*
 * Copyright 2013 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.refaster;

import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.BoundKind;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.TreeScanner;
import java.lang.annotation.Annotation;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Test {@link UTree#unify} against real compiled ASTs.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@RunWith(JUnit4.class)
public class UnificationTest extends CompilerBasedTest {

  public void expectMatches(final Template<?> template, Match... expected) {
    final Set<Match> expectedMatches = Sets.newHashSet(expected);
    TreeScanner matchScanner =
        new TreeScanner() {
          @Override
          public void scan(JCTree tree) {
            if (tree == null) {
              return;
            }
            for (TemplateMatch templateMatch : template.match(tree, context)) {
              Match match = Match.create(templateMatch);
              if (!expectedMatches.remove(match)) {
                fail(String.format("Unexpected match against template %s:%n%s", template, match));
              }
            }
            super.scan(tree);
          }
        };
    for (JCCompilationUnit unit : compilationUnits) {
      matchScanner.scan(unit);
    }
    for (Match missingMatch : expectedMatches) {
      fail(
          String.format(
              "Expected match against template %s not found: %s", template, missingMatch));
    }
  }

  @Test
  public void binary() {
    // template: (a + b) / 2
    ExpressionTemplate template =
        ExpressionTemplate.create(
            ImmutableMap.of(
                "a", UPrimitiveType.INT,
                "b", UPrimitiveType.INT),
            UBinary.create(
                Kind.DIVIDE,
                UParens.create(
                    UBinary.create(Kind.PLUS, UFreeIdent.create("a"), UFreeIdent.create("b"))),
                ULiteral.intLit(2)),
            UPrimitiveType.INT);

    compile(
        "import java.util.Random;",
        "class BinaryExample {",
        "  public void example(int x, int y) {",
        // positive examples
        "    System.out.println((3 + 5) / 2);",
        "    System.out.println((x + y) / 2 + 20);",
        "    System.err.println((y + new Random().nextInt()) / 2);",
        // negative examples
        "    System.out.println((x - y) / 2);",
        "    System.out.println((x * y) / 2);",
        "    System.out.println((x + y) / 3);",
        "    System.out.println((x + 5L) / 2);",
        "  }",
        "}");
    expectMatches(
        template,
        Match.create(ImmutableMap.of("a", "3", "b", "5")),
        Match.create(ImmutableMap.of("a", "x", "b", "y")),
        Match.create(ImmutableMap.of("a", "y", "b", "new Random().nextInt()")));
  }

  @Test
  public void compoundAssignment() {
    // template: String += int
    ExpressionTemplate template =
        ExpressionTemplate.create(
            ImmutableMap.of("str", UClassType.create("java.lang.String"), "n", UPrimitiveType.INT),
            UAssignOp.create(
                UFreeIdent.create("str"), Kind.PLUS_ASSIGNMENT, UFreeIdent.create("n")),
            UClassType.create("java.lang.String"));
    compile(
        "class CompoundAssignmentExample {",
        "  public void example() {",
        "    String foo = \"\";",
        "    foo += 5;",
        "    foo += \"bar\";",
        "    foo += 10;",
        "  }",
        "}");
    expectMatches(
        template,
        Match.create(ImmutableMap.of("str", "foo", "n", "5")),
        Match.create(ImmutableMap.of("str", "foo", "n", "10")));
  }

  @Test
  public void methodInvocation() {
    // template : md.digest(str.getBytes())
    UType byteArrayType = UArrayType.create(UPrimitiveType.BYTE);
    ExpressionTemplate template =
        ExpressionTemplate.create(
            ImmutableMap.of(
                "md", UClassType.create("java.security.MessageDigest"),
                "str", UClassType.create("java.lang.String")),
            UMethodInvocation.create(
                UMemberSelect.create(
                    UFreeIdent.create("md"),
                    "digest",
                    UMethodType.create(byteArrayType, byteArrayType)),
                UMethodInvocation.create(
                    UMemberSelect.create(
                        UFreeIdent.create("str"), "getBytes", UMethodType.create(byteArrayType)))),
            byteArrayType);

    compile(
        "import java.security.MessageDigest;",
        "import java.security.NoSuchAlgorithmException;",
        "import java.nio.charset.Charset;",
        "class MethodInvocationExample {",
        "  public void example(MessageDigest digest, String string)",
        "      throws NoSuchAlgorithmException {",
        // positive examples
        "    MessageDigest.getInstance(\"MD5\").digest(\"foo\".getBytes());",
        "    digest.digest(\"foo\".getBytes());",
        "    MessageDigest.getInstance(\"SHA1\").digest(string.getBytes());",
        "    digest.digest((string + 90).getBytes());",
        // negative examples
        "    System.out.println(\"foo\".getBytes());",
        "  }",
        "}");

    expectMatches(
        template,
        Match.create(ImmutableMap.of("md", "MessageDigest.getInstance(\"MD5\")", "str", "\"foo\"")),
        Match.create(ImmutableMap.of("md", "digest", "str", "\"foo\"")),
        Match.create(ImmutableMap.of("md", "MessageDigest.getInstance(\"SHA1\")", "str", "string")),
        Match.create(ImmutableMap.of("md", "digest", "str", "(string + 90)")));
  }

  @Test
  public void staticMethodInvocation() {
    // Template: BigInteger.valueOf(int)
    ExpressionTemplate template =
        ExpressionTemplate.create(
            ImmutableMap.of("x", UPrimitiveType.INT),
            UMethodInvocation.create(
                UStaticIdent.create(
                    "java.math.BigInteger",
                    "valueOf",
                    UMethodType.create(
                        UClassType.create("java.math.BigInteger"), UPrimitiveType.LONG)),
                UFreeIdent.create("x")),
            UClassType.create("java.math.BigInteger"));

    compile(
        "import java.math.BigInteger;",
        "class Foo {",
        "  public void example(int x) {",
        // positive examples
        "    BigInteger.valueOf(32);",
        "    BigInteger.valueOf(x * 15);",
        "    BigInteger.valueOf(Integer.parseInt(\"3\"));",
        // negative examples
        "    BigInteger.valueOf(32L);",
        "    super.equals(32);",
        "  }",
        "}");

    expectMatches(
        template,
        Match.create(ImmutableMap.of("x", "32")),
        Match.create(ImmutableMap.of("x", "x * 15")),
        Match.create(ImmutableMap.of("x", "Integer.parseInt(\"3\")")));
  }

  @Test
  public void multipleMatch() {
    ExpressionTemplate template =
        ExpressionTemplate.create(
            ImmutableMap.of("x", UPrimitiveType.INT),
            UBinary.create(Kind.MINUS, UFreeIdent.create("x"), UFreeIdent.create("x")),
            UPrimitiveType.INT);

    compile(
        "import java.math.BigInteger;",
        "class MultipleMatchExample {",
        "  public void example(int n) {",
        // positive examples
        "    System.out.println(3 - 3);",
        "    BigInteger.valueOf((n * 2) - (n * 2));",
        // negative examples
        "    System.err.println(3 - 3L);",
        "    System.err.println((n * 2) - n * 2);",
        "  }",
        "}");

    expectMatches(
        template,
        Match.create(ImmutableMap.of("x", "3")),
        Match.create(ImmutableMap.of("x", "(n * 2)")));
  }

  @Test
  public void returnTypeMatters() {
    /* Template:
     * <E> List<E> template(List<E> list) {
     *   return Collections.unmodifiableList(list);
     * }
     *
     * Note that Collections.unmodifiableList takes a List<? extends T>,
     * but we're restricting to the case where the return type is the same.
     */
    UTypeVar tVar = UTypeVar.create("T");
    UTypeVar eVar = UTypeVar.create("E");
    ExpressionTemplate template =
        ExpressionTemplate.create(
            ImmutableClassToInstanceMap.<Annotation>builder().build(),
            ImmutableList.of(eVar),
            ImmutableMap.of("list", UClassType.create("java.util.List", eVar)),
            UMethodInvocation.create(
                UStaticIdent.create(
                    "java.util.Collections",
                    "unmodifiableList",
                    UForAll.create(
                        ImmutableList.of(tVar),
                        UMethodType.create(
                            UClassType.create("java.util.List", tVar),
                            UClassType.create(
                                "java.util.List", UWildcardType.create(BoundKind.EXTENDS, tVar))))),
                UFreeIdent.create("list")),
            UClassType.create("java.util.List", eVar));
    compile(
        "import java.util.ArrayList;",
        "import java.util.Collections;",
        "import java.util.List;",
        "class ReturnTypeMattersExample {",
        "  public void example() {",
        // positive examples
        "    Collections.unmodifiableList(new ArrayList<String>());",
        "    List<Integer> ints = Collections.unmodifiableList(Collections.singletonList(1));",
        // negative examples
        "    List<CharSequence> seqs = Collections.<CharSequence>unmodifiableList(",
        "        new ArrayList<String>());",
        "  }",
        "}");
    expectMatches(
        template,
        Match.create(
            ImmutableMap.of(
                "list", "new ArrayList<String>()",
                "E", "java.lang.String")),
        Match.create(
            ImmutableMap.of(
                "list", "Collections.singletonList(1)",
                "E", "java.lang.Integer")));
  }

  @Test
  public void recursiveType() {
    /*
     * Template:
     * <E extends Enum<E>> String example(E e) {
     *  return e.name();
     * }
     */
    UTypeVar eTypeVar = UTypeVar.create("E");
    eTypeVar.setUpperBound(UClassType.create("java.lang.Enum", eTypeVar));
    ExpressionTemplate template =
        ExpressionTemplate.create(
            ImmutableClassToInstanceMap.<Annotation>builder().build(),
            ImmutableList.of(eTypeVar),
            ImmutableMap.of("value", eTypeVar),
            UMethodInvocation.create(
                UMemberSelect.create(
                    UFreeIdent.create("value"),
                    "name",
                    UMethodType.create(UClassType.create("java.lang.String")))),
            UClassType.create("java.lang.String"));
    compile(
        "import java.math.RoundingMode;",
        "class RecursiveTypeExample {",
        "  public void example() {",
        "    System.out.println(RoundingMode.FLOOR.name());",
        "  }",
        "}");
    expectMatches(
        template,
        Match.create(
            ImmutableMap.of(
                "value", "RoundingMode.FLOOR",
                "E", "java.math.RoundingMode")));
  }

  @Test
  public void blockTemplate() {
    BlockTemplate blockTemplate =
        BlockTemplate.create(
            // <E>
            ImmutableList.of(UTypeVar.create("E")),
            ImmutableMap.of(
                // Collection<E> collection
                "collection", UClassType.create("java.util.Collection", UTypeVar.create("E")),
                // Comparator<? super E> comparator
                "comparator",
                    UClassType.create(
                        "java.util.Comparator",
                        UWildcardType.create(BoundKind.SUPER, UTypeVar.create("E")))),
            UVariableDecl.create(
                "list",
                UTypeApply.create("java.util.List", UTypeVarIdent.create("E")),
                UNewClass.create(
                    UTypeApply.create("java.util.ArrayList", UTypeVarIdent.create("E")),
                    UFreeIdent.create("collection"))),
            UExpressionStatement.create(
                UMethodInvocation.create(
                    UStaticIdent.create(
                        "java.util.Collections",
                        "sort",
                        UForAll.create(
                            ImmutableList.of(UTypeVar.create("T")),
                            UMethodType.create(
                                UPrimitiveType.VOID,
                                UClassType.create("java.util.List", UTypeVar.create("T")),
                                UClassType.create(
                                    "java.util.Comparator",
                                    UWildcardType.create(BoundKind.SUPER, UTypeVar.create("T")))))),
                    ULocalVarIdent.create("list"),
                    UFreeIdent.create("comparator"))));
    compile(
        "import java.util.ArrayList;",
        "import java.util.Comparator;",
        "import java.util.Collections;",
        "import java.util.List;",
        "class BlockTemplateExample {",
        "  public void example(Comparator<String> cmp) {",
        "    List<String> foo = new ArrayList<String>();",
        "    foo.add(\"bar\");",
        "    List<String> sorted = new ArrayList<String>(foo);",
        "    Collections.sort(sorted, cmp);",
        "  }",
        "}");
    expectMatches(
        blockTemplate,
        Match.create(
            ImmutableMap.of(
                "collection", "foo",
                "comparator", "cmp",
                "E", "java.lang.String",
                "list", "sorted")));
  }

  @Test
  public void ifBlockTemplate() {
    /*
     * Template:
     *
     * if (cond) {
     *   x = y;
     * } else {
     *   x = z;
     * }
     */
    BlockTemplate blockTemplate =
        BlockTemplate.create(
            ImmutableList.of(UTypeVar.create("T")),
            ImmutableMap.of(
                "cond", UPrimitiveType.BOOLEAN,
                "x", UTypeVar.create("T"),
                "y", UTypeVar.create("T"),
                "z", UTypeVar.create("T")),
            UIf.create(
                UFreeIdent.create("cond"),
                UBlock.create(
                    UExpressionStatement.create(
                        UAssign.create(UFreeIdent.create("x"), UFreeIdent.create("y")))),
                UBlock.create(
                    UExpressionStatement.create(
                        UAssign.create(UFreeIdent.create("x"), UFreeIdent.create("z"))))));

    compile(
        "class IfBlockExample {",
        "  public void example(String x) {",
        "    if (Math.random() > 0.5) {",
        "      x = \"foo\";",
        "    } else {",
        "      x = \"bar\";",
        "    }",
        "  }",
        "}");
    expectMatches(
        blockTemplate,
        Match.create(
            ImmutableMap.of(
                "cond", "(Math.random() > 0.5)",
                "x", "x",
                "y", "\"foo\"",
                "z", "\"bar\"",
                "T", "java.lang.String")));
  }

  @Test
  public void newArray() {
    // Template: new String[] {str}
    ExpressionTemplate template =
        ExpressionTemplate.create(
            ImmutableMap.of("str", UClassType.create("java.lang.String")),
            UNewArray.create(
                UClassIdent.create("java.lang.String"),
                ImmutableList.<UExpression>of(),
                ImmutableList.of(UFreeIdent.create("str"))),
            UArrayType.create(UClassType.create("java.lang.String")));
    compile(
        "class Foo {",
        "  public void example() {",
        // positive examples
        "    String[] array1 = new String[] {\"foo\"};",
        // negative examples
        "    String[] array2 = {\"bar\"};",
        "  }",
        "}");
    expectMatches(template, Match.create(ImmutableMap.of("str", "\"foo\"")));
  }
}
