Java assertions do not necessarily execute at runtime; they may be enabled and
disabled depending on which options are passed to the JVM invocation. An assert
false statement may be intended to ensure that the program never proceeds beyond
that statement. If the correct execution of the program depends on that being
the case, consider throwing an exception instead, so that execution is halted
regardless of runtime configuration.
