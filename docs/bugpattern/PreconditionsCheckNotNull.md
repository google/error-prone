Preconditions.checkNotNull() takes two arguments. The first is the reference
that should be non-null. The second is the error message to print (usually a
string literal). Often the order of the two arguments is swapped, and the
reference is never actually checked for nullity. This check ensures that the
first argument to Preconditions.checkNotNull() is not a literal.
