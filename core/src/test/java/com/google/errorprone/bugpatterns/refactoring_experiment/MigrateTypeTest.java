package com.google.errorprone.bugpatterns.refactoring_experiment;

import static com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.bugpatterns.CanonicalDuration;
import com.google.errorprone.bugpatterns.refactoringexperiment.refactor.MigrateType;

/**
 * Created by ameya on 3/14/18.
 */
//@RunWith(JUnit4.class)
public class MigrateTypeTest {

    public void testFuncIntInt_IntUnary_positive() throws Exception {
        BugCheckerRefactoringTestHelper.newInstance(new MigrateType(), getClass())
                .addInputLines(
                        "TestGoal1.java",
                        "import java.util.function.Function;",
                        "class TestGoal1 {",
                        "public int test(Function<Integer,Integer> f){",
                        "return f.apply(5);",
                        "}",
                        "public int boo5(){",
                        "return test(x->x + 5);",
                        "}",
                        "public int boo6(){",
                        "return test(x->x + 6);",
                        "}",
                        "}")
                .addOutputLines(
                        "TestGoal1.java",
                        "import java.util.function.Function;",
                        "import java.util.function.IntUnaryOperator;",
                        "class TestGoal1 {",
                        "public int test(IntUnaryOperator f){",
                        "return f.applyAsInt(5);",
                        "}",
                        "public int boo5(){",
                        "return test(x->x + 5);",
                        "}",
                        "public int boo6(){",
                        "return test(x->x + 6);",
                        "}",
                        "}")
                .doTest(TEXT_MATCH);
    }

    public void testFuncBooleanInt_ToInt_positive() throws Exception {
        BugCheckerRefactoringTestHelper.newInstance(new MigrateType(), getClass())
                .addInputLines(
                        "TestGoal1.java",
                        "import java.util.function.Function;",
                        "class TestGoal1 {",
                        "public int test(Function<Boolean,Integer> f){",
                        "return f.apply(false);",
                        "}",
                        "public int boo5(){",
                        "return test(x->1);",
                        "}",
                        "public int boo6(){",
                        "return test(x->2);",
                        "}",
                        "}")
                .addOutputLines(
                        "TestGoal1.java",
                        "import java.util.function.Function;",
                        "import java.util.function.ToIntFunction;",
                        "class TestGoal1 {",
                        "public int test(ToIntFunction<Boolean> f){",
                        "return f.applyAsInt(false);",
                        "}",
                        "public int boo5(){",
                        "return test(x->1);",
                        "}",
                        "public int boo6(){",
                        "return test(x->2);",
                        "}",
                        "}")
                .doTest(TEXT_MATCH);
    }

    public void testFuncIntString_IntFunc_positive() throws Exception {
        BugCheckerRefactoringTestHelper.newInstance(new MigrateType(), getClass())
                .addInputLines(
                        "TestGoal1.java",
                        "import java.util.function.Function;",
                        "class TestGoal1 {",
                        "public String test(Function<Integer,String> f){",
                        "return f.apply(5);",
                        "}",
                        "public int boo5(){",
                        "return test(x->\"foo\");",
                        "}",
                        "public int boo6(){",
                        "return test(x->\"boo\");",
                        "}",
                        "}")
                .addOutputLines(
                        "TestGoal1.java",
                        "import java.util.function.Function;",
                        "import java.util.function.IntFunction;",
                        "class TestGoal1 {",
                        "public int test(IntFunction<String> f){",
                        "return f.apply(5);",
                        "}",
                        "public int boo5(){",
                        "return test(x->\"foo\");",
                        "}",
                        "public int boo6(){",
                        "return test(x->\"boo\");",
                        "}",
                        "}")
                .doTest(TEXT_MATCH);
    }

    public void testFuncIntInt_IntUnary_negative() throws Exception {
        BugCheckerRefactoringTestHelper.newInstance(new CanonicalDuration(), getClass())
                .addInputLines(
                        "TestGoal1.java",
                        "import java.util.function.Function;",
                        "class TestGoal1 {",
                        "Function<Integer,Integer> f;",
                        "public int test(Function<Integer,Integer> f){",
                        "this.f = f;",
                        "return f.apply(5);",
                        "}",
                        "public int boo5(){",
                        "return test(x->x + 5);",
                        "}",
                        "public int boo6(){",
                        "return test(x->x + 6);",
                        "}",
                        "}")
                .addOutputLines(
                        "TestGoal1.java",
                        "import java.util.function.Function;",
                        "class TestGoal1 {",
                        "Function<Integer,Integer> f;",
                        "public int test(Function<Integer,Integer> f){",
                        "this.f = f;",
                        "return f.apply(5);",
                        "}",
                        "public int boo5(){",
                        "return test(x->x + 5);",
                        "}",
                        "public int boo6(){",
                        "return test(x->x + 6);",
                        "}",
                        "}")
                .doTest(TEXT_MATCH);
    }

    public void testFuncIntInt_IntUnary_Hierarchy_positive() throws Exception {
        BugCheckerRefactoringTestHelper.newInstance(new MigrateType(), getClass())
                .addInputLines(
                        "TestInterface.java",
                        "import java.util.function.Function;",
                        "interface TestGoal1 {",
                        "public int test(Function<Integer,Integer> f);",
                        "}",
                        "TestGoal1.java",
                        "import java.util.function.Function;",
                        "class TestGoal1 implements {",
                        "@Override",
                        "public int test(Function<Integer,Integer> f){",
                        "return f.apply(5);",
                        "}",
                        "public int boo5(){",
                        "return test(x->x + 5);",
                        "}",
                        "public int boo6(){",
                        "return test(x->x + 6);",
                        "}",
                        "}")
                .addOutputLines(
                        "TestInterface.java",
                        "import java.util.function.Function;",
                        "import java.util.function.IntUnaryOperator;",
                        "interface TestGoal1 {",
                        "public int test(IntUnaryOperator f);",
                        "}",
                        "TestGoal1.java",
                        "import java.util.function.Function;",
                        "import java.util.function.IntUnaryOperator;",
                        "class TestGoal1 implements {",
                        "@Override",
                        "public int test(IntUnaryOperator f){",
                        "return f.applyAsInt(5);",
                        "}",
                        "public int boo5(){",
                        "return test(x->x + 5);",
                        "}",
                        "public int boo6(){",
                        "return test(x->x + 6);",
                        "}",
                        "}")
                .doTest(TEXT_MATCH);
    }

    public void testFuncIntInt_IntUnary_Hierarchy_negative() throws Exception {
        BugCheckerRefactoringTestHelper.newInstance(new MigrateType(), getClass())
                .addInputLines(
                        "TestInterface.java",
                        "import java.util.function.Function;",
                        "interface TestGoal1 {",
                        "public int test(Function<T,U> f);",
                        "}",
                        "TestGoal1.java",
                        "import java.util.function.Function;",
                        "class TestGoal1 implements {",
                        "@Override",
                        "public int test(Function<Integer,Integer> f){",
                        "return f.apply(5);",
                        "}",
                        "public int boo5(){",
                        "return test(x->x + 5);",
                        "}",
                        "public int boo6(){",
                        "return test(x->x + 6);",
                        "}",
                        "}")
                .addOutputLines(
                        "TestInterface.java",
                        "import java.util.function.Function;",
                        "interface TestGoal1 {",
                        "public int test(Function<T,U> f);",
                        "}",
                        "TestGoal1.java",
                        "import java.util.function.Function;",
                        "class TestGoal1 implements {",
                        "@Override",
                        "public int test(Function<Integer,Integer> f){",
                        "return f.apply(5);",
                        "}",
                        "public int boo5(){",
                        "return test(x->x + 5);",
                        "}",
                        "public int boo6(){",
                        "return test(x->x + 6);",
                        "}",
                        "}")
                .doTest(TEXT_MATCH);
    }

    public void testFuncIntInt_passed_to_generic_negative() throws Exception {
        BugCheckerRefactoringTestHelper.newInstance(new MigrateType(), getClass())
                .addInputLines(
                        "TestExternal.java",
                        "import java.util.function.Function;",
                        "import java.util.Arrays;",
                        "import java.util.List;",
                        "interface TestExternal {",
                        "public int test(Function<Integer,Integer> ext){",
                        "List<Function<Integer,Integer>> list = Arrays.asList(ext);",
                        "return ext.apply(5);",
                        "}",
                        "public int test1(Function<Integer,Integer> ext1){",
                        "return identity(ext1).apply(5);",
                        "}",
                        "public <T> T identity(T t){",
                        "return  t;",
                        "}",
                        "}")
                .addOutputLines(
                        "TestExternal.java",
                        "import java.util.function.Function;",
                        "import java.util.Arrays;",
                        "import java.util.List;",
                        "interface TestExternal {",
                        "public int test(Function<Integer,Integer> ext){",
                        "List<Function<Integer,Integer>> list = Arrays.asList(ext);",
                        "return ext.apply(5);",
                        "}",
                        "public int test1(Function<Integer,Integer> ext1){",
                        "return identity(ext1).apply(5);",
                        "}",
                        "public <T> T identity(T t){",
                        "return  t;",
                        "}",
                        "}")
                .doTest(TEXT_MATCH);
    }
}
