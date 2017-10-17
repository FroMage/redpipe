package org.vertxrs.engine.template;

import java.util.Map;

import javax.ws.rs.core.Response;

import rx.Single;

public interface TemplateRenderer {

	boolean supportsTemplate(String name);

	Single<Response> render(String name, Map<String, Object> variables);

}
