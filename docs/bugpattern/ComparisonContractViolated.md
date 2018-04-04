The comparison contract states that `sgn(compare(x, y)) == -sgn(compare(y, x))`.
(An immediate corollary is that `compare(x, x) == 0`.) This comparison
implementation either a) cannot return 0, b) cannot return a negative value but
may return a positive value, or c) cannot return a positive value but may return
a negative value.

The results of violating this contract can include `TreeSet.contains` never
returning true or `Collections.sort` failing with an IllegalArgumentException
arbitrarily.

In the long term, essentially all Comparators should be rewritten to use the
Java 8 Comparator factory methods, but our automated migration tools will, of
course, only work for correctly implemented Comparators.

