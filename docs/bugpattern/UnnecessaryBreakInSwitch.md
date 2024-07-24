The newer arrow (`->`) syntax for switches does not permit fallthrough between
cases. A `break` statement is allowed to break out of the switch, but including
a `break` as the last statement in a case body is unnecessary.
