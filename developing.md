---
title: Developing
layout: master
---

# Getting started

We use the Maven build system. We are using version 3. [Download Maven](http://maven.apache.org/download.html)

error-prone uses some compiler internals, so we don't support all JDK implementations. We test with OpenJDK 6 and 7, so those are currently recommended. Do whatever your system requires to use one of those JDKs.

Build the library:
<pre>
$ cd error-prone
$ mvn -Ptools.jar package
</pre>

If you don't want to remember the -P flag every time, just edit your `~/.m2/settings.xml`:
{% highlight xml %}
<activeProfiles>
  <activeProfile>tools.jar</activeProfile>
</activeProfiles>
{% endhighlight %}
(if you don't have that file already, get the template at http://maven.apache.org/settings.html#Quick_Overview )

You'll also need to configure your IDE to build with a supported JDK.

A Maven plugin for your IDE should setup the project very conveniently, and a git plugin can simplify source code management. The core developers use IntelliJ IDEA or Eclipse.

It's very useful and recommended to locate the sources for your JDK and attach them in the IDE, so you can navigate into the javac libraries when needed.

We generally follow the Sun style guide, and limit lines to 100 cols.

# Write a checker

Let's say you want to write a checker and contribute it to the project.  Here are the steps you should follow.

## Set up your work environment

Follow the Getting Started steps above.  Then create a branch in which to do your work.

You should also add the check to the [Issue Tracker](https://code.google.com/p/error-prone/issues/list?can=2&q=Type%3DNewCheck) (if it is not already there), comment that you are taking the issue over, and change the status from "New" to "Accepted".

## Write your checker

Checkers are in the package com.google.errorprone.bugpatterns.  Follow the example of one of the checkers in that package to create your own checker.  Don't forget to write tests!  

## Request a code review

A member of error-prone team needs to review your code and merge it into the mainline project.  We use [https://code.google.com/p/rietveld/ Rietveld] for code reviews.  

Assuming you are in your work branch, and that branch contains all the changes you made, you want to diff the state of that branch against the master branch and upload the diff to Rietveld.

To do that, first download the Rietveld [https://codereview.appspot.com/static/upload.py upload.py] script.  Then run the following command:
<pre>
$ upload.py --rev master
</pre>

If you are using two-factor authentication, you may need to create an [https://support.google.com/accounts/answer/185833?hl=en application-specific password].  You can also choose a particular revision identifier to diff against, if you didn't do all your work in a branch.

The upload.py script will output a link to the code review.  Please go to the URL and edit the description to explain what your checker does.  Then click "Publish + Mail Comments," put "eaftan@google.com" (important: the "@google.com" is necessary or we won't get your code review) in the reviewers field, and hit "Publish all my drafts."

Now wait for your code review and address the comments.

## Iterate on code review

After you address a round of comments in the code review tool, please update your Rietveld code review with the diff from the previously uploaded revision.  This lets your reviewer easily see the changes you made to address her comments.  For example, if previously the head revision was 12345 and you uploaded a patch against master, and your Rietveld issue number was 987, you would use this command:
<pre>
$ upload.py --rev 12345 -i 987
</pre>

Update the review thread stating that you've addressed the comments.

## Send us a patch

After we've approved your code review, you need to get your changes to us.  We use git-formatted patches for this.

Create a patch that includes the changes against the master branch:
<pre>
$ git format-patch master --stdout > mypatch.patch
</pre>

Either email the patch to a team member or attach the patch to your code review request in the issue tracker.  A member of the team will then merge your change into head:

For the team: Apply the patch by typing
<pre>
$ git am mypatch.patch
</pre>