/*
 * Copyright 2016 The Error Prone Authors.
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

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link ProtoStringFieldReferenceEquality}Test */
@RunWith(JUnit4.class)
public class ProtoStringFieldReferenceEqualityTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(ProtoStringFieldReferenceEquality.class, getClass());

  @Test
  public void positive() {
    compilationHelper
        .addSourceLines(
            "com/google/protobuf/GeneratedMessage.java",
            "package com.google.protobuf;",
            "public class GeneratedMessage {}")
        .addSourceLines(
            "Proto.java",
            "public abstract class Proto extends com.google.protobuf.GeneratedMessage {",
            "  public abstract String getMessage();",
            "}")
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  boolean g(Proto proto) {",
            "    boolean r = false;",
            "    // BUG: Diagnostic contains: proto.getMessage().equals(\"\")",
            "    r |= proto.getMessage() == \"\";",
            "    // BUG: Diagnostic contains: \"\".equals(proto.getMessage())",
            "    r |= \"\" == proto.getMessage();",
            "    // BUG: Diagnostic contains: !proto.getMessage().equals(\"\")",
            "    r |= proto.getMessage() != \"\";",
            "    return r;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    compilationHelper
        .addSourceLines(
            "com/google/protobuf/GeneratedMessage.java",
            "package com.google.protobuf;",
            "public class GeneratedMessage {}")
        .addSourceLines(
            "Proto.java",
            "public abstract class Proto extends com.google.protobuf.GeneratedMessage {",
            "  public abstract int getId();",
            "  public abstract String getMessage();",
            "}")
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  boolean g(Proto proto) {",
            "    boolean r = false;",
            "    r |= proto.getId() == 0;",
            "    r |= proto.getMessage() == null;",
            "    return r;",
            "  }",
            "}")
        .doTest();
  }
}
