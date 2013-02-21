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
import com.google.inject.servlet.RequestScoped;
import com.google.inject.assistedinject.Assisted;

/**
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class GuiceAssistedInjectScopingPositiveCases {

  //BUG: Suggestion includes "remove this line"
  @Singleton
  public class TestClass {
    public TestClass(@Assisted String assistedParam) {
    }
  }
  
  //BUG: Suggestion includes "remove this line"
  @RequestScoped
  public class TestClass2 {
    public TestClass2(@Assisted String assistedParam) {
    }
  }
  
  //BUG: Suggestion includes "remove this line"
  @Singleton
  public class TestClass3 {
    public TestClass3(String unassistedParam, @Assisted String assistedParam) {
    }
  }
  
  /**
   * Multiple constructors, but only one with @Inject, and that one matches.
   */
  //BUG: Suggestion includes "remove this line"
  @Singleton
  public class TestClass4 {
    @Inject
    public TestClass4(String unassistedParam, @Assisted String assistedParam) {
    }
    
    public TestClass4(String unassistedParam, int i) {
    }
    
    public TestClass4(int i, String assistedParam) {
    }
  }
    
  /**
   * Multiple constructors, none with @Inject, one matches.
   */
  //BUG: Suggestion includes "remove this line"
  @Singleton
  public class TestClass5 {
    public TestClass5(String unassistedParam, String unassistedParam2) {
    }
    
    public TestClass5(String unassistedParam, int i) {
    }
    
    public TestClass5(int i, @Assisted String assistedParam) {
    }
    
  }


}
