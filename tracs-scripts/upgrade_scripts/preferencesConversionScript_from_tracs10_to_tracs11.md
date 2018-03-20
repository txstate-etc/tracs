

##### sakai 11 structured user's site preferences differently with sakai 11

###### Therefore need to convert existing users' site preferences to fit into sakai 11's preferences

Here are the steps:

1. Go to server host
2. ``` cd /tomcat/server/directory```
3. ``` java -cp "lib/*" -Dtomcat.dir="$PWD" org.sakaiproject.user.util.ConvertUserFavoriteSitesSakai11```


Note: for staging, it only took 13 mins to finish for the 132075 preferences to migrate.  Script could be run multiple times if interrupted. 