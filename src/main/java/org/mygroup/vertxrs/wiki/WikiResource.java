package org.mygroup.vertxrs.wiki;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.mygroup.vertxrs.Async;

import com.github.rjeschke.txtmark.Processor;

import freemarker.cache.FileTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import io.vertx.core.buffer.Buffer;
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
	public Single<Response> index(){
		return SQL.doInConnection(connection -> connection.rxQuery(SQL.SQL_ALL_PAGES))
		.map(res -> {
			List<String> pages = res
					.getResults()
					.stream()
					.map(json -> json.getString(0))
					.sorted()
					.collect(Collectors.toList());

			Map<String, Object> context = new HashMap<>();
			context.put("title", "Wiki home");
			context.put("pages", pages);
			context.put("uriInfo", uriInfo);

			return Response.ok(render("templates/index.ftl", context).toString(), MediaType.TEXT_HTML_TYPE).build();
		});
	}

	@Async
	@Path("/wiki/{page}")
	@GET
	public Single<Response> renderPage(@PathParam("page") String page){
		return SQL.doInConnection(connection -> connection.rxQueryWithParams(SQL.SQL_GET_PAGE, new JsonArray().add(page)))
		.map(res -> {
			JsonArray row = res.getResults()
					.stream()
					.findFirst()
					.orElseGet(() -> new JsonArray().add(-1).add(EMPTY_PAGE_MARKDOWN));
			Integer id = row.getInteger(0);
			String rawContent = row.getString(1);

			Map<String, Object> context = new HashMap<>();
			context.put("title", page);
			context.put("id", id);
			context.put("newPage", res.getResults().size() == 0 ? "yes" : "no");
			context.put("rawContent", rawContent);
			context.put("content", Processor.process(rawContent));
			context.put("timestamp", new Date().toString());
			context.put("uriInfo", uriInfo);

			return Response.ok(render("templates/page.ftl", context).toString(), MediaType.TEXT_HTML_TYPE).build();
		  });

	}

	@Path("/save")
	@POST
	public void save(){}

	@Path("/create")
	@POST
	public Response create(@FormParam("name") String name){
		UriBuilder builder = uriInfo.getBaseUriBuilder();
		URI location;
		if (name == null || name.isEmpty()) {
			location = builder.path(WikiResource.class, "index").build();
		}else{
			location = builder.path(WikiResource.class, "renderPage").build(name);
		}
		return Response.seeOther(location).build();
	}

	@Path("/delete")
	@POST
	public void delete(){}
	
	public Buffer render(String template, Map<String,Object> variables){
		Configuration configuration = new Configuration(Configuration.VERSION_2_3_23);
		try{
			configuration.setTemplateLoader(new FileTemplateLoader(new File("src/main/resources")));
			Template templ = configuration.getTemplate(template);
			try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
				// FIXME: workaround to follow wiki
				Map<String, Object> variables2 = new HashMap<>(1);
				variables2.put("context", variables);
				templ.process(variables2, new OutputStreamWriter(baos));
				return Buffer.buffer(baos.toByteArray());
			}
		}catch(TemplateException | IOException x){
			throw new RuntimeException(x);
		}
	}
}
