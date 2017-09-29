/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

package android.support.v4.app;

import static android.support.annotation.RestrictTo.Scope.GROUP_ID;

import android.support.annotation.RestrictTo;

public class RestrictToEnforcerPositiveCasesApi {

  public static class RestrictedMethodClass {
    @RestrictTo(GROUP_ID)
    public static CharSequence doSomething() {
      return null;
    }
  }

  public static class RestrictedConstructor {
    @RestrictTo(GROUP_ID)
    public RestrictedConstructor() {}
  }

  @RestrictTo(GROUP_ID)
  public static class RestrictedOuterClass {
    public static class UnrestrictedClass {
      public static CharSequence doSomething() {
        return null;
      }
    }
  }

  @RestrictTo(GROUP_ID)
  public static class RestrictedClass {
    public static CharSequence doSomething() {
      return null;
    }

    public void instanceMethod() {}
  }

  public static class UnrestrictedSubclass extends RestrictedClass {}

  public static class UnrestrictedSuperClass {
    public void superClassMethod() {}
  }

  @RestrictTo(GROUP_ID)
  public static class RestrictedSubClass extends UnrestrictedSuperClass {}

  @RestrictTo(GROUP_ID)
  public interface RestrictedInterface {
    void interfaceMethod();
  }

  public static void consumesRestrictedInterface(RestrictedInterface restricted) {}

  public static RestrictedInterface returnsRestrictedInterface() {
    return null;
  }

  public abstract static class UnrestrictedRestrictedInterfaceImplementor
      implements RestrictedInterface {}

  public abstract static class RestrictedAbstractMethod implements RestrictedInterface {
    @RestrictTo(GROUP_ID)
    public abstract void restricted();
  }
}
