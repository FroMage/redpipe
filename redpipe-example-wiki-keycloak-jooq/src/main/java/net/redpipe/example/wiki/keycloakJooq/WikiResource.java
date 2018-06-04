package net.redpipe.example.wiki.keycloakJooq;

import static net.redpipe.fibers.Fibers.await;
import static net.redpipe.fibers.Fibers.fiber;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

import org.jooq.impl.DSL;

import com.github.rjeschke.txtmark.Processor;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.oauth2.AccessToken;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.auth.User;
import io.vertx.rxjava.ext.web.Session;
import io.vertx.rxjava.ext.web.client.HttpResponse;
import io.vertx.rxjava.ext.web.client.WebClient;
import io.vertx.rxjava.ext.web.codec.BodyCodec;
import net.redpipe.engine.core.AppGlobals;
import net.redpipe.engine.core.MainResource;
import net.redpipe.engine.security.AuthorizationException;
import net.redpipe.engine.security.RequiresPermissions;
import net.redpipe.engine.security.RequiresUser;
import net.redpipe.engine.template.Template;
import net.redpipe.example.wiki.keycloakJooq.jooq.Tables;
import net.redpipe.example.wiki.keycloakJooq.jooq.tables.daos.PagesDao;
import net.redpipe.example.wiki.keycloakJooq.jooq.tables.pojos.Pages;
import net.redpipe.router.Router;
import rx.RxReactiveStreams;
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
	private User user;

	@GET
	public Single<Template> index(){
		return fiber(() -> {
			PagesDao dao = (PagesDao) AppGlobals.get().getGlobal("dao");
			List<Pages> res = await(dao.findAll());
			List<String> pages = res
					.stream()
					.map(page -> page.getName())
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

	private String getUserName() {
		AccessToken tok = (AccessToken) user.getDelegate();
		return tok.accessToken().getString("preferred_username");
	}

	@Path("/wiki/{page}")
	@GET
	public Single<Template> renderPage(@PathParam("page") String page){
		return fiber(() -> {
			PagesDao dao = (PagesDao) AppGlobals.get().getGlobal("dao");
			Optional<Pages> res = await(dao.findOneByName(page));
			Integer id; 
			String rawContent;
			boolean newPage = !res.isPresent();
			if(newPage){
				id = -1;
				rawContent = EMPTY_PAGE_MARKDOWN;
			}else{
				id = res.get().getId();
				rawContent = res.get().getContent();
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
		return fiber(() -> {
			boolean isNewPage = "yes".equals(newPage);
			String requiredPermission = isNewPage ? "create" : "update";
			if(!await(user.rxIsAuthorised(requiredPermission)))
				throw new AuthorizationException("Not authorized");

			PagesDao dao = (PagesDao) AppGlobals.get().getGlobal("dao");
			io.reactivex.Single<Integer> query;
			if(isNewPage)
		        query = dao.insert(new Pages().setName(title).setContent(markdown));
			else
				query = dao.update(new Pages().setId(Integer.valueOf(id)).setContent(markdown));
			await(query);
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
		return fiber(() -> {
			PagesDao dao = (PagesDao) AppGlobals.get().getGlobal("dao");
			await(dao.deleteById(Integer.valueOf(id)));
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
		
		return fiber(() -> {
			PagesDao dao = (PagesDao) AppGlobals.get().getGlobal("dao");
			List<Pages> pages = await(dao.findAll());

			JsonObject filesObject = new JsonObject();
			JsonObject gistPayload = new JsonObject() 
					.put("files", filesObject)
					.put("description", "A wiki backup")
					.put("public", true);

			for(Pages page : pages) {
				JsonObject fileObject = new JsonObject(); 
				filesObject.put(page.getName(), fileObject);
				fileObject.put("content", page.getContent());
			}

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
