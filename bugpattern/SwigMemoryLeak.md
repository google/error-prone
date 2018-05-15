---
title: SwigMemoryLeak
summary: SWIG generated code that can't call a C++ destructor will leak memory
layout: bugpattern
tags: ''
severity: WARNING
providesFix: NO_FIX
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
SWIG is a tool that will automatically generate Java bindings to C++ code. It is
possible to %ignore in SWIG a C++ object's destructor, this is trivially
achieved when using %ignoreall and then selectively %unignore-ing an API. SWIG
cleans up C++ objects using a delete method, which is most commonly called by a
finalizer. When a SWIG generated delete method can't call a destructor, as it is
hidden, the delete method throws an exception. However, in the case of a hidden
C++ destructor SWIG also doesn't generate a finalizer, and so the most common
call to the delete method is removed. The consequence of this is that the SWIG
objects leak their C++ counterpart and no warnings or exceptions are thrown.

This check looks for the pattern of a memory leaking SWIG generated object and
warns about the potential memory leak. The most straightforward fix is to the
SWIG input code to tell it not to %ignore the C++ code's destructor.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("SwigMemoryLeak")` to the enclosing element.

----------

### Positive examples
__SwigMemoryLeakPositiveCases.java__

{% highlight java %}
/*
 * Copyright 2017 The Error Prone Authors.
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

/** @author irogers@google.com (Ian Rogers) */
public class SwigMemoryLeakPositiveCases {
  private long swigCPtr;
  protected boolean swigCMemOwn;

  public SwigMemoryLeakPositiveCases(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  public synchronized void delete() {
    if (swigCPtr != 0) {
      if (swigCMemOwn) {
        swigCMemOwn = false;
        // BUG: Diagnostic contains: SWIG generated code that can't call a C++ destructor will leak
        // memory
        throw new UnsupportedOperationException("C++ destructor does not have public access");
      }
      swigCPtr = 0;
    }
  }
}
{% endhighlight %}

### Negative examples
__SwigMemoryLeakNegativeCases.java__

{% highlight java %}
/*
 * Copyright 2017 The Error Prone Authors.
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

/** @author irogers@google.com (Ian Rogers) */
public class SwigMemoryLeakNegativeCases {
  private long swigCPtr;
  protected boolean swigCMemOwn;

  public SwigMemoryLeakNegativeCases(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if (swigCPtr != 0) {
      if (swigCMemOwn) {
        swigCMemOwn = false;
        nativeDelete(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  private static native void nativeDelete(long cptr);
}
{% endhighlight %}

