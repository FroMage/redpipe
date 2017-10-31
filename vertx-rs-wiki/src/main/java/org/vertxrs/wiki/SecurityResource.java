package org.vertxrs.wiki;

import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.vertxrs.engine.security.BaseSecurityResource;
import org.vertxrs.engine.template.Template;

@Path("/")
public class SecurityResource extends BaseSecurityResource {
	@Override
	public Template login(@Context UriInfo uriInfo){
		return new Template("templates/login.ftl")
				.set("title", "Login")
				.set("uriInfo", uriInfo)
				// workaround because I couldn't find how to put class literals in freemarker
				.set("SecurityResource", BaseSecurityResource.class);
	}
}
