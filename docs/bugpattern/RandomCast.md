`Math.random()`, `Random#nextFloat`, and `Random#nextDouble` return results in
the range `[0.0, 1.0)`. Therefore, casting the result to `(int)` or `(long)`
*always* results in the value of `0`.
