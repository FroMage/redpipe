package org.mygroup.vertxrs.security;

import java.net.URI;
import java.net.URISyntaxException;

import javax.inject.Inject;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.SubjectContext;
import org.apache.shiro.subject.support.DefaultSubjectContext;
import org.apache.shiro.util.ThreadContext;
import org.mygroup.vertxrs.Session;

@Path("/")
public abstract class BaseSecurityResource {

	public static final String REDIRECT_KEY = "__login_redirect";
	@Inject
	private Class mainClass;
	
	@GET
	@Path("/login")
	public abstract Object login(@Context UriInfo uriInfo);

	@POST
	@Path("/loginAuth")
	public Response loginAuth(@FormParam("username") String username,
			@FormParam("password") String password,
			@FormParam("return_url") String returnUrl, 
			@Context Session session) throws URISyntaxException{
		if(username == null || username.isEmpty()
				|| password == null || password.isEmpty())
			return Response.status(Status.BAD_REQUEST).build();
		
		SecurityManager securityManager = ThreadContext.getSecurityManager();
		SubjectContext subjectContext = new DefaultSubjectContext();
		Subject subject = securityManager.createSubject(subjectContext);
		AuthenticationToken token = new UsernamePasswordToken(username, password);
		try {
			subject.login(token);
		} catch (AuthenticationException e) {
			return Response.status(Status.FORBIDDEN).build();
		}

		String redirectUrl = session.get(REDIRECT_KEY);
		if(redirectUrl == null)
			redirectUrl = returnUrl;
		if(redirectUrl == null)
			redirectUrl = "/";
		
		// should be enough
		session.put(SessionUserFilter.USER_KEY, username);
		session.remove(BaseSecurityResource.REDIRECT_KEY);

		return Response.status(Status.FOUND).location(new URI(redirectUrl)).build();
	}
	
	@GET
	@Path("/logout")
	public Response logout(@Context UriInfo uriInfo, @Context Session session){
		session.clear();
		UriBuilder builder = uriInfo.getBaseUriBuilder();
		URI rootUri = builder.path(mainClass).build();
		return Response.status(Status.FOUND).location(rootUri).build();
	}
}
