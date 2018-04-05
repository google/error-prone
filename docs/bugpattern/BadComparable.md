A narrowing integral conversion can cause a sign flip, since it simply discards
all but the n lowest order bits, where n is the number of bits used to represent
the target type (JLS 5.1.3). In a compare or compareTo method, this can cause
incorrect and unstable sort orders.
