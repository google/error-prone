Prefer using unqualified names to refer to inherited nested classes.

For example, when extending a class with nested types (like Truth's `Subject`
and its nested `Factory` interface), prefer this:

```java
import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;

class MySubject extends Subject {
  protected MySubject(FailureMetadata metadata, Object actual) {
    super(metadata, actual);
  }

  static Factory<MySubject, Object> factory() {
    return MySubject::new;
  }
}
```

instead of this:

```java
import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;

class MySubject extends Subject {
  protected MySubject(FailureMetadata metadata, Object actual) {
    super(metadata, actual);
  }

  static Subject.Factory<MySubject, Object> factory() {
    return MySubject::new;
  }
}
```
