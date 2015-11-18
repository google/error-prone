/*
 * Copyright 2011 Google Inc. All Rights Reserved.
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

/**
 * @author yuan@ece.toronto.edu (Ding Yuan)
 */
public class AbortInOvercatchPositiveCases {  
  class ExitMethods {
      public void iwillabortNOW () {
        System.out.println("I am about to abort.");
      }
    
      public void willShutDownSoon () {
        System.out.println("About to shut down.");
      }
      
      public void abort() {
        System.out.println("Aborting..");
      }
  }
  
  static class StaticExitMethods {
      public static void willABORTnow () {
        System.out.println("I am about to abort.");
      }

      public static void willSHUTDOWNnow () {
        System.out.println("About to shut down.");
      }
  }
  
  ExitMethods exitMethods = new ExitMethods();

  public void error() throws IllegalArgumentException {
    throw new IllegalArgumentException("Fake exception.");
  }
  
  public void abortInThrowable() {
    try {
      error();
    } // BUG: Diagnostic contains: 
    catch (Throwable t) {
      System.exit(1);
    }
    
    try {
        error();
    } // BUG: Diagnostic contains: 
    catch (Throwable t) {
        exitMethods.iwillabortNOW();
    }
    try {
        error();
    } // BUG: Diagnostic contains:
    catch (Throwable t) {
       exitMethods.willShutDownSoon();
    }

    try {
      error();
    } // BUG: Diagnostic contains:
    catch (Throwable t) {
      StaticExitMethods.willABORTnow();
    }

    try {
      error();
    } // BUG: Diagnostic contains:
    catch (Throwable t) {
      StaticExitMethods.willSHUTDOWNnow();
    }    
  }
  
  public void abortInException() {
    try {
      error();
    } // BUG: Diagnostic contains: 
    catch (Exception e) {
      System.exit(1);
    }
    
    try {
      error();
    } // BUG: Diagnostic contains:
    catch (Exception e) {
      exitMethods.iwillabortNOW();
    }
    
    try {
        error();
    } // BUG: Diagnostic contains:
    catch (Exception e) {
       exitMethods.willShutDownSoon();
    }

    try {
      error();
    } // BUG: Diagnostic contains:
    catch (Exception e) {
      StaticExitMethods.willABORTnow();
    }

    try {
      error();
    } // BUG: Diagnostic contains:
    catch (Exception e) {
      StaticExitMethods.willSHUTDOWNnow();
    }

    try {
        error();
    } // BUG: Diagnostic contains:
    catch (Exception e) {
       exitMethods.abort();
    }
  }
}
