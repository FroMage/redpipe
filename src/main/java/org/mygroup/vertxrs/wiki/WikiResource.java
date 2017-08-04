package org.mygroup.vertxrs.wiki;

import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.mygroup.vertxrs.Async;
import org.mygroup.vertxrs.MainResource;
import org.mygroup.vertxrs.Template;
import org.mygroup.vertxrs.security.RequiresPermissions;
import org.mygroup.vertxrs.security.RequiresUser;
import org.mygroup.vertxrs.security.User;

import com.github.rjeschke.txtmark.Processor;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.client.WebClient;
import io.vertx.rxjava.ext.web.codec.BodyCodec;
import rx.Single;

@RequiresUser
@MainResource
@Path("/wiki")
public class WikiResource {

	private Map<String, Object> flash = new HashMap<String, Object>();

	private static final String EMPTY_PAGE_MARKDOWN =
			"# A new page\n" +
					"\n" +
					"Feel-free to write in Markdown!\n";

	@Context
	private UriInfo uriInfo;
	@Context
	private User user;

	@Async
	@GET
	public Single<Template> index(){
		return SQL.doInConnection(connection -> connection.rxQuery(SQL.SQL_ALL_PAGES))
				.map(res -> res
						.getResults()
						.stream()
						.map(json -> json.getString(0))
						.sorted()
						.collect(Collectors.toList()))
				.zipWith(user.isAuthorised("create"), 
						(pages, canCreatePage) -> {
							return new Template("templates/index.ftl")
									.set("title", "Wiki home")
									.set("pages", pages)
									.set("uriInfo", uriInfo)
									.set("canCreatePage", canCreatePage)
									.set("username", user.getUsername())
									// workaround because I couldn't find how to put class literals in freemarker
									.set("WikiResource", WikiResource.class)
									.set("SecurityResource", SecurityResource.class)
									.set("backup_gist_url", flash.get("backup_gist_url"));
						});
	}

	@Async
	@Path("/wiki/{page}")
	@GET
	public Single<Object> renderPage(@PathParam("page") String page){
		return SQL.doInConnection(connection -> connection.rxQueryWithParams(SQL.SQL_GET_PAGE, new JsonArray().add(page)))
				.flatMap(res -> {
					JsonArray row = res.getResults()
							.stream()
							.findFirst().orElse(null);
					Integer id; 
					String rawContent;
					boolean newPage = row == null;
					if(newPage){
						id = -1;
						rawContent = EMPTY_PAGE_MARKDOWN;
					}else{
						id = row.getInteger(0);
						rawContent = row.getString(1);
					}
					Single<Boolean> permCheck;
					if(newPage)
						permCheck = user.isAuthorised("create");
					else 
						permCheck = Single.just(true);
					// FIXME: replace with an exception-throwing call?
					return permCheck.flatMap(hasPermission -> {
						if(!hasPermission)
							return Single.just(Response.status(Status.FORBIDDEN).build());
						return user.isAuthorised("update")
								.flatMap(canUpdate ->
								user.isAuthorised("delete")
								.map(canDelete -> 
								new Template("templates/page.ftl")
								.set("title", page)
								.set("id", id)
								.set("newPage", newPage ? "yes" : "no")
								.set("rawContent", rawContent)
								.set("canUpdatePage", canUpdate)
								.set("canDeletePage", canDelete)
								.set("content", Processor.process(rawContent))
								.set("timestamp", new Date().toString())
								.set("uriInfo", uriInfo)
								// workaround because I couldn't find how to put class literals in freemarker
								.set("WikiResource", WikiResource.class)
										));
					});
				});
	}

	@Path("/save")
	@POST
	@Async
	public Single<Response> save(@FormParam("id") String id,
			@FormParam("title") String title,
			@FormParam("markdown") String markdown,
			@FormParam("newPage") String newPage){
		boolean isNewPage = "yes".equals(newPage);
		String requiredPermission = isNewPage ? "create" : "update";
		return user.isAuthorised(requiredPermission)
				.flatMap(hasPermission -> {
					if(!hasPermission)
						return Single.just(Response.status(Status.FORBIDDEN).build());

					String sql = isNewPage ? SQL.SQL_CREATE_PAGE : SQL.SQL_SAVE_PAGE;
					JsonArray params = new JsonArray();
					if (isNewPage) {
						params.add(title).add(markdown);
					} else {
						params.add(markdown).add(id);
					}
					return SQL.doInConnection(connection -> connection.rxUpdateWithParams(sql, params))
							.map(res -> {
								UriBuilder builder = uriInfo.getBaseUriBuilder();
								URI location = builder.path(WikiResource.class).path(WikiResource.class, "renderPage").build(title);
								return Response.seeOther(location).build();
							});
				});
	}

	@RequiresPermissions("create")
	@Path("/create")
	@POST
	public Response create(@FormParam("name") String name){
		UriBuilder builder = uriInfo.getBaseUriBuilder();
		URI location;
		if (name == null || name.isEmpty()) {
			// tricky: if I specify "index" it bitches that it has no @Path...
			location = builder.path(WikiResource.class).build();
		}else{
			location = builder.path(WikiResource.class).path(WikiResource.class, "renderPage").build(name);
		}
		return Response.seeOther(location).build();
	}

	@RequiresPermissions("delete")
	@Async
	@Path("/delete")
	@POST
	public Single<Response> delete(@FormParam("id") String id){
		return SQL.doInConnection(connection -> connection.rxUpdateWithParams(SQL.SQL_DELETE_PAGE, new JsonArray().add(id)))
				.map(res -> {
					UriBuilder builder = uriInfo.getBaseUriBuilder();
					URI location = builder.path(WikiResource.class).build();
					return Response.seeOther(location).build();
				});
	}

	@RequiresPermissions("create")
	@Async
	@Path("/backup")
	@POST
	public Single<Object> backup(@Context Vertx vertx){
		return SQL.doInConnection(connection -> connection.rxQuery(SQL.SQL_ALL_PAGES_DATA))
				.flatMap(res -> {
					JsonObject filesObject = new JsonObject();
					JsonObject gistPayload = new JsonObject() 
							.put("files", filesObject)
							.put("description", "A wiki backup")
							.put("public", true);

					res.getResults()
					.forEach(page -> {
						JsonObject fileObject = new JsonObject(); 
						filesObject.put(page.getString(0), fileObject);
						fileObject.put("content", page.getString(2));
					});

					WebClient webClient = WebClient.create(vertx, new WebClientOptions()
							.setSsl(true)
							.setUserAgent("vert-x3"));
					return webClient.post(443, "api.github.com", "/gists") 
							.putHeader("Accept", "application/vnd.github.v3+json") 
							.putHeader("Content-Type", "application/json")
							.as(BodyCodec.jsonObject())
							.rxSendJsonObject(gistPayload);
				}).flatMap(response -> {
					if (response.statusCode() == 201) {
						flash.put("backup_gist_url", response.body().getString("html_url"));
						return index();
					} else {
						StringBuilder message = new StringBuilder()
								.append("Could not backup the wiki: ")
								.append(response.statusMessage());
						JsonObject body = response.body();
						if (body != null) {
							message.append(System.getProperty("line.separator"))
							.append(body.encodePrettily());
						}
						return Single.just(Response.status(Status.BAD_GATEWAY).entity(message).build());
					}
				});
	}
}
