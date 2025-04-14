/*
 * Copyright 2025 The Error Prone Authors.
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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
@Ignore("b/130670719")
public final class UnnecessaryCopyTest {
  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(UnnecessaryCopy.class, getClass());

  @Test
  public void positiveViaVariable() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import com.google.common.collect.ImmutableList;
            import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;
            import com.google.errorprone.bugpatterns.proto.ProtoTest.TestFieldProtoMessage;
            import java.util.List;

            class Test {
              List<TestFieldProtoMessage> f(TestProtoMessage m) {
                ImmutableList<TestFieldProtoMessage> l = ImmutableList.copyOf(m.getMultiFieldList());
                return l;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.google.common.collect.ImmutableList;
            import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;
            import com.google.errorprone.bugpatterns.proto.ProtoTest.TestFieldProtoMessage;
            import java.util.List;

            class Test {
              List<TestFieldProtoMessage> f(TestProtoMessage m) {
                List<TestFieldProtoMessage> l = m.getMultiFieldList();
                return l;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void positiveViaVariable_usageIsMethodInvocation() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import com.google.common.collect.ImmutableList;
            import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;
            import com.google.errorprone.bugpatterns.proto.ProtoTest.TestFieldProtoMessage;
            import java.util.List;

            class Test {
              List<TestFieldProtoMessage> f(TestProtoMessage m) {
                ImmutableList<TestFieldProtoMessage> l = ImmutableList.copyOf(m.getMultiFieldList());
                return l.stream().map(x -> x).collect(ImmutableList.toImmutableList());
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.google.common.collect.ImmutableList;
            import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;
            import com.google.errorprone.bugpatterns.proto.ProtoTest.TestFieldProtoMessage;
            import java.util.List;

            class Test {
              List<TestFieldProtoMessage> f(TestProtoMessage m) {
                List<TestFieldProtoMessage> l = m.getMultiFieldList();
                return l.stream().map(x -> x).collect(ImmutableList.toImmutableList());
              }
            }
            """)
        .doTest();
  }

  @Test
  public void positiveViaVariable_map() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import com.google.common.collect.ImmutableMap;
            import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;
            import com.google.errorprone.bugpatterns.proto.ProtoTest.TestFieldProtoMessage;
            import java.util.Map;

            class Test {
              Map<Integer, Integer> f(TestProtoMessage m) {
                ImmutableMap<Integer, Integer> l = ImmutableMap.copyOf(m.getWeightMap());
                return l;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.google.common.collect.ImmutableMap;
            import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;
            import com.google.errorprone.bugpatterns.proto.ProtoTest.TestFieldProtoMessage;
            import java.util.Map;

            class Test {
              Map<Integer, Integer> f(TestProtoMessage m) {
                Map<Integer, Integer> l = m.getWeightMap();
                return l;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void positiveViaVariable_rawType() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import com.google.common.collect.ImmutableList;
            import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;
            import com.google.errorprone.bugpatterns.proto.ProtoTest.TestFieldProtoMessage;
            import java.util.List;

            class Test {
              List<TestFieldProtoMessage> f(TestProtoMessage m) {
                ImmutableList l = ImmutableList.copyOf(m.getMultiFieldList());
                return l;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.google.common.collect.ImmutableList;
            import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;
            import com.google.errorprone.bugpatterns.proto.ProtoTest.TestFieldProtoMessage;
            import java.util.List;

            class Test {
              List<TestFieldProtoMessage> f(TestProtoMessage m) {
                List l = m.getMultiFieldList();
                return l;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void positiveUsedDirectly() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import com.google.common.collect.ImmutableList;
            import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;
            import com.google.errorprone.bugpatterns.proto.ProtoTest.TestFieldProtoMessage;

            class Test {
              void f(TestProtoMessage m) {
                for (var x : ImmutableList.copyOf(m.getMultiFieldList())) {}
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.google.common.collect.ImmutableList;
            import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;
            import com.google.errorprone.bugpatterns.proto.ProtoTest.TestFieldProtoMessage;

            class Test {
              void f(TestProtoMessage m) {
                for (var x : m.getMultiFieldList()) {}
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negative() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import com.google.common.collect.ImmutableList;
            import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;
            import com.google.errorprone.bugpatterns.proto.ProtoTest.TestFieldProtoMessage;
            import java.util.List;

            class Test {
              ImmutableList<TestFieldProtoMessage> f(TestProtoMessage m) {
                ImmutableList<TestFieldProtoMessage> l = ImmutableList.copyOf(m.getMultiFieldList());
                return l;
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void field_noFinding() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import com.google.common.collect.ImmutableList;
            import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;
            import com.google.errorprone.bugpatterns.proto.ProtoTest.TestFieldProtoMessage;
            import java.util.List;

            class Test {
              private static final TestProtoMessage PROTO = TestProtoMessage.getDefaultInstance();

              private static final ImmutableList<TestFieldProtoMessage> FIELDS =
                  ImmutableList.copyOf(PROTO.getMultiFieldList());
            }
            """)
        .expectUnchanged()
        .doTest();
  }
}
