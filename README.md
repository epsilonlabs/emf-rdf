# RDF driver for Epsilon

This is a prototype of an [Epsilon Model Connectivity](https://eclipse.dev/epsilon/doc/emc/) driver for RDF and related technologies, based on [Apache Jena](https://jena.apache.org/).

## Features and limitations

Currently, the driver can:

* Read and query one or more RDF documents in the formats supported by Jena (Turtle and RDF/XML have been tested).

For now, the driver has these limitations:

* Does *not* support modifying or saving RDF documents.
* Does *not* integrate the OWL or RDFS support in Jena.

## Installation

### Eclipse IDE

Install the features from this update site:

https://epsilonlabs.github.io/emc-rdf/updates/

Example projects are available from the [`examples`](./examples) folder of this repository.

### Maven

The RDF driver is available as a Maven dependency from [Github Packages](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry):

```
<dependency>
  <groupId>org.eclipse.epsilon</groupId>
  <artifactId>org.eclipse.epsilon.emc.rdf</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Features

This is a high-level description of the features of the driver.
The features are described using examples based on the [W3C RDF 1.2 Turtle](https://www.w3.org/TR/rdf12-turtle/#sec-intro) example:

```
BASE <http://example.org/>
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
PREFIX rel: <http://www.perceive.net/schemas/relationship/>

<#green-goblin>
  rel:enemyOf <#spiderman> ;
  a foaf:Person ;    # in the context of the Marvel universe
  foaf:name "Green Goblin" .

<#spiderman>
  rel:enemyOf <#green-goblin> ;
  a foaf:Person ;
  foaf:name "Spiderman", "Человек-паук"@ru .
```

### Accessing model elements

To obtain a specific RDF resource by its URL:

```
var goblin = Model.getElementById('http://example.org/#green-goblin');
```

To list all the resources that have an `rdf:type` predicate with a certain object acting as their type, use `Type.all`:

```
`foaf:Person`.all.println('All people: ');
```

Note the above use of `foaf:Person`, using both the prefix and the local name, and the use of backticks to escape the name.
If there is no risk of ambiguity, you can just use the local name:

```
Person.all.println('All people: ');
```

By default, the prefixes are read from the documents, but you can also specify custom prefixes while loading the model.

### Accessing predicates

Using `resource.p`, you can access all the objects of the `p` predicate where `resource` is the subject.
For example:

```
goblin.`rel:enemyOf`.println('Enemies of the Green Goblin: ');
```

Note that currently `resource.p` will always return a collection, as we do not leverage yet the RDFS descriptions that could indicate the cardinality of `p`.

If there is no risk of ambiguity, you can also drop the prefix:

```
goblin.enemyOf.println('Enemies of the Green Goblin: ');
```

If there are predicates with the same local name but different namespace URIs in the graph formed by all the loaded documents in a model, a warning will be issued.
In this case, you should be using a prefix to avoid the ambiguity.

It is possible to mention a language suffix, to limit the results to literals with that language.
For instance:

```
var spider = Model.getElementById('http://example.org/#spiderman');
spider.`name@ru`.println('Name of Spiderman in Russian: ');
```
