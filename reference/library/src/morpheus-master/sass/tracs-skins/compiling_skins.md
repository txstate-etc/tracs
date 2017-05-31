**To compile a skin:**

`$ cd $TRACS_REPO_PATH/reference`

`$ mvn clean install sakai:deploy -Dmaven.tomcat.home=$TRACS_REPO_PATH/tracs-docker/ -Dsakai.skin.target=<NAME OF SKIN> -Dsakai.skin.customization.file=<LOCATION OF CUSTOMIZATION FILE>`


For example, to compile the TRACS default skin:

`$ mvn clean install sakai:deploy -Dmaven.tomcat.home=$TRACS_REPO_PATH/tracs-docker/ -Dsakai.skin.target=tracs-default -Dsakai.skin.customization.file=./src/morpheus-master/sass/tracs-skins/_tracs-default.scss`

