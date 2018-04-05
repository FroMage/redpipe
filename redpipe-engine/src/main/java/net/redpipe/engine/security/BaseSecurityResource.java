package net.redpipe.engine.security;

import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import net.redpipe.engine.core.AppGlobals;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.auth.AuthProvider;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.Session;
import rx.Single;

@Path("/")
public abstract class BaseSecurityResource {

	public static final String REDIRECT_KEY = "__login_redirect";

	@GET
	@Path("/login")
	public abstract Object login(@Context UriInfo uriInfo);

	@POST
	@Path("/loginAuth")
	public Single<Response> loginAuth(@FormParam("username") String username, @FormParam("password") String password,
			@FormParam("return_url") String returnUrl, @Context Session session, @Context RoutingContext ctx,
			@Context AuthProvider auth) throws URISyntaxException {
		if (username == null || username.isEmpty() || password == null || password.isEmpty())
			return Single.just(Response.status(Status.BAD_REQUEST).build());

		JsonObject authInfo = new JsonObject().put("username", username).put("password", password);
		return auth.rxAuthenticate(authInfo).map(user -> {
			ctx.setUser(user);
			if (session != null) {
				// the user has upgraded from unauthenticated to authenticated
				// session should be upgraded as recommended by owasp
				session.regenerateId();
			}
			String redirectUrl = session.remove(REDIRECT_KEY);
			if (redirectUrl == null)
				redirectUrl = returnUrl;
			if (redirectUrl == null)
				redirectUrl = "/";

			try {
				return Response.status(Status.FOUND).location(new URI(redirectUrl)).build();
			} catch (URISyntaxException e) {
				throw new RuntimeException(e);
			}
		}).onErrorReturn(t -> {
			return Response.status(Status.FORBIDDEN).entity(t.getMessage()).type(MediaType.TEXT_PLAIN).build();
		});
	}

	@GET
	@Path("/logout")
	public Response logout(@Context UriInfo uriInfo, @Context RoutingContext ctx, @Context AppGlobals globals) {
		ctx.clearUser();
		UriBuilder builder = uriInfo.getBaseUriBuilder();
		URI rootUri = builder.path(globals.getMainClass()).build();
		return Response.status(Status.FOUND).location(rootUri).build();
	}
}
