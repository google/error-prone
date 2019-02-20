Requiring the argument to `equals` to be of a specific concrete type is
incorrect, because it precludes any subtype of this class from obeying the
[substitution principle](https://en.wikipedia.org/wiki/Liskov_substitution_principle).
Such code should be modified to use an `instanceof` test instead of `getClass`.

## Extending a value class to add attributes

The most common objection objection to this rule arises from a scenario like the
following:

```
class Point {
  double x() {...}
  double y() {...}
}

class ColoredPoint {
  double x() {...}
  double y() {...}
  Color color() {...}
}
```

It's reasonable to think first of modeling `ColoredPoint` as a subclass of
`Point`. First, because on a conceptual level, there is a clear "is-a"
relationship between the two, which we have been taught to model as inheritance.
But also because of a few concrete advantages:

1.  You get the `x` and `y` fields/accessors for free.
2.  You get any other methods defined on `Point` for free.
3.  Users can pass a `ColoredPoint` to anything that expects a `Point`. (This is
    by far the primary advantage, since the first two are just one-time-only
    implementation helpers for the `Point` authors themselves.)

Although these same advantages *can* be achieved via composition, it's no longer
quite "for free"; the frequent need to call `myColoredPoint.asPoint()` is a pain
that feels unjustified, and thus subclassing is often chosen.

Unfortunately, `equals` now creates a big problem. Two `Points` should be seen
as interchangeable whenever they have the same `x` and `y` coordinates. But two
`ColoredPoints` are only equivalent if their coordinates *and* color are the
same. This, plus the general contract of `equals` (e.g. symmetry), ends up
forcing `Point(1, 2)` to respond `false` if asked whether it is equal to
`ColoredPoint(1, 2, BLUE)`.

This can be achieved by changing `equals` to be based on `getClass` instead of
`instanceof`. But do we even want to do that? Recall the main advantage we cited
for using subtyping: so that we "can pass `ColoredPoint` to anything that
expects a `Point`", we said. Yet we are in fact *not* achieving that after all.
Put simply, `ColoredPoint` is incapable of functioning properly *as a `Point`*
because it cannot participate in equality checks with other `Point`s.

The composition approach may be annoying (the frequent need for `.asPoint()`),
but aside from that annoyance, it at least achieves the three advantages
correctly.

In summation, while it is true *conceptually* that a `ColoredPoint` "is-a"
`Point`, a `ColoredPoint` is nevertheless unable to properly *function* as a
`Point`, and modeling it with subtyping is not a good idea for that reason.

## What about final or effectively-final classes?

In this case there is no disadvantage to the `getClass` trick - but there's no
great advantage to it either. Most unsafe idioms have circumstances in which
they are safe, but this doesn't change the fact that they are *generally* unsafe
and not worth propagating and legitimizing.

## More information

See [Effective Java, 2nd Edition, Item 8][ej8] ("Obey the general contract when
overriding equals").

[ej8]: https://books.google.com/books?id=ka2VUBqHiWkC
