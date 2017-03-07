# Syn
[![Build Status](https://travis-ci.org/jonathangreen/Syn.svg?branch=master)](https://travis-ci.org/jonathangreen/Syn)
[![Contribution Guidelines](http://img.shields.io/badge/CONTRIBUTING-Guidelines-blue.svg)](./CONTRIBUTING.md)
[![LICENSE](https://img.shields.io/badge/license-MIT-blue.svg?style=flat-square)](./LICENSE)
[![codecov](https://codecov.io/gh/jonathangreen/Syn/branch/master/graph/badge.svg)](https://codecov.io/gh/jonathangreen/Syn)

## Description

A valve for Tomcat8 that authenticates the JWT tokens created by Islandora in order to provide sessionless Authentication for Fedora4.

## Building

This project requires Java 8 and can be built with [Gradle](https://gradle.org). To build and test locally, use `./gradlew build`.

## Installing

### Download dependancies
Download artifacts from Maven Central Repo
* [java-jwt-3.1.0.jar](https://repo1.maven.org/maven2/com/auth0/java-jwt/3.1.0/java-jwt-3.1.0.jar)
* [bcprov-jdk15on-1.56.jar](http://central.maven.org/maven2/org/bouncycastle/bcprov-jdk15on/1.56/bcprov-jdk15on-1.56.jar)
and place into `TOMCAT_HOME/lib` directory. Can be found in Ubuntu at: `/var/lib/tomcat8/lib/`.

### Copy Syn JAR
Copy the JAR that was built above from `build/libs/islandora-syn-X.X.X.jar` and place into `TOMCAT_HOME/lib` directory.

### Register Valve
Now register the valve in Tomcat configuration file.
In Ubuntu this file is located at: `/var/lib/tomcat8/conf/context.xml` 

```xml
<Valve className="ca.islandora.syn.valve.SynValve" 
	  		 pathname="conf/syn-settings.xml" />
```

where:
* ***pathname***: The location of the settings file. Defaults to `$CATALINA_BASE/conf/syn-settings.xml`.

### Enable `security-contraint`
The valve checks if requested url is under **security contraints**. So, valve will activate only if the Fedora4  *web.xml* file contains something like:

```xml
<security-constraint>
    <web-resource-collection>
      <web-resource-name>Fedora4</web-resource-name>
      <url-pattern>/*</url-pattern>
    </web-resource-collection>
    <auth-constraint>
        <role-name>*</role-name>
    </auth-constraint>
    <user-data-constraint>
      <transport-guarantee>NONE</transport-guarantee>
    </user-data-constraint>
</security-constraint>
<security-role>
    <role-name>islandora</role-name>
</security-role>
<login-config>
  <auth-method>BASIC</auth-method>
  <realm-name>fcrepo</realm-name>
</login-config>
```

On ubuntu this file can be found at: 
`/var/lib/tomcat8/webapps/fcrepo/WEB-INF/web.xml`

## Maintainers

* [Jonathan Green](https://github.com/jonathangreen/)

## Development

If you would like to contribute, please get involved by attending our weekly [Tech Call](https://github.com/Islandora-CLAW/CLAW/wiki). We love to hear from you!

If you would like to contribute code to the project, you need to be covered by an Islandora Foundation [Contributor License Agreement](http://islandora.ca/sites/default/files/islandora_cla.pdf) or [Corporate Contributor Licencse Agreement](http://islandora.ca/sites/default/files/islandora_ccla.pdf). Please see the [Contributors](http://islandora.ca/resources/contributors) pages on Islandora.ca for more information.

## Licensing

MIT
