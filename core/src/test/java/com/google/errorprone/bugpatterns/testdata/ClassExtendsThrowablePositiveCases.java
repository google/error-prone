/*
 * Copyright 2022 The Error Prone Authors.
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

// BUG: Diagnostic contains: Class should not extend throwable. Extend Exception or RuntimeException instead.
public class ClassExtendsThrowablePositiveCases extends Throwable{

    public int ClassExtendsThrowablePositiveCases()    {
        return 0;
    }

    public int test(int ignore)   {
        return ignore;
    }

    // BUG: Diagnostic contains: Class should not extend throwable. Extend Exception or RuntimeException instead.
    private class extendThrowable extends Throwable {
        private int ignore;
    }
}
