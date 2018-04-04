Classes that only include static members have no behavior particular to any
given instance, so instantiating them is nonsense. To prevent users from
mistakenly creating instances, the class should include a private constructor.
See Effective Java, Second Edition - Item 4.
