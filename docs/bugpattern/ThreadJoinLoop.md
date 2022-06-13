Thread.join() can be interrupted, and so requires users to catch
InterruptedException. Most users should be looping until the join() actually
succeeds.

Instead of writing your own try-catch and loop to handle it properly, you may
use **Uninterruptibles.joinUninterruptibly** which does the same for you.

Example:

```
Thread thread = new Thread(new Runnable() {...});

Uninterruptibles.joinUninterruptibly(thread);
```
