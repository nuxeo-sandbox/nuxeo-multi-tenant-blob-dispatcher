# Nuxeo Multi-Tenant Blob Dispatcher

[![Build Status](https://qa.nuxeo.org/jenkins/buildStatus/icon?job=Sandbox/sandbox_nuxeo-multi-tenant-blob-dispatcher-master)](https://qa.nuxeo.org/jenkins/view/Sandbox/job/Sandbox/job/sandbox_nuxeo-multi-tenant-blob-dispatcher-master/)

Multi-Tenant blob dispatcher to store blobs by tenant identifier.

# This version requires at least 10.10-HF26 of the Nuxeo Platform

## Build and Install

Build with maven (at least 3.3)

```
mvn clean install
```
> Package built here: `nuxeo-multi-tenant-blob-dispatcher-package/target`

> Install with `nuxeoctl mp-install <package>`

## Usage

Multi-Tenant blob dispatcher, that uses the repository name as the blob provider.

Alternatively, it can be configured through properties to dispatch to a blob provider based on document properties instead of the repository name.

The property name is a list of comma-separated clauses, with each clause consisting of a property, an operator and a value. The property can be a Document xpath, ecm:repositoryName, or, to match the current blob being dispatched, blob:name, blob:mime-type, blob:encoding, blob:digest, blob:length or blob:xpath. Comma-separated clauses are ANDed together. The special name default defines the default provider, and must be present.

Binaries may be stored by tenant with use of the ecm:tenant property. Tenant name is case sensitive.

Available operators between property and value are =, !=, <, > and ~. The operators < and > work with integer values. The operator ~ does glob matching using ? to match a single arbitrary character, and * to match any number of characters (including none).

For example, to dispatch to the "first" provider if dc:format is "video", to the "second" provider if the blob's MIME type is "video/mp4", to the "third" provider if the blob is stored as a secondary attached file, to the "fourth" provider if the lifecycle state is "approved" and the document is in the default repository, and otherwise to the "other" provider:

```xml
<extension target="org.nuxeo.ecm.core.blob.DocumentBlobManager" point="configuration">

    <blobdispatcher>
      <class>org.nuxeo.ecm.multi.tenant.MultiTenantBlobDispatcher</class>
      <property name="dc:format=video">first</property>
      <property name="blob:mime-type=video/mp4">second</property>
      <property name="blob:xpath~files/*/file">third</property>
      <property name="ecm:repositoryName=default,ecm:lifeCycleState=approved">fourth</property>
      <property name="ecm:tenant=foobar">fifth</property>
      <property name="default">other</property>
    </blobdispatcher>

  </extension>

```

## Support

**These features are sand-boxed and not yet part of the Nuxeo Production platform.**

These solutions are provided for inspiration and we encourage customers to use them as code samples and learning resources.

This is a moving project (no API maintenance, no deprecation process, etc.) If any of these solutions are found to be useful for the Nuxeo Platform in general, they will be integrated directly into platform, not maintained here.

## Licensing

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)

## About Nuxeo

Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris.

More information is available at [www.nuxeo.com](http://www.nuxeo.com).

