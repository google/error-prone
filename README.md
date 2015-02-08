# error-prone

This repo comprises the code telemetry generation for the part A project for UCLA CS239. It features a modified version of the project error-prone,
the build process for which has been reconfigured to instrument each method, and to output all test results to file.

To use this functionality, there are two basic commands:  
1) mvn -Dmaven.test.skip=true package  
     Compile and instrument all project files, without running tests.  
2) ./run-tests.sh  
     Run all tests in the package, saving all results to disk.  

### Authors:
Alan Litteneker  
Justin Morgan  
Sam Tarin  
Pedro Perez  

---

Catch common Java mistakes as compile-time errors

[![Build Status](https://travis-ci.org/google/error-prone.svg?branch=master)](https://travis-ci.org/google/error-prone)

Our documentation for users is at http://errorprone.info

To develop and build error-prone, see our documentation on the [wiki](https://github.com/google/error-prone/wiki/For-Developers).

## Links

- [Javadoc](http://errorprone.info/api/latest/)
- Pre-release snapshots are available from [sonatype's snapshot repository](https://oss.sonatype.org/content/repositories/snapshots/com/google/errorprone/).
