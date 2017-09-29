/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

package com.google.errorprone.bugpatterns.android.testdata;

import static android.support.annotation.RestrictTo.Scope.GROUP_ID;
import static android.support.v4.app.RestrictToEnforcerPositiveCasesApi.RestrictedAbstractMethod;
import static android.support.v4.app.RestrictToEnforcerPositiveCasesApi.RestrictedClass;
import static android.support.v4.app.RestrictToEnforcerPositiveCasesApi.RestrictedConstructor;
import static android.support.v4.app.RestrictToEnforcerPositiveCasesApi.RestrictedInterface;
import static android.support.v4.app.RestrictToEnforcerPositiveCasesApi.RestrictedMethodClass;
import static android.support.v4.app.RestrictToEnforcerPositiveCasesApi.RestrictedOuterClass.UnrestrictedClass;
import static android.support.v4.app.RestrictToEnforcerPositiveCasesApi.RestrictedSubClass;
import static android.support.v4.app.RestrictToEnforcerPositiveCasesApi.UnrestrictedRestrictedInterfaceImplementor;
import static android.support.v4.app.RestrictToEnforcerPositiveCasesApi.UnrestrictedSubclass;

import android.support.annotation.RestrictTo;
import android.support.v4.app.RestrictToEnforcerPositiveCasesApi;
import java.util.concurrent.atomic.AtomicReference;

public class RestrictToEnforcerPositiveCases {

  private abstract class ExtendsRestricted extends RestrictedAbstractMethod {}

  private final class ExtendsExtendsRestricted extends ExtendsRestricted {
    @Override
    // BUG: Diagnostic contains: RestrictedAbstractMethod.restricted() is restricted
    public void restricted() {}

    @Override
    // no bug - interface is restricted but method isn't
    public void interfaceMethod() {}
  }

  private abstract class Unrestricted {
    public void interfaceMethod() {}
  }

  // No bug - extending an unrestricted class and method is not marked restricted
  private final class UnrestrictedSubclassSubslass extends UnrestrictedSubclass {

    // No bug - extending an unrestricted class and method is not marked restricted
    @Override
    public void instanceMethod() {}
  }

  // BUG: Diagnostic contains: RestrictedInterface is restricted
  private final class ExtendsUnrestricted extends Unrestricted implements RestrictedInterface {

    @Override
    public void interfaceMethod() { // method name comes from both restricted and unrestricted
    }
  }

  // BUG: Diagnostic contains: @RestrictTo cannot be used outside
  @RestrictTo(GROUP_ID)
  public void cantUseRestrictToOutsideSupportLib() {}

  // BUG: Diagnostic contains: @RestrictTo cannot be used outside
  @RestrictTo(GROUP_ID)
  public static final class CantUseRestrictToOutsideSupportLib {}

  // BUG: Diagnostic contains: RestrictedInterface is restricted
  public void consumesRestricted(RestrictedInterface param) {}

  private final class ExtendsUnrestrictedRestrictedInterfaceImplementor
      extends UnrestrictedRestrictedInterfaceImplementor {
    public void interfaceMethod() {} // No bug, method is unrestricted on superclass
  }

  void check() {

    // No bug - restrictto not enforced outside support lib
    CantUseRestrictToOutsideSupportLib var = null;

    // No bug - restrictto not enforced outside support lib
    cantUseRestrictToOutsideSupportLib();

    // BUG: Diagnostic contains: RestrictedMethodClass.doSomething() is restricted
    String string = (String) RestrictedMethodClass.doSomething();

    // BUG: Diagnostic contains: RestrictedClass.doSomething() is restricted
    String string2 = (String) RestrictedClass.doSomething();

    ExtendsExtendsRestricted obj = null;

    // BUG: Diagnostic contains: RestrictedAbstractMethod.restricted() is restricted
    obj.restricted();

    // no bug - method is not restricted
    obj.interfaceMethod();

    // no bug - method is not restricted
    UnrestrictedSubclass.doSomething();

    // no bug - method is not restricted
    new UnrestrictedSubclass().instanceMethod();

    Runnable unrestricted = new UnrestrictedSubclass()::instanceMethod;

    // BUG: Diagnostic contains: RestrictedInterface is restricted
    ((RestrictedInterface) obj).toString();

    // BUG: Diagnostic contains: RestrictedInterface is restricted
    RestrictedInterface obj2 = obj;

    // BUG: Diagnostic contains: RestrictedInterface is restricted
    if (obj instanceof RestrictedInterface) {}

    // BUG: Diagnostic contains: RestrictedInterface.interfaceMethod() is restricted
    RestrictToEnforcerPositiveCasesApi.returnsRestrictedInterface().interfaceMethod();

    RestrictToEnforcerPositiveCasesApi.consumesRestrictedInterface(
        // BUG: Diagnostic contains: RestrictedInterface is restricted
        () -> {
          return;
        });

    // BUG: Diagnostic contains: RestrictedInterface is restricted
    RestrictToEnforcerPositiveCasesApi.consumesRestrictedInterface(System.out::println);

    // BUG: Diagnostic contains: RestrictedMethodClass.doSomething() is restricted
    Runnable runnable = RestrictedMethodClass::doSomething;

    // No bug - calling method on our own class
    new ExtendsUnrestrictedRestrictedInterfaceImplementor().interfaceMethod();

    // No bug - calling method on our own class
    Runnable runnable2 = new ExtendsUnrestrictedRestrictedInterfaceImplementor()::interfaceMethod;

    // BUG: Diagnostic contains: RestrictedOuterClass.UnrestrictedClass.doSomething() is restricted
    UnrestrictedClass.doSomething();

    new RestrictedAbstractMethod() {
      @Override
      // BUG: Diagnostic contains: RestrictedAbstractMethod.restricted() is restricted
      public void restricted() {}

      @Override
      public void interfaceMethod() {}
    };

    // BUG: Diagnostic contains: RestrictedClass is restricted
    AtomicReference<? extends RestrictedClass> ref = new AtomicReference<>(null);

    // BUG: Diagnostic contains: RestrictedClass.doSomething() is restricted
    ref.get().doSomething();

    // BUG: Diagnostic contains: RestrictedSubClass is restricted
    RestrictedSubClass subClass = null;

    // BUG: Diagnostic contains: RestrictedSubClass.superClassMethod() is restricted
    subClass.superClassMethod();

    // BUG: Diagnostic contains: RestrictedConstructor.RestrictedConstructor() is restricted
    new RestrictedConstructor();
  }
}
