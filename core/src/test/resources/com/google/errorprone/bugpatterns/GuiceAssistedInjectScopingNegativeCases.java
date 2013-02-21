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
package com.google.errorprone.bugpatterns;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.servlet.RequestScoped;

/**
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class GuiceAssistedInjectScopingNegativeCases {
  
  public class TestClass {
    public TestClass(String unassistedParam1, String unassistedParam2) {
    }
  }
  
  @Singleton
  public class TestClass2 {
    public TestClass2(String unassistedParam1, String unassistedParam2) {
    }
  }
    
  public class TestClass3 {
    public TestClass3(@Assisted String assistedParam) {
    }
  }

  @SuppressWarnings("foo")
  public class TestClass4 {
    public TestClass4(String unassistedParam, @Assisted String assistedParam) {
    }
  }
  
  @Singleton
  public class TestClass5 {
    public TestClass5(String unassistedParam, String unassistedParam2) {
    }
    
    public TestClass5(String unassistedParam, int i) {
    }
    
    public TestClass5(int i, String unassistedParam) {
    }
  }
    
  /**
   * Multiple constructors, one with @Inject, non-@Inject ones match.
   */
  @Singleton
  public class TestClass6 {
    @Inject
    public TestClass6(String unassistedParam, String unassistedParam2) {
    }
    
    public TestClass6(@Assisted String assistedParam, int i) {
    }
    
    public TestClass6(int i, @Assisted String assistedParam) {
    }
  }

}
