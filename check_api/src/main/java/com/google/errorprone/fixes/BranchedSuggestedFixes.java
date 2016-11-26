/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

import com.google.common.collect.ImmutableList;

/**
 * Helper class for accumulating a branching tree of alternative fixes designed to help build as set
 * of potential fixes with different options in them.
 *
 * <p>Consider building a list of fixes from a set of operations A followed by B or C then D or E.
 * The resulting list should be ABD, ACD, ABE, ACE.
 *
 * <pre>{@code
 * BranchedSuggestedFixes a = BranchedSuggestedFixes.builder()
 *   .startWith(A)
 *   .then()
 *   .addOption(B)
 *   .addOption(C)
 *   .then()
 *   .addOption(D)
 *   .addOption(E)
 *   .build();
 * }</pre>
 *
 * This class assumes that in order to build a valid set of fixes you must make some progress at
 * each branch. So two calls to branch with no merges in between will result in an empty list of
 * fixes at the end.
 *
 * @author andrewrice@google.com (Andrew Rice)
 */
public class BranchedSuggestedFixes {

  private final ImmutableList<SuggestedFix> fixes;

  private BranchedSuggestedFixes(ImmutableList<SuggestedFix> fixes) {
    this.fixes = fixes;
  }

  public ImmutableList<SuggestedFix> getFixes() {
    return fixes;
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Builder class for BranchedSuggestedFixes */
  public static class Builder {

    private ImmutableList.Builder<SuggestedFix> builder = ImmutableList.builder();
    private ImmutableList<SuggestedFix> savedList = ImmutableList.of();

    public Builder startWith(SuggestedFix fix) {
      savedList = ImmutableList.of();
      builder = ImmutableList.<SuggestedFix>builder().add(fix);
      return this;
    }

    public Builder addOption(SuggestedFix fix) {
      if (!savedList.isEmpty()) {
        for (SuggestedFix s : savedList) {
          builder.add(SuggestedFix.builder().merge(s).merge(fix).build());
        }
      }
      return this;
    }

    public Builder then() {
      savedList = builder.build();
      builder = ImmutableList.builder();
      return this;
    }

    public BranchedSuggestedFixes build() {
      return new BranchedSuggestedFixes(builder.build());
    }
  }
}
