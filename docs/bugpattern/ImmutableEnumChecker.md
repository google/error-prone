All fields in your enum class should be final and either be primitive or refer
to deeply immutable objects.

Static state is dangerous to begin with, but much worse for enums. We all think
of enum values as constants -- and even refer to them as "enum constants" --
and would be very surprised if any of their state ever changed, or was not
thread-safe.

