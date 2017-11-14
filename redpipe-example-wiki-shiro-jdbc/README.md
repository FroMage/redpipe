This project contains an example Wiki application based on [A gentle guide to asynchronous 
programming with Eclipse Vert.x for Java developers](http://vertx.io/docs/guide-for-java-devs).

It is using Apache Shiro for authentication, with users and roles defined in `src/main/resources/wiki-users.properties`
and plain old Jdbc for database connection, using an Hsql database.

You can run the example by executing the `net.redpipe.example.wiki.shiroJdbc.Main` class and
visiting the [Wiki page](http://localhost:9000/wiki), which should redirect you to a login page
where you can try any of the user names defined for Apache Shiro (try `root`/`w00t` for admin access).

It also contains a [REST endpoint](http://localhost:9000/wiki/api) which uses JWT tokens (see tests),
and an [AngularJS application using that endpoint](http://localhost:9000/wiki/app).
   