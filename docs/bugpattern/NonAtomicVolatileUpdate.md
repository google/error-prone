The volatile modifier ensures that updates to a variable are propagated
predictably to other threads. A read of a volatile variable always returns the
most recent write by any thread.

However, this does not mean that all updates to a volatile variable are atomic.
For example, if you increment or decrement a volatile variable, you are actually
doing (1) a read of the variable, (2) an increment or decrement of a local copy,
and (3) a write back to the variable. Each step is atomic individually, but the
whole sequence is not, and it will cause a race condition if two threads try to
increment or decrement a volatile variable at the same time. The same is true
for compound assignment, e.g. foo += bar.

If you intended for this update to be atomic, you should wrap all update
operations on this variable in a synchronized block. If the variable is an
integer, you could use an AtomicInteger instead of a volatile int.
