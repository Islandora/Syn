# ![Syn](https://cloud.githubusercontent.com/assets/2371345/23724175/2998ecb0-0422-11e7-9009-aee3f129633f.png) Syn
[![Build Status](https://travis-ci.org/Islandora-CLAW/Syn.svg?branch=master)](https://travis-ci.org/Islandora-CLAW/Syn)
[![Contribution Guidelines](http://img.shields.io/badge/CONTRIBUTING-Guidelines-blue.svg)](./CONTRIBUTING.md)
[![LICENSE](https://img.shields.io/badge/license-MIT-blue.svg?style=flat-square)](./LICENSE)
[![codecov](https://codecov.io/gh/Islandora-CLAW/Syn/branch/master/graph/badge.svg)](https://codecov.io/gh/Islandora-CLAW/Syn)

## Description

A ServletFilter that authenticates the JWT tokens created by Islandora in order to provide sessionless Authentication for Fedora4. Named after the Norse goddess [Syn](https://en.wikipedia.org/wiki/Syn_(goddess)).

## Building

This project requires Java 8 and can be built with [Gradle](https://gradle.org). To build and test locally, use `./gradlew build`.

## Installing

### Copy Syn JAR
Copy the JAR that was built above from `build/libs/islandora-syn-X.X.X-all.jar` and place into `$TOMCAT_HOME/lib` directory or the individual webapps `WEB-INF/lib` directory. Can be found in Ubuntu at: `/var/lib/tomcat8/lib/`. Note that this JAR is built to contain all the dependancies.

### Register Filter
Now register the filter in web applications' `web.xml` file by adding something like.

```xml
  <filter>
    <filter-name>SynFilter</filter-name>
    <filter-class>ca.islandora.syn.valve.SynFilter</filter-class>
    <init-param>
      <param-name>settings-path</param-name>
      <param-value>/var/lib/tomcat8/conf/syn-settings.yml</param-value>
    </init-param>
  </filter>

  <filter-mapping>
    <filter-name>SynFilter</filter-name>
    <url-pattern>/*</url-pattern>
  </filter-mapping>
```

Where the **settings-path** `param-value` is the the location of the settings file.

On ubuntu this file can be found at: 
`/var/lib/tomcat8/webapps/fcrepo/WEB-INF/web.xml`

### Setup Syn Configuration
Modify the [example configuration](./conf/syn-settings.example.yaml) and move it to: `$CATALINA_BASE/conf/syn-settings.xml`. Then use this path when configuring the application's filter `init-param`s.

## Maintainers

* [Jonathan Green](https://github.com/jonathangreen/)
* [Jared Whiklo](https://github.com/whikloj)

## Development

If you would like to contribute, please get involved by attending our weekly [Tech Call](https://github.com/Islandora-CLAW/CLAW/wiki). We love to hear from you!

If you would like to contribute code to the project, you need to be covered by an Islandora Foundation [Contributor License Agreement](http://islandora.ca/sites/default/files/islandora_cla.pdf) or [Corporate Contributor Licencse Agreement](http://islandora.ca/sites/default/files/islandora_ccla.pdf). Please see the [Contributors](http://islandora.ca/resources/contributors) pages on Islandora.ca for more information.

## Licensing

MIT
