---
layout: default
title: Vertxrs
---

# vertx-rs

Vertx-rs is a Web Framework that unites the power and versatility of [Eclipse Vert.x](http://vertx.io), the conciseness of 
[JAX-RS](https://javaee.github.io/tutorial/jaxrs002.html#GILIK) (with [Resteasy](http://resteasy.jboss.org)), 
and the non-blocking reactive composition of [RxJava](https://github.com/ReactiveX/RxJava).

The main idea is that with vertx-rs you write your endpoints in JAX-RS, using RxJava composition if you want, and underneath
it all, Vert.x is running the show and you can always access it if you need it.

## Set-up

Include the following dependency in your `pom.xml`:

{% highlight xml %}
<dependency>
  <groupId>org.vertx-rs</groupId>
  <artifactId>vertx-rs-engine</artifactId>
  <version>0.0.1-SNAPSHOT</version>
</dependency>
{% endhighlight %}

## Quick Examples

Create a `Main` class to start your server:

{% highlight java %}
import org.vertxrs.engine.core.Server;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class Main {
  public static void main( String[] args ){
    new Server()
     .start(HelloResource.class)
     .subscribe(
        v -> System.err.println("Server started"),
        x -> x.printStackTrace());
  }
}
{% endhighlight %}

### Creating a resource

Create a resource:

{% highlight java %}
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/")
public class HelloResource {
  @GET
  public String hello() {
    return "Hello World";
  }
}
{% endhighlight %}

Start your `Main` program and if you head over to [http://localhost:9000](http://localhost:9000) you should see `Hello World`.

### Creating a reactive resource

If you want to return a reactive resource, you can simply return a reactive type from your resource,
such as those provided by RxJava.

For a reactive Hello World, simply add this method to your resource:

{% highlight java %}
@Path("reactive")
@GET
public Single<String> helloReactive() {
  return Single.just("Hello Reactive World");
}
{% endhighlight %}

Restart your `Main` program and if you head over to [http://localhost:9000/reactive](http://localhost:9000/reactive)
you should see `Hello Reactive World`.

## RxJava support

Out of the box, we support RxJava's [`Single`](http://reactivex.io/documentation/single.html)
and [`Observable`](http://reactivex.io/documentation/observable.html) types.

If your resource returns a `Single<T>`, the `T` will be send to your client asynchronously as if you
returned it directly. In particular, standard and custom
[`MessageBodyWriter<T>`](https://javaee.github.io/javaee-spec/javadocs/javax/ws/rs/ext/MessageBodyWriter.html)
apply as soon as the `Single` is _resolved_, as do 
[interceptors](http://docs.jboss.org/resteasy/docs/3.1.4.Final/userguide/html/Interceptors.html).

If your resource returns an `Observable<T>`:

- By default, every item will be collected, and once complete, assembled in a `List<T>` and treated
  as if your method had returned it directly (standard and custom body writers and interceptors apply
  to it).
- If you annotate your method with `@Stream` (from `org.jboss.resteasy.annotations`), then every item
  will be sent to the client as soon as it is produced (again, via standard and custom body writers).
  This is mostly useful for streaming bytes, buffers, or strings, which can be split up and buffered.
- If you annotate your method with `@Produces(MediaType.SERVER_SENT_EVENTS)`, then every item will
  be sent to the client over Server-Sent Events (SSE). As always, standard and custom body writers
  are called to serialise your entities to events.

## Fibers

The optional module `vertx-rs-fibers` allows you to write your reactive code in _fibers_, which are
light-weight threads, also known as _coroutines_, while interacting seamlessly with RxJava.

Consider the following traditional RxJava code to forward the result of two web service invocations:

{% highlight java %}
@Path("composed")
@GET
public Single<String> helloComposed(@Context Vertx vertx,
                                    @Context UriInfo uriInfo) {
  Single<String> request1 = get(vertx, getUri(uriInfo, null));
  Single<String> request2 = get(vertx, getUri(uriInfo, "helloReactive"));
      
  return request1.zipWith(request2, (hello1, hello2) -> hello1 + "\n" + hello2);
}

private URI getUri(UriInfo uriInfo, String methodName) {
  UriBuilder builder = uriInfo.getBaseUriBuilder();
  if(methodName != null)
    builder.path(HelloResource.class, methodName);
  else
    builder.path(HelloResource.class);
  return builder.build();
}

private Single<String> get(Vertx vertx, URI uri){
  WebClient client = WebClient.create(vertx);
  Single<HttpResponse<Buffer>> responseHandler = 
    client.get(uri.getPort(), uri.getHost(), uri.getPath()).rxSend();

  return responseHandler.map(response -> response.body().toString());
}
{% endhighlight %}

Rather than composing sequential `Single` values, you can write a _fiber_ that can
_await_ those values in what now looks like sequential code:

{% highlight java %}
@Path("fiber")
@GET
public Single<String> helloFiber(@Context Vertx vertx,
                                 @Context UriInfo uriInfo) {
  return Fibers.fiber(() -> {
    String hello1 = Fibers.await(get(vertx, getUri(uriInfo, null)));
    String hello2 = Fibers.await(get(vertx, getUri(uriInfo, "helloReactive")));
    
    return hello1 + "\n" + hello2;
  });
}
{% endhighlight %}

A _fiber_, here, is also a `Single` which can be integrated with the rest of your RxJava
operations, and it can `await` RxJava `Single` values to obtain their resolved value.

You can call `await` from any fiber body, or from any method annotated with `@Suspendable`.

You need to import the following module in order to use fibers:

{% highlight xml %}
<dependency>
  <groupId>org.vertx-rs</groupId>
  <artifactId>vertx-rs-fibers</artifactId>
  <version>0.0.1-SNAPSHOT</version>
</dependency>
{% endhighlight %}

And also the following Maven build plug-in to set-up your fibers at compile-time:

{% highlight xml %}
<build>
  <plugins>
    <plugin>
      <groupId>com.vlkan</groupId>
      <artifactId>quasar-maven-plugin</artifactId>
      <version>0.7.9</version>
      <configuration>
        <check>true</check>
        <debug>true</debug>
        <verbose>true</verbose>
      </configuration>
      <executions>
        <execution>
          <goals>
            <goal>instrument</goal>
          </goals>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
{% endhighlight %}

See [Quasar's documentation](http://docs.paralleluniverse.co/quasar/) for more information on fibers. 

## Resource scanning

You can either declare your JAX-RS resources and providers when instantiating the `Server` (as shown
above), or you can use two options to scan your packages to discover them.

### Fast-classpath-scanner

If you include this dependency, [fast-classpath-scanner](https://github.com/lukehutch/fast-classpath-scanner)
will be used to scan your classpath for resources and providers:

{% highlight xml %}
<dependency>
  <groupId>org.vertx-rs</groupId>
  <artifactId>vertx-rs-fast-classpath-scanner</artifactId>
  <version>0.0.1-SNAPSHOT</version>
</dependency>
{% endhighlight %}

You just need to set the `scan` configuration to an array of package names to scan.

### CDI

Alternately, you can delegate scanning of resources and providers to [CDI](http://cdi-spec.org):

{% highlight xml %}
<dependency>
  <groupId>org.vertx-rs</groupId>
  <artifactId>vertx-rs-cdi</artifactId>
  <version>0.0.1-SNAPSHOT</version>
</dependency>
{% endhighlight %}

All your jars containing a `META-INF/beans.xml` file will be scanned by CDI and your resources and providers
will become CDI beans, with CDI injection supported.

Note that this uses [Weld](http://weld.cdi-spec.org) and the weld-vertx extension which brings a 
[lot of goodies for CDI and Vert.x](http://docs.jboss.org/weld/weld-vertx/latest/).

## Configuration

Unless you call `Server.start()` with a `JsonObject` configuration to override it, 
the `conf/config.json` file will be loaded and used for configuration. Here are the configuration options
available:

<table>
 <caption>General options</caption>
 <tr>
  <th>Name</th>
  <th>Type</th>
  <th>Description</th>
  <th>Default</th>
 </tr>
 <tr>
  <td>db_url</td>
  <td>String</td>
  <td>Jdbc Url to use for connections to the database.</td>
  <td>jdbc:hsqldb:file:db/wiki</td>
 </tr>
 <tr>
  <td>db_driver</td>
  <td>String</td>
  <td>Jdbc driver class.</td>
  <td>org.hsqldb.jdbcDriver</td>
 </tr>
 <tr>
  <td>db_max_pool_size</td>
  <td>Integer</td>
  <td>Maximum pool size for database connections.</td>
  <td>30</td>
 </tr>
 <tr>
  <td>http_port</td>
  <td>Integer</td>
  <td>Http port to bind to.</td>
  <td>9000</td>
 </tr>
</table> 

<table>
 <caption>For the `vertx-rs-fast-classpath-scanner` module</caption>
 <tr>
  <th>Name</th>
  <th>Type</th>
  <th>Description</th>
  <th>Default</th>
 </tr>
 <tr>
  <td>scan</td>
  <td>String[]</td>
  <td>List of packages to scan for JAX-RS resources and providers.</td>
  <td></td>
 </tr>
</table> 

You can access the current configuration in your application via the `AppGlobals.getConfig()` method.

## Injection

On top of optional CDI support, JAX-RS supports injection of certain resources via the `@Context` annotation
on method parameters and members. Besides the regular JAX-RS resources, the following resources can be
injected:

<table>
 <caption>Injectable global resources</caption>
 <tr>
  <th>Type</th>
  <th>Description</th>
 </tr>
 <tr>
  <td>io.vertx.rxjava.core.Vertx</td>
  <td>The Vert.x instance.</td>
 </tr>
 <tr>
  <td>org.vertxrs.engine.core.AppGlobals</td>
  <td>A global context object.</td>
 </tr>
</table> 

<table>
 <caption>Injectable per-request resources</caption>
 <tr>
  <th>Type</th>
  <th>Description</th>
 </tr>
 <tr>
  <td>io.vertx.rxjava.ext.web.RoutingContext</td>
  <td>The Vert.x Web `RoutingContext`.</td>
 </tr>
 <tr>
  <td>io.vertx.rxjava.core.http.HttpServerRequest</td>
  <td>The Vert.x request.</td>
 </tr>
 <tr>
  <td>io.vertx.rxjava.core.http.HttpServerResponse</td>
  <td>The Vert.x response.</td>
 </tr>
 <tr>
  <td>io.vertx.rxjava.ext.auth.AuthProvider</td>
  <td>The Vert.x `AuthProvider` instance, if any (defaults to `null`).</td>
 </tr>
 <tr>
  <td>io.vertx.rxjava.ext.auth.User</td>
  <td>The Vert.x `User`, if any (defaults to `null`).</td>
 </tr>
 <tr>
  <td>io.vertx.rxjava.ext.web.Session</td>
  <td>The Vert.x Web `Session` instance, if any (defaults to `null`).</td>
 </tr>
</table> 

## Templating

We support the following plugable template engines, which you just have to add a dependency on:

<tale>
 <caption>Plugable template engine modules</caption>
 <tr>
  <th>Name</th>
  <th>Dependency</th>
 </tr>
 <tr>
  <td>[FreeMarker](http://freemarker.apache.org)</td>
  <td>
{% highlight xml %}
<dependency>
  <groupId>org.vertx-rs</groupId>
  <artifactId>vertx-rs-templating-freemarker</artifactId>
  <version>0.0.1-SNAPSHOT</version>
</dependency>
{% endhighlight %}
  </td>
 </tr>
</table>

In order to declare templates, simply place them in the `src/main/resources/templates` folder. For
example, here's our `src/main/resources/templates/index.ftl` template:

{% highlight html %}
<html>
 <head>
  <title>${context.title}</title>
 </head>
 <body>${context.message}</body>
</html>
{% endhighlight %}

In order to return a rendered template, just return them from your resource, directly or as a `Single`:

{% highlight java %}
@GET
@Path("template")
public Template template(){
  return new Template("templates/index.ftl")
          .set("title", "My page")
          .set("message", "Hello");
}
{% endhighlight %}

### Writing your own template renderer

You can write your own template renderer by declaring a `META-INF/services/org.vertxrs.engine.template.TemplateRenderer` file in your
`src/main/resources` folder, containing the name of the class that extends the 
`org.vertxrs.engine.template.TemplateRenderer` interface.

For example, this is the FreeMarker renderer, which simply wraps the [Vert.x Web support for 
freemarker](https://github.com/vert-x3/vertx-web/tree/master/vertx-template-engines/vertx-web-templ-freemarker):

{% highlight java %}
public class FreeMarkerTemplateRenderer implements TemplateRenderer {

  private final FreeMarkerTemplateEngine templateEngine = FreeMarkerTemplateEngine.create();

  @Override
  public boolean supportsTemplate(String name) {
    return name.toLowerCase().endsWith(".ftl");
  }

  @Override
  public Single<Response> render(String name, Map<String, Object> variables) {
    RoutingContext context = ResteasyProviderFactory.getContextData(RoutingContext.class);
    for (Entry<String, Object> entry : variables.entrySet()) {
      context.put(entry.getKey(), entry.getValue());
    }
    return templateEngine.rxRender(context, name)
            .map(buffer -> Response.ok(buffer, MediaType.TEXT_HTML).build());
  }
}
{% endhighlight %}


## Serving static files

If you want to serve static files you can place them in `src/main/resources/webroot` and declare the
following resource:

{% highlight java %}
@Path("")
public class AppResource extends FileResource {
  @Path("webroot{path:(/.*)?}")
  @GET
  public Response get(@PathParam("path") String path) throws IOException{
    return super.getFile(path);
  }
}
{% endhighlight %}

Your files will then be accessible from the `/webroot/` path prefix.

## Plugins

You can write plugins by declaring a `META-INF/services/org.vertxrs.engine.spi.Plugin` file in your
`src/main/resources` folder, containing the name of the class that extends the 
`org.vertxrs.engine.spi.Plugin` class.

From there you can hook into various parts of the application (start-up, requests…).

## Swagger

## Examples

- Hello World
- Wiki (Reactive, DB, Fibers, Auth, Templates, REST, JWT, SocksJS, Angular) 
 - With Keycloak and Jooq
 - With Apache Shiro and Jdbc
- Kafka (SSE)

## How-to

### DB

### Auth

#### Keycloak

#### Apache Shiro

#### JWT

### SocksJS

### Kafka
