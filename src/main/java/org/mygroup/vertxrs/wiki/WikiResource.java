package org.mygroup.vertxrs.wiki;

import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.mygroup.vertxrs.Async;
import org.mygroup.vertxrs.Template;

import com.github.rjeschke.txtmark.Processor;

import io.vertx.core.json.JsonArray;
import rx.Single;

@Path("/wiki")
public class WikiResource {

	private static final String EMPTY_PAGE_MARKDOWN =
			  "# A new page\n" +
			    "\n" +
			    "Feel-free to write in Markdown!\n";

	@Context
	private UriInfo uriInfo;
	
	@Async
	@GET
	public Single<Template> index(){
		return SQL.doInConnection(connection -> connection.rxQuery(SQL.SQL_ALL_PAGES))
		.map(res -> {
			List<String> pages = res
					.getResults()
					.stream()
					.map(json -> json.getString(0))
					.sorted()
					.collect(Collectors.toList());

			return new Template("templates/index.ftl")
					.set("title", "Wiki home")
					.set("pages", pages)
					.set("uriInfo", uriInfo)
					// workaround because I couldn't find how to put class literals in freemarker
					.set("WikiResource", WikiResource.class);
		});
	}

	@Async
	@Path("/wiki/{page}")
	@GET
	public Single<Template> renderPage(@PathParam("page") String page){
		return SQL.doInConnection(connection -> connection.rxQueryWithParams(SQL.SQL_GET_PAGE, new JsonArray().add(page)))
		.map(res -> {
			JsonArray row = res.getResults()
					.stream()
					.findFirst()
					.orElseGet(() -> new JsonArray().add(-1).add(EMPTY_PAGE_MARKDOWN));
			Integer id = row.getInteger(0);
			String rawContent = row.getString(1);

			return new Template("templates/page.ftl")
					.set("title", page)
					.set("id", id)
					.set("newPage", res.getResults().size() == 0 ? "yes" : "no")
					.set("rawContent", rawContent)
					.set("content", Processor.process(rawContent))
					.set("timestamp", new Date().toString())
					.set("uriInfo", uriInfo)
					// workaround because I couldn't find how to put class literals in freemarker
					.set("WikiResource", WikiResource.class);
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
	}

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
	
}
