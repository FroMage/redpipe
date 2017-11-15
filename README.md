Redpipe is a Web Framework that unites the power and versatility of Eclipse Vert.x, 
the conciseness of JAX-RS (with Resteasy), and the non-blocking reactive composition
of RxJava.

The main idea is that with Redpipe you write your endpoints in JAX-RS, using RxJava
composition if you want, and underneath it all, Vert.x is running the show and you
can always access it if you need it.

Redpipe is opinionated, favors convention over configuration, and lets you combine the
following technologies easily to write reactive web applications:

- Eclipse Vert.x and Netty, for all the low-level plumbing,
- JAX-RS (Resteasy), for all your web endpoints,
- RxJava for your reactive code,
- Vert.x Web for all existing filters, templating support,
- Swagger to describe your web endpoints,
- CDI (Weld) for JAX-RS discovery and injection (optional),
- Bean Validation (Hibernate Validator) to validate your web endpoints’ input (optional),
- Coroutines (Quasar) to write synchronous-looking reactive code (optional).

More info at http://redpipe.net
