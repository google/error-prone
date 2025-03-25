/*
 * Copyright 2019 The Error Prone Authors.
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

/**
 * @author friedj@google.com (Jake Fried)
 */
@RunWith(JUnit4.class)
public class RxReturnValueIgnoredTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(RxReturnValueIgnored.class, getClass())
          // Rx1 stubs
          .addSourceLines(
              "rx1/Observable.java",
              """
              package rx;

              public class Observable<T> {}
              """ //
              )
          .addSourceLines(
              "rx1/Single.java",
              """
              package rx;

              public class Single<T> {}
              """ //
              )
          .addSourceLines(
              "rx1/Completable.java",
              """
              package rx;

              public class Completable<T> {}
              """ //
              )
          // Rx2 stubs
          .addSourceLines(
              "rx2/Observable.java",
              """
              package io.reactivex;

              public class Observable<T> {}
              """ //
              )
          .addSourceLines(
              "rx2/Single.java",
              """
              package io.reactivex;

              public class Single<T> {}
              """ //
              )
          .addSourceLines(
              "rx2/Completable.java",
              """
              package io.reactivex;

              public class Completable<T> {}
              """ //
              )
          .addSourceLines(
              "rx2/Maybe.java",
              """
              package io.reactivex;

              public class Maybe<T> {}
              """ //
              )
          .addSourceLines(
              "rx2/Flowable.java",
              """
              package io.reactivex;

              public class Flowable<T> {}
              """ //
              );

  @Test
  public void positiveCases() {
    compilationHelper
        .addSourceLines(
            "RxReturnValueIgnoredPositiveCases.java",
"""
package com.google.errorprone.bugpatterns.testdata;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author friedj@google.com (Jake Fried)
 */
public class RxReturnValueIgnoredPositiveCases {
  private static Observable getObservable() {
    return null;
  }

  private static Single getSingle() {
    return null;
  }

  private static Flowable getFlowable() {
    return null;
  }

  private static Maybe getMaybe() {
    return null;
  }

  {
    new Observable();
    new Single();
    new Flowable();
    new Maybe();

    // BUG: Diagnostic contains: Rx objects must be checked.
    getObservable();
    // BUG: Diagnostic contains: Rx objects must be checked.
    getSingle();
    // BUG: Diagnostic contains: Rx objects must be checked.
    getFlowable();
    // BUG: Diagnostic contains: Rx objects must be checked.
    getMaybe();

    // BUG: Diagnostic contains: Rx objects must be checked.
    Arrays.asList(1, 2, 3).forEach(n -> getObservable());
    // BUG: Diagnostic contains: Rx objects must be checked.
    Arrays.asList(1, 2, 3).forEach(n -> getSingle());
    // BUG: Diagnostic contains: Rx objects must be checked.
    Arrays.asList(1, 2, 3).forEach(n -> getFlowable());
    // BUG: Diagnostic contains: Rx objects must be checked.
    Arrays.asList(1, 2, 3).forEach(n -> getMaybe());
  }

  private abstract static class IgnoringParent<T> {
    @CanIgnoreReturnValue
    abstract T ignoringFunction();
  }

  private class NonIgnoringObservableChild extends IgnoringParent<Observable<Integer>> {
    @Override
    Observable<Integer> ignoringFunction() {
      return null;
    }
  }

  private class NonIgnoringSingleChild extends IgnoringParent<Single<Integer>> {
    @Override
    Single<Integer> ignoringFunction() {
      return null;
    }
  }

  private class NonIgnoringFlowableChild extends IgnoringParent<Flowable<Integer>> {
    @Override
    Flowable<Integer> ignoringFunction() {
      return null;
    }
  }

  private class NonIgnoringMaybeChild extends IgnoringParent<Maybe<Integer>> {
    @Override
    Maybe<Integer> ignoringFunction() {
      return null;
    }
  }

  public void inheritanceTest() {
    NonIgnoringObservableChild observableChild = new NonIgnoringObservableChild();
    NonIgnoringSingleChild singleChild = new NonIgnoringSingleChild();
    NonIgnoringFlowableChild flowableChild = new NonIgnoringFlowableChild();
    NonIgnoringMaybeChild maybeChild = new NonIgnoringMaybeChild();

    // BUG: Diagnostic contains: Rx objects must be checked.
    observableChild.ignoringFunction();
    // BUG: Diagnostic contains: Rx objects must be checked.
    singleChild.ignoringFunction();
    // BUG: Diagnostic contains: Rx objects must be checked.
    flowableChild.ignoringFunction();
    // BUG: Diagnostic contains: Rx objects must be checked.
    maybeChild.ignoringFunction();
  }

  public void conditional() {
    if (false) {
      // BUG: Diagnostic contains: Rx objects must be checked.
      getObservable();
      // BUG: Diagnostic contains: Rx objects must be checked.
      getSingle();
      // BUG: Diagnostic contains: Rx objects must be checked.
      getFlowable();
      // BUG: Diagnostic contains: Rx objects must be checked.
      getMaybe();
    }

    return;
  }

  static void getFromMap() {
    Map<Object, Observable> map1 = new HashMap<>();
    Map<Object, Single> map2 = new HashMap<>();
    Map<Object, Flowable> map3 = new HashMap<>();
    Map<Object, Maybe> map4 = new HashMap<>();

    // BUG: Diagnostic contains: Rx objects must be checked.
    map1.get(null);
    // BUG: Diagnostic contains: Rx objects must be checked.
    map2.get(null);
    // BUG: Diagnostic contains: Rx objects must be checked.
    map3.get(null);
    // BUG: Diagnostic contains: Rx objects must be checked.
    map4.get(null);
  }
}\
""")
        .doTest();
  }

  @Test
  public void negativeCases() {
    compilationHelper
        .addSourceLines(
            "RxReturnValueIgnoredNegativeCases.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            import com.google.errorprone.annotations.CanIgnoreReturnValue;
            import io.reactivex.Flowable;
            import io.reactivex.Maybe;
            import io.reactivex.Observable;
            import io.reactivex.Single;
            import java.util.HashMap;
            import java.util.Map;

            /**
             * @author friedj@google.com (Jake Fried)
             */
            public class RxReturnValueIgnoredNegativeCases {
              interface CanIgnoreMethod {
                @CanIgnoreReturnValue
                Observable<Object> getObservable();

                @CanIgnoreReturnValue
                Single<Object> getSingle();

                @CanIgnoreReturnValue
                Flowable<Object> getFlowable();

                @CanIgnoreReturnValue
                Maybe<Object> getMaybe();
              }

              public static class CanIgnoreImpl implements CanIgnoreMethod {
                @Override
                public Observable<Object> getObservable() {
                  return null;
                }

                @Override
                public Single<Object> getSingle() {
                  return null;
                }

                @Override
                public Flowable<Object> getFlowable() {
                  return null;
                }

                @Override
                public Maybe<Object> getMaybe() {
                  return null;
                }
              }

              static void callIgnoredInterfaceMethod() {
                new CanIgnoreImpl().getObservable();
                new CanIgnoreImpl().getSingle();
                new CanIgnoreImpl().getFlowable();
                new CanIgnoreImpl().getMaybe();
              }

              static void putInMap() {
                Map<Object, Observable<?>> map1 = new HashMap<>();
                Map<Object, Single<?>> map2 = new HashMap<>();
                Map<Object, Maybe<?>> map3 = new HashMap<>();
                HashMap<Object, Flowable<?>> map4 = new HashMap<>();

                map1.put(new Object(), null);
                map2.put(new Object(), null);
                map3.put(new Object(), null);
                map4.put(new Object(), null);
              }

              @CanIgnoreReturnValue
              Observable<Object> getObservable() {
                return null;
              }

              @CanIgnoreReturnValue
              Single<Object> getSingle() {
                return null;
              }

              @CanIgnoreReturnValue
              Flowable<Object> getFlowable() {
                return null;
              }

              @CanIgnoreReturnValue
              Maybe<Object> getMaybe() {
                return null;
              }

              void checkIgnore() {
                getObservable();
                getSingle();
                getFlowable();
                getMaybe();
              }
            }\
            """)
        .doTest();
  }

  @Test
  public void rx2Observable() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import io.reactivex.Observable;

            class Test {
              Observable getObservable() {
                return null;
              }

              void f() {
                // BUG: Diagnostic contains: Rx objects must be checked.
                getObservable();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void rx2Single() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import io.reactivex.Single;

            class Test {
              Single getSingle() {
                return null;
              }

              void f() {
                // BUG: Diagnostic contains: Rx objects must be checked.
                getSingle();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void rx2Completable() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import io.reactivex.Completable;

            class Test {
              Completable getCompletable() {
                return null;
              }

              void f() {
                // BUG: Diagnostic contains: Rx objects must be checked.
                getCompletable();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void rx2Flowable() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import io.reactivex.Flowable;

            class Test {
              Flowable getFlowable() {
                return null;
              }

              void f() {
                // BUG: Diagnostic contains: Rx objects must be checked.
                getFlowable();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void rx2Maybe() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import io.reactivex.Maybe;

            class Test {
              Maybe getMaybe() {
                return null;
              }

              void f() {
                // BUG: Diagnostic contains: Rx objects must be checked.
                getMaybe();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void rx1Observable() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import rx.Observable;

            class Test {
              Observable getObservable() {
                return null;
              }

              void f() {
                // BUG: Diagnostic contains: Rx objects must be checked.
                getObservable();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void rx1Single() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import rx.Single;

            class Test {
              Single getSingle() {
                return null;
              }

              void f() {
                // BUG: Diagnostic contains: Rx objects must be checked.
                getSingle();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void rx1Completable() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import rx.Completable;

            class Test {
              Completable getCompletable() {
                return null;
              }

              void f() {
                // BUG: Diagnostic contains: Rx objects must be checked.
                getCompletable();
              }
            }
            """)
        .doTest();
  }
}
