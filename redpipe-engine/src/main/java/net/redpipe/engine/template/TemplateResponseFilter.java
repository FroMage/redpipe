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
		FileSystem fs = AppGlobals.get().getVertx().fileSystem();
		return fs.rxReadDir(templateDir)
			.flatMap(list -> findVariantTemplate(fs, templateDir, namePart, list, request));
	}

	private Single<String> findVariantTemplate(FileSystem fs, String templateDir, String namePart,
			List<String> list, Request request) {
		Map<Variant, String> variants = new HashMap<>();
		String defaultTemplateExtension = "";
		for (String entry : list) {
			// classpath dir entries are expanded into temp folders which we get as absolute paths
			int lastSlash = entry.lastIndexOf('/');
			if(lastSlash != -1)
				entry = entry.substring(lastSlash+1);
			if(!entry.equals(namePart)
					&& entry.startsWith(namePart)) {
				String extensionWithDot = entry.substring(namePart.length());
				if(!extensionWithDot.startsWith("."))
					continue;
				// get rid of the template extension
				int templateExtension = extensionWithDot.indexOf('.', 1);
				if(templateExtension == -1) {
					// we have a single extension, it's probably the default template extension
					defaultTemplateExtension = extensionWithDot;
					continue;
				}
				String mediaExtension = extensionWithDot.substring(1, templateExtension);
				MediaType mediaType = Template.parseMediaType(mediaExtension);
				variants.put(new Variant(mediaType, (String)null, null), entry);
			}
		}
		// no variant
		if(variants.isEmpty())
			return Single.just(templateDir+"/"+namePart+defaultTemplateExtension);
		Variant selectedVariant = request.selectVariant(new ArrayList<>(variants.keySet()));
		// no acceptable variant
		if(selectedVariant == null) {
			// if it does not exist, that's special
			String template = templateDir+"/"+namePart+defaultTemplateExtension;
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
