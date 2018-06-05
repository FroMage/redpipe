package net.redpipe.example.wiki.keycloakJooq;

import static net.redpipe.fibers.Fibers.await;
import static net.redpipe.fibers.Fibers.fiber;

import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.github.rjeschke.txtmark.Processor;

import io.reactivex.Single;
import io.vertx.core.VertxException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTOptions;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.ext.auth.AuthProvider;
import io.vertx.reactivex.ext.auth.User;
import io.vertx.reactivex.ext.auth.jwt.JWTAuth;
import net.redpipe.engine.core.AppGlobals;
import net.redpipe.engine.security.NoAuthFilter;
import net.redpipe.engine.security.NoAuthRedirect;
import net.redpipe.engine.security.RequiresPermissions;
import net.redpipe.engine.security.RequiresUser;
import net.redpipe.example.wiki.keycloakJooq.jooq.tables.daos.PagesDao;
import net.redpipe.example.wiki.keycloakJooq.jooq.tables.pojos.Pages;

@NoAuthRedirect
@RequiresUser
@Produces(MediaType.APPLICATION_JSON)
@Path("/wiki/api")
public class ApiResource {
	
	@NoAuthFilter
	@Produces("text/plain")
	@GET
	@Path("token")
	public Single<Response> token(@HeaderParam("login") String username, 
			@HeaderParam("password") String password,
			@Context JWTAuth jwt,
			@Context AuthProvider auth){
		
		JsonObject creds = new JsonObject()
				.put("username", username)
				.put("password", password);
		return fiber(() -> {
			User user;
			try {
				user = await(auth.rxAuthenticate(creds));
			}catch(VertxException x) {
				return Response.status(Status.FORBIDDEN).build();
			}
			
			boolean canCreate = await(user.rxIsAuthorised("create"));
			boolean canUpdate = await(user.rxIsAuthorised("update"));
			boolean canDelete = await(user.rxIsAuthorised("delete"));
			JsonArray permissions = new JsonArray();
			if(canCreate)
				permissions.add("create");
			if(canUpdate)
				permissions.add("update");
			if(canDelete)
				permissions.add("delete");
			
	        String jwtToken = jwt.generateToken(
	        		new JsonObject()
	        		.put("username", username)
	        		.put("permissions", permissions),
	                new JWTOptions()
	                  .setSubject("Wiki API")
	                  .setIssuer("Vert.x"));
	        return Response.ok(jwtToken).build();
		});
	}
	
	@GET
	@Path("pages")
	public Single<Response> apiRoot(){
		PagesDao dao = (PagesDao) AppGlobals.get().getGlobal("dao");
		return dao.findAll()
				.map(res -> {
					JsonObject response = new JsonObject();
					List<JsonObject> pages = res
							.stream()
							.map(page -> new JsonObject()
									.put("id", page.getId())
									.put("name", page.getName()))
							.collect(Collectors.toList());
					response
					.put("success", true)
					.put("pages", pages);
					return Response.ok(response).build();
				});
	}

	@GET
	@Path("pages/{id}")
	public Single<Response> apiGetPage(@PathParam("id") String id){
		PagesDao dao = (PagesDao) AppGlobals.get().getGlobal("dao");
		return dao.findOneById(Integer.valueOf(id))
				.map(res -> {
					JsonObject response = new JsonObject();
					if (res.isPresent()) {
						JsonObject payload = new JsonObject()
								.put("name", res.get().getName())
								.put("id", id)
								.put("markdown", res.get().getContent())
								.put("html", Processor.process(res.get().getContent()));
						response
						.put("success", true)
						.put("page", payload);
						return Response.ok(response).build();
					} else {
						response
						.put("success", false)
						.put("error", "There is no page with ID " + id);
						return Response.status(Status.NOT_FOUND).entity(response).build();
					}
				});
	}

	@RequiresPermissions("create")
	@POST
	@Path("pages")
	public Single<Response> apiCreatePage(@ApiUpdateValid({"name", "markdown"}) JsonObject page, 
			@Context HttpServerRequest req){
		PagesDao dao = (PagesDao) AppGlobals.get().getGlobal("dao");
		return dao.insert(new Pages()
				.setName(page.getString("name"))
				.setContent(page.getString("markdown")))
				.map(res -> Response.status(Status.CREATED).entity(new JsonObject().put("success", true)).build());
	}

	@RequiresPermissions("update")
	@PUT
	@Path("pages/{id}")
	public Single<Response> apiUpdatePage(@PathParam("id") String id, 
			@ApiUpdateValid("markdown") JsonObject page,
			@Context HttpServerRequest req,
			@Context Vertx vertx){
		PagesDao dao = (PagesDao) AppGlobals.get().getGlobal("dao");
		return dao.update(new Pages().setId(Integer.valueOf(id)).setContent(page.getString("markdown")))
				.map(res -> {
				    JsonObject event = new JsonObject()
				    	      .put("id", id)
				    	      .put("client", page.getString("client"));
				    vertx.eventBus().publish("page.saved", event);
					return Response.ok(new JsonObject().put("success", true)).build();
				});
	}

	@RequiresPermissions("delete")
	@DELETE
	@Path("pages/{id}")
	public Single<Response> apiDeletePage(@PathParam("id") String id){
		PagesDao dao = (PagesDao) AppGlobals.get().getGlobal("dao");
		return dao.deleteById(Integer.valueOf(id))
				.map(res -> Response.ok(new JsonObject().put("success", true)).build());
	}
}
