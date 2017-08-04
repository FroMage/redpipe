package org.mygroup.vertxrs.security;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;


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
    	for (Map.Entry<AuthorizingAnnotationHandler, Annotation> authzCheck : authzChecks.entrySet()) {
    		AuthorizingAnnotationHandler handler = authzCheck.getKey();
    		Annotation authzSpec = authzCheck.getValue();
    		handler.assertAuthorized(authzSpec);
    	}
    }


}
