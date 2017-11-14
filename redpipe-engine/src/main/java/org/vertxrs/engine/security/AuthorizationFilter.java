package org.vertxrs.engine.security;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;

import org.jboss.resteasy.core.interception.jaxrs.PreMatchContainerRequestContext;

import rx.Single;


public class AuthorizationFilter implements ContainerRequestFilter {
    private final Map<AuthorizingAnnotationHandler, Annotation> authzChecks;

    public AuthorizationFilter(Collection<Annotation> authzSpecs) {
        Map<AuthorizingAnnotationHandler, Annotation> authChecks = new HashMap<>(authzSpecs.size());
        for (Annotation authSpec : authzSpecs) {
            authChecks.put(createHandler(authSpec), authSpec);
        }
        this.authzChecks = Collections.unmodifiableMap(authChecks);
    }

    private static AuthorizingAnnotationHandler createHandler(Annotation annotation) {
        Class<?> t = annotation.annotationType();
        if (RequiresPermissions.class.equals(t)) return new VertxPermissionAnnotationHandler();
        else if (RequiresUser.class.equals(t)) return new VertxUserAnnotationHandler();
        else throw new IllegalArgumentException("Cannot create a handler for the unknown for annotation " + t);
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
    	Single<Boolean> ret = null;
    	for (Map.Entry<AuthorizingAnnotationHandler, Annotation> authzCheck : authzChecks.entrySet()) {
    		AuthorizingAnnotationHandler handler = authzCheck.getKey();
    		Annotation authzSpec = authzCheck.getValue();
    		Single<Boolean> check = handler.assertAuthorized(authzSpec);
    		if(ret == null)
    			ret = check;
    		else
    			ret = ret.zipWith(check, (a, b) -> a && b);
    	}
    	if(ret != null) {
    		PreMatchContainerRequestContext context = (PreMatchContainerRequestContext)requestContext;
    		context.suspend();
			ret.subscribe(result -> {
				if (result)
					context.resume();
				else
					context.resume(new AuthorizationException("Authorization failed"));
			}, error -> {
				context.resume(error);
			});
    	}
    }


}
