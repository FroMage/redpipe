package org.mygroup.vertxrs.security;

import java.io.IOException;
import java.net.URI;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;

import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.mygroup.vertxrs.Session;

public class LoginRedirectFilter implements ContainerRequestFilter {

    public LoginRedirectFilter() {
    }


    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
    	User subject = ResteasyProviderFactory.getContextData(User.class);
    	if(subject == null){
    		UriBuilder builder = requestContext.getUriInfo().getBaseUriBuilder();
    		Session session = ResteasyProviderFactory.getContextData(Session.class);
    		session.put(BaseSecurityResource.REDIRECT_KEY, requestContext.getUriInfo().getPath());
    		URI loginUri = builder.path(BaseSecurityResource.class).path(BaseSecurityResource.class, "login").build();
    		requestContext.abortWith(Response.status(Status.TEMPORARY_REDIRECT).location(loginUri).build());
    	}
    }


}
