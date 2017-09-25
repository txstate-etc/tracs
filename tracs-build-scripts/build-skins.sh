REPO_ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && cd .. && pwd )"

echo "building skins..."

cd $REPO_ROOT/reference

mvn clean compile  -Dsakai.skin.target=tracs-default -Dsakai.skin.customization.file=./src/morpheus-master/sass/tracs-skins/_tracs-default.scss
mvn clean compile  -Dsakai.skin.target=tracs-blue -Dsakai.skin.customization.file=./src/morpheus-master/sass/tracs-skins/_tracs-blue.scss
mvn clean compile  -Dsakai.skin.target=tracs-yellow -Dsakai.skin.customization.file=./src/morpheus-master/sass/tracs-skins/_tracs-yellow.scss

#clear the customizations so that the default sakai skin can be built
#> library/src/morpheus-master/sass/_customization.scss

#cd $REPO_ROOT

#mvn clean install sakai:deploy -Dmaven.tomcat.home=$REPO_ROOT/tracs-docker/

echo "finished building skins"
