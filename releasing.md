---
title: Releasing
layout: master
---

# Keeping the build green
We run a [continuous build](https://travis-ci.org/google/error-prone).

[![Build Status](https://travis-ci.org/google/error-prone.svg?branch=master)](https://travis-ci.org/google/error-prone)

If the build is broken, then we can't do a release. Keep the build green!

# Release to Maven Central

Prerequisite: we sign the released artifacts with GnuPG, so you'll need gpg on your path, and also a default key installed (run `gpg --gen-key`). You also need to upload your ASCII-armored public key to a common repo like http://pgp.mit.edu. You also need to have permission with sonatype, which requires following instructions like steps 2 & 3 of the  here: https://docs.sonatype.org/display/Repository/Sonatype+OSS+Maven+Repository+Usage+Guide. (more info on sonatype and google: http://code.google.com/p/google-maven-repository/wiki/GuidelinesForGoogleMavenProjects).

Here is an example ticket to grant publish rights: 
https://issues.sonatype.org/browse/OSSRH-7782

You will need to set up a settings.xml file for maven in your ~/.m2 directory. if you don't have that file already, get the template at http://maven.apache.org/settings.html#Quick_Overview. Then add various server options: https://issues.sonatype.org/browse/OSSRH-3462?page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel&focusedCommentId=162066#comment-162066


Currently we build the output JARs with JDK7 but with source and target set to 6.  Please ensure that Maven is building with JDK7.  You can check what JDK version Maven is using by typing:
<pre>
$ mvn --version
</pre>

Look at the pom.xml file to see what version is being snapshotted, eg. 0.6-SNAPSHOT means we want to release 0.6

<pre>
$ mvn release:prepare -Dtag=v0.x
# accept the default suggestions
$ mvn release:perform -Darguments=-Dgpg.passphrase=[XXX] 
</pre>

Now the release is "staged" at Sonatype.
Go to https://oss.sonatype.org and login, then take a look at the staged artifacts. Check that the right classes appear in the right jars, and the sizes are reasonable.

Then, "close" and then "release" the staging repository to make the release public. More instructions in case you run into trouble:
https://docs.sonatype.org/display/Repository/Sonatype+OSS+Maven+Repository+Usage+Guide#SonatypeOSSMavenRepositoryUsageGuide-8.ReleaseIt

If you get a key error, you might need to upload this output to http://pgp.mit.edu:
`gpg -a --export`

*Update installation instructions*

Update the version of the error-prone-core dependency in the example pom.xml file here:
https://code.google.com/p/error-prone/wiki/InstallingErrorProne#Maven

*Update submodule versions*

The release:update-versions plugin only updates the versions of submodules that were active during the release, so manually bump the versions of the jdk6 and jdk8 submodules.

Example: https://code.google.com/p/error-prone/source/detail?r=cc09335b3a7f300cd6a41d997c76e78d44331e6a

Check if http://jira.codehaus.org/browse/MNG-624 is fixed yet.

# Update IDEA plugin

(note: we'd like the plugin to be owned by JetBrains at some point, so they'd take responsibility for keeping it working. Alex has a thread with Sergey Simonchek to follow up on.)

1. Sync your working copy to a tagged release:
<pre>
$ git checkout v1.0.8
</pre>

2. Edit idea-plugin/META-INF/plugin.xml : version tag to match the synced VCS tag.
3. Make sure your Compiler -> Java Compiler has Project bytecode version 1.6 (makes -target 1.6)
4. In IDEA, select the idea-plugin module and use menus: Build -> Prepare plugin module 'idea-plugin' for Deployment. It builds and points you to the JAR file.
5. Verify that the resulting JAR will work when IDEA is run on JRE 1.6:
<pre>
$ javap -classpath ~/Projects/error-prone/out/production/idea-plugin/ -verbose com.google.errorprone.intellij.ErrorProneIdeaCompiler | grep version:
  minor version: 0
  major version: 50
</pre>
6. Go to http://plugins.jetbrains.com/plugin/edit?pluginId=7349 (logged in as Alex?) and upload the new JAR.

# Update wiki documentation

We auto-generate wiki documentation for the bugs found by error-prone. To update the documentation, first check out the wiki repository:
<pre>
$ mkdir ~/error-prone.wiki
$ cd ~/error-prone.wiki
$ git clone https://code.google.com/p/error-prone.wiki/ ./
</pre>
Next, generate and copy the generated wiki files from the Maven release goal into the wiki directory:
<pre>
$ cd ~/error-prone
$ mvn clean
$ mvn -P run-annotation-processor compile site
$ cp core/target/generated-wiki/*.wiki ~/error-prone.wiki/
</pre>
Commit and push:
<pre>
$ cd ~/error-prone.wiki
$ git add --all
$ git commit -a -m "Update wiki for v1.x release"
$ git push
</pre>

We also publish javadocs. First check out the Javadoc repository:
<pre>
$ mkdir ~/error-prone.docs
$ cd ~/error-prone.docs
$ git clone https://code.google.com/p/error-prone.docs ./
</pre>

To create the Javadocs, you need a JAVA_HOME env like
<pre>
$ export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.7.0_10.jdk/Contents/Home
</pre>
Then run 
<pre>
$ mvn install
$ mvn javadoc:javadoc
</pre>
to create them. Then we need to check them into the "docs" repository:
<pre>
$ cp -prv core/target/site/apidocs/* ~/error-prone.docs/
$ cd ~/error-prone.docs/
$ git add --all
$ git commit -a -m "Update Javadoc for v1.x release"
$ git push
</pre>

# Generating wiki docs

If you just want to generate the wiki documentation without doing a release, run
<pre>
$ mvn clean
$ cd docgen; mvn package; cd ..
$ mvn -P run-annotation-processor compile site
</pre>

Note that the "mvn clean" at the beginning is necessary since the wiki docs are generated by an annotation processor that also compiles the code.  If Maven thinks the code does not need to be recompiled, the wiki docs will not be generated either.