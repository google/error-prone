/*
 * Copyright 2018 The Error Prone Authors.
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

/** Test for ByteBufferBackingArray bug checker */
@RunWith(JUnit4.class)
public class ByteBufferBackingArrayTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(ByteBufferBackingArray.class, getClass());

  @Test
  public void positiveCases() {
    compilationHelper
        .addSourceLines(
            "ByteBufferBackingArrayPositiveCases.java",
            """
package com.google.errorprone.bugpatterns.testdata;

import com.google.errorprone.bugpatterns.ByteBufferBackingArrayTest;
import java.nio.ByteBuffer;

/** Positive cases for {@link ByteBufferBackingArrayTest}. */
public class ByteBufferBackingArrayPositiveCases {

  public void array_notPrecededByOffsetNorValidInitializer_asLocalVariable_isFlagged() {
    ByteBuffer buff = null;
    // BUG: Diagnostic contains: ByteBuffer.array()
    buff.array();
  }

  class A {

    ByteBuffer buff = null;

    void array_notPrecededByOffsetNorValidInitializer_asField_isFlagged() {
      // BUG: Diagnostic contains: ByteBuffer.array()
      buff.array();
    }
  }

  class ArrayCalledInFieldNotPrecededByOffsetNorValidInitializerAsFieldIsFlagged {
    ByteBuffer buffer = null;
    // BUG: Diagnostic contains: ByteBuffer.array()
    byte[] array = buffer.array();
  }

  void array_notPrecededByOffsetNorValidInitializer_asMethodParameter_isFlagged(ByteBuffer buffer) {
    // BUG: Diagnostic contains: ByteBuffer.array()
    buffer.array();
  }

  void array_followedByWrap_isFlagged() {
    ByteBuffer buff = null;
    // BUG: Diagnostic contains: ByteBuffer.array()
    buff.array();
    buff = ByteBuffer.wrap(new byte[] {1});
  }

  void array_followedByAllocate_isFlagged() {
    ByteBuffer buff = null;
    // BUG: Diagnostic contains: ByteBuffer.array()
    buff.array();
    buff = ByteBuffer.allocate(1);
  }

  void array_precededByAllocateDirect_isFlagged() {
    ByteBuffer buff = null;
    buff = ByteBuffer.allocateDirect(1);
    // BUG: Diagnostic contains: ByteBuffer.array()
    buff.array();
  }

  void array_precededByAllocateOnAnotherBuffer_isFlagged() {
    ByteBuffer otherBuff = ByteBuffer.allocate(1);
    ByteBuffer buff = null;
    otherBuff.arrayOffset();
    // BUG: Diagnostic contains: ByteBuffer.array()
    buff.array();
  }

  void array_precededByNotAValidMethod_isFlagged() {
    ByteBuffer buff = null;
    buff.position();
    // BUG: Diagnostic contains: ByteBuffer.array()
    buff.array();
  }
}\
""")
        .doTest();
  }

  @Test
  public void negativeCases() {
    compilationHelper
        .addSourceLines(
            "ByteBufferBackingArrayNegativeCases.java",
            """
package com.google.errorprone.bugpatterns.testdata;

import com.google.errorprone.bugpatterns.ByteBufferBackingArrayTest;
import java.nio.ByteBuffer;
import java.util.function.Function;

/** Negative cases for {@link ByteBufferBackingArrayTest}. */
public class ByteBufferBackingArrayNegativeCases {

  void noArrayCall_isNotFlagged() {
    ByteBuffer buffer = null;
    buffer.position();
  }

  void array_precededByArrayOffset_isNotFlagged() {
    ByteBuffer buffer = null;
    buffer.arrayOffset();
    buffer.array();
  }

  void array_precededByArrayOffset_onOuterScope_isNotFlagged() {
    ByteBuffer buffer = null;
    buffer.arrayOffset();
    if (true) {
      while (true) {
        buffer.array();
      }
    }
  }

  void array_precededByByteBufferWrap_isNotFlagged() {
    ByteBuffer buffer = ByteBuffer.wrap(new byte[] {1});
    buffer.array();
  }

  void array_precededByByteBufferAllocate_isNotFlagged() {
    ByteBuffer buffer = ByteBuffer.allocate(1);
    buffer.array();
  }

  // Ideally, this case should be flagged though.
  void array_followedByByteBufferArrayOffset_isNotFlagged() {
    ByteBuffer buffer = null;
    buffer.array();
    buffer.arrayOffset();
  }

  // Ideally, this case should be flagged though.
  void array_followedByArrayOffset_inExpression_isNotFlagged() {
    ByteBuffer buffer = null;
    byte[] outBytes;
    int outOffset;
    int outPos;
    if (buffer.hasArray()) {
      outBytes = buffer.array();
      outPos = outOffset = buffer.arrayOffset() + buffer.position();
    }
  }

  void array_precededByByteBufferAllocate_inSplitMethodChain_isNotFlagged() {
    ByteBuffer buffer = ByteBuffer.allocate(1).put((byte) 'a');
    buffer.array();
  }

  public void array_immediatelyPrecededByByteBufferAllocate_inContinuousMethodChain_isNotFlagged()
      throws Exception {
    ByteBuffer.allocate(0).array();
  }

  void array_precededByByteBufferAllocate_inContinuousMethodChain_isNotFlagged() {
    ByteBuffer.allocate(Long.SIZE / Byte.SIZE).putLong(1L).array();
  }

  byte[] array_inMethodChain_precededByByteBufferAllocate_isNotFlagged() {
    ByteBuffer buffer = ByteBuffer.allocate(1);
    return buffer.put(new byte[] {1}).array();
  }

  class A {
    // Ideally, this case should be flagged though.
    void array_inMethodChain_whereByteBufferIsNotAtStartOfChain_isNotFlagged() {
      A helper = new A();
      helper.getBuffer().put((byte) 1).array();
    }

    ByteBuffer getBuffer() {
      return null;
    }
  }

  class B {
    ByteBuffer buffer = ByteBuffer.allocate(1);

    void array_precededByByteBufferAllocate_inField_isNotFlagged() {
      buffer.array();
    }
  }

  class C {
    ByteBuffer buffer = ByteBuffer.allocate(1);

    class A {
      void array_precededByByteBufferAllocate_inFieldOfParentClass_isNotFlagged() {
        buffer.array();
      }
    }
  }

  class ArrayInFieldPrecededByByteBufferAllocateInFieldIsNotFlagged {
    ByteBuffer buffer = ByteBuffer.allocate(1);
    byte[] array = buffer.array();
  }

  void array_inAnonymousClass_precededByByteBufferAllocate_isNotFlagged() {
    final ByteBuffer buffer = ByteBuffer.allocate(0);

    new Function<Object, Object>() {
      @Override
      public Object apply(Object o) {
        buffer.array();
        return null;
      }
    };
  }

  void array_inLambdaExpression_precededByByteBufferAllocate_isNotFlagged() {
    final ByteBuffer buffer = ByteBuffer.allocate(0);

    Function<Void, Void> f =
        (Void unused) -> {
          buffer.array();
          return null;
        };
  }
}\
""")
        .doTest();
  }

  @Test
  public void i1004() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.nio.ByteBuffer;

            public class Test {
              public void ByteBufferBackingArrayTest() {
                byte[] byteArray = ((ByteBuffer) new Object()).array();
              }
            }
            """)
        .doTest();
  }
}
