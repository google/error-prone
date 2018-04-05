An `Iterator` is a *state-ful* instance that enables you to check whether it has
more elements (via `hasNext()`) and moves to the next one if any (via `next()`),
while an `Iterable` is a representation of literally iterable elements. An
`Iterable` can generate multiple valid `Iterator`s, though.
