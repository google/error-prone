/* Copyright 2016 Google Inc. All Rights Reserved.
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

/**
 * Created by sulku and mariasam on 6/6/17.
 *
 * @author mariasam (Maria Sam)
 * @author sulku (Marsela Sulku)
 */
public class InstanceOfAndCastMatchWrongTypePositiveCases {

  private static void basicIllegalCast(Object foo2) {
    if (foo2 instanceof SuperClass) {
      // BUG: Diagnostic contains: Casting inside
      String str = ((String) foo2).toString();
    }
  }

  private static void basicIllegalCastJavaClass(Object foo2) {
    if (foo2 instanceof String) {
      // BUG: Diagnostic contains: Casting inside
      double val = ((Integer) foo2).doubleValue();
    }
  }

  private static void andsInIf(Object foo2) {
    if (foo2 instanceof String && 7 == 7) {
      // BUG: Diagnostic contains: Casting inside
      double val = ((Integer) foo2).doubleValue();
    }
  }

  private static void andsInIfInstanceOfLast(Object foo2) {
    if (7 == 7 && foo2 instanceof String) {
      // BUG: Diagnostic contains: Casting inside
      double val = ((Integer) foo2).doubleValue();
    }
  }

  private static void andsInIfInstanceOfMiddle(Object foo2) {
    if (7 == 7 && foo2 instanceof String && 8 == 8) {
      // BUG: Diagnostic contains: Casting inside
      double val = ((Integer) foo2).doubleValue();
    }
  }

  private static void castingInIfWithElse(Object foo2) {
    if (foo2 instanceof String) {
      // BUG: Diagnostic contains: Casting inside
      String str = ((Integer) foo2).toString();
    } else {
      String str = "";
    }
  }

  private static void castMultipleInIfAndElse(Object foo2, Object foo3) {
    if (foo2 instanceof String) {
      String str = ((Integer) foo3).toString();
      // BUG: Diagnostic contains: Casting inside
      String str2 = ((Integer) foo2).toString();
    } else {
      String str = ((Integer) foo3).toString();
      String str2 = "";
    }
  }

  private static void multipleAndsInIf(Object foo2) {
    // BUG: Diagnostic contains: Casting inside
    if (7 == 7 && (foo2 instanceof SuperClass) && (((String) foo2).equals(""))) {
      String str = "";
    }
  }

  private static void castOneObjectWithMultipleObjectsInIf(Object foo2, Object foo3) {
    if (7 == 7 && foo3 instanceof String && foo2 instanceof String) {
      // BUG: Diagnostic contains: Casting inside
      String str = ((Integer) foo2).toString();
    }
  }

  private static void aboveTestButDifferentOrder(Object foo2, Object foo3) {
    if (7 == 7 && foo2 instanceof String && foo3 instanceof String) {
      // BUG: Diagnostic contains: Casting inside
      String str = ((Integer) foo2).toString();
    }
  }

  private static void nestedIf(Object foo2) {
    if (foo2 instanceof String) {
      if (7 == 7) {
        // BUG: Diagnostic contains: Casting inside
        String str = ((Integer) foo2).toString();
      }
    }
  }

  private static void nestedIfWithElse(Object foo2) {
    if (foo2 instanceof String) {
      if (7 == 7) {
        String str = "";
      } else {
        // BUG: Diagnostic contains: Casting inside
        String str = ((Integer) foo2).toString();
      }
    }
  }

  private static void assignmentInBlockDiffVariable(Object foo2) {
    String foo1;
    if (foo2 instanceof SuperClass) {
      foo1 = "";
      // BUG: Diagnostic contains: Casting inside
      String str = ((Integer) foo2).toString();
    }
  }

  private static void assignmentInBlock(Object foo2) {
    if (foo2 instanceof SuperClass) {
      // BUG: Diagnostic contains: Casting inside
      String str = ((Integer) foo2).toString();
      foo2 = "";
    }
  }

  private static void assignmentInBlockTwice(Object foo2) {
    Object foo1 = null;
    if (foo2 instanceof SuperClass) {
      foo1 = "";
      // BUG: Diagnostic contains: Casting inside
      String str = ((Integer) foo2).toString();
      foo2 = "";
    }
  }

  private static void testSameClass(Object foo) {
    if (foo instanceof String) {
      InstanceOfAndCastMatchWrongTypePositiveCases other =
          // BUG: Diagnostic contains: Casting inside
          (InstanceOfAndCastMatchWrongTypePositiveCases) foo;
    }
  }

  private static void testElseIf(Object foo) {
    if (foo instanceof String) {
      String str = (String) foo;
    } else if (foo instanceof String) {
      // BUG: Diagnostic contains: Casting inside
      Integer i = (Integer) foo;
    } else {
      foo = (SuperClass) foo;
    }
  }

  public static String testCall() {
    return "";
  }
}

class SuperClass {}
