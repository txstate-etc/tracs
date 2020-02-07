REPO_ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && cd ../.. && pwd )"

echo "Starting build..."
cd $REPO_ROOT
MAVEN_OPTS="-Xmx1024m -XX:+TieredCompilation -XX:TieredStopAtLevel=1"
mvn clean install sakai:deploy -Dmaven.tomcat.home=$REPO_ROOT/tracs-docker
