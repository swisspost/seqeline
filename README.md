<p align='right'>A <a href="https://developer.post.ch/">swisspost</a> project <a href="https://developer.post.ch/" border=0><img align="top"  src='https://avatars.githubusercontent.com/u/92710854?s=32&v=4'></a></p>


# seqeline

Generate RDF data lineage graph from PL/SQL code.

## Install

### Prerequisites
- Java 17+
- Maven 3.6+

### Install PL/SQL grammar
```
git clone https://github.com/antlr/grammars-v4.git
cd grammars-v4
mvn install -am -pl sql/plsql
```

### Install seqeline
```
git clone https://github.com/swisspost/seqeline.git
mvn install
```

## Run

### Collect Metadata

In order to compute lineage information, seqeline needs schema metadata from the database.

```
seqeline --database=<db-url> -u <username> -p <password> 
```

### Usage

```
Usage: seqeline [-git] [--publish] [--tree-only] [-a=<application>]
                 [-c=<cacheDir>] [-d=<domain>]
                 [--graphdb-url=<graphDbRepositoryUrl>] [-o=<outputDir>]
                 ([<paths>...] | [-b=<dbUrl> -u=<username> -p=<password>])
Generate RDF data lineage graph from PL/SQL code.
  -a, --application=<application>
                           Application name
  -c, --cache-dir=<cacheDir>
                           Cache directory
  -d, --domain=<domain>    Domain name to use in RDF URLs
  -g, --force-graph        Ignore cached files and force graph generation
      --graphdb-url=<graphDbRepositoryUrl>
                           GraphDB repository to publish to.
  -i, --ignore-errors      Continue on errors
  -o, --output-dir=<outputDir>
                           Output directory for graphs
      --publish            Publish graphs to GraphDB
  -t, --force-tree         Ignore cached files and force tree generation
      --tree-only          Only generate tree
File system Sources
      <paths>...           Source files or directories
Fetch metadata from database
  -b, --database=<dbUrl>   JDBC URL to fetch metadata (if present, seqeline
                             only fetches the metadata)
  -p, --password=<password>
                           Database password or @<file> containing password.
  -u, --username=<username>
                           Database user
```
