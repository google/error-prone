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
}
