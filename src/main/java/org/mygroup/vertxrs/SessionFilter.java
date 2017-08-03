package org.mygroup.vertxrs;

import java.io.IOException;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.spi.ResteasyProviderFactory;

@Priority(Priorities.AUTHENTICATION - 500)
@PreMatching
@Provider
public class SessionFilter implements ContainerRequestFilter, ContainerResponseFilter {

	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
			throws IOException {
		Session session = ResteasyProviderFactory.getContextData(Session.class);
		if(session != null)
			session.save(responseContext);
	}

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		Session session = SessionImpl.restore(requestContext);
		ResteasyProviderFactory.pushContext(Session.class, session);
	}
}
