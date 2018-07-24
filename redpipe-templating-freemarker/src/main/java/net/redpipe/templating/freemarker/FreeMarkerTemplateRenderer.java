package net.redpipe.templating.freemarker;

import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.core.Response;

import org.jboss.resteasy.spi.ResteasyProviderFactory;

import io.reactivex.Single;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.templ.FreeMarkerTemplateEngine;
import net.redpipe.engine.template.Template;
import net.redpipe.engine.template.TemplateRenderer;

public class FreeMarkerTemplateRenderer implements TemplateRenderer {

	private final FreeMarkerTemplateEngine templateEngine = FreeMarkerTemplateEngine.create();

	@Override
	public boolean supportsTemplate(String name) {
		return name.toLowerCase().endsWith(".ftl");
	}

	@Override
	public Single<Response> render(String name, Map<String, Object> variables) {
		RoutingContext context = ResteasyProviderFactory.getContextData(RoutingContext.class);
		for (Entry<String, Object> entry : variables.entrySet()) {
			context.put(entry.getKey(), entry.getValue());
		}
		context.put("route", new RouterFunction());
		return templateEngine.rxRender(context, name)
				.map(buffer -> Response.ok(buffer, Template.parseMediaType(name, ".ftl")).build());
	}
}
