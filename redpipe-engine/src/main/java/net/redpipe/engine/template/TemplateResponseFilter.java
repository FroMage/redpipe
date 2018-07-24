package net.redpipe.engine.template;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Variant;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.core.interception.jaxrs.SuspendableContainerResponseContext;

import io.reactivex.Single;
import io.vertx.reactivex.core.file.FileSystem;
import net.redpipe.engine.core.AppGlobals;

@Provider
public class TemplateResponseFilter implements ContainerResponseFilter {

	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
			throws IOException {
		if(responseContext.getEntityClass() == Template.class) {
			SuspendableContainerResponseContext ctx = (SuspendableContainerResponseContext) responseContext;
			ctx.suspend();
			Template template = (Template) responseContext.getEntity();
			try {
				selectVariantTemplate(requestContext.getRequest(), template)
				.flatMap(templateVariant -> template.render(templateVariant))
				.subscribe(resp -> {
					ctx.setEntity(resp.getEntity(), null, resp.getMediaType());
					ctx.setStatus(resp.getStatus());
					ctx.resume();
				}, err -> {
					ctx.resume(err);
				});
			}catch(Throwable t) {
				ctx.resume(t);
			}
		}
	}

	private Single<String> selectVariantTemplate(Request request, Template template) {
		String path = template.getName();
		int lastSlash = path.lastIndexOf('/');
		String templateDir;
		String namePart; 
		if(lastSlash != -1) {
			templateDir = path.substring(0, lastSlash);
			namePart = path.substring(lastSlash+1);
		} else {
			templateDir = ""; // current dir
			namePart = path;
		}
		int lastDot = namePart.lastIndexOf('.');
		if(lastDot == -1)
			throw new RuntimeException("Missing required template extension: "+namePart);
		String templateExtension = namePart.substring(lastDot);
		String namePartNoExtension = namePart.substring(0, lastDot);
		FileSystem fs = AppGlobals.get().getVertx().fileSystem();
		return fs.rxReadDir(templateDir)
			.flatMap(list -> findVariantTemplate(fs, templateDir, namePartNoExtension, templateExtension, list, request));
	}

	private Single<String> findVariantTemplate(FileSystem fs, String templateDir, String namePartNoExtension, String templateExtension,
			List<String> list, Request request) {
		Map<Variant, String> variants = new HashMap<>();
		String originalTemplate = namePartNoExtension+templateExtension;
		for (String entry : list) {
			// FIXME: is this for real?!?!
			int lastSlash = entry.lastIndexOf('/');
			if(lastSlash != -1)
				entry = entry.substring(lastSlash+1);
			if(!entry.equals(originalTemplate)
					&& entry.startsWith(namePartNoExtension)
					&& entry.endsWith(templateExtension)) {
				String extensionWithDot = entry.substring(namePartNoExtension.length(), entry.length()-templateExtension.length());
				if(!extensionWithDot.startsWith("."))
					continue;
				MediaType mediaType = Template.parseMediaType(extensionWithDot.substring(1));
				variants.put(new Variant(mediaType, (String)null, null), entry);
			}
		}
		// no variant
		if(variants.isEmpty())
			return Single.just(templateDir+"/"+originalTemplate);
		Variant selectedVariant = request.selectVariant(new ArrayList<>(variants.keySet()));
		// no acceptable variant
		if(selectedVariant == null) {
			// if it does not exist, that's special
			String template = templateDir+"/"+originalTemplate;
			return fs.rxExists(template)
					.map(exists -> {
						if(exists)
							return template;
						throw new WebApplicationException(Status.NOT_ACCEPTABLE);
					});
		}
		return Single.just(templateDir+"/"+variants.get(selectedVariant));
	}
}
