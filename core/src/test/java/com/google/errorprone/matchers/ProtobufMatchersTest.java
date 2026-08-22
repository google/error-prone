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

package com.google.errorprone.matchers;

import static com.google.errorprone.matchers.ProtobufMatchers.IS_LITE_PROTO;
import static com.google.errorprone.matchers.ProtobufMatchers.IS_LITE_PROTO_ENUM;
import static com.google.errorprone.matchers.ProtobufMatchers.IS_ONEOF_ENUM;
import static com.google.errorprone.matchers.ProtobufMatchers.PROTO_BUILDER_MUTATOR;
import static com.google.errorprone.matchers.ProtobufMatchers.PROTO_BUILD_METHOD;
import static com.google.errorprone.matchers.ProtobufMatchers.PROTO_GETTER;
import static com.google.errorprone.matchers.ProtobufMatchers.PROTO_NEW_BUILDER_METHOD;
import static com.google.errorprone.matchers.ProtobufMatchers.PROTO_TIME_STATIC_FACTORIES;
import static com.google.errorprone.matchers.ProtobufMatchers.PROTO_TO_BUILDER_METHOD;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link ProtobufMatchers}. */
@RunWith(JUnit4.class)
public final class ProtobufMatchersTest {

  private final CompilationTestHelper methodCompilationHelper =
      CompilationTestHelper.newInstance(TestProtoMethodChecker.class, getClass());

  private final CompilationTestHelper typeCompilationHelper =
      CompilationTestHelper.newInstance(TestProtoTypeChecker.class, getClass());

  @Test
  public void testProtoTypePredicates() {
    typeCompilationHelper
        .addSourceLines(
            "TestTypes.java",
            """
            import com.google.errorprone.bugpatterns.proto.ProtoTest.TestOneOfMessage;
            import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;

            class TestTypes {
              // Full protos are not lite protos
              // BUG: Diagnostic contains: []
              TestProtoMessage fullMsg;

              // BUG: Diagnostic contains: [LITE_PROTO_ENUM]
              com.google.protobuf.Internal.EnumLite liteEnum;

              // BUG: Diagnostic contains: []
              com.google.protobuf.ProtocolMessageEnum fullEnum;

              // BUG: Diagnostic contains: [ONEOF_ENUM]
              TestOneOfMessage.OneOfCase oneOfEnum;

              // BUG: Diagnostic contains: []
              String other;
            }
            """)
        .doTest();
  }

  @Test
  public void testInternalOneOfEnumInterfaceNotMatched() {
    typeCompilationHelper
        .addSourceLines(
            "com/google/protobuf/TestOneOfInterface.java",
            """
            package com.google.protobuf;

            class TestOneOfInterface {
              // BUG: Diagnostic contains: []
              AbstractMessageLite.InternalOneOfEnum oneOfEnumInterface;
            }
            """)
        .doTest();
  }

  @Test
  public void testProtoLifecycleMethodMatchers() {
    methodCompilationHelper
        .addSourceLines(
            "TestLifecycle.java",
            """
            import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;

            class TestLifecycle {
              void test(TestProtoMessage msg, TestProtoMessage.Builder bld) {
                // BUG: Diagnostic contains: [NEW_BUILDER]
                TestProtoMessage.newBuilder();
                // BUG: Diagnostic contains: [TO_BUILDER]
                msg.toBuilder();
                // BUG: Diagnostic contains: [BUILD]
                bld.build();
                // BUG: Diagnostic contains: [BUILD]
                bld.buildPartial();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void testProtoFieldAccessorsAndMutators() {
    methodCompilationHelper
        .addSourceLines(
            "TestAccessors.java",
            """
            import com.google.errorprone.bugpatterns.proto.ProtoTest.TestFieldProtoMessage;
            import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;
            import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessageOrBuilder;

            class TestAccessors {
              void test(
                  TestProtoMessage msg,
                  TestProtoMessage.Builder bld,
                  TestProtoMessageOrBuilder orBld,
                  TestFieldProtoMessage field) {
                // BUG: Diagnostic contains: [GETTER]
                msg.getMessage();
                // BUG: Diagnostic contains: [GETTER]
                orBld.getMessage();
                // BUG: Diagnostic contains: [GETTER]
                msg.getMultiFieldCount();
                // BUG: Diagnostic contains: [GETTER]
                orBld.getMultiFieldCount();
                // BUG: Diagnostic contains: [GETTER]
                msg.getMultiFieldList();
                // BUG: Diagnostic contains: [GETTER]
                orBld.getMultiFieldList();
                // BUG: Diagnostic contains: [BUILDER_MUTATOR]
                bld.setMessage(field);
                // BUG: Diagnostic contains: [BUILDER_MUTATOR]
                bld.clearMessage();
                // BUG: Diagnostic contains: [BUILDER_MUTATOR]
                bld.clear();
                // BUG: Diagnostic contains: [BUILDER_MUTATOR]
                bld.addMultiField(field);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void testProtoTimeFactories() {
    methodCompilationHelper
        .addSourceLines(
            "TestTimeMethods.java",
            """
            import com.google.protobuf.util.Durations;
            import com.google.protobuf.util.Timestamps;

            class TestTimeMethods {
              void test() {
                // BUG: Diagnostic contains: [TIME_FACTORY]
                Durations.fromSeconds(5);
                // BUG: Diagnostic contains: [TIME_FACTORY]
                Timestamps.fromMillis(100);
              }
            }
            """)
        .doTest();
  }

  /** BugChecker for testing method matchers. */
  @BugPattern(
      summary = "Reports matched method categories from ProtobufMatchers",
      severity = SeverityLevel.WARNING)
  public static final class TestProtoMethodChecker extends BugChecker
      implements MethodInvocationTreeMatcher {

    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
      List<String> matched = new ArrayList<>();
      if (PROTO_BUILD_METHOD.matches(tree, state)) {
        matched.add("BUILD");
      }
      if (PROTO_NEW_BUILDER_METHOD.matches(tree, state)) {
        matched.add("NEW_BUILDER");
      }
      if (PROTO_TO_BUILDER_METHOD.matches(tree, state)) {
        matched.add("TO_BUILDER");
      }
      if (PROTO_GETTER.matches(tree, state)) {
        matched.add("GETTER");
      }
      if (PROTO_BUILDER_MUTATOR.matches(tree, state)) {
        matched.add("BUILDER_MUTATOR");
      }
      if (PROTO_TIME_STATIC_FACTORIES.matches(tree, state)) {
        matched.add("TIME_FACTORY");
      }
      if (matched.isEmpty()) {
        return Description.NO_MATCH;
      }
      return buildDescription(tree).setMessage(matched.toString()).build();
    }
  }

  /** BugChecker for testing type predicates. */
  @BugPattern(
      summary = "Reports matched type predicates from ProtobufMatchers",
      severity = SeverityLevel.WARNING)
  public static final class TestProtoTypeChecker extends BugChecker implements VariableTreeMatcher {

    @Override
    public Description matchVariable(VariableTree tree, VisitorState state) {
      Symbol sym = ASTHelpers.getSymbol(tree);
      if (sym == null || sym.type == null) {
        return Description.NO_MATCH;
      }
      List<String> matched = new ArrayList<>();
      if (IS_LITE_PROTO.apply(sym.type, state)) {
        matched.add("LITE_PROTO");
      }
      if (IS_LITE_PROTO_ENUM.apply(sym.type, state)) {
        matched.add("LITE_PROTO_ENUM");
      }
      if (IS_ONEOF_ENUM.apply(sym.type, state)) {
        matched.add("ONEOF_ENUM");
      }
      return buildDescription(tree).setMessage(matched.toString()).build();
    }
  }
}
