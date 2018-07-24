package net.redpipe.engine.template;

import java.util.Map;

import javax.ws.rs.core.Response;

import io.reactivex.Single;


public interface TemplateRenderer {

	boolean supportsTemplate(String name);

	Single<Response> render(String name, Map<String, Object> variables, String variant);
}
