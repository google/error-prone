/*
 * Copyright 2023 The Error Prone Authors.
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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link DoNotUseRuleChain} */
@RunWith(JUnit4.class)
public class DoNotUseRuleChainTest {

  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(DoNotUseRuleChain.class, getClass());

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(DoNotUseRuleChain.class, getClass());

  @Test
  public void negativeEmptyFile() {
    compilationHelper.addSourceLines("EmptyFile.java", "// Empty file").doTest();
  }

  @Test
  public void negativeRuleChainNoInitializer() {
    compilationHelper
        .addSourceLines(
            "RuleChainNoInitializer.java",
            "package rulechainnoinitializer;",
            "import org.junit.Rule;",
            "import org.junit.rules.RuleChain;",
            "import org.junit.rules.TestRule;",
            "import org.junit.runner.Description;",
            "import org.junit.runners.model.Statement;",
            "public class RuleChainNoInitializer {",
            "@Rule",
            "public RuleChain notInitializedRule;",
            "}")
        .doTest();
  }

  @Test
  public void negativeEmptyRuleChain() {
    compilationHelper
        .addSourceLines(
            "EmptyRuleChain.java",
            "package emptyrulechain;",
            "import org.junit.Rule;",
            "import org.junit.rules.RuleChain;",
            "import org.junit.rules.TestRule;",
            "import org.junit.runner.Description;",
            "import org.junit.runners.model.Statement;",
            "public class EmptyRuleChain {",
            "@Rule",
            "public final RuleChain emptyRuleChain = RuleChain.emptyRuleChain();",
            customRuleClasses(),
            "}")
        .doTest();
  }

  @Test
  public void negativeNullRuleChain() {
    compilationHelper
        .addSourceLines(
            "NullRuleChain.java",
            "package nullrulechain;",
            "import org.junit.Rule;",
            "import org.junit.rules.RuleChain;",
            "import org.junit.rules.TestRule;",
            "import org.junit.runner.Description;",
            "import org.junit.runners.model.Statement;",
            "public class NullRuleChain {",
            "@Rule",
            "public final RuleChain nullRuleChain = null;",
            customRuleClasses(),
            "}")
        .doTest();
  }

  @Test
  public void negativeLocalVariable() {
    compilationHelper
        .addSourceLines(
            "LocalVariable.java",
            "package localvariable;",
            "import org.junit.Rule;",
            "import org.junit.rules.RuleChain;",
            "import org.junit.rules.TestRule;",
            "import org.junit.runner.Description;",
            "import org.junit.runners.model.Statement;",
            "public class LocalVariable {",
            "public void doNothing() {",
            "RuleChain ruleChain = RuleChain.outerRule(new Rule2()).around(new Rule3());",
            "}",
            customRuleClasses(),
            "}")
        .doTest();
  }

  @Test
  public void negativeSingleRules() {
    compilationHelper
        .addSourceLines(
            "SingleRule.java",
            "package singlerule;",
            "import org.junit.Rule;",
            "import org.junit.rules.TestRule;",
            "import org.junit.runner.Description;",
            "import org.junit.runners.model.Statement;",
            "public class SingleRule {",
            "@Rule",
            "public final Rule1 ruleOne = new Rule1(null, null);",
            customRuleClasses(),
            "}")
        .doTest();
  }

  @Test
  public void negativeOrderedRules() {
    compilationHelper
        .addSourceLines(
            "OrderedRules.java",
            "package orderedrules;",
            "import org.junit.Rule;",
            "import org.junit.rules.TestRule;",
            "import org.junit.runner.Description;",
            "import org.junit.runners.model.Statement;",
            "public class OrderedRules {",
            "@Rule(order = 0)",
            "public final Rule1 ruleOne = new Rule1(\"unused\", new Rule2());",
            "@Rule(order = 1)",
            "public final Rule2 ruleTwo = new Rule2();",
            "@Rule(order = 2)",
            "public final Rule3 ruleThree = new Rule3();",
            customRuleClasses(),
            "}")
        .doTest();
  }

  @Test
  public void negativeOrderedRulesClassRules() {
    compilationHelper
        .addSourceLines(
            "OrderedRulesClassRules.java",
            "package orderedrulesclassRules;",
            "import org.junit.Rule;",
            "import org.junit.ClassRule;",
            "import org.junit.rules.TestRule;",
            "import org.junit.runner.Description;",
            "import org.junit.runners.model.Statement;",
            "public class OrderedRulesClassRules {",
            "@Rule(order = 0)",
            "public final Rule1 ruleOne = new Rule1(\"unused\", new Rule2());",
            "@Rule(order = 1)",
            "public final Rule2 ruleTwo = new Rule2();",
            "@ClassRule(order = 0)",
            "public static final Rule4 ruleFour = new Rule4();",
            customRuleClasses(),
            "}")
        .doTest();
  }

  @Test
  public void negativeChainedRulesChain() {
    compilationHelper
        .addSourceLines(
            "OrderedRuleChain.java",
            "package orderedrulechain;",
            "import org.junit.Rule;",
            "import org.junit.rules.RuleChain;",
            "import org.junit.rules.TestRule;",
            "import org.junit.runner.Description;",
            "import org.junit.runners.model.Statement;",
            "public class OrderedRuleChain {",
            "@Rule(order = 0)",
            "public final RuleChain orderedRuleChain = RuleChain.outerRule(new Rule3())",
            ".around(RuleChain.outerRule(new Rule2()).around(new Rule3()));",
            customRuleClasses(),
            "}")
        .doTest();
  }

  @Test
  public void negativeMultipleRuleChains() {
    compilationHelper
        .addSourceLines(
            "MultipleRuleChains.java",
            "package multiplerulechains;",
            "import org.junit.Rule;",
            "import org.junit.rules.RuleChain;",
            "import org.junit.rules.TestRule;",
            "import org.junit.runner.Description;",
            "import org.junit.runners.model.Statement;",
            "public class MultipleRuleChains {",
            "public final Rule5<Rule2> varRule52 = new Rule5();",
            "@Rule",
            "public final RuleChain ruleChain = RuleChain.outerRule(",
            "new Rule1(\"unused\", new Rule2()))",
            ".around(new Rule2()).around(new Rule5());",
            "@Rule",
            "public final RuleChain ruleChain2 = RuleChain.outerRule(new Rule3()).around(",
            "varRule52);",
            customRuleClasses(),
            "}")
        .doTest();
  }

  @Test
  public void refactoringOrderedRulesChain() {
    refactoringHelper
        .addInputLines(
            "OrderedRuleChain.java",
            "package orderedrulechain;",
            "import org.junit.Rule;",
            "import org.junit.rules.RuleChain;",
            "import org.junit.rules.TestRule;",
            "import org.junit.runner.Description;",
            "import org.junit.runners.model.Statement;",
            "public class OrderedRuleChain {",
            "@Rule(order = 0)",
            "// BUG: Diagnostic contains: Do not use RuleChain",
            "public final RuleChain orderedRuleChain = RuleChain.outerRule(new Rule3())",
            ".around(new Rule2());",
            customRuleClasses(),
            "}")
        .addOutputLines(
            "OrderedRuleChain.java",
            "package orderedrulechain;",
            "import org.junit.Rule;",
            "import org.junit.rules.TestRule;",
            "import org.junit.runner.Description;",
            "import org.junit.runners.model.Statement;",
            "public class OrderedRuleChain {",
            "@Rule(order = 0)",
            "public final Rule3 testRuleRule3 = new Rule3();",
            "@Rule(order = 1)",
            "public final Rule2 testRuleRule2 = new Rule2();",
            customRuleClasses(),
            "}")
        .doTest();
  }

  @Test
  public void refactoringUnorderedRuleChainWithNewObjects() {
    refactoringHelper
        .addInputLines(
            "UnorderedRuleChainWithNewObjects.java",
            "package orderedrulechainwithnewobjects;",
            "import org.junit.Rule;",
            "import org.junit.rules.RuleChain;",
            "import org.junit.rules.TestRule;",
            "import org.junit.runner.Description;",
            "import org.junit.runners.model.Statement;",
            "public class UnorderedRuleChainWithNewObjects {",
            "@Rule",
            "// BUG: Diagnostic contains: Do not use RuleChain",
            "public final RuleChain ruleChain = RuleChain.outerRule(",
            "new Rule1(\"really big string so that the new falls into another line\"",
            ", new Rule2()))",
            ".around(new Rule2()).around(new Rule5()).around((base, description) ->",
            "new Statement() {",
            "@Override",
            "public void evaluate() throws Throwable {",
            "// Do nothing",
            "}",
            "});",
            customRuleClasses(),
            "}")
        .addOutputLines(
            "UnorderedRuleChainWithNewObjects.java",
            "package orderedrulechainwithnewobjects;",
            "import org.junit.Rule;",
            "import org.junit.rules.TestRule;",
            "import org.junit.runner.Description;",
            "import org.junit.runners.model.Statement;",
            "public class UnorderedRuleChainWithNewObjects {",
            "@Rule(order = 0)",
            "public final Rule1 testRuleRule1 =",
            "new Rule1(\"really big string so that the new falls into another line\"",
            ", new Rule2());",
            "@Rule(order = 1)",
            "public final Rule2 testRuleRule2 = new Rule2();",
            "@Rule(order = 2)",
            "public final Rule5 testRuleRule5 = new Rule5();",
            "@Rule(order = 3)",
            "public final TestRule testRuleTestRule = (base, description) ->",
            "new Statement() {",
            "@Override",
            "public void evaluate() throws Throwable {",
            "// Do nothing",
            "}",
            "};",
            customRuleClasses(),
            "}")
        .doTest();
  }

  @Test
  public void refactoringUnorderedRuleChainWithExistingObjects() {
    refactoringHelper
        .addInputLines(
            "UnorderedRuleChainWithExistingObjects.java",
            "package orderedrulechainwithexistingobjects;",
            "import org.junit.Rule;",
            "import org.junit.rules.RuleChain;",
            "import org.junit.rules.TestRule;",
            "import org.junit.runner.Description;",
            "import org.junit.runners.model.Statement;",
            "public class UnorderedRuleChainWithExistingObjects {",
            "public final Rule1 varRule1 = new Rule1(\"unused\", new Rule2());",
            "public final Rule2 varRule2 = new Rule2();",
            "@Rule",
            "// BUG: Diagnostic contains: Do not use RuleChain",
            "public final RuleChain ruleChain = RuleChain.outerRule(varRule1).around(varRule2);",
            customRuleClasses(),
            "}")
        .addOutputLines(
            "UnorderedRuleChainWithExistingObjects.java",
            "package orderedrulechainwithexistingobjects;",
            "import org.junit.Rule;",
            "import org.junit.rules.TestRule;",
            "import org.junit.runner.Description;",
            "import org.junit.runners.model.Statement;",
            "public class UnorderedRuleChainWithExistingObjects {",
            "public final Rule1 varRule1 = new Rule1(\"unused\", new Rule2());",
            "public final Rule2 varRule2 = new Rule2();",
            "@Rule(order = 0)",
            "public final Rule1 testRuleRule1 = varRule1;",
            "@Rule(order = 1)",
            "public final Rule2 testRuleRule2 = varRule2;",
            customRuleClasses(),
            "}")
        .doTest();
  }

  @Test
  public void refactoringUnorderedRuleChainWithGenericClass() {
    refactoringHelper
        .addInputLines(
            "UnorderedRuleChainWithGenericClass.java",
            "package orderedrulechainwithgenericclass;",
            "import org.junit.Rule;",
            "import org.junit.rules.RuleChain;",
            "import org.junit.rules.TestRule;",
            "import org.junit.runner.Description;",
            "import org.junit.runners.model.Statement;",
            "public class UnorderedRuleChainWithGenericClass {",
            "public final Rule5<Rule2> varRule52 = new Rule5();",
            "public final Rule6<Rule3, Rule2> varRule532 = new Rule6();",
            "@Rule",
            "// BUG: Diagnostic contains: Do not use RuleChain",
            "public final RuleChain ruleChain = RuleChain.outerRule(varRule52).around(varRule532);",
            customRuleClasses(),
            "}")
        .addOutputLines(
            "UnorderedRuleChainWithGenericClass.java",
            "package orderedrulechainwithgenericclass;",
            "import org.junit.Rule;",
            "import org.junit.rules.TestRule;",
            "import org.junit.runner.Description;",
            "import org.junit.runners.model.Statement;",
            "public class UnorderedRuleChainWithGenericClass {",
            "public final Rule5<Rule2> varRule52 = new Rule5();",
            "public final Rule6<Rule3, Rule2> varRule532 = new Rule6();",
            "@Rule(order = 0)",
            "public final Rule5<Rule2> testRuleRule5Rule2 = varRule52;",
            "@Rule(order = 1)",
            "public final Rule6<Rule3, Rule2> testRuleRule6Rule3Rule2 = varRule532;",
            customRuleClasses(),
            "}")
        .doTest();
  }

  @Test
  public void refactoringUnorderedTwoRuleChainWithClassRule() {
    refactoringHelper
        .addInputLines(
            "UnorderedRuleChainWithClassRule.java",
            "package orderedrulechainwithclassrule;",
            "import org.junit.ClassRule;",
            "import org.junit.Rule;",
            "import org.junit.rules.RuleChain;",
            "import org.junit.rules.TestRule;",
            "import org.junit.runner.Description;",
            "import org.junit.runners.model.Statement;",
            "public class UnorderedRuleChainWithClassRule {",
            "@Rule",
            "// BUG: Diagnostic contains: Do not use RuleChain",
            "public final RuleChain ruleChain = RuleChain.outerRule(",
            "new Rule2()).around(new Rule3());",
            "@ClassRule",
            "// BUG: Diagnostic contains: Do not use RuleChain",
            "public static final RuleChain classRuleChain = RuleChain.outerRule(new Rule4());",
            customRuleClasses(),
            "}")
        .addOutputLines(
            "UnorderedRuleChainWithClassRule.java",
            "package orderedrulechainwithclassrule;",
            "import org.junit.ClassRule;",
            "import org.junit.Rule;",
            "import org.junit.rules.TestRule;",
            "import org.junit.runner.Description;",
            "import org.junit.runners.model.Statement;",
            "public class UnorderedRuleChainWithClassRule {",
            "@Rule(order = 0)",
            "public final Rule2 testRuleRule2 = new Rule2();",
            "@Rule(order = 1)",
            "public final Rule3 testRuleRule3 = new Rule3();",
            "@ClassRule(order = 0)",
            "public static final Rule4 testRuleRule4 = new Rule4();",
            customRuleClasses(),
            "}")
        .doTest();
  }

  @Test
  public void refactoringUnorderedRuleChainWithoutTestRuleImport() {
    refactoringHelper
        .addInputLines(
            "UnorderedRuleChainWithoutTestRuleImport.java",
            "package orderedrulechainwithouttestruleimport;",
            "import org.junit.Rule;",
            "import org.junit.rules.RuleChain;",
            "import org.junit.runners.model.Statement;",
            "public class UnorderedRuleChainWithoutTestRuleImport {",
            "@Rule",
            "// BUG: Diagnostic contains: Do not use RuleChain",
            "public final RuleChain ruleChain = RuleChain.outerRule((base, description) ->",
            "new Statement() {",
            "@Override",
            "public void evaluate() throws Throwable {",
            "// Do nothing",
            "}",
            "});",
            "}")
        .addOutputLines(
            "UnorderedRuleChainWithoutTestRuleImport.java",
            "package orderedrulechainwithouttestruleimport;",
            "import org.junit.Rule;",
            "import org.junit.rules.TestRule;",
            "import org.junit.runners.model.Statement;",
            "public class UnorderedRuleChainWithoutTestRuleImport {",
            "@Rule(order = 0)",
            "public final TestRule testRuleTestRule =",
            "(base, description) ->",
            "new Statement() {",
            "@Override",
            "public void evaluate() throws Throwable {",
            "// Do nothing",
            "}",
            "};",
            "}")
        .doTest();
  }

  private static String customRuleClasses() {
    return "private class BaseCustomRule implements TestRule {\n"
        + "@Override\n"
        + "public Statement apply(Statement base, Description description) {\n"
        + "return new Statement() {\n"
        + "@Override\n"
        + "public void evaluate() throws Throwable {\n"
        + "// Do nothing\n"
        + "}\n"
        + "};\n"
        + "}\n"
        + "}\n"
        + "private class Rule1 extends BaseCustomRule {\n"
        + "private Rule1(String unusedString, TestRule unusedRule) {\n"
        + " // Example with parameter\n"
        + "}\n"
        + "}\n"
        + "private class Rule2 extends BaseCustomRule {}\n"
        + "private class Rule3 extends BaseCustomRule {}\n"
        + "private static class Rule4 implements TestRule {\n"
        + "@Override\n"
        + "public Statement apply(Statement base, Description description) {\n"
        + "return new Statement() {\n"
        + "@Override\n"
        + "public void evaluate() throws Throwable {\n"
        + "// Do nothing\n"
        + "}\n"
        + "};\n"
        + "}\n"
        + "}\n"
        + "private class Rule5<T extends TestRule> extends BaseCustomRule {}\n"
        + "private class Rule6<T extends TestRule, V extends TestRule> extends BaseCustomRule {}\n";
  }
}
