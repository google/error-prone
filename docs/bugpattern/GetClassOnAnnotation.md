Instances of an annotation interface generally return a random proxy class when `getClass()` is called on them; to get the actual annotation type use `annotationType()`.

In the following example, calling `getClass()` on the annotation instance
returns a proxy class like `com.sun.proxy.$Proxy1`, while `annotationType()`
returns `Deprecated`.

```java
@Deprecated
public class Test {
  static void printAnnotationClass(Annotation annotation) {
    System.err.println(annotation.getClass());
    System.err.println(annotation.annotationType());
  }

  public static void main(String[] args) {
    printAnnotationClass(Test.class.getAnnotation(Deprecated.class));
  }
}
```

Prints:

```
class com.sun.proxy.$Proxy1
interface java.lang.Deprecated
```
