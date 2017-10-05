package org.mygroup.vertxrs.security;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.Priorities;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;

@Provider
public class AuthorizationFilterFeature implements DynamicFeature {

    private static List<Class<? extends Annotation>> filterAnnotations = Collections.unmodifiableList(Arrays.asList(
            RequiresPermissions.class,
            RequiresUser.class));

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {

        List<Annotation> authzSpecs = new ArrayList<>();
        boolean canRedirect = true;
        for (Class<? extends Annotation> annotationClass : filterAnnotations) {
            // XXX What is the performance of getAnnotation vs getAnnotations?
            Annotation classAuthzSpec = resourceInfo.getResourceClass().getAnnotation(annotationClass);
            Annotation methodAuthzSpec = resourceInfo.getResourceMethod().getAnnotation(annotationClass);

            if (classAuthzSpec != null) authzSpecs.add(classAuthzSpec);
            if (methodAuthzSpec != null) authzSpecs.add(methodAuthzSpec);
            
            if(resourceInfo.getResourceClass().isAnnotationPresent(NoAuthRedirect.class)
            		|| resourceInfo.getResourceMethod().isAnnotationPresent(NoAuthRedirect.class))
            	canRedirect = false;
            if(resourceInfo.getResourceClass().isAnnotationPresent(NoAuthFilter.class)
            		|| resourceInfo.getResourceMethod().isAnnotationPresent(NoAuthFilter.class))
            	return;
        }

        if (!authzSpecs.isEmpty()) {
        	if(canRedirect)
        		context.register(new LoginRedirectFilter(), Priorities.AUTHENTICATION + 1);
            context.register(new AuthorizationFilter(authzSpecs), Priorities.AUTHORIZATION);
        }
    }

}
