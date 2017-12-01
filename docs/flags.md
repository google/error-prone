---
title: Command-line flags
layout: documentation
---

### Severity

Error Prone lets the user enable and disable specific checks as well as override
their built-in severity levels (warning vs. error) by passing options to the
Error Prone compiler invocation.

A valid Error Prone command-line option looks like:

```bash
-Xep:<checkName>[:severity]
```

`checkName` is required and is the canonical name of the check, e.g.
"ReferenceEquality". `severity` is one of {"OFF", "WARN", "ERROR"}. Multiple
flags must be passed to enable or disable multiple checks. The last flag for a
specific check wins.

Examples of usage follow:

```bash
-Xep:ReferenceEquality  [turns on ReferenceEquality check with the severity level from its BugPattern annotation]
-Xep:ReferenceEquality:OFF  [turns off ReferenceEquality check]
-Xep:ReferenceEquality:WARN  [turns on ReferenceEquality check as a warning]
-Xep:ReferenceEquality:ERROR  [turns on ReferenceEquality check as an error]
-Xep:ReferenceEquality:OFF -Xep:ReferenceEquality  [turns on ReferenceEquality check]
```

There are also a few blanket severity-changing flags:

*   `-XepAllErrorsAsWarnings`
*   `-XepAllDisabledChecksAsWarnings`
*   `-XepDisableAllChecks`
*   `-XepDisableWarningsInGeneratedCode` : Disables warnings in classes annotated with @javax.annotation.Generated

Additionally, you can completely exclude certain paths
from any Error Prone checking via the `-XepExcludedPaths` flag.  The
flag takes as an argument a regular expression that is matched against
a source file's path to determine whether it should be excluded.
So, to exclude files in any sub-directory of a path containing `build/generated`,
use the option:

```bash
-XepExcludedPaths:.*/build/generated/.*
```

If you pass a flag that refers to an unknown check name, by default Error Prone
will throw an error. You can allow the use of unknown check names by passing the
`-XepIgnoreUnknownCheckNames` flag.

We no longer support the old-style Error Prone disabling flags that used the
`-Xepdisable:<checkName>` syntax.

### Patching

There are a couple of flags for configuration of patching in suggested fixes,
e.g. `-XepPatchChecks:VALUE` and `-XepPatchLocation:VALUE`. See the [patching
docs](http://errorprone.info/docs/patching) for more info.

### Pass Additional Info to BugCheckers

To configure checks, you can use custom flags to pass info directly to
BugCheckers. A valid custom flag looks like this:

```bash
-XepOpt:[Namespace:]FlagName[=Value]
```

By convention, if a flag is only relevant to one check or a group of checks, to
prevent name collision, you should prefix your flag's name with an optional
namespace and a colon, e.g. `-XepOpt:JUnit4TestNotRun:ExpandedHeuristic=true`.

If a flag is set with no value provided, that flag is set to `true`, e.g.
`-XepOpt:MakeAwesome` is equivalent to `-XepOpt:MakeAwesome=true`.

Some examples:

```bash
-XepOpt:FlagName=SomeValue        (flags["FlagName"] = "SomeValue")
-XepOpt:BooleanFlag               (flags["BooleanFlag"] = "true")
-XepOpt:ListFlag=1,2,3            (flags["ListFlag"] = "1,2,3")
-XepOpt:Namespace:SomeFlag=AValue (flags["Namespace:SomeFlag"] = "AValue")
```

These flags can be accessed in a BugChecker just by adding a one-argument
constructor that takes an `ErrorProneFlags` object, like so:

```java
public class MyChecker extends BugChecker implements SomeTreeMatcher {

  private final boolean coolness;

  public MyChecker(ErrorProneFlags flags) {
    // The ErrorProneFlags get* methods return an Optional<*>, use
    // Optional.orElse(...) and related methods to get with default, etc.
    this.coolness = flags.getBoolean("ErrorProne:IsCool").orElse(true);
  }

  public Description matchSomething(...) {...}
}
```

## Maven

To pass Error Prone flags to Maven, use the `compilerArgs` parameter in the
plugin's configuration. To enable warnings, the `showWarnings` parameter must
also be set:

```xml
<project>
  <build>
    <plugins>
      <plugin>
        <artifactId>maven-compiler-plugin</artifactId>
        <configuration>
          <compilerId>javac-with-errorprone</compilerId>
          <showWarnings>true</showWarnings>
          <compilerArgs>
            <arg>-Xep:DeadException:WARN</arg>
            <arg>-Xep:GuardedBy:OFF</arg>
          </compilerArgs>
        </configuration>
      </build>
    </plugins>
  </plugin>
</project>
```
