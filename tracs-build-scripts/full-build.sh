REPO_ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && cd .. && pwd )"

echo "Removing files from previous builds..."
rm -r $REPO_ROOT/docker/components $REPO_ROOT/docker/lib $REPO_ROOT/docker/webapps
mkdir $REPO_ROOT/docker/lib
mkdir -p /tmp/tracs-apache
echo "Copying core tomcat libraries..."
wget -qO- http://archive.apache.org/dist/tomcat/tomcat-8/v8.0.36/bin/apache-tomcat-8.0.36.tar.gz | tar -xz --strip-component 1 -C /tmp/tracs-apache
cp -r /tmp/tracs-apache/lib/* $REPO_ROOT/docker/lib/
rm -r /tmp/tracs-apache

echo "Starting build..."
cd $REPO_ROOT
MAVEN_OPTS="-Xmx1024m -XX:+TieredCompilation -XX:TieredStopAtLevel=1"
mvn clean install sakai:deploy -Dmaven.tomcat.home=$REPO_ROOT/docker -P mysql
