/*
 * Copyright 2013 Google Inc. All Rights Reserved.
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

import java.io.IOException;

/**
 * @author cushon@google.com (Liam Miller-Cushon)
 */
public class FinallyPositiveCase2 {
  public void completeWithReturn(boolean flag) {
    try {
      
    } finally {
      // BUG: Diagnostic contains: 
      return;
    }
  }
  
  public void completeWithThrow(boolean flag) throws Exception {
    try {
    
    } finally {
      // BUG: Diagnostic contains: 
      throw new Exception();
    }
  }
   
  public void unreachableThrow(boolean flag) throws Exception {
    try {
    
    } finally {
      if (flag) {
        // BUG: Diagnostic contains: 
        throw new Exception(); 
      }
    }
  }
  
  public void nestedBlocks(int i, boolean flag) throws Exception {
    try {
    
    } finally {
      switch (i) {
        default:
        {
          while (flag) {
            do {
              if (flag) {
              } else {
                // BUG: Diagnostic contains: 
                throw new Exception();
              }
            } while (flag);
          }
        }
      }
    }
  }
  
  public void nestedFinally() throws Exception {
    try {
    
    } finally {
      try {
      } finally {
        // BUG: Diagnostic contains: 
        throw new IOException();
      }
    }
  }
  
  public void returnFromTryNestedInFinally() {
    try {
    } finally {
      try {
        // BUG: Diagnostic contains: 
        return;
      } finally {
      }
    }
  }
  
  public void returnFromCatchNestedInFinally() {
    try {
    } finally {
      try {
      } catch (Exception e) {
        // BUG: Diagnostic contains: 
        return;
      } finally {
      }
    }
  }
  
  public void throwUncaughtFromNestedTryInFinally() throws Exception {
    try {
    } finally {
      try {
        // BUG: Diagnostic contains: 
        throw new Exception();
      } finally {
      }
    }
  }
  
  public void throwFromNestedCatchInFinally() throws Exception {
    try {
    } finally {
      try {
      } catch (Exception e) {
        // BUG: Diagnostic contains: 
        throw new Exception();
      } finally {
      }
    }
  }
}
