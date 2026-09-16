package com.google.errorprone;

import static java.util.stream.Collectors.joining;

import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskEvent.Kind;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.util.Log;
import java.time.Duration;
import java.util.Map;

final class TimingReporter implements TaskListener {
  private final ErrorProneTimings errorProneTimings;
  private final Log log;

  TimingReporter(ErrorProneTimings errorProneTimings, Log log) {
    this.errorProneTimings = errorProneTimings;
    this.log = log;
  }

  @Override
  public void finished(TaskEvent event) {
    if (event.getKind() != Kind.COMPILATION) {
      return;
    }

    Map<String, Duration> timings = errorProneTimings.timings();
    if (timings.isEmpty()) {
      return;
    }

    Duration totalTime = timings.values().stream().reduce(Duration.ZERO, Duration::plus);
    String slowestChecks =
        timings.entrySet().stream()
            .sorted(Map.Entry.<String, Duration>comparingByValue().reversed())
            .limit(10)
            .map(e -> e.getValue() + ": " + e.getKey())
            .collect(joining("\n   ", "   ", ""));

    log.printVerbose(
        "error.prone.timing", totalTime, errorProneTimings.initializationTime(), slowestChecks);
  }
}
