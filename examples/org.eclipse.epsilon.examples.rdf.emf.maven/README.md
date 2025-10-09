# Maven example

This is an example of how to run from Maven an EOL program that queries an RDF file.
The Maven build uses the [maven-antrun-plugin](https://maven.apache.org/plugins/maven-antrun-plugin/) to reuse the [Epsilon Ant tasks](https://eclipse.dev/epsilon/doc/workflow/).

## Requirements

This project requires Java 17+ and Maven 3 to be installed and available from the `PATH`.

In order to access the Maven artifacts of this repository, you will need a [fine-grained personal access token](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens#creating-a-fine-grained-personal-access-token) that can read public repositories.

Once you have generated it, you can associate it with the repository in this project's `pom.xml` file from your `.m2/settings.xml` file, like this:

```xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                      http://maven.apache.org/xsd/settings-1.0.0.xsd">
  <servers>
    <server>
      <id>github-emf-rdf</id>
      <username>your_github_username</username>
      <password>your_personal_access_token</password>
    </server>
  </servers>
</settings>
```

Alternatively, you can remove the `<repositories>` from the `pom.xml`, and instead build and install locally the plain Maven artifact for the EMF repository.
Follow [the instructions in the CONTRIBUTING](../../CONTRIBUTING.md#building-locally-the-plain-maven-artifacts) for details.

## Execution

To try it out, run the following command:

```bash
mvn compile
```

## Process outline

The Ant build inside the Maven file first registers the Epsilon Ant tasks.
It uses a Groovy script to associate the RDF resource factory with the `.rdfres` file extension.

After this, we can use the Epsilon Ant tasks as normal.
In this specific case, the `program.eol` prints out the names of various employees in the RDF graph:

```
Employees in the Development Team team: Sequence {"Jane Sue", "John Doe"}
Employees in the Marketing Team team: Sequence {}
Names of all employees: Sequence {"Jane Sue", "John Doe"}
```