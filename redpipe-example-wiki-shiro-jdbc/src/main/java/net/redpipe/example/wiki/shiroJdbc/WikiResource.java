package net.redpipe.example.wiki.shiroJdbc;

import static net.redpipe.fibers.Fibers.await;
import static net.redpipe.fibers.Fibers.fiber;

import java.net.URI;
import java.net.URISyntaxException;
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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.github.rjeschke.txtmark.Processor;

import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.auth.User;
import io.vertx.reactivex.ext.sql.SQLConnection;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.ext.web.codec.BodyCodec;
import io.vertx.rxjava.ext.web.Session;
import net.redpipe.engine.core.MainResource;
import net.redpipe.engine.security.AuthorizationException;
import net.redpipe.engine.security.HasPermission;
import net.redpipe.engine.security.RequiresPermissions;
import net.redpipe.engine.security.RequiresUser;
import net.redpipe.engine.template.Template;
import net.redpipe.router.Router;

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
					.set("canCreatePage", canCreatePage)
					.set("username", getUserName())
					.set("backup_gist_url", flash.get("backup_gist_url"));
		});
	}

	@GET
	@Path("index2")
	public Single<Template> index2(@Context SQLConnection connection,
			@Context @HasPermission("create") boolean canCreatePage){
		return connection.rxQuery(SQL.SQL_ALL_PAGES)
				.map(res -> {
					List<String> pages = res.getResults()
							.stream()
							.map(json -> json.getString(0))
							.sorted()
							.collect(Collectors.toList());
					return new Template("templates/index.ftl")
							.set("title", "Wiki home")
							.set("pages", pages)
							.set("canCreatePage", canCreatePage)
							.set("username", getUserName())
							.set("backup_gist_url", flash.get("backup_gist_url"));
		});
	}

	private String getUserName() {
		return user.principal().getString("username");
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
					.set("username", getUserName())
					.set("title", page)
					.set("id", id)
					.set("newPage", newPage ? "yes" : "no")
					.set("rawContent", rawContent)
					.set("canUpdatePage", canUpdate)
					.set("canDeletePage", canDelete)
					.set("content", Processor.process(rawContent))
					.set("timestamp", new Date().toString());
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
			URI location = Router.getURI(WikiResource::renderPage, title);
			return Response.seeOther(location).build();
		});
	}

	@RequiresPermissions("create")
	@Path("/create")
	@POST
	public Response create(@FormParam("name") String name){
		URI location;
		if (name == null || name.isEmpty()) {
			location = Router.getURI(WikiResource::index);
		}else{
			location = Router.getURI(WikiResource::renderPage, name);
		}
		return Response.seeOther(location).build();
	}

	@RequiresPermissions("delete")
	@Path("/delete")
	@POST
	public Single<Response> delete(@FormParam("id") String id){
		return fiber((con) -> {
			await(con.rxUpdateWithParams(SQL.SQL_DELETE_PAGE, new JsonArray().add(id)));
			URI location = Router.getURI(WikiResource::index);
			return Response.seeOther(location).build();
		});
	}

	@Path("/github-login")
	@GET
	public Template githubLoginForm(){
		return new Template("templates/githubLogin.ftl")
					.set("title", "Login to Github");
	}

	@Path("/github-login")
	@POST
	public Response githubLogin(@FormParam("clientId") String clientId, 
			@FormParam("clientSecret") String clientSecret,
			@Context Session session) throws URISyntaxException{
		// https://github.com/login/oauth/authorize?scope=user:email&client_id=
		session.put("github_client_id", clientId);
		session.put("github_client_secret", clientSecret);
		return Response.seeOther(new URI("https://github.com/login/oauth/authorize?scope=gist&client_id="+clientId)).build();
	}

	@Path("/github-callback")
	@GET
	public Single<Response> githubCallback(@QueryParam("code") String code,
			@Context Vertx vertx,
			@Context Session session) throws URISyntaxException{
		return fiber(() -> {

			WebClient webClient = WebClient.create(vertx, new WebClientOptions()
					.setSsl(true)
					.setUserAgent("vert-x3"));
			String clientId = session.get("github_client_id");
			String clientSecret = session.get("github_client_secret");
			JsonObject payload = new JsonObject()
					.put("client_id", clientId)
					.put("client_secret", clientSecret)
					.put("code", code);
			HttpResponse<JsonObject> response = await(webClient.post(443, "github.com", "/login/oauth/access_token") 
					.putHeader("Accept", "application/json") 
					.putHeader("Content-Type", "application/json")
					.as(BodyCodec.jsonObject())
					.rxSendJsonObject(payload));

			if (response.statusCode() == 200) {
				session.put("github_token", response.body().getValue("access_token"));
				return Response.seeOther(Router.getURI(WikiResource::index)).build();
			} else {
				StringBuilder message = new StringBuilder()
						.append("Could not get access token: ")
						.append(response.statusMessage());
				JsonObject body = response.body();
				if (body != null) {
					message.append(System.getProperty("line.separator"))
					.append(body.encodePrettily());
				}
				return Response.status(Status.BAD_GATEWAY).type(MediaType.TEXT_PLAIN).entity(message).build();
			}
		});
	}

	@RequiresPermissions("create")
	@Path("/backup")
	@POST
	public Single<Object> backup(@Context Vertx vertx, @Context Session session){
		String token = session.get("github_token");
		if(token == null) {
			return Single.just(Response.seeOther(Router.getURI(WikiResource::githubLoginForm)).build());
		}

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
					.putHeader("Authorization", "token "+token)
					.as(BodyCodec.jsonObject())
					.rxSendJsonObject(gistPayload));
			webClient.close();

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
				return Response.status(Status.BAD_GATEWAY).type(MediaType.TEXT_PLAIN).entity(message).build();
			}
		});
	}
}
