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

package com.google.errorprone.util;

import com.google.auto.value.AutoBuilder;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.sun.source.tree.Tree;

/** Class to hold AST nodes annotated with the comments that are associated with them */
public record Commented(
    Tree tree,
    ImmutableList<ErrorProneComment> beforeComments,
    ImmutableList<ErrorProneComment> afterComments) {
  /** Identifies the position of a comment relative to the associated treenode. */
  public enum Position {
    BEFORE,
    AFTER,
    ANY
  }

  static Builder builder() {
    return new AutoBuilder_Commented_Builder();
  }

  @AutoBuilder
  abstract static class Builder {

    abstract Builder setTree(Tree tree);

    protected abstract ImmutableList.Builder<ErrorProneComment> beforeCommentsBuilder();

    protected abstract ImmutableList.Builder<ErrorProneComment> afterCommentsBuilder();

    @CanIgnoreReturnValue
    Builder addComment(
        ErrorProneComment comment, int nodePosition, int tokenizingOffset, Position position) {
      ErrorProneComment offsetComment = comment.withOffset(tokenizingOffset);

      if (comment.getSourcePos(0) < nodePosition) {
        if (position.equals(Position.BEFORE) || position.equals(Position.ANY)) {
          beforeCommentsBuilder().add(offsetComment);
        }
      } else {
        if (position.equals(Position.AFTER) || position.equals(Position.ANY)) {
          afterCommentsBuilder().add(offsetComment);
        }
      }
      return this;
    }

    @CanIgnoreReturnValue
    Builder addAllComment(
        Iterable<ErrorProneComment> comments,
        int nodePosition,
        int tokenizingOffset,
        Position position) {
      for (ErrorProneComment comment : comments) {
        addComment(comment, nodePosition, tokenizingOffset, position);
      }
      return this;
    }

    abstract Commented build();
  }
}
