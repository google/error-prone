Any private helper method that never accesses `this` (even implicitly) is
already static in spirit. Adding an explicit static keyword makes it clear that
the method makes no use of instance state, and prevents a future editor from
doing so accidentally.

