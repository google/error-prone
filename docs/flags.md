---
title: Command-line flags
layout: documentation
---

Error Prone lets the user enable and disable specific checks as well as
override their built-in severity levels (warning vs. error) by passing options
to the Error Prone compiler invocation.

A valid Error Prone command-line option looks like:

```bash
-Xep:<checkName>[:severity]
```

`checkName` is required and is the canonical name of the check, e.g.
"ReferenceEquality".  `severity` is one of {"OFF", "WARN", "ERROR"}.  Multiple
flags must be passed to enable or disable multiple checks.  The last flag for a
specific check wins.

Examples of usage follow:

```bash
-Xep:ReferenceEquality  [turns on ReferenceEquality check with the severity level from its BugPattern annotation]
-Xep:ReferenceEquality:OFF  [turns off ReferenceEquality check]
-Xep:ReferenceEquality:WARN  [turns on ReferenceEquality check as a warning]
-Xep:ReferenceEquality:ERROR  [turns on ReferenceEquality check as an error]
-Xep:ReferenceEquality:OFF -Xep:ReferenceEquality  [turns on ReferenceEquality check]
```

If you pass a flag that refers to an unknown check name, by default Error Prone
will throw an error. You can allow the use of unknown check names by passing
the `-XepIgnoreUnknownCheckNames` flag.

We no longer support the old-style Error Prone disabling flags that used the
`-Xepdisable:<checkName>` syntax.

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
            <arg>-Xep:GuardedByValidator:OFF</arg>
          </compilerArgs>
        </configuration>
      </build>
    </plugins>
  </plugin>
</project>
```
