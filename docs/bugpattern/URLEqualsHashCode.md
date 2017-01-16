Equals and HashCode method of java.net.URL make blocking network calls. Either
use java.net.URI or if that isn't possible, use Collection<URL> or List<URL>.
