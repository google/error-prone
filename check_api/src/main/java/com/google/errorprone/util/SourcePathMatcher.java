/*
 * Copyright 2026 The Error Prone Authors.
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.errorprone.VisitorState;
import java.util.Arrays;
import org.jspecify.annotations.Nullable;

/// Matcher for canonical source file paths supporting exact source file paths and directory
/// prefixes.
///
/// Matches are evaluated against canonical repository/workspace-relative paths as returned by
/// [ASTHelpers#getSourcePath].
///
/// ## Supported Pattern Syntax
///
/// * **Exact file paths**: Matches the exact relative path for a `.java`, `.kt`, or `.kts` source
///   file (e.g., `java/com/google/foo/Bar.java`, `java/com/google/foo/Bar.kt`).
/// * **Directory prefixes**: Trailing `/` matches any file in the directory hierarchy (e.g.,
///   `java/com/google/foo/`).
///
/// Patterns must be repository-relative and must not start with a leading `/`. Wildcards (`*`, `?`)
/// are not supported.
public final class SourcePathMatcher {

  /// Normalizes a source path by stripping repository workspace roots and build output directories
  /// to produce a canonical repository-relative path.
  static String canonicalizePath(String path) {
    checkNotNull(path, "path");

    if (path.startsWith("file://")) {
      path = path.substring("file://".length());
    } else if (path.startsWith("file:")) {
      path = path.substring("file:".length());
    }

    // Bazel execroots in sandbox: `/execroot/<workspace_name>/...` (e.g.
    // `/execroot/_main/path/Foo.java` -> `path/Foo.java`).
    if (path.startsWith("/execroot/")) {
      String afterWorkspace = substringAfter(path.substring("/execroot/".length()), '/');
      if (afterWorkspace != null) {
        path = afterWorkspace;
      }
    }

    // Bazel runfiles in sandbox: `/runfiles/<workspace_name>/...` (e.g.
    // `/runfiles/my_workspace/path/Foo.java` -> `path/Foo.java`).
    if (path.startsWith("/runfiles/")) {
      String afterWorkspace = substringAfter(path.substring("/runfiles/".length()), '/');
      if (afterWorkspace != null) {
        path = afterWorkspace;
      }
    }

    if (path.startsWith("/")) {
      path = path.substring(1);
    }

    // Build output directories: `(blaze-out|bazel-out)/<config-spec>/(bin|genfiles)/...`
    // for generated files or test binaries (`.../<test>.runfiles/<workspace_name>/...`).
    if (path.startsWith("blaze-out/") || path.startsWith("bazel-out/")) {
      String afterDotRunfiles = substringAfter(path, ".runfiles/");
      if (afterDotRunfiles != null) {
        String afterWorkspace = substringAfter(afterDotRunfiles, '/');
        if (afterWorkspace != null) {
          path = afterWorkspace;
        }
      } else {
        int slash2 = nthIndexOf(path, '/', 2);
        int slash3 = nthIndexOf(path, '/', 3);
        if (slash2 >= 0 && slash3 >= 0) {
          String dir = path.substring(slash2 + 1, slash3);
          if (dir.equals("bin") || dir.equals("genfiles")) {
            path = path.substring(slash3 + 1);
          }
        }
      }
    }

    return path;
  }

  private static @Nullable String substringAfter(String str, String delimiter) {
    int pos = str.indexOf(delimiter);
    return pos >= 0 ? str.substring(pos + delimiter.length()) : null;
  }

  private static @Nullable String substringAfter(String str, char delimiter) {
    int pos = str.indexOf(delimiter);
    return pos >= 0 ? str.substring(pos + 1) : null;
  }

  private static int nthIndexOf(String str, char ch, int n) {
    int pos = -1;
    for (int i = 0; i < n; i++) {
      pos = str.indexOf(ch, pos + 1);
      if (pos < 0) {
        return -1;
      }
    }
    return pos;
  }

  private static final SourcePathMatcher EMPTY =
      new SourcePathMatcher(ImmutableSet.of(), ImmutableSortedSet.of());

  private final ImmutableSet<String> exactPaths;
  private final ImmutableSortedSet<String> prefixes;

  private SourcePathMatcher(ImmutableSet<String> exactPaths, ImmutableSortedSet<String> prefixes) {
    this.exactPaths = exactPaths;
    this.prefixes = prefixes;
  }

  /// Returns an empty [SourcePathMatcher].
  public static SourcePathMatcher empty() {
    return EMPTY;
  }

  /// Creates a [SourcePathMatcher] from the given path patterns (exact file paths or directory
  /// prefixes ending with `/`).
  public static SourcePathMatcher create(String... pathPatterns) {
    return create(Arrays.asList(pathPatterns));
  }

  /// Creates a [SourcePathMatcher] from the given path patterns (exact file paths or directory
  /// prefixes ending with `/`).
  public static SourcePathMatcher create(Iterable<String> pathPatterns) {
    ImmutableSet.Builder<String> exactBuilder = ImmutableSet.builder();
    ImmutableList.Builder<String> prefixesBuilder = ImmutableList.builder();

    for (String pattern : pathPatterns) {
      parsePath(pattern, exactBuilder, prefixesBuilder);
    }

    ImmutableSet<String> exact = exactBuilder.build();
    ImmutableSortedSet<String> pref = sortAndPrunePrefixes(prefixesBuilder.build());

    if (exact.isEmpty() && pref.isEmpty()) {
      return EMPTY;
    }
    return new SourcePathMatcher(exact, pref);
  }

  // Sorts directory prefixes and prunes subsumed descendant prefixes. Because directory prefixes
  // end with '/', any descendant prefix (e.g. `a/b/c/`) sorts lexicographically after its ancestor
  // prefix (e.g. `a/b/`). Pruning redundant prefixes ensures no prefix in the list is a prefix of
  // another, which allows `matchesPrefix` to test only the predecessor candidate from `floor`.
  private static ImmutableSortedSet<String> sortAndPrunePrefixes(Iterable<String> prefixes) {
    ImmutableList<String> sorted = ImmutableList.sortedCopyOf(prefixes);
    ImmutableSortedSet.Builder<String> pruned = ImmutableSortedSet.naturalOrder();
    String last = null;
    for (String prefix : sorted) {
      if (last == null || !prefix.startsWith(last)) {
        pruned.add(prefix);
        last = prefix;
      }
    }
    return pruned.build();
  }

  private static void parsePath(
      String pattern,
      ImmutableSet.Builder<String> exactBuilder,
      ImmutableList.Builder<String> prefixesBuilder) {
    checkNotNull(pattern, "pattern");
    checkArgument(!pattern.isEmpty(), "Path pattern must not be empty");
    checkArgument(
        !pattern.startsWith("/"),
        "Path patterns must be repository-relative and must not start with '/': %s",
        pattern);
    checkArgument(!pattern.contains("//"), "Path patterns must not contain '//': %s", pattern);
    checkArgument(!pattern.contains(".."), "Path patterns must not contain '..': %s", pattern);
    checkArgument(
        !pattern.startsWith("./") && !pattern.contains("/./"),
        "Path patterns must not contain './': %s",
        pattern);
    checkArgument(
        !pattern.contains("*") && !pattern.contains("?"),
        "Wildcards are not supported: %s",
        pattern);

    if (pattern.endsWith("/")) {
      checkArgument(!pattern.equals("/"), "Path pattern must not be '/': %s", pattern);
      prefixesBuilder.add(pattern);
      return;
    }

    checkArgument(
        pattern.endsWith(".java") || pattern.endsWith(".kt") || pattern.endsWith(".kts"),
        "Exact path pattern must end with '.java', '.kt', or '.kts' (use a trailing '/' for"
            + " directory prefixes): %s",
        pattern);
    exactBuilder.add(pattern);
  }

  /// Returns `true` if this matcher contains no patterns.
  private boolean isEmpty() {
    return exactPaths.isEmpty() && prefixes.isEmpty();
  }

  /// Returns `true` if the given path matches any of the patterns in this matcher.
  public boolean matches(String path) {
    checkNotNull(path, "path");
    if (isEmpty()) {
      return false;
    }
    return matchesCanonical(canonicalizePath(path));
  }

  /// Returns `true` if the compilation unit in `state` matches any of the patterns in this matcher.
  public boolean matches(VisitorState state) {
    String canonicalPath = ASTHelpers.getSourcePath(state);
    return matchesCanonical(canonicalPath);
  }

  private boolean matchesCanonical(String canonicalPath) {
    if (isEmpty()) {
      return false;
    }
    if (!exactPaths.isEmpty() && exactPaths.contains(canonicalPath)) {
      return true;
    }
    return matchesPrefix(canonicalPath);
  }

  private boolean matchesPrefix(String canonicalPath) {
    String prefix = prefixes.floor(canonicalPath);
    return prefix != null && canonicalPath.startsWith(prefix);
  }
}
