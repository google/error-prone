/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Basic class with an untagged setUp method */
@RunWith(JUnit4.class)
public class JUnit4SetUpNotRunPositiveCases {
  // BUG: Diagnostic contains: @Before
  public void setUp() {}
}

@RunWith(JUnit4.class)
class J4PositiveCase2 {
  // BUG: Diagnostic contains: @Before
  protected void setUp() {}
}

/**
 * Replace @After with @Before
 */
@RunWith(JUnit4.class)
class J4AfterToBefore {
  // BUG: Diagnostic contains: @Before
  @After
  protected void setUp() {}
}

/**
 * Replace @AfterClass with @BeforeClass
 */
@RunWith(JUnit4.class)
class J4AfterClassToBeforeClass {
  // BUG: Diagnostic contains: @BeforeClass
  @AfterClass
  protected void setUp() {}
}

class BaseTestClass {
  void setUp() {}
}

/**
 * This is the ambiguous case that we want the developer to make the determination as to
 * whether to rename setUp()
 */
@RunWith(JUnit4.class)
class J4Inherit extends BaseTestClass {
  // BUG: Diagnostic contains: @Before
  protected void setUp() {}
}

/**
 * setUp() method overrides parent method with @Override, but that method isn't @Before in the
 * superclass
 */
@RunWith(JUnit4.class)
class J4OverriddenSetUp extends BaseTestClass {
  // BUG: Diagnostic contains: @Before
  @Override protected void setUp() {}
}

@RunWith(JUnit4.class)
class J4OverriddenSetUpPublic extends BaseTestClass {
  // BUG: Diagnostic contains: @Before
  @Override public void setUp() {}
}

