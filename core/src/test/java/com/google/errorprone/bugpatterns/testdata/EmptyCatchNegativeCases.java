/*
 * Copyright 2020 The Error Prone Authors.
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

import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import org.junit.Test;

/** @author yuan@ece.toronto.edu (Ding Yuan) */
public class EmptyCatchNegativeCases {
  public void error() throws IllegalArgumentException {
    throw new IllegalArgumentException("Fake exception.");
  }

  public void harmlessError() throws FileNotFoundException {
    throw new FileNotFoundException("harmless exception.");
  }

  public void close() throws IllegalArgumentException {
    // close() is an allowed method, so any exceptions
    // thrown by this method can be ignored!
    throw new IllegalArgumentException("Fake exception.");
  }

  public void handledException() {
    int a = 0;
    try {
      error();
    } catch (Exception e) {
      a++; // handled here
    }
  }

  public void exceptionHandledByDataflow() {
    int a = 0;
    try {
      error();
      a = 10;
    } catch (Throwable t) {
      /* Although the exception is ignored here, it is actually
       * handled by the if check below.
       */
    }
    if (a != 10) {
      System.out.println("Exception is handled here..");
      a++;
    }
  }

  public void exceptionHandledByControlFlow() {
    try {
      error();
      return;
    } catch (Throwable t) {
      /* Although the exception is ignored here, it is actually
       * handled by the return statement in the try block.
       */
    }
    System.out.println("Exception is handled here..");
  }

  public void alreadyInCatch() {
    try {
      error();
    } catch (Throwable t) {
      try {
        error();
      } catch (Exception e) {
        // Although e is ignored, it is OK b/c we're already
        // in a nested catch block.
      }
    }
  }

  public void harmlessException() {
    try {
      harmlessError();
    } catch (FileNotFoundException e) {
      /* FileNotFoundException is a harmless exception and
       * it is OK to ignore it.
       */
    }
  }

  public void exemptedMethod() {
    try {
      close();
    } catch (Exception e) {
      // Although the exception is ignored, we can allow this b/c
      // it is thrown by an exempted method.
    }
  }

  public void comment() {
    int a = 0; // TODO
    try {
      error();
      // TODO
      /* FIXME */
    } catch (Throwable t) {
      // ignored
    }
  }

  public void catchIsLoggedOnly() {
    try {
      error();
    } catch (Throwable t) {
      System.out.println("Caught an exception: " + t);
    }
  }

  @Test
  public void expectedException() {
    try {
      System.err.println();
      fail();
    } catch (Exception expected) {
    }
  }
}
