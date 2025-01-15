# RDF driver for Epsilon

This is a prototype of an [Epsilon Model Connectivity](https://eclipse.dev/epsilon/doc/emc/) driver for RDF and related technologies, based on [Apache Jena](https://jena.apache.org/).

This document provides instructions for users.
For instructions on how to set up a development environment, please see [`CONTRIBUTING.md`](./CONTRIBUTING.md).

## Features and limitations

Currently, the driver can:

* Read and query one or more RDF documents in the formats supported by Jena (Turtle and RDF/XML have been tested).

For now, the driver has these limitations:

* Does *not* support modifying or saving RDF documents.
* Does *not* integrate the OWL or RDFS support in Jena.

The driver requires Epsilon 2.1 or newer: it will not work on Epsilon 1.x due to breaking API changes from 1.x to 2.x.

## Installation

### Eclipse IDE

Install the features from this update site:

https://epsilonlabs.github.io/emc-rdf/updates/

Alternatively, you can download a [zipped version](https://epsilonlabs.github.io/emc-rdf/updates.zip) of the update site.

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

To list all the resources that have an `rdf:type` predicate with a certain object acting as their type, use `Prefix::Type.all`:

```
foaf::Person.all.println('All people: ');
```

You could also use `` `foaf:Person` `` to follow a more RDF-like style, but you would need backticks to escape the name.

If there is no risk of ambiguity, you can just use the type name:

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

If we need to specify a prefix, we can use `` x.`prefix:localName` `` or `` x.`prefix::localName` `` (the EOL grammar requires backticks for colons inside property names, whether it's `:` or `::`).

If there is no risk of ambiguity, you can also drop the prefix:

```
goblin.enemyOf.println('Enemies of the Green Goblin: ');
```

If there are predicates with the same local name but different namespace URIs in the graph formed by all the loaded documents in a model, a warning will be issued.
In this case, you should be using a prefix to avoid the ambiguity.

**Note:** currently `resource.p` will always return a collection, as we do not leverage yet the RDFS descriptions that could indicate the cardinality of `p`.

### Values of predicates

The values in `resource.p` will be either other resources, or the values of the associated literals (without filtering by language tags).

It is possible to mention a language suffix, to limit the results to literals with that language.
For instance:

```
var spider = Model.getElementById('http://example.org/#spiderman');
spider.`name@ru`.println('Name of Spiderman in Russian: ');
```

### Accessing literal objects

If you would like to access the `RDFLiteral` objects rather than just their values, use a `_literal` suffix as part of the local name (before any language tags).
For instance, we could change the above example to:

```
var spider = Model.getElementById('http://example.org/#spiderman');
spider.`name_literal@ru`.println('Name literal of Spiderman in Russian: ');
```

`RDFLiteral` objects have several properties:

* `value`: the raw value of the literal (usually a String, but it can be different for typed literals - see [Apache Jena typed literals](https://jena.apache.org/documentation/notes/typed-literals.html)).
* `language`: the language tag for the literal (if any).
* `datatypeURI`: the datatype URI for the literal.

### Limiting returned literals to preferred languages

The "Language tag preference" section of the RDF model configuration dialog allows for specifying a comma-separated list of [BCP 47](https://www.ietf.org/rfc/bcp/bcp47.txt) language tags.
If these preferences are set, `x.property` will filter literals, by only returning the values for the first tag with matches, or falling back to the untagged values if no matches are found for any of the mentioned tags.

For instance, if we set the language preferences to `en-gb,en`, filtering `x.property` will work as follows:

* If any `en-gb` literals exist, return only those.
* If any `en` literals exist, return only those.
* Otherwise, return the untagged literals (if any).

Language preferences do not apply if an explicit language tag is used: `x.property@en` will always get the `en`-tagged literals, and `x.property@` will always get the untagged literals.

### Data models, schema models and reasoners

*Currently, all data models and schema models are combined into single models for each respective type. These combined models are then passed to a reasoner to produce a single (OWL) inferred model which can be queried.*

RDF models are loaded as Ontology Resource Models with Jena's default OWL reasoner. This reasoner infers OWL concepts on to an RDF data model when it is loaded. Future updates to this driver could enable different Jena reasoners to be selected.

On the RDF model configuration dialog there is a section "Data Model URLs to load", this section enables several URLs to be added for RDF Data models. Each of the Data model in the list is loaded and added to a single Data Model. URLs for Schema Models can be added in the section "Schema Model URLs to load"; schema models are added to a single Schema Model. The resulting combined Data and Schema Models are then processed by Jena's reasoner using the default OWL settings. The resulting inferred model is then used as the RDFModel that queries can be performed on. 

#### Schema defined restrictions (max cardinality)

An RDF Schema can contain restrictions for some properties in an RDF data model such as maximum cardinality. If a maximum cardinality is defined for a property in the RDF data model that have been loaded, then **returned property values are limited to the most restrictive max cardinality that is applied to a property**. 
 
