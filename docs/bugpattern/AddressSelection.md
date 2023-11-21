Avoid APIs that convert a hostname to a single IP address:

*   [`java.net.Socket(String,int)`](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/net/Socket.html#%3Cinit%3E\(java.lang.String,int,boolean\))
*   [`java.net.InetSocketAddress(String,int)`](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/net/InetSocketAddress.html#%3Cinit%3E\(java.lang.String,int\))
*   [`java.net.InetAddress.html#getByName(String)`](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/net/InetAddress.html#getByName\(java.lang.String\))

Depending on the value of the
[`-Djava.net.preferIPv6Addresses=true`](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/net/doc-files/net-properties.html)
system property, those APIs will return an IPv4 or IPv6 address. If a client
only has IPv4 connectivity, it will fail to connect with
`-Djava.net.preferIPv6Addresses=true`. If a client only has IPv6 connectivity,
it will fail to connect with `-Djava.net.preferIPv6Addresses=false`.

The preferred alternative is for clients to consider all addresses returned by
[`java.net.InetAddress.html#getAllByName(String)`](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/net/InetAddress.html#getAllByName\(java.lang.String\)),
and try to connect to each one until a successful connection is made.

TIP: To resolve a loopback address, prefer `InetAddress.getLoopbackAddress()`
over hard-coding an IPv4 or IPv6 loopback address with
`InetAddress.getByName("127.0.0.1")` or `InetAddress.getByName("::1")`.

This is, prefer this:

```java
  Socket doConnect(String hostname, int port) throws IOException {
    IOException exception = null;
    for (InetAddress address : InetAddress.getAllByName(hostname)) {
      try {
        return new Socket(address, port);
      } catch (IOException e) {
        if (exception == null) {
          exception = e;
        } else {
          exception.addSuppressed(e);
        }
      }
    }
    throw exception;
  }
```

```java
  Socket doConnect(String hostname, int port) throws IOException {
    IOException exception = null;
    for (InetAddress address : InetAddress.getAllByName(hostname)) {
      try {
        Socket s = new Socket();
        s.connect(new InetSocketAddress(address, port));
        return s;
      } catch (IOException e) {
        if (exception == null) {
          exception = e;
        } else {
          exception.addSuppressed(e);
        }
      }
    }
    throw exception;
  }
```

instead of this:

```java
  Socket doConnect(String hostname, int port) throws IOException {
    return new Socket(hostname, port);
  }
```

```java
  void doConnect(String hostname, int port) throws IOException {
    Socket s = new Socket();
    s.connect(new InetSocketAddress(hostname, port));
  }
```

```java
  void doConnect(String hostname, int port) throws IOException {
    Socket s = new Socket();
    s.connect(new InetSocketAddress(InetAddress.getByName(hostname), port));
  }
```
