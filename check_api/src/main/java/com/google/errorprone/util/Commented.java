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

    protected abstract ImmutableList.Builder<Comment> beforeCommentsBuilder();

    protected abstract ImmutableList.Builder<Comment> afterCommentsBuilder();

    Builder<T> addComment(Comment comment, int nodePosition, int tokenizingOffset) {
      OffsetComment offsetComment = new OffsetComment(comment, tokenizingOffset);

      if (comment.getSourcePos(0) < nodePosition) {
        beforeCommentsBuilder().add(offsetComment);
      } else {
        afterCommentsBuilder().add(offsetComment);
      }
      return this;
    }

    Builder<T> addAllComment(
        Iterable<? extends Comment> comments, int nodePosition, int tokenizingOffset) {
      for (Comment comment : comments) {
        addComment(comment, nodePosition, tokenizingOffset);
      }
      return this;
    }

    abstract Commented<T> build();
  }

  private static final class OffsetComment implements Comment {

    private final Comment wrapped;
    private final int offset;

    private OffsetComment(Comment wrapped, int offset) {
      this.wrapped = wrapped;
      this.offset = offset;
    }

    @Override
    public String getText() {
      return wrapped.getText();
    }

    @Override
    public int getSourcePos(int i) {
      return wrapped.getSourcePos(i) + offset;
    }

    @Override
    public CommentStyle getStyle() {
      return wrapped.getStyle();
    }

    @Override
    public boolean isDeprecated() {
      return false;
    }
  }
}
