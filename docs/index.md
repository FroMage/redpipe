---
layout: default
title: Vertxrs
---

vertx-rs
========

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
        	JsonObject config = new JsonObject()
        			.put("scan", new JsonArray()
        					.add(Main.class.getPackage().getName()));
        	
        	new Server().start(config).subscribe(v -> System.err.println("Server started"),
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

Start your `Main` program and if you head over to `http://localhost:9000` you should see `Hello World`.

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

Restart your `Main` program and if you head over to `http://localhost:9000/reactive` you should see 
`Hello Reactive World`.
