Use `assertEquals(expected, actual, delta)` to compare floating-point numbers.
This call to `assertEquals()` will either fail or not compile in JUnit 4. Use
`assertEquals(expected, actual, 0.0)` if the delta must be `0.0`.

Alternatively, one can use Google Truth library's `DoubleSubject` class which
has both `isEqualTo()` for delta `0.0` and `isWithin()` for non-zero deltas.
