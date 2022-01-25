# Java find reference based on [Java Language Server ](https://github.com/georgewfraser/java-language-server)

A command line tool to find java leaf reference.


## Motivation

In a big java project, after change some code, it is good to have a tool to find the impacted methods.
The leaf reference maybe unit test cases, public methods.
It would be good that developer can prioritize to test those impacted leaf reference.

Of cause developer can use `find reference` in IDE or experience to figure out the scope to test the change, but CI have no idea about the change.
In case there are many unit test cases and function test cases need to be run in CI before merge the code, it's wast to run all of them when developer
only change a little bit.

The ambition of this tool is to help developer find the leaf reference and provide extra information to CI or other tools.

## Installation

- Checkout this repository

- Execute `mvn package`

- Collect the script and jar files under directory `dist`

## Usage

```
./find_reference_xxx.sh -help
Parsing failed.  Reason: Missing required option: workspace
usage: java-find-reference
 -help                      print this message
 -outputJson <file>         Output to json file
 -searchDepth <depth>       Search reference depth. 0: don't find
                            reference just care about the function
                            contains the change, -1 search the code until
                            highest layer.
 -specialMethods <method>   The special methods which are leaves by will
                            be executed by all methods like setUp,
                            tearDown in Junit, split with comma(,).
 -workspace <dir>           Workspace to parse the code. For maven
                            project, input the directory contains the
                            pom.xml.
```

## Contributing
### Setup develop environment
- JDK 14
- Maven
