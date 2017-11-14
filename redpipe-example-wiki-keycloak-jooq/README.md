This project contains an example Wiki application based on [A gentle guide to asynchronous 
programming with Eclipse Vert.x for Java developers](http://vertx.io/docs/guide-for-java-devs).

It is using Keycloak for authentication, with the setup described below,
and Jooq for database connection, using a Postgres database with the setup described below.

You can run the example by executing the `net.redpipe.example.wiki.keycloakJooq.Main` class and
visiting the [Wiki page](http://localhost:9000/wiki), which should redirect you to a login page
where you can try any of the user names described below (try `root`/`w00t` for admin access).

It also contains a [REST endpoint](http://localhost:9000/wiki/api) which uses JWT tokens (see tests),
and an [AngularJS application using that endpoint](http://localhost:9000/wiki/app).

== Note on tests ==

The tests use Docker images to set up Keycloak and Postgres.

== Keycloak setup ==

1. [Download Keycloak Standalone Server](http://www.keycloak.org/downloads.html) and run it.
1. [Create the admin account](http://localhost:8080/auth) with username `admin` and password `admin`.
1. Go to [the admin console](http://localhost:8080/auth/admin/) and log in with `admin`/`admin`.
1. [Create a new Realm](http://localhost:8080/auth/admin/master/console/#/create/realm) with name `demo`.
1. [Create a new Client](http://localhost:8080/auth/admin/master/console/#/create/client/stef) with `Client ID` set to `vertx` and
 `rootUrl` set to `http://localhost:9000`.
1. Once you have created the client, you're allowed to set up more properties, so stay in the page and change:
 1. `Access Type` to `confidential`
 1. `Valid Redirect URIs` to `http://localhost:9000/callback/*`  
1. Click on the `Roles` tab at the top
1. Click on `Add Role` for the following roles:
 1. `admin`
 1. `writer`
 1. `editor`
 1. `create`
 1. `update`
 1. `delete`
1. From the `Roles` tab, click `Edit` for the `admin` role
 1. Set `Composite Roles` to `true`
 1. In the bottom-most select-box named `Client Roles`, select `vertx`
 1. In the `Available Roles` list that appeared to the right, select `create`, `update`, `delete` and click on `Add selected`
1. From the `Roles` tab, click `Edit` for the `editor` role
 1. Set `Composite Roles` to `true`
 1. In the bottom-most select-box named `Client Roles`, select `vertx`
 1. In the `Available Roles` list that appeared to the right, select `create`, `update`, `delete` and click on `Add selected`
1. From the `Roles` tab, click `Edit` for the `writer` role
 1. Set `Composite Roles` to `true`
 1. In the bottom-most select-box named `Client Roles`, select `vertx`
 1. In the `Available Roles` list that appeared to the right, select `create` and click on `Add selected`
1. From the `Credentials` tab, copy the `Secret` value and store it in the `conf/config.json` file in `keycloak/credentials/secret`.
1. [Go to the keys page](http://localhost:8080/auth/admin/master/console/#/realms/demo/keys) and click on `Public key.
 1. Copy the displayed key and store it in the `conf/config.json` file in `keycloak/realm-public-key`.
1. [Go the users page](http://localhost:8080/auth/admin/master/console/#/realms/demo/users) and click on `Add user`
 1. Set `Username` to `root` and click `Save`
 1. Click on the `Credentials` top tab and set its password to `w00t`, then click `Reset password`
 1. Click on the `Role Mappings` top tab
  1. In the bottom-most select-box named `Client Roles`, select `vertx`
  1. In the `Available Roles` list that appeared to the right, select `admin` and click on `Add selected`
1. [Go the users page](http://localhost:8080/auth/admin/master/console/#/realms/demo/users) and click on `Add user`
 1. Set `Username` to `foo` and click `Save`
 1. Click on the `Credentials` top tab and set its password to `bar`, then click `Reset password`
 1. Click on the `Role Mappings` top tab
  1. In the bottom-most select-box named `Client Roles`, select `vertx`
  1. In the `Available Roles` list that appeared to the right, select `editor`, `writer` and click on `Add selected`
1. [Go the users page](http://localhost:8080/auth/admin/master/console/#/realms/demo/users) and click on `Add user`
 1. Set `Username` to `bar` and click `Save`
 1. Click on the `Credentials` top tab and set its password to `baz`, then click `Reset password`
 1. Click on the `Role Mappings` top tab
  1. In the bottom-most select-box named `Client Roles`, select `vertx`
  1. In the `Available Roles` list that appeared to the right, select `writer` and click on `Add selected`
1. [Go the users page](http://localhost:8080/auth/admin/master/console/#/realms/demo/users) and click on `Add user`
 1. Set `Username` to `baz` and click `Save`
 1. Click on the `Credentials` top tab and set its password to `baz`, then click `Reset password`

You're all set for Keycloak!

== Postgres set-up ==

1. Install Postgres
1. Log in as the `postgres` user
    $ sudo su - postgres
1. Create a `redpipewiki` user (with `redpipewiki` as password):
    postgres $ createuser -PSRD redpipewiki
1. Create a `redpipewiki` database:
    postgres $ createdb -O redpipewiki -E utf8 redpipewiki
1. Log out of the `postgres` user:
    postgres $ exit
1. Load the database schema:
    $ psql -h localhost -U redpipewiki redpipewiki
    redpipewiki=> \i vertx-rs-wiki/db.sql 

You're all set for Postgres!    
