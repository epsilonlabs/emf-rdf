# Instructions for contributors

## Building locally the Jena uber-JAR

Due to Java classloader issues, Apache Jena must be packaged into an uber-JAR when running from an OSGi environment like Eclipse.

The [`jena-uberjar`](./jena-uberjar/) folder contains a Maven build to produce this uber-JAR.
Before setting up a development environment, build and install it locally with:

```shell
cd jena-uberjar
mvn install
```

## Setting up a development environment

You will need to install a recent version of the "Eclipse IDE for Eclipse Committers" package from the [official Eclipse website](https://www.eclipse.org/downloads/packages/).

After this, you should install the [Target Platform Definition DSL and Generator](https://github.com/eclipse-cbi/targetplatform-dsl) into Eclipse.

Import the Eclipse projects in these folders:

* [`bundles`](./bundles/): Eclipse plugins with the driver implementation and developer tools
* [`features`](./features/): features that users can install to use the driver from Eclipse
* [`releng`](./releng/): release engineering projects (target platform + update site)
* [`tests`](./tests/): automated tests to be run from plain Java and from an OSGi environment

Open the [`org.eclipse.epsilon.emc.rdf.target/org.eclipse.epsilon.emc.rdf.target.target`](./releng/org.eclipse.epsilon.emc.rdf.target/org.eclipse.epsilon.emc.rdf.target.target) file, and wait for the platform to resolve.
Click on "Set as Target Platform".

After some time, all the code should have been compiled without errors.

## Running tests

To execute all the tests via Tycho in a plain Java environment:

```shell
mvn test
```

To execute all the tests via Tycho in an OSGi environment:

```shell
mvn integration-test
```
