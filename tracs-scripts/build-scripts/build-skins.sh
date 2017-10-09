REPO_ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && cd ../.. && pwd )"

echo "building skins..."

cd $REPO_ROOT/reference

SKINS="tracs-default tracs-blue tracs-yellow morpheus-default"

for SKIN in $SKINS
 do
   mvn clean compile -P $SKIN,compile-skin
done

mvn clean install sakai:deploy -Dmaven.tomcat.home=$REPO_ROOT/tracs-docker/

echo "finished building skins"
