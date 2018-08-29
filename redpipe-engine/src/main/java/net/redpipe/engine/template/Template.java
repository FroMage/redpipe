package net.redpipe.engine.template;

import java.util.ArrayList;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Variant;

import io.reactivex.Single;
import io.vertx.reactivex.core.file.FileSystem;
import net.redpipe.engine.core.AppGlobals;
import net.redpipe.engine.util.RedpipeUtil;

public class Template extends AbstractTemplate {

	public Template(String name, Map<String, Object> variables){
		super(name, variables);
	}

	public Template(String name){
		super(name);
	}

	public Template(){
		super();
	}

	@Override
	public Template set(String name, Object value) {
		super.set(name, value);
		return this;
	}
	
	public Single<Response> render(Request request) {
		return selectVariant(request)
				.flatMap(template -> {
					TemplateRenderer renderer = AppGlobals.get().getTemplateRenderer(template);
					if(renderer == null)
						throw new RuntimeException("Failed to find template renderer for template "+template);
					return renderer.render(template, variables);
				}).onErrorReturn(t -> {
					if(t instanceof WebApplicationException)
						return ((WebApplicationException) t).getResponse();
					return RedpipeUtil.rethrow(t);
				});
	}

	public Single<String> selectVariant(Request request){
		return loadVariants()
				.flatMap(variants -> {
					// no variant
					if(variants.variants.isEmpty())
						return Single.just(variants.defaultTemplate);
					Variant selectedVariant = request.selectVariant(new ArrayList<>(variants.variants.keySet()));
					// no acceptable variant
					if(selectedVariant == null) {
						// if it does not exist, that's special
						String template = variants.defaultTemplate;
						FileSystem fs = AppGlobals.get().getVertx().fileSystem();
						return fs.rxExists(template)
								.map(exists -> {
									if(exists)
										return template;
									throw new WebApplicationException(Status.NOT_ACCEPTABLE);
								});
					}
					return Single.just(variants.variants.get(selectedVariant));
				});
	}
	
}