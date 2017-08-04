package org.mygroup.vertxrs.security;

import java.io.IOException;

import javax.annotation.Priority;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.util.AnnotationLiteral;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.realm.text.PropertiesRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.SubjectContext;
import org.apache.shiro.subject.support.DefaultSubjectContext;
import org.apache.shiro.util.ThreadContext;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.mygroup.vertxrs.Config;
import org.mygroup.vertxrs.Session;

import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Vertx;

@Priority(Priorities.AUTHENTICATION)
@PreMatching
@Provider
public class SessionUserFilter implements ContainerRequestFilter {

	public static final String USER_KEY = "__user";
	private DefaultSecurityManager securityManager;
	private String realmName;

	public SessionUserFilter() {
		PropertiesRealm realm = new PropertiesRealm();
		JsonObject config = CDI.current().select(JsonObject.class, new AnnotationLiteral<Config>() {}).get();
		realm.setResourcePath(config.getString("security_definitions"));
		realm.init();
		this.securityManager = new DefaultSecurityManager(realm);
		this.realmName = realm.getName();
	}

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		// just always bind it on every thread just in case
		ThreadContext.bind(securityManager);

		Session session = ResteasyProviderFactory.getContextData(Session.class);
		Vertx vertx = ResteasyProviderFactory.getContextData(Vertx.class);
		String username = session.get(USER_KEY);
		User user = null;
		Subject subject = null;
		if(username != null && !username.isEmpty()){
			SubjectContext subjectContext = new DefaultSubjectContext();
			PrincipalCollection coll = new SimplePrincipalCollection(username, realmName);
			subjectContext.setPrincipals(coll);
			subject = securityManager.createSubject(subjectContext);
			user = new ShiroUser(vertx, subject);
		}
		ResteasyProviderFactory.pushContext(User.class, user);
		ResteasyProviderFactory.pushContext(Subject.class, subject);
	}
}
