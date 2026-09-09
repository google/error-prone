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

package com.google.errorprone;

import static com.google.common.truth.Truth.assertThat;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.matchers.Suppressible;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Name;
import java.lang.annotation.Annotation;
import java.time.Duration;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Fails when a check's run time stops reaching the report it belongs in.
 *
 * <p>{@link ErrorProneTimings#span} reaches a check's state one way for a {@link BugChecker}, which
 * keeps the state in a field, and another for a {@link Suppressible} that does not. {@link
 * TimedCheck} covers the first and {@link NamedCheck} the second.
 */
@RunWith(JUnit4.class)
public final class ErrorProneTimingsTest {

  /** A {@link Suppressible} that answers only what the timings read. */
  private static final class NamedCheck implements Suppressible {

    private final String canonicalName;

    NamedCheck(String canonicalName) {
      this.canonicalName = canonicalName;
    }

    @Override
    public String canonicalName() {
      return canonicalName;
    }

    @Override
    public Set<String> allNames() {
      return Set.of(canonicalName);
    }

    @Override
    public boolean supportsSuppressWarnings() {
      return true;
    }

    @Override
    public Set<Class<? extends Annotation>> customSuppressionAnnotations() {
      return Set.of();
    }

    @Override
    public boolean suppressedByAnyOf(Set<Name> annotations, VisitorState state) {
      return false;
    }
  }

  /** A {@link BugChecker} that exists to be timed. */
  @BugPattern(summary = "A check that exists to be timed.", severity = WARNING)
  public static final class TimedCheck extends BugChecker {}

  private final ErrorProneTimings timings = ErrorProneTimings.instance(new Context());

  private void run(Suppressible check, int invocations) throws Exception {
    for (int i = 0; i < invocations; i++) {
      timings.span(check).close();
    }
  }

  /** Runs one span and returns the state it used, which is closed by the time it comes back. */
  private AutoCloseable runOnce(Suppressible check) throws Exception {
    AutoCloseable span = timings.span(check);
    span.close();
    return span;
  }

  @Test
  public void aCheckThatIsNotABugCheckerIsStillTimed() throws Exception {
    run(new NamedCheck("Alpha"), 3);

    assertThat(timings.counts()).containsExactly("Alpha", 3L);
    assertThat(timings.timings().keySet()).containsExactly("Alpha");
  }

  @Test
  public void aBugCheckerUsesItsOwnSlot() throws Exception {
    TimedCheck check = new TimedCheck();

    assertThat(runOnce(check)).isSameInstanceAs(check.checkTiming());
    assertThat(timings.counts()).containsExactly("TimedCheck", 1L);
  }

  @Test
  public void twoChecksSharingACanonicalNameAreSummed() throws Exception {
    TimedCheck first = new TimedCheck();
    TimedCheck second = new TimedCheck();
    assertThat(first.checkTiming()).isNotSameInstanceAs(second.checkTiming());

    run(first, 3);
    run(second, 5);

    assertThat(timings.counts()).containsExactly("TimedCheck", 8L);
  }

  @Test
  public void aNameReportedThroughBothArmsIsSummed() throws Exception {
    TimedCheck owned = new TimedCheck();
    NamedCheck unowned = new NamedCheck("TimedCheck");

    AutoCloseable ownedSlot = runOnce(owned);
    AutoCloseable unownedSlot = runOnce(unowned);

    assertThat(ownedSlot).isNotSameInstanceAs(unownedSlot);
    assertThat(timings.counts()).containsExactly("TimedCheck", 2L);
  }

  @Test
  public void everyColumnFoldsAcrossTheSlotsOfOneName() throws Exception {
    // Two slots under one name, and a second name with one slot: a fold across all names rather
    // than within one would report both keys as 12000 ns.
    ((CheckTiming) timings.span(new TimedCheck())).closeWith(1000);
    ((CheckTiming) timings.span(new TimedCheck())).closeWith(4000);
    ((CheckTiming) timings.span(new NamedCheck("Other"))).closeWith(7000);

    assertThat(timings.counts()).containsExactly("TimedCheck", 2L, "Other", 1L);
    assertThat(timings.timings())
        .containsExactly("TimedCheck", Duration.ofNanos(5000), "Other", Duration.ofNanos(7000));
    assertThat(timings.maxNanos()).containsExactly("TimedCheck", 4000L, "Other", 7000L);
  }

  @Test
  public void twoUnownedChecksSharingANameShareOneSlot() throws Exception {
    AutoCloseable first = runOnce(new NamedCheck("Shared"));
    AutoCloseable second = runOnce(new NamedCheck("Shared"));
    AutoCloseable other = runOnce(new NamedCheck("Other"));

    assertThat(first).isSameInstanceAs(second);
    assertThat(first).isNotSameInstanceAs(other);
    assertThat(timings.counts()).containsExactly("Shared", 2L, "Other", 1L);
  }
}
