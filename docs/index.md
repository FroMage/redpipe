---
layout: default
title: Vertxrs
---

# vertx-rs

Vertx-rs is a Web Framework that unites the power and versatility of Eclipse Vert.x, the conciseness of JAX-RS (with Resteasy), 
and the non-blocking reactive composition of RxJava.

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

Out of the box, we support RxJava's `Single` and `Observable` types.

If your resource returns a `Single<T>`, the `T` will be send to your client asynchronously as if you
returned it directly. In particular, standard and custom `MessageBodyWriter<T>` apply as soon as the
`Single` is _resolved_, as do interceptors.

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

### Fast-classpath-scanner

### CDI

## Injection

## Templating

Engines, rendering

## Plugins

How to write them

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
