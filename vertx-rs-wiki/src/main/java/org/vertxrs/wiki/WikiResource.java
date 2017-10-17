package org.vertxrs.wiki;

import static org.vertxrs.fibers.Fibers.await;
import static org.vertxrs.fibers.Fibers.fiber;

import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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

import org.vertxrs.engine.core.MainResource;
import org.vertxrs.engine.security.AuthorizationException;
import org.vertxrs.engine.security.RequiresPermissions;
import org.vertxrs.engine.security.RequiresUser;
import org.vertxrs.engine.template.Template;

import com.github.rjeschke.txtmark.Processor;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.auth.User;
import io.vertx.rxjava.ext.web.client.HttpResponse;
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

	@GET
	public Single<Template> index(){
		return fiber((con) -> {
			ResultSet res = await(con.rxQuery(SQL.SQL_ALL_PAGES));
			List<String> pages = res.getResults()
					.stream()
					.map(json -> json.getString(0))
					.sorted()
					.collect(Collectors.toList());
			boolean canCreatePage = await(user.rxIsAuthorised("create"));
			return new Template("templates/index.ftl")
					.set("title", "Wiki home")
					.set("pages", pages)
					.set("uriInfo", uriInfo)
					.set("canCreatePage", canCreatePage)
					.set("username", user.principal().getString("username"))
					// workaround because I couldn't find how to put class literals in freemarker
					.set("WikiResource", WikiResource.class)
					.set("SecurityResource", SecurityResource.class)
					.set("backup_gist_url", flash.get("backup_gist_url"));
		});
	}

	@Path("/wiki/{page}")
	@GET
	public Single<Template> renderPage(@PathParam("page") String page){
		return fiber((con) -> {
			ResultSet res = await(con.rxQueryWithParams(SQL.SQL_GET_PAGE, new JsonArray().add(page)));
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
			if(newPage && !await(user.rxIsAuthorised("create")))
				throw new AuthorizationException("Not authorized");
			boolean canUpdate = await(user.rxIsAuthorised("update"));
			boolean canDelete = await(user.rxIsAuthorised("delete"));
			return new Template("templates/page.ftl")
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
					.set("WikiResource", WikiResource.class);
		});
	}

	@Path("/save")
	@POST
	public Single<Response> save(@FormParam("id") String id,
			@FormParam("title") String title,
			@FormParam("markdown") String markdown,
			@FormParam("newPage") String newPage){
		return fiber((con) -> {
			boolean isNewPage = "yes".equals(newPage);
			String requiredPermission = isNewPage ? "create" : "update";
			if(!await(user.rxIsAuthorised(requiredPermission)))
				throw new AuthorizationException("Not authorized");
			String sql = isNewPage ? SQL.SQL_CREATE_PAGE : SQL.SQL_SAVE_PAGE;
			JsonArray params = new JsonArray();
			if (isNewPage) {
				params.add(title).add(markdown);
			} else {
				params.add(markdown).add(id);
			}
			await(con.rxUpdateWithParams(sql, params));
			UriBuilder builder = uriInfo.getBaseUriBuilder();
			URI location = builder.path(WikiResource.class).path(WikiResource.class, "renderPage").build(title);
			return Response.seeOther(location).build();
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
	@Path("/delete")
	@POST
	public Single<Response> delete(@FormParam("id") String id){
		return fiber((con) -> {
			await(con.rxUpdateWithParams(SQL.SQL_DELETE_PAGE, new JsonArray().add(id)));
			UriBuilder builder = uriInfo.getBaseUriBuilder();
			URI location = builder.path(WikiResource.class).build();
			return Response.seeOther(location).build();
		});
	}

	@RequiresPermissions("create")
	@Path("/backup")
	@POST
	public Single<Object> backup(@Context Vertx vertx){
		return fiber((con) -> {
			ResultSet res = await(con.rxQuery(SQL.SQL_ALL_PAGES_DATA));

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
			HttpResponse<JsonObject> response = await(webClient.post(443, "api.github.com", "/gists") 
					.putHeader("Accept", "application/vnd.github.v3+json") 
					.putHeader("Content-Type", "application/json")
					.as(BodyCodec.jsonObject())
					.rxSendJsonObject(gistPayload));

			if (response.statusCode() == 201) {
				flash.put("backup_gist_url", response.body().getString("html_url"));
				return await(index());
			} else {
				StringBuilder message = new StringBuilder()
						.append("Could not backup the wiki: ")
						.append(response.statusMessage());
				JsonObject body = response.body();
				if (body != null) {
					message.append(System.getProperty("line.separator"))
					.append(body.encodePrettily());
				}
				return Response.status(Status.BAD_GATEWAY).entity(message).build();
			}
		});
	}
}
