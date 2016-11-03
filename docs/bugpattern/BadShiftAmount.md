For shift operations on int types, only the five lowest-order bits of the shift
amount are used as the shift distance.  This means that shift amounts that are
not in the range 0 to 31, inclusive, are silently mapped to values in that
range.  For example, a shift of an int by 32 is equivalent to shifting by 0,
i.e., a no-op.

See JLS 15.19, "Shift Operators", for more details.
