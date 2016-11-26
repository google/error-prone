`@GuardedBy(lock)` documents that a field or method should be accessed only with
a specific lock held. The lock argument identifies the lock that should be held
when accessing the annotated field or method. The possible values for lock are:

*   `@GuardedBy("this")`, meaning the intrinsic lock on the containing object
    (the object of which the method or field is a member);
*   `@GuardedBy("fieldName")`, meaning the lock associated with the object
    referenced by the named field, either an intrinsic lock (for fields that do
    not refer to a Lock) or an explicit Lock (for fields that refer to a Lock);
*   `@GuardedBy("ClassName.fieldName")`, like `@GuardedBy("fieldName")`, but
    referencing a lock object held in a static field of another class;
*   `@GuardedBy("methodName()")`, meaning the lock object that is returned by
    calling the named method;
*   `@GuardedBy("ClassName.class")`, meaning the class literal object for the
    named class.
