Use assertEquals(expected, actual, delta) to compare floating-point numbers.
This call to assertEquals() will either fail or not compile in JUnit 4. Use
assertEquals(expected, actual, 0.0) if the delta must be 0.
