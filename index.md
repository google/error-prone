---
title: Error-Prone
layout: master
---

### It's common for even the best programmers to make simple mistakes. And sometimes a refactoring which seems safe can leave behind code which will never do what's intended.

We're used to getting help from the compiler, but it doesn't do much beyond static type checking. Using error-prone to augment the compiler's type analysis, you can catch more mistakes before they cost you time, or end up as bugs in production. We use error-prone in Google's Java build system to eliminate classes of serious bugs from entering our code, and we've open-sourced it, so you can too!

__error prone ...__

* __hooks into your standard build, so all developers run it without thinking__
* __tells you about mistakes immediately after they're made__
* __produces suggested fixes, allowing you to build tooling on it__

## How it works
__src/Main.java__
{% highlight java linenos %}
public class Main {
  public static void main(String[] args) {
    if (args.length < 1) {
      new IllegalArgumentException("Missing required argument");
    }
  }
}
{% endhighlight %}
{% highlight bash %}
$ ant
Buildfile: ./error-prone/examples/ant/build.xml

compile:
    [javac] Compiling 1 source file to ./src/error-prone/examples/ant/build
    [javac] src/Main.java:20: error: [DeadException] Exception created but not thrown
    [javac]       new IllegalArgumentException("Missing required argument");
    [javac]       ^
    [javac]     (see http://errorprone.info/bugpattern/DeadException)
    [javac]   Did you mean 'throw new IllegalArgumentException("Missing required argument");'?
    [javac] 1 error

BUILD FAILED
{% endhighlight %}
