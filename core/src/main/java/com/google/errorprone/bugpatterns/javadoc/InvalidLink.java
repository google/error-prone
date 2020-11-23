/*
 * Copyright 2018 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.javadoc;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.bugpatterns.javadoc.Utils.diagnosticPosition;
import static com.google.errorprone.bugpatterns.javadoc.Utils.getDocComment;
import static com.google.errorprone.bugpatterns.javadoc.Utils.getDocTreePath;
import static com.google.errorprone.bugpatterns.javadoc.Utils.getStartPosition;
import static com.google.errorprone.bugpatterns.javadoc.Utils.replace;
import static com.google.errorprone.matchers.Description.NO_MATCH;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.ErroneousTree;
import com.sun.source.doctree.LinkTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.DocTreePath;
import com.sun.source.util.DocTreePathScanner;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.tree.DCTree.DCDocComment;
import com.sun.tools.javac.tree.DCTree.DCText;
import com.sun.tools.javac.util.Log;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.lang.model.element.Element;

/** Finds some common errors in {@literal @}link tags. */
@BugPattern(name = "InvalidLink", summary = "This @link tag looks wrong.", severity = WARNING)
public final class InvalidLink extends BugChecker
    implements ClassTreeMatcher, MethodTreeMatcher, VariableTreeMatcher {

  private static final Pattern EXTRACT_TARGET = Pattern.compile("([^}]*)}");

  @Override
  public Description matchClass(ClassTree classTree, VisitorState state) {
    DocTreePath path = getDocTreePath(state);
    if (path != null) {
      new InvalidLinkChecker(state, classTree, /* parameters= */ ImmutableSet.of())
          .scan(path, null);
    }
    return NO_MATCH;
  }

  @Override
  public Description matchMethod(MethodTree methodTree, VisitorState state) {
    DocTreePath path = getDocTreePath(state);
    if (path != null) {
      ImmutableSet<String> parameters =
          methodTree.getParameters().stream()
              .map(v -> v.getName().toString())
              .collect(toImmutableSet());
      new InvalidLinkChecker(state, methodTree, parameters).scan(path, null);
    }
    return NO_MATCH;
  }

  @Override
  public Description matchVariable(VariableTree variableTree, VisitorState state) {
    DocTreePath path = getDocTreePath(state);
    if (path != null) {
      new InvalidLinkChecker(state, variableTree, /* parameters= */ ImmutableSet.of())
          .scan(path, null);
    }
    return NO_MATCH;
  }

  private final class InvalidLinkChecker extends DocTreePathScanner<Void, Void> {
    private final VisitorState state;
    private final Tree tree;
    private final ImmutableSet<String> parameters;

    private InvalidLinkChecker(VisitorState state, Tree tree, ImmutableSet<String> parameters) {
      this.state = state;
      this.tree = tree;
      this.parameters = parameters;
    }

    @Override
    public Void visitErroneous(ErroneousTree erroneousTree, Void unused) {
      String body = erroneousTree.getBody();
      if (body.startsWith("{@link ")) {
        DocTree parent = getCurrentPath().getParentPath().getLeaf();
        if (!(parent instanceof DCDocComment)) {
          return null;
        }
        DCDocComment comment = (DCDocComment) parent;
        int nextIndex = comment.getFullBody().indexOf(erroneousTree) + 1;
        if (nextIndex >= comment.getFullBody().size()) {
          return null;
        }
        DocTree next = comment.getFullBody().get(nextIndex);
        Matcher match = EXTRACT_TARGET.matcher(next.toString());
        if (!match.matches()) {
          return null;
        }
        String target = match.group(1);
        String reference = erroneousTree.getBody().replaceFirst("\\{@link ", "");
        String fixedLink = fixLink(reference, target);
        DCDocComment docComment = getDocComment(state, tree);
        if (!(next instanceof DCText)) {
          return null;
        }
        DCText nextText = (DCText) next;
        int endPos = docComment.comment.getSourcePos(nextText.pos + nextText.text.indexOf("}") + 1);
        SuggestedFix fix =
            SuggestedFix.replace(getStartPosition(erroneousTree, state), endPos, fixedLink);
        state.reportMatch(
            buildDescription(diagnosticPosition(getCurrentPath(), state))
                .setMessage("{@link} cannot be used for HTTP links. Use an <a> tag instead.")
                .addFix(fix)
                .build());
      }
      return super.visitErroneous(erroneousTree, null);
    }

    @Override
    public Void visitLink(LinkTree linkTree, Void unused) {
      if (linkTree.getReference() == null) {
        return super.visitLink(linkTree, null);
      }
      String reference = linkTree.getReference().getSignature();
      Element element = null;
      Log log = Log.instance(state.context);
      // Install a deferred diagnostic handler before calling DocTrees.getElement(DocTreePath)
      // TODO(cushon): revert if https://bugs.openjdk.java.net/browse/JDK-8248117 is fixed
      Log.DeferredDiagnosticHandler deferredDiagnosticHandler =
          new Log.DeferredDiagnosticHandler(log);
      try {
        element =
            JavacTrees.instance(state.context)
                .getElement(new DocTreePath(getCurrentPath(), linkTree.getReference()));
      } catch (NullPointerException e) {
        // TODO(cushon): remove once JDK 12 is the minimum supported version
        // https://bugs.openjdk.java.net/browse/JDK-8200432
      } finally {
        log.popDiagnosticHandler(deferredDiagnosticHandler);
      }
      // Don't warn about fully qualified types; they won't always be known at compile-time.
      if (element != null || reference.contains(".")) {
        return super.visitLink(linkTree, null);
      }
      if (parameters.contains(reference)) {
        String message =
            String.format(
                "`%s` is a parameter; use {@code paramName} to refer to parameters inline.",
                reference);
        state.reportMatch(
            buildDescription(diagnosticPosition(getCurrentPath(), state))
                .setMessage(message)
                .addFix(replace(linkTree, String.format("{@code %s}", reference), state))
                .build());
        return super.visitLink(linkTree, null);
      }
      if (Character.isLowerCase(reference.charAt(0)) && !reference.contains("#")) {
        String message =
            String.format(
                "`%s` is not known here. Should it be a reference to a method?", reference);
        // TODO(ghm): Find a way (JavacTrees#searchMethod?) to check whether the suggestion is
        // valid.
        int pos = getStartPosition(linkTree.getReference(), state);
        state.reportMatch(
            buildDescription(diagnosticPosition(getCurrentPath(), state))
                .setMessage(message)
                .addFix(SuggestedFix.replace(pos, pos, "#"))
                .build());
        return super.visitLink(linkTree, null);
      }
      if (reference.charAt(0) == '#') {
        state.reportMatch(
            buildDescription(diagnosticPosition(getCurrentPath(), state))
                .setMessage(
                    String.format(
                        "The reference `%s` to a method doesn't resolve to anything. Is it"
                            + " misspelt, or is the parameter list not correct? See"
                            + " https://docs.oracle.com/javase/8/docs/technotes/tools/unix/javadoc.html#JSSOR654"
                            + " for documentation on how to form method links.",
                        reference))
                .build());
      }
      // TODO(ghm): If this is a method reference, we could check whether class is available but the
      // method isn't.
      return super.visitLink(linkTree, null);
    }

    private String fixLink(String reference, String label) {
      if (label.isEmpty()) {
        label = "link";
      }
      return String.format("<a href=\"%s\">%s</a>", reference, label);
    }
  }
}
