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
}
