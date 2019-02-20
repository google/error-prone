The `outline` CSS property provides visual indicators as to which element is
currently selected within a web page. These are the dotted lines you see
surrounding links, etc. when you tab to them using the keyboard.

![A link shown with and without a focus outline.](https://github.com/google/error-prone/blob/gh-pages/images/Outline%20Demonstration.png)

These indicators are important for users navigating without a mouse (such as
those with visual or mobility impairments). Setting `outline` style to `"none"`
or `0` removes these indicators, leaving these users without any way to tell
where they are within the page, therefore making the page inaccessible.

Caveat: `outline` is not the *only* way to emphasize selected elements. You may
instead choose to change the background color, add an underline, or otherwise
make them visually distinct. Learn more & get alternative suggestions at
[OutlineNone.com](http://outlinenone.com).
