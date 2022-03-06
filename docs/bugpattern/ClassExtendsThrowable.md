A class should not be an extension of the Throwable type. 
It violates the principle of least surprise in that systems 
should behave as most users expect it to. Extend Exception 
or Error instead.

The following example would trigger the check, warning the user of the usage of `extends Throwable`
```java
    public class ClassExtendsThrowable extends Throwable{
        public int returnsZero(){return 0;}
    }
```