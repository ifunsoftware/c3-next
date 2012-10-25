The project requires some Eclipse Orbit artifacts in order to be built and run.

Artifacts can be installed to local maven repository using following command (for apache lucene):

for i in $(ls); do GROUP="org.apache.lucene"; ARTIFACT=$(echo -n $i | cut -d _ -f 1); VERSION=$(echo -n $i | cut -d _ -f 2 | cut -d - -f 1); mvn install:install-file -Dfile=$i -DgroupId=$GROUP -DartifactId=$ARTIFACT -Dversion=$VERSION -Dpackaging=jar; done;