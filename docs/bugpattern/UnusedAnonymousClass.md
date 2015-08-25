Creating a side-effect-free anonymous class and never using it is usually
a mistake.

For example:

```java
public static void main(String[] args) {
  new Thread(new Runnable() {
    @Override public void run() {
      preventMissionCriticalDisasters();
    }
  }); // did you mean to call Thread#start()?
}
```
