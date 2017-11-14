package net.redpipe.templating.freemarker;

import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.spi.ResteasyProviderFactory;
import net.redpipe.engine.template.TemplateRenderer;

import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.templ.FreeMarkerTemplateEngine;
import rx.Single;

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
		return templateEngine.rxRender(context, name)
				// FIXME: other media types perhaps?
				.map(buffer -> Response.ok(buffer, MediaType.TEXT_HTML).build());
	}
}
