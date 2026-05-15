/*
 * Copyright 2011 The Error Prone Authors.
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

package com.google.errorprone.fixes;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.fixes.Replacements.CoalescePolicy;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collector;
import org.jspecify.annotations.Nullable;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
@AutoValue
public abstract class SuggestedFix implements Fix {

  abstract ImmutableList<FixOperation> fixes();

  private static SuggestedFix create(SuggestedFix.Builder builder) {
    return new AutoValue_SuggestedFix(
        builder.coalescePolicy,
        ImmutableList.copyOf(builder.fixes),
        ImmutableSet.copyOf(builder.importsToAdd),
        ImmutableSet.copyOf(builder.importsToRemove),
        builder.shortDescription);
  }

  @Override
  public boolean isEmpty() {
    return fixes().isEmpty() && getImportsToAdd().isEmpty() && getImportsToRemove().isEmpty();
  }

  @Override
  public abstract ImmutableSet<String> getImportsToAdd();

  @Override
  public abstract ImmutableSet<String> getImportsToRemove();

  @Override
  public String toString(JCCompilationUnit compilationUnit) {
    StringBuilder result = new StringBuilder("replace ");
    for (Replacement replacement : getReplacements(ErrorProneEndPosTable.create(compilationUnit))) {
      result.append(
          String.format(
              "position %d:%d with \"%s\" ",
              replacement.startPosition(), replacement.endPosition(), replacement.replaceWith()));
    }
    return result.toString();
  }

  @Override
  public abstract String getShortDescription();

  @Memoized
  @Override
  public abstract int hashCode();

  @Override
  public ImmutableSet<Replacement> getReplacements(ErrorProneEndPosTable endPositions) {
    if (endPositions == null) {
      throw new IllegalArgumentException(
          "Cannot produce correct replacements without endPositions.");
    }
    Replacements replacements = new Replacements();
    for (FixOperation fix : fixes()) {
      replacements.add(fix.getReplacement(endPositions), getCoalescePolicy());
    }
    return replacements.ascending();
  }

  /** {@link Builder#replace(Tree, String)} */
  public static SuggestedFix replace(Tree tree, String replaceWith) {
    return builder().replace(tree, replaceWith).build();
  }

  /**
   * Replace the characters from startPos, inclusive, until endPos, exclusive, with the given
   * string.
   *
   * @param startPos The position from which to start replacing, inclusive
   * @param endPos The position at which to end replacing, exclusive
   * @param replaceWith The string to replace with
   */
  public static SuggestedFix replace(int startPos, int endPos, String replaceWith) {
    return builder().replace(startPos, endPos, replaceWith).build();
  }

  /**
   * Replace a tree node with a string, but adjust the start and end positions as well. For example,
   * if the tree node begins at index 10 and ends at index 30, this call will replace the characters
   * at index 15 through 25 with "replacement":
   *
   * <pre>
   * {@code fix.replace(node, "replacement", 5, -5)}
   * </pre>
   *
   * @param node The tree node to replace
   * @param replaceWith The string to replace with
   * @param startPosAdjustment The adjustment to add to the start position (negative is OK)
   * @param endPosAdjustment The adjustment to add to the end position (negative is OK)
   */
  public static SuggestedFix replace(
      Tree node, String replaceWith, int startPosAdjustment, int endPosAdjustment) {
    return builder().replace(node, replaceWith, startPosAdjustment, endPosAdjustment).build();
  }

  /** {@link Builder#prefixWith(Tree, String)} */
  public static SuggestedFix prefixWith(Tree node, String prefix) {
    return builder().prefixWith(node, prefix).build();
  }

  /** {@link Builder#postfixWith(Tree, String)} */
  public static SuggestedFix postfixWith(Tree node, String postfix) {
    return builder().postfixWith(node, postfix).build();
  }

  /** {@link Builder#delete(Tree)} */
  public static SuggestedFix delete(Tree node) {
    return delete(ErrorPronePosition.from(node));
  }

  /** {@link Builder#delete(ErrorPronePosition)} */
  public static SuggestedFix delete(ErrorPronePosition position) {
    return builder().delete(position).build();
  }

  /** {@link Builder#swap(Tree, Tree, VisitorState)} */
  public static SuggestedFix swap(Tree node1, Tree node2, VisitorState state) {
    return builder().swap(node1, node2, state).build();
  }

  private static final SuggestedFix EMPTY = builder().build();

  /** Creates an empty {@link SuggestedFix}. */
  public static SuggestedFix emptyFix() {
    return EMPTY;
  }

  public static SuggestedFix merge(SuggestedFix first, SuggestedFix second, SuggestedFix... more) {
    var builder = builder().merge(first).merge(second);
    for (SuggestedFix fix : more) {
      builder.merge(fix);
    }
    return builder.build();
  }

  public static Collector<SuggestedFix, ?, SuggestedFix> mergeFixes() {
    return Collector.of(
        SuggestedFix::builder, Builder::merge, Builder::merge, SuggestedFix.Builder::build);
  }

  public static Builder builder() {
    return new Builder();
  }

  public Builder toBuilder() {
    return SuggestedFix.builder().merge(this);
  }

  /** Builds {@link SuggestedFix}s. */
  public static class Builder {

    private final List<FixOperation> fixes = new ArrayList<>();
    private final Set<String> importsToAdd = new LinkedHashSet<>();
    private final Set<String> importsToRemove = new LinkedHashSet<>();
    private CoalescePolicy coalescePolicy = CoalescePolicy.EXISTING_FIRST;
    private String shortDescription = "";

    protected Builder() {}

    public boolean isEmpty() {
      return fixes.isEmpty() && importsToAdd.isEmpty() && importsToRemove.isEmpty();
    }

    public SuggestedFix build() {
      return create(this);
    }

    @CanIgnoreReturnValue
    private Builder with(FixOperation fix) {
      fixes.add(fix);
      return this;
    }

    /**
     * Sets a custom short description for this fix. This is useful for differentiating multiple
     * fixes from the same finding.
     *
     * <p>Should be limited to one sentence.
     */
    @CanIgnoreReturnValue
    public Builder setShortDescription(String shortDescription) {
      this.shortDescription = shortDescription;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setCoalescePolicy(CoalescePolicy coalescePolicy) {
      this.coalescePolicy = coalescePolicy;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder replace(Tree node, String replaceWith) {
      return replace(ErrorPronePosition.from(node), replaceWith);
    }

    @CanIgnoreReturnValue
    public Builder replace(ErrorPronePosition position, String replaceWith) {
      checkExplicitSource(position.getTree());
      return with(ReplacementFix.create(position, replaceWith));
    }

    /**
     * Replace the characters from startPos, inclusive, until endPos, exclusive, with the given
     * string.
     *
     * @param startPos The position from which to start replacing, inclusive
     * @param endPos The position at which to end replacing, exclusive
     * @param replaceWith The string to replace with
     */
    @CanIgnoreReturnValue
    public Builder replace(int startPos, int endPos, String replaceWith) {
      IndexedPosition pos = new IndexedPosition(startPos, endPos);
      return with(ReplacementFix.create(pos, replaceWith));
    }

    /**
     * Replace a tree node with a string, but adjust the start and end positions as well. For
     * example, if the tree node begins at index 10 and ends at index 30, this call will replace the
     * characters at index 15 through 25 with "replacement":
     *
     * <pre>
     * {@code fix.replace(node, "replacement", 5, -5)}
     * </pre>
     *
     * @param node The tree node to replace
     * @param replaceWith The string to replace with
     * @param startPosAdjustment The adjustment to add to the start position (negative is OK)
     * @param endPosAdjustment The adjustment to add to the end position (negative is OK)
     */
    @CanIgnoreReturnValue
    public Builder replace(
        Tree node, String replaceWith, int startPosAdjustment, int endPosAdjustment) {
      checkExplicitSource(node);
      return with(
          ReplacementFix.create(
              new AdjustedPosition((JCTree) node, startPosAdjustment, endPosAdjustment),
              replaceWith));
    }

    @CanIgnoreReturnValue
    public Builder prefixWith(Tree node, String prefix) {
      return prefixWith(ErrorPronePosition.from(node), prefix);
    }

    @CanIgnoreReturnValue
    public Builder prefixWith(ErrorPronePosition position, String prefix) {
      checkNotSyntheticConstructor(position.getTree());
      return with(PrefixInsertion.create(position, prefix));
    }

    @CanIgnoreReturnValue
    public Builder postfixWith(Tree node, String postfix) {
      return postfixWith(ErrorPronePosition.from(node), postfix);
    }

    @CanIgnoreReturnValue
    public Builder postfixWith(ErrorPronePosition position, String postfix) {
      checkExplicitSource(position.getTree());
      return with(PostfixInsertion.create(position, postfix));
    }

    @CanIgnoreReturnValue
    public Builder delete(Tree node) {
      return delete(ErrorPronePosition.from(node));
    }

    @CanIgnoreReturnValue
    public Builder delete(ErrorPronePosition node) {
      checkExplicitSource(node.getTree());
      return replace(node, "");
    }

    @CanIgnoreReturnValue
    public Builder swap(Tree node1, Tree node2, VisitorState state) {
      checkExplicitSource(node1);
      checkExplicitSource(node2);
      fixes.add(
          ReplacementFix.create(ErrorPronePosition.from(node1), state.getSourceForNode(node2)));
      fixes.add(
          ReplacementFix.create(ErrorPronePosition.from(node2), state.getSourceForNode(node1)));
      return this;
    }

    /**
     * Add an import statement as part of this SuggestedFix. Import string should be of the form
     * "foo.bar.SomeClass".
     */
    @CanIgnoreReturnValue
    public Builder addImport(String importString) {
      importsToAdd.add("import " + importString);
      return this;
    }

    /**
     * Add a static import statement as part of this SuggestedFix. Import string should be of the
     * form "foo.bar.SomeClass.someMethod" or "foo.bar.SomeClass.SOME_FIELD".
     */
    @CanIgnoreReturnValue
    public Builder addStaticImport(String importString) {
      importsToAdd.add("import static " + importString);
      return this;
    }

    /**
     * Remove an import statement as part of this SuggestedFix. Import string should be of the form
     * "foo.bar.SomeClass".
     */
    @CanIgnoreReturnValue
    public Builder removeImport(String importString) {
      importsToRemove.add("import " + importString);
      return this;
    }

    /**
     * Remove a static import statement as part of this SuggestedFix. Import string should be of the
     * form "foo.bar.SomeClass.someMethod" or "foo.bar.SomeClass.SOME_FIELD".
     */
    @CanIgnoreReturnValue
    public Builder removeStaticImport(String importString) {
      importsToRemove.add("import static " + importString);
      return this;
    }

    /**
     * Merges all edits from {@code other} into {@code this}. If {@code other} is null, do nothing.
     */
    @CanIgnoreReturnValue
    public Builder merge(@Nullable Builder other) {
      if (other == null) {
        return this;
      }
      if (shortDescription.isEmpty()) {
        shortDescription = other.shortDescription;
      }
      fixes.addAll(other.fixes);
      importsToAdd.addAll(other.importsToAdd);
      importsToRemove.addAll(other.importsToRemove);
      return this;
    }

    /**
     * Merges all edits from {@code other} into {@code this}. If {@code other} is null, do nothing.
     */
    @CanIgnoreReturnValue
    public Builder merge(@Nullable SuggestedFix other) {
      if (other == null) {
        return this;
      }
      if (shortDescription.isEmpty()) {
        shortDescription = other.getShortDescription();
      }
      fixes.addAll(other.fixes());
      importsToAdd.addAll(other.getImportsToAdd());
      importsToRemove.addAll(other.getImportsToRemove());
      return this;
    }

    /**
     * Prevent attempts to modify implicit default constructors, since they are one of the few
     * synthetic constructs added to the AST early enough to be visible from Error Prone.
     */
    private static void checkNotSyntheticConstructor(Tree tree) {
      if (tree instanceof MethodTree methodTree && ASTHelpers.isGeneratedConstructor(methodTree)) {
        throw new IllegalArgumentException("Cannot edit synthetic AST nodes");
      }
    }

    private static void checkExplicitSource(Tree tree) {
      ErrorProneEndPosTable.checkExplicitSource(tree);
      checkNotSyntheticConstructor(tree);
    }
  }

  /** Models a single fix operation. */
  interface FixOperation {
    /** Calculate the replacement operation once end positions are available. */
    Replacement getReplacement(ErrorProneEndPosTable endPositions);
  }

  /** Inserts new text at a specific insertion point (e.g. prefix or postfix). */
  abstract static class InsertionFix implements FixOperation {
    protected abstract int getInsertionIndex(ErrorProneEndPosTable endPositions);

    protected abstract ErrorPronePosition position();

    protected abstract String insertion();

    @Override
    public Replacement getReplacement(ErrorProneEndPosTable endPositions) {
      int insertionIndex = getInsertionIndex(endPositions);
      return Replacement.create(insertionIndex, insertionIndex, insertion());
    }
  }

  @AutoValue
  abstract static class PostfixInsertion extends InsertionFix {

    public static PostfixInsertion create(ErrorPronePosition position, String insertion) {
      checkArgument(position.getStartPosition() >= 0, "invalid start position");
      return new AutoValue_SuggestedFix_PostfixInsertion(position, insertion);
    }

    @Override
    protected int getInsertionIndex(ErrorProneEndPosTable endPositions) {
      return position().getEndPosition(endPositions);
    }
  }

  @AutoValue
  abstract static class PrefixInsertion extends InsertionFix {

    public static PrefixInsertion create(ErrorPronePosition position, String insertion) {
      checkArgument(position.getStartPosition() >= 0, "invalid start position");
      return new AutoValue_SuggestedFix_PrefixInsertion(position, insertion);
    }

    @Override
    protected int getInsertionIndex(ErrorProneEndPosTable endPositions) {
      return position().getStartPosition();
    }
  }

  /** Replaces an entire diagnostic position (from start to end) with the given string. */
  private record ReplacementFix(ErrorPronePosition original, String replacement)
      implements FixOperation {
    ReplacementFix {
      checkArgument(original.getStartPosition() >= 0, "invalid start position");
    }

    static ReplacementFix create(ErrorPronePosition original, String replacement) {
      return new ReplacementFix(original, replacement);
    }

    @Override
    public Replacement getReplacement(ErrorProneEndPosTable endPositions) {
      return Replacement.create(
          original().getStartPosition(), original().getEndPosition(endPositions), replacement());
    }
  }
}
