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

package com.google.errorprone.util;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.parser.Tokens.Comment;

/** Class to hold AST nodes annotated with the comments that are associated with them */
@AutoValue
public abstract class Commented<T extends Tree> {

  public abstract T tree();

  public abstract ImmutableList<Comment> beforeComments();

  public abstract ImmutableList<Comment> afterComments();

  static <T extends Tree> Builder builder() {
    return new AutoValue_Commented.Builder<T>();
  }

  @AutoValue.Builder
  abstract static class Builder<T extends Tree> {

    abstract Builder<T> setTree(T tree);

    abstract ImmutableList.Builder<Comment> beforeCommentsBuilder();

    abstract ImmutableList.Builder<Comment> afterCommentsBuilder();

    Builder<T> addComment(Comment comment, int nodePosition) {
      if (comment.getSourcePos(0) < nodePosition) {
        beforeCommentsBuilder().add(comment);
      } else {
        afterCommentsBuilder().add(comment);
      }
      return this;
    }

    Builder<T> addAllComment(Iterable<? extends Comment> comments, int nodePosition) {
      for (Comment comment : comments) {
        addComment(comment, nodePosition);
      }
      return this;
    }

    abstract Commented<T> build();
  }
}
