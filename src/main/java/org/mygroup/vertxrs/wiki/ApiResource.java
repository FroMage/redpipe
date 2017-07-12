package org.mygroup.vertxrs.wiki;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.mygroup.vertxrs.Async;

import com.github.rjeschke.txtmark.Processor;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.http.HttpServerRequest;
import rx.Single;

@Produces(MediaType.APPLICATION_JSON)
@Path("/wiki/api")
public class ApiResource {
	@Async
	@GET
	@Path("pages")
	public Single<Response> apiRoot(){
		return SQL.doInConnection(connection -> connection.rxQuery(SQL.SQL_ALL_PAGES_DATA))
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
				}).onErrorResumeNext(x -> Single.error(new ApiException(x)));
	}

	@Async
	@GET
	@Path("pages/{id}")
	public Single<Response> apiGetPage(@PathParam("id") String id){
		return SQL.doInConnection(connection -> connection.rxQueryWithParams(SQL.SQL_GET_PAGE_BY_ID, new JsonArray().add(id)))
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
				}).onErrorResumeNext(x -> Single.error(new ApiException(x)));
	}

	@Async
	@POST
	@Path("pages")
	public Single<Response> apiCreatePage(@ApiUpdateValid({"name", "markdown"}) JsonObject page, 
			@Context HttpServerRequest req){
		JsonArray params = new JsonArray();
		params.add(page.getString("name")).add(page.getString("markdown"));
		return SQL.doInConnection(connection -> connection.rxUpdateWithParams(SQL.SQL_CREATE_PAGE, params))
				.map(res -> Response.status(Status.CREATED).entity(new JsonObject().put("success", true)).build())
				.onErrorResumeNext(x -> Single.error(new ApiException(x)));
	}

	@Async
	@PUT
	@Path("pages/{id}")
	public Single<Response> apiUpdatePage(@PathParam("id") String id, 
			@ApiUpdateValid("markdown") JsonObject page,
			@Context HttpServerRequest req){
		JsonArray params = new JsonArray();
		params.add(page.getString("markdown")).add(id);
		return SQL.doInConnection(connection -> connection.rxUpdateWithParams(SQL.SQL_SAVE_PAGE, params))
				.map(res -> Response.ok(new JsonObject().put("success", true)).build())
				.onErrorResumeNext(x -> Single.error(new ApiException(x)));
	}

	@Async
	@DELETE
	@Path("pages/{id}")
	public Single<Response> apiDeletePage(@PathParam("id") String id){
		return SQL.doInConnection(connection -> connection.rxUpdateWithParams(SQL.SQL_DELETE_PAGE, new JsonArray().add(id)))
				.map(res -> Response.ok(new JsonObject().put("success", true)).build())
				.onErrorResumeNext(x -> Single.error(new ApiException(x)));
	}
}
