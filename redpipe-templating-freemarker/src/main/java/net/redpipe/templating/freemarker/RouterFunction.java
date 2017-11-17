package net.redpipe.templating.freemarker;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import javax.ws.rs.Path;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.jboss.resteasy.spi.ResteasyProviderFactory;

import freemarker.template.SimpleScalar;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import net.redpipe.engine.core.AppGlobals;

public class RouterFunction implements TemplateMethodModelEx {

	@Override
	public Object exec(List arguments) throws TemplateModelException {
		if(arguments == null || arguments.size() < 1)
			throw new TemplateModelException("Syntax: route('Class.method', arg1, arg2…)");
		Object arg0 = arguments.get(0);
		if(arg0 instanceof SimpleScalar != true)
			throw new TemplateModelException("Syntax: route('Class.method', arg1, arg2…)");
		String arg1 = ((SimpleScalar)arg0).getAsString();
		int dot = arg1.indexOf('.');
		if(dot == -1)
			throw new TemplateModelException("Syntax: route('Class.method', arg1, arg2…)");
		String klass = arg1.substring(0, dot);
		String method = arg1.substring(dot+1);
		
		for (Class resource : AppGlobals.get().getDeployment().getActualResourceClasses()) {
			dot = resource.getName().lastIndexOf('.');
			String shortName = dot == -1 ? resource.getName() : resource.getName().substring(dot+1);
			if(shortName.equals(klass)) {
				for (Method m : resource.getMethods()) {
					// FIXME: overloading?
					if(m.getName().equals(method)
							&& Modifier.isPublic(m.getModifiers())) {
						UriInfo uriInfo = ResteasyProviderFactory.getContextData(UriInfo.class);
						UriBuilder builder = uriInfo.getBaseUriBuilder().path(resource);
						if(m.isAnnotationPresent(Path.class))
							builder.path(m);
						Object[] params = arguments.subList(1, arguments.size()).toArray();
						return builder.build(params).toString();
					}
				}
				throw new TemplateModelException("Could not find method named "+method+" in resource class "+resource.getName());
			}
		}
		throw new TemplateModelException("Could not find resource class named "+klass);
	}

}
