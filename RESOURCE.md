# EMF resource for RDF graphs

This repository includes a prototype implementation of an EMF resource for RDF graphs, on top of [Apache Jena](https://jena.apache.org/).

It can be installed from the repository's update site: see [`README`](./README.md) for details.

This RDF-EMF resource produces an EMF model representation of an RDF model; this could be an EMF model save as RDF or an RDF model from another programming. One or more RDF model files and schemas can be combined/reasoned before being deserialising against an EMF ECore meta-model. An EMF model instance composed of `EObject`s for the RDF model element that have an `rdf:type` statement that matches an `EClass` in the configured ECore meta-model.

## Differences with emf-triple

This implementation has some major differences with [emf-triple](https://github.com/ghillairet/emftriple):

* A single resource can combine information from multiple sources (e.g. Turtle or RDF/XML files).
* OWL inference is supported.

These differences are achieved by loading an intermediary `.rdfres` file with all the data and schema models to be combined, as well as any relevant options.

## Current limitations

Saving has only been tested against file-based locations. We have not tested saving into triple stores.

The name spaces of the EMF meta-model and the subject (class name) in RDF type statement need to match for an EMF model to be produced by the resource.

Names spaces for creating EMF models must be configured in the rdfres, only one is supported at the moment.

## .rdfres file format

Suppose you have a `model.ttl` Turtle file with some statements of interest, written against an ontology in `schema.ttl`.

Suppose as well that the RDF resources in `model.ttl` follows certain conventions that relate them to an Ecore metamodel, in the [MOF2RDF](https://www.omg.org/spec/MOF2RDF/1.0/About-MOF2RDF) style:

* There are `rdf:type` predicates from the RDF resource to another RDF resource whose URI is `ePackageNamespaceURI#eClassName`.
* Statements use predicates with URIs of the form `ePackageNamespaceURI#eStructuralFeatureName`:
  * Predicate objects can be other RDF resources (in the case of `EReference`s), or literals (in the case of `EAttribute`s).
  * RDF lists are supported for many-valued features.

In that case, you could write an `.rdfres` file like this, and load it as an EMF resource where the relevant RDF resources would be deserialised into EMF `EObject`s:

```yaml
dataModels:
  - model.ttl
schemaModels:
  - schema.ttl
```

The `.rdfres` file can then be loaded and used by any EMF-compatible tool as usual.
Note that the elements in `dataModels` and `schemaModels` can be arbitrary URIs understood by the [RIOT](https://jena.apache.org/documentation/io/) system in Jena, and not just relative paths from the folder of the `.rdfres` file.

### Model validation

RDF model validation can be enabled by adding a line in the `.rdfres`, e.g. `validationMode: jena-clean`. The following validation modes are available :

- none
- jena-valid: validation passes if the model has no internal inconsistencies, even though there may be some warnings.
- jena-clean: validation passes if the model has no internal inconsistencies and there are no warnings.

```yaml
validationMode: jena-clean
dataModels:
  - model.ttl
schemaModels:
  - schema.ttl
```

### Multi-value attributes

`EAttribute`s with cardinality > 1 ("multi-value attributes") are supported by the resource.
This includes support for the `unique` and `ordered` flags.

Note that unique values are not enforced by RDF: the resource leaves it to EMF `EList`s to ensure uniqueness of values.
Duplicates in an RDF data model are persisted when handled as an EMF model.
Removing a value from a unique where there are duplicates of a value will remove all instances of the duplicate value: this maintains an equivalence between EMF and RDF represenations of the model.

The resource represents multi-value attributes in RDF using [Containers](https://www.w3.org/TR/rdf-schema/#ch_containervocab) or [Lists](https://www.w3.org/TR/rdf-schema/#ch_collectionvocab).
The resource will opt to update the RDF representation of a multi-value attribute based on its current data structure (container/list) in the RDF data model.
However, when there is no existing structure, by default a container will be used.
If you would prefer to use lists for representing multi-value attributes, you can specify `multiValueAttributeMode: List` in the `.rdfres` file, as follows:

```yaml
validationMode: jena-clean
dataModels:
  - model.ttl
schemaModels:
  - schema.ttl
multiValueAttributeMode: List
```
