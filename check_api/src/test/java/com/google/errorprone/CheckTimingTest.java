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
import static org.junit.Assert.assertThrows;

import java.time.Duration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Fails when the sampled estimator stops producing the numbers a timing report prints.
 *
 * <p>Every case supplies the elapsed time through {@link CheckTiming#closeWith} rather than letting
 * the clock decide it, so the sampling schedule and the weighted total are determined. The expected
 * values are worked out from the rule the class documents: invocations are timed one at a time until
 * the count reaches 256, and each timed invocation afterwards counts for the {@code count / 256}
 * invocations it stands in for.
 */
@RunWith(JUnit4.class)
public final class CheckTimingTest {

  /** The invocation count at which the stride first moves off one. */
  private static final int FIRST_SKIP = 512;

  private final CheckTiming timing = new CheckTiming();

  @Test
  public void aTimingThatNeverRanIsEmpty() {
    assertThat(timing.count()).isEqualTo(0);
    assertThat(timing.elapsed()).isEqualTo(Duration.ZERO);
    assertThat(timing.maxNanos()).isEqualTo(0);
  }

  @Test
  public void everyInvocationIsTimedBelowTheStrideThreshold() {
    timing.claim(this);
    for (int i = 0; i < FIRST_SKIP; i++) {
      timing.begin();
      assertThat(timing.sampled()).isTrue();
      timing.closeWith(100);
    }
    assertThat(timing.count()).isEqualTo(FIRST_SKIP);
    assertThat(timing.elapsed()).isEqualTo(Duration.ofNanos(FIRST_SKIP * 100L));
  }

  @Test
  public void aSampleCountsForTheInvocationsItStandsIn() {
    timing.claim(this);
    for (int i = 0; i < FIRST_SKIP; i++) {
      timing.begin();
      timing.closeWith(100);
    }
    long timedOneAtATime = timing.elapsed().toNanos();

    // The 512th invocation moved the stride to two, so the 513th is skipped.
    timing.begin();
    assertThat(timing.sampled()).isFalse();
    timing.closeWith(999);
    assertThat(timing.elapsed().toNanos()).isEqualTo(timedOneAtATime);

    // The 514th is timed, and stands in for itself and the one that was skipped.
    timing.begin();
    assertThat(timing.sampled()).isTrue();
    timing.closeWith(1000);
    assertThat(timing.elapsed().toNanos()).isEqualTo(timedOneAtATime + 2 * 1000);
    assertThat(timing.count()).isEqualTo(FIRST_SKIP + 2);
  }

  @Test
  public void anExpensiveCheckIsTimedOnEveryInvocation() {
    timing.claim(this);
    int invocations = 600;
    for (int i = 0; i < invocations; i++) {
      timing.begin();
      assertThat(timing.sampled()).isTrue();
      timing.closeWith(2000);
    }
    assertThat(timing.count()).isEqualTo(invocations);
    assertThat(timing.elapsed()).isEqualTo(Duration.ofNanos(invocations * 2000L));
  }

  @Test
  public void spansKeepWorkingOnceSamplingStops() {
    timing.claim(this);
    int invocations = 600;
    int skipped = 0;
    for (int i = 0; i < invocations; i++) {
      timing.begin();
      if (!timing.sampled()) {
        skipped++;
      }
      timing.closeWith(100);
    }
    assertThat(skipped).isGreaterThan(0);
    assertThat(timing.count()).isEqualTo(invocations);
  }

  @Test
  public void theLongestInvocationIsKept() {
    timing.claim(this);
    for (long elapsedNanos : new long[] {100, 5000, 200}) {
      timing.begin();
      timing.closeWith(elapsedNanos);
    }
    assertThat(timing.maxNanos()).isEqualTo(5000);
    assertThat(timing.elapsed()).isEqualTo(Duration.ofNanos(5300));
  }

  @Test
  public void aSecondOwnerStartsFromZero() {
    timing.claim(this);
    assertThat(timing.claim(this)).isFalse();
    timing.begin();
    timing.closeWith(4000);
    assertThat(timing.count()).isEqualTo(1);

    assertThat(timing.claim(new Object())).isTrue();
    assertThat(timing.count()).isEqualTo(0);
    assertThat(timing.elapsed()).isEqualTo(Duration.ZERO);
    assertThat(timing.maxNanos()).isEqualTo(0);
  }

  @Test
  public void aSecondCloseRecordsNothing() {
    timing.claim(this);
    timing.begin();
    timing.closeWith(4000);
    long recorded = timing.elapsed().toNanos();

    timing.closeWith(9_000_000);

    assertThat(timing.elapsed().toNanos()).isEqualTo(recorded);
    assertThat(timing.maxNanos()).isEqualTo(4000);
  }

  @Test
  public void aNestedSpanIsRejected() {
    timing.claim(this);
    timing.begin();
    assertThat(timing.sampled()).isTrue();

    assertThrows(IllegalStateException.class, timing::begin);
  }

  @Test
  public void aNestedSpanIsRejectedOnceSamplingHasStopped() {
    timing.claim(this);
    for (int i = 0; i < FIRST_SKIP; i++) {
      timing.begin();
      timing.closeWith(100);
    }
    timing.begin();
    assertThat(timing.sampled()).isFalse();

    assertThrows(IllegalStateException.class, timing::begin);
  }

  @Test
  public void aNewOwnerReleasesAnOpenSpan() {
    timing.claim(this);
    timing.begin();

    assertThat(timing.claim(new Object())).isTrue();

    timing.begin();
    timing.closeWith(100);
    assertThat(timing.count()).isEqualTo(1);
  }
}
