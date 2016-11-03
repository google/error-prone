Like many other languages, Java provides automatic memory management. In Java,
this feature incurs an runtime cost, and can also lead to unpredictable
execution pauses. In most cases, this is a reasonable tradeoff, but sometimes
the loss of performance or predictability is unacceptable. Examples include
pause-sensitive user interface handlers, high query rate server response
handlers, or other soft-realtime applications.

In these situations, you can annotate a few carefully written methods with
@NoAllocation. Methods with this annotation will avoid allocations in most
cases, reducing pressure on the garbage collector. Note that allocations may
still occur in methods with @NoAllocation if the compiler or runtime system
inserts them.

To ease the use of exceptions, allocations are allowed if they occur within a
throw statement. But if the throw statement contains a nested class with methods
annotated with @NoAllocation, those methods will be disallowed from allocating.
