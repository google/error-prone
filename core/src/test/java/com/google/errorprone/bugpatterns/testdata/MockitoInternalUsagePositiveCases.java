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

import java.io.Serializable;

class MockitoInternalUsagePositiveCases {

  public void newObject() {
    // BUG: Diagnostic contains:
    new org.mockito.internal.MockitoCore();
    // BUG: Diagnostic contains:
    new InternalConsumer(new org.mockito.internal.MockitoCore());
  }

  public void staticMethodInvocation() {
    // BUG: Diagnostic contains:
    org.mockito.internal.configuration.GlobalConfiguration.validate();
  }

  public void variableTypeDeclaration() {
    // BUG: Diagnostic contains:
    org.mockito.internal.stubbing.InvocationContainer container = null;
  }

  // BUG: Diagnostic contains:
  class ExtendsClause extends org.mockito.internal.MockitoCore {}

  // BUG: Diagnostic contains:
  abstract class ImplementsClause implements org.mockito.internal.stubbing.InvocationContainer {}

  abstract class SecondImplementsClause
      // BUG: Diagnostic contains:
      implements Serializable, org.mockito.internal.stubbing.InvocationContainer {}

  // BUG: Diagnostic contains:
  class ExtendsGeneric<T extends org.mockito.internal.stubbing.InvocationContainer> {}

  // BUG: Diagnostic contains:
  class SecondExtendsGeneric<R, T extends org.mockito.internal.stubbing.InvocationContainer> {}

  class FieldClause {
    // BUG: Diagnostic contains:
    org.mockito.internal.MockitoCore core;
  }

  class MethodArgumentClause {
    // BUG: Diagnostic contains:
    public void methodArgument(org.mockito.internal.MockitoCore core) {}
  }

  static class InternalConsumer {
    // BUG: Diagnostic contains:
    InternalConsumer(org.mockito.internal.MockitoCore core) {}
  }

}
