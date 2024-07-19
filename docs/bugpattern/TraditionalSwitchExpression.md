The newer arrow (`->`) syntax for switches is preferred to the older colon (`:`)
syntax. The main reason for continuing to use the colon syntax in switch
*statements* is that it allows fall-through from one statement group to the
next. But in a switch *expression*, fall-through would only be useful if the
code that falls through has side effects. Burying side effects inside a switch
expression makes code hard to understand.
