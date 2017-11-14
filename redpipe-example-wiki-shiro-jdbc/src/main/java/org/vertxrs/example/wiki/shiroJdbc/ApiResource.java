package org.vertxrs.example.wiki.shiroJdbc;

import static org.vertxrs.fibers.Fibers.await;
import static org.vertxrs.fibers.Fibers.fiber;

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

import org.vertxrs.engine.db.SQLUtil;
import org.vertxrs.engine.security.NoAuthFilter;
import org.vertxrs.engine.security.NoAuthRedirect;
import org.vertxrs.engine.security.RequiresPermissions;
import org.vertxrs.engine.security.RequiresUser;

import com.github.rjeschke.txtmark.Processor;

import io.vertx.core.VertxException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTOptions;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.http.HttpServerRequest;
import io.vertx.rxjava.ext.auth.AuthProvider;
import io.vertx.rxjava.ext.auth.User;
import io.vertx.rxjava.ext.auth.jwt.JWTAuth;
import rx.Single;

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
		return SQLUtil.doInConnection(connection -> connection.rxQuery(SQL.SQL_ALL_PAGES_DATA))
				.map(res -> {
					JsonObject response = new JsonObject();
					List<JsonObject> pages = res.getResults()
							.stream()
							.map(obj -> new JsonObject()
									.put("id", obj.getInteger(1))
									.put("name", obj.getString(0)))
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
		return SQLUtil.doInConnection(connection -> connection.rxQueryWithParams(SQL.SQL_GET_PAGE_BY_ID, new JsonArray().add(id)))
				.map(res -> {
					JsonObject response = new JsonObject();
					if (!res.getResults().isEmpty()) {
						JsonArray row = res.getResults().get(0);
						JsonObject payload = new JsonObject()
								.put("name", row.getString(0))
								.put("id", id)
								.put("markdown", row.getString(1))
								.put("html", Processor.process(row.getString(1)));
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
		JsonArray params = new JsonArray();
		params.add(page.getString("name")).add(page.getString("markdown"));
		return SQLUtil.doInConnection(connection -> connection.rxUpdateWithParams(SQL.SQL_CREATE_PAGE, params))
				.map(res -> Response.status(Status.CREATED).entity(new JsonObject().put("success", true)).build());
	}

	@RequiresPermissions("update")
	@PUT
	@Path("pages/{id}")
	public Single<Response> apiUpdatePage(@PathParam("id") String id, 
			@ApiUpdateValid("markdown") JsonObject page,
			@Context HttpServerRequest req,
			@Context Vertx vertx){
		JsonArray params = new JsonArray();
		params.add(page.getString("markdown")).add(id);
		return SQLUtil.doInConnection(connection -> connection.rxUpdateWithParams(SQL.SQL_SAVE_PAGE, params))
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
		return SQLUtil.doInConnection(connection -> connection.rxUpdateWithParams(SQL.SQL_DELETE_PAGE, new JsonArray().add(id)))
				.map(res -> Response.ok(new JsonObject().put("success", true)).build());
	}
}
