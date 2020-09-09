Prefer specialized functional interface types for primitives, for example
`IntToLongFunction` instead of `Function<Integer, Long>`, to avoid boxing
overhead.
