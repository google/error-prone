/*
 * Copyright 2011 Google Inc. All Rights Reserved.
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

package com.google.errorprone;

import com.sun.source.util.TreePath;
import com.sun.tools.javac.util.Context;

/**
 * Carries the current state of the visitor as it visits tree nodes.
 * @author alexeagle@google.com (Alex Eagle)
 */
public class SearchingVisitorState extends VisitorState {

  private final MatchListener listener;

  public SearchingVisitorState(Context context, MatchListener listener, TreePath path) {
    super(context, path);
    this.listener = listener;
  }

  public SearchingVisitorState(Context context, MatchListener listener) {
    this(context, listener, null);
  }

  public MatchListener getListener() {
    return listener;
  }

  @Override
  public SearchingVisitorState withPath(TreePath path) {
    return new SearchingVisitorState(context, listener, path);
  }
}
