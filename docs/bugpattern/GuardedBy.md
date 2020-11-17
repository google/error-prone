The GuardedBy analysis checks that fields or methods annotated with
`@GuardedBy(lock)` are only accessed when the specified lock is held.

Example:

```java
import com.google.errorprone.annotations.concurrent.GuardedBy;

class Account {
  @GuardedBy("this")
  private int balance;

  public synchronized int getBalance() {
    return balance; // OK: implicit 'this' lock is held.
  }

  public synchronized void withdraw(int amount) {
    setBalance(balance - amount); // OK: implicit 'this' lock is held.
  }

  public void deposit(int amount) {
    setBalance(balance + amount); // ERROR: access to 'balance' not guarded by 'this'.
  }

  @GuardedBy("this")
  private void setBalance(int newBalance) {
    checkState(newBalance >= 0, "Balance cannot be negative.");
    balance = newBalance; // OK: 'this' must be held by caller of 'setBalance'.
  }
}
```

This above example uses implicit locks (via the 'synchronized' modifier). The
analysis also supports synchronized statements and java.util.concurrent locks.

## Basic Concepts

The analysis provides a way of associating members with locks. A member is a
field or a method. A lock can be the implicit lock of an object, or a
java.util.concurrent Lock.

An implicit lock is acquired using the built in synchronization features of the
language. Adding the 'synchronized' modifier to an instance method causes the
implicit lock of the enclosing instance to be acquired for the duration of the
method. Adding the 'synchronized' modifier to a static method is similar, except
the implicit lock of the Class object is acquired instead.

The Locks defined in java.util.concurrent are acquired with explicit
lock()/unlock() methods. The use of these methods in Java should always
correspond to a try/finally block, to ensure that the locks are released on all
execution paths.

## Reference

### Lock expression syntax

The following syntax can be used to describe a lock:

<table><tr><td><code>

this
</code></td><td>
The implicit object lock of the enclosing class.
</td></tr><tr><td><code>

ClassName.this
</code></td><td>
The implicit object lock of the enclosing class specified by ClassName.
(For inner classes, the ClassName.this designation allows you to specify which
'this' reference is intended.)

</td></tr><tr><td><code>
fieldName <br>
this.fieldName <br>
ClassName.this.fieldName
</code></td><td>
The final instance field specified by fieldName.

</td></tr><tr><td><code>
methodName() <br>
this.methodName() <br>
ClassName.this.methodName()
</code></td><td>
The instance method specified by methodName(). Methods called to return
locks should be deterministic.

</td></tr><tr><td><code>
ClassName.class
</code></td><td>
The implicit lock of specified Class object.

</td></tr><tr><td><code>
ClassName.fieldName
</code></td><td>
The static final field specified by fieldName.

</td></tr><tr><td><code>
ClassName.methodName()
</code></td><td>
The static method specified by methodName(). Methods called to return locks
should be deterministic.

</td></tr></table>

### Annotations

#### @GuardedBy

com.google.errorprone.annotations.concurrent.GuardedBy

The @GuardedBy annotation is used to document that a member (a field or a
method) can only be accessed when the specified lock is held.

@GuardedBy can be used with both implicit locks and java.util.concurrent Locks.

```java
final Lock lock = new ReentrantLock();

@GuardedBy("lock")
int x;

void m() {
  x++;  // error: access of 'x' not guarded by 'lock'
  lock.lock();
  try {
    x++;  // OK: guarded by 'lock'
  } finally {
    lock.unlock();
  }
}
```

Note: there are a couple more annotations called `@GuardedBy`, including
`javax.annotation.concurrent.GuardedBy` and
`org.checkerframework.checker.lock.qual.GuardedBy`. The check recognizes those
versions of the annotation, but we recommend using
`com.google.errorprone.annotations.concurrent.GuardedBy`.

#### @LockMethod

com.google.errorprone.bugpatterns.threadsafety.annotations.LockMethod

The method to which this annotation is applied acquires one or more locks. The
caller will hold the locks when the function finishes execution.

This annotation does not apply to implicit locks, which cannot be acquired
without being released in the same method.

```java
final Lock lock = new ReentrantLock();

@LockMethod("lock")
acquireLock() {}  // error: 'lock' was not acquired
```

#### @UnlockMethod

com.google.errorprone.bugpatterns.threadsafety.annotations.UnlockMethod

The method to which this annotation is applied releases one or more locks. The
caller must hold the locks when the function is entered, and will not hold them
when it completes.

This annotation does not apply to implicit locks, which cannot be released
without being acquired in the same method.

```java
final Lock lock = new ReentrantLock();

@GuardedBy("lock")
int x;

@UnlockMethod("lock")
void releaseLock() {
  lock.unlock();
}

@LockMethod("lock")
void acquireLock() {
  lock.lock();
}

public void increment() {
  acquireLock();
  try {
    x++;  // OK: 'lock' is held
  } finally {
    releaseLock();
  }
}
```

#### Limitations

Anonymous classes and lambdas need to re-acquire locks that may be held by an
enclosing block. For example, consider:

```java
class Transaction {
  @GuardedBy("this")
  int x;

  public synchronized void handle() {
    doSomething(() -> {
      x++;  // Error: access of 'x' not guarded by 'Transaction.this'
    });
  }
}
```

The analysis is intra-procedural, meaning it doesn't consider the implementation
of `doSomething`.

The checker doesn't know if `doSomething` immediately calls the provided lambda
while the lock is still held by the enclosing method `handle`, for example:

```java
private void doSomething(Runnable r) {
  r.run();
}
```

... or whether the lambda could be called later after `handle` has released the
lock, for example:

```java
private void doSomething(Runnable r) {
  // runs `r` at some point in the future
  someExecutor.execute(r);
}
```

#### False negatives with aliasing

```java
class Names {
  @GuardedBy("this")
  List<String> names = new ArrayList<>();

  public void addName(String name) {
    List<String> copyOfNames;
    synchronized (this) {
      copyOfNames = names;  // OK: access of 'names' guarded by 'this'
    }
    copyOfNames.add(name);  // should be an error: this access is not thread-safe!
  }
}
```

The analysis does not track aliasing, so it's possible to circumvent the safety
it provides by copying references to guarded members.

In the example, the guarded field 'names' can be accessed via a copy even if the
required lock is not held.
