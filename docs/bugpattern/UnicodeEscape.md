Using unicode escapes in Java for printable characters is obfuscated. Worse,
given the compiler allows unicode literals outside of `String` literals, it can
be potentially unsafe.

Prefer using literal characters for printable characters.

For an example of malicious code, consider:

```java
class Evil {
  public static void main(String... args) {
    // Don't run this, it would be really unsafe!
    // \u000d Runtime.exec("rm -rf /");
  }
}
```

`\u000d` encodes a newline character, so `Runtime.exec` appears on its own line
and will execute.
