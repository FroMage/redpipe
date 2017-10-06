Vert.x + JAX-RS + CDI prototype, with Wiki application sample from http://vertx.io/docs/guide-for-java-devs

How to launch:

    $ mvn clean install
    $ cd vertx-rs-wiki
    $ mvn exec:java -Dexec.mainClass=org.vertxrs.wiki.Main

Then go to http://localhost:9000/wiki and log in with `root/w00t`.
