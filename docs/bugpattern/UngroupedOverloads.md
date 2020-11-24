The [Google Java Style Guide §3.4.2.1][style] requires overloads to be grouped
together:

> When a class has multiple constructors, or multiple methods with the same
> name, these appear sequentially, with no other code in between (not even
> private members).

Overloaded methods/constructors represent the same functionality, but with
different modes of interaction with the caller. They belong together. More
concretely, one danger of splitting overloads is that someone looking for one
but finding the other may easily assume the other does not exist.

If the ungrouped overloads do not represent the same functionality, consider
renaming the methods.

NOTE: this rule implies that a private helper used by a single method, which you
might normally place just below that method, should be placed below *all*
overloads of that method.

[style]: https://google.github.io/styleguide/javaguide.html#s3.4.2.1-overloads-never-split
