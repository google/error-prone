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

import java.io.FilterWriter;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Created by sulku and mariasam on 6/6/17.
 *
 * @author mariasam (Maria Sam)
 * @author sulku (Marsela Sulku)
 */
public class InstanceOfAndCastMatchWrongTypeNegativeCases {

  public static void notCustomClass(Object objSubClass) {
    if (!(objSubClass instanceof SuperNegativeClass)) {
      DisjointClass str = (DisjointClass) objSubClass;
    }
  }

  public static void hi(String foo) {
    if (foo instanceof String) {
      ((String) foo).charAt(0);
    }
  }

  public static void castToSameClass(String foo) {
    if (foo instanceof String) {
      ((String) foo).charAt(0);
    }
  }

  public static void castToSameClassWithExtraLines(String foo) {
    if (foo instanceof String) {
      String somethingBefore = "hello";
      ((String) foo).charAt(0);
      String somethingAfter = "goodbye";
    }
  }

  public static void castAMethod() {
    if (testCall() instanceof String) {
      String bar = (String) testCall();
    }
  }

  public static void castToSuperType(String foo) {
    if (foo instanceof String) {
      Object bar = ((Object) foo).toString();
    }
  }

  public static void castMethodToSuperType(String foo) {
    if (testCall() instanceof String) {
      Object bar = (Object) testCall();
    }
  }

  public static void castToCustomSuperType() {
    SuperNegativeClass superClass = new SuperNegativeClass();
    SubNegativeClass subClass = new SubNegativeClass();

    if (subClass instanceof SubNegativeClass) {
      String str = ((SuperNegativeClass) subClass).toString();
    }
  }

  public static void castToSubtype(String foo) {
    if (foo instanceof Object) {
      String somethingBefore = "hello";
      String bar = ((String) foo).toString();
      String somethingAfter = "goodbye";
    }
  }

  public static void nestedIfStatements(String foo) {
    if (7 == 7) {
      if (foo instanceof Object) {
        String bar = ((String) foo).toString();
      }
    }
  }

  public static void castMethodToSubType() {
    if (testCall() instanceof Object) {
      String bar = ((String) testCall()).toString();
    }
  }

  public static void castAMethodInElse() {
    if (testCall() instanceof Object) {
      String str = "";
    } else {
      String bar = ((String) testCall()).toString();
    }
  }

  public static void nestedIfOutside() {
    SubNegativeClass subClass = new SubNegativeClass();
    if (subClass instanceof SuperNegativeClass) {
      if (7 == 7) {
        String bar = ((SuperNegativeClass) subClass).toString();
      }
    }
  }

  public static void nestedIfElseInIf() {
    SubNegativeClass subClass = new SubNegativeClass();
    if (subClass instanceof SuperNegativeClass) {
      if (7 == 7) {
        String bar = ((SuperNegativeClass) subClass).toString();
      } else {
        String str = "";
      }
    }
  }

  public static void elseIfMethod() {
    if (testCall() instanceof Object) {
      String str = "";
    } else if (7 == 7) {
      String bar = ((String) testCall()).toString();
    } else {
      String str = "";
    }
  }

  public static void nestedSubClasses(Object objSubClass) {
    if (objSubClass instanceof SuperNegativeClass) {
      if (objSubClass instanceof DisjointClass) {
        DisjointClass disClass = (DisjointClass) objSubClass;
      }
    }
  }

  public static void switchCaseStatement(Object objSubClass) {
    Integer datatype = 0;
    if (objSubClass instanceof SuperNegativeClass) {
      String str = "";
    } else {
      switch (datatype) {
        case 0:
          DisjointClass str = (DisjointClass) objSubClass;
          break;
        default:
          break;
      }
    }
  }

  public static void nestedAnd(String foo, Object foo3) {
    if (foo instanceof String) {
      if (foo3 instanceof SuperNegativeClass && ((SuperNegativeClass) foo3).toString().equals("")) {
        String str = foo3.toString();
      }
    }
  }

  private static void multipleElseIf(Object foo3) {
    if (foo3 instanceof String) {
      String str = "";
    } else if (7 == 7 && foo3 instanceof SuperNegativeClass) {
      ((SuperNegativeClass) foo3).toString();
    } else if (8 == 8) {
      DisjointClass dis = (DisjointClass) foo3;
    }
  }

  private static void orInCondition(Object foo3) {
    if (foo3 instanceof String || 7 == 7) {
      String str = ((DisjointClass) foo3).toString();
    }
  }

  private static void castInElse(Object foo3) {
    if (foo3 instanceof String) {
      String str = "";
    } else {
      String str = ((DisjointClass) foo3).toString();
    }
  }

  private static void multipleObjectCasts(Object foo2, Object foo3) {
    if (foo3 instanceof String) {
      String str = ((DisjointClass) foo2).toString();
    } else {
      String str = ((DisjointClass) foo3).toString();
    }
  }

  private static void orsAndAnds(Object foo2) {
    if (7 == 7 && (foo2 instanceof DisjointClass) && (!((DisjointClass) foo2).equals(""))) {
      String str = "";
    }
  }

  private static void assignmentInBlock(Object foo2) {
    if (foo2 instanceof SuperNegativeClass) {
      foo2 = "";
      String str = ((Integer) foo2).toString();
    }
  }

  private static void assignmentInBlockElse(Object foo2) {
    String foo1;
    if (foo2 instanceof SuperNegativeClass) {
      String str = "";
    } else {
      foo1 = "";
      String str = ((Integer) foo2).toString();
    }
  }

  private static void assignmentInBlockElseIf(Object foo2) {
    Object foo1 = null;
    if (foo2 instanceof SuperNegativeClass) {
      String str = "";
    } else if (foo2 == foo1) {
      foo1 = "";
      String str = ((Integer) foo2).toString();
    }
  }

  private static void innerClassDecl(Object[] list) {
    for (Object c : list) {
      if (c instanceof String) {
        try {
          Writer fw =
              new FilterWriter(new StringWriter()) {
                public void write(int c) {
                  char a = (char) c;
                }
              };
        } catch (Exception e) {
          String str = "";
        }
      }
    }
  }

  private static void randomCode(Object foo) {
    if (7 == 7) {
      System.out.println("test");
      foo = (Integer) foo;
    }
  }

  private static void twoAssignments(Object foo, Object foo2) {
    if (foo instanceof String) {
      foo2 = "";
      String str = (String) foo;
      foo = "";
    }
  }

  public static String testCall() {
    return "";
  }

  public static Object testCallReturnsObject() {
    return new Object();
  }

  static class SuperNegativeClass {}

  static class SubNegativeClass extends SuperNegativeClass {}

  static class DisjointClass {}
}
