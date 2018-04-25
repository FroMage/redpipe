package net.redpipe.engine.core;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import com.google.common.reflect.TypeToken;

import io.swagger.annotations.Api;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.jaxrs.config.BeanConfig;

/**
 * Subtype of BeanConfig that doesn't scan the whole damn jar
 *
 * @author Stéphane Épardaud <stef@epardaud.fr>
 */
public class MyBeanConfig extends BeanConfig {
	@Override
	public Set<Class<?>> classes() {
        ConfigurationBuilder config = new ConfigurationBuilder();
        Set<String> acceptablePackages = new HashSet<String>();

        boolean allowAllPackages = false;

        String resourcePackage = getResourcePackage();
        if (resourcePackage != null && !"".equals(resourcePackage)) {
            String[] parts = resourcePackage.split(",");
            for (String pkg : parts) {
                if (!"".equals(pkg)) {
                    acceptablePackages.add(pkg);
                    config.addUrls(ClasspathHelper.forPackage(pkg));
                }
            }
        } else {
            allowAllPackages = true;
        }

        config.setScanners(new ResourcesScanner(), new TypeAnnotationsScanner(), new SubTypesScanner());
        // Stef: don't scan the whole damn jar!!
        if(!allowAllPackages) {
        	Set<String> acceptableBinaryPackages = new HashSet<>();
        	for (String pkg : acceptablePackages) {
				acceptableBinaryPackages.add(pkg.replace('.', '/'));
			}
        	config.filterInputsBy(filePath -> {
        		if(!filePath.toLowerCase().endsWith(".class"))
        			return false;
        		int lastSlash = filePath.lastIndexOf('/');
        		String binaryPkg = lastSlash == -1 ? "" : filePath.substring(0, lastSlash);
        		boolean ret = acceptableBinaryPackages.contains(binaryPkg);
        		return ret;
        	});
        }

        final Reflections reflections = new Reflections(config);
        Set<Class<?>> classes = reflections.getTypesAnnotatedWith(javax.ws.rs.Path.class);
        Set<Class<?>> typesAnnotatedWith = reflections.getTypesAnnotatedWith(SwaggerDefinition.class);
				classes.addAll(typesAnnotatedWith);
        
        /*
         * Find concrete types annotated with @Api, but with a supertype annotated with @Path.
         * This would handle split resources where the interface has jax-rs annotations
         * and the implementing class has Swagger annotations 
         */
        for (Class<?> cls : reflections.getTypesAnnotatedWith(Api.class)) {
        	for (Class<?> intfc : TypeToken.of(cls).getTypes().interfaces().rawTypes()) {
        		Annotation ann = intfc.getAnnotation(javax.ws.rs.Path.class);
        		if (ann != null) {
        			classes.add(cls);
        			break;
        		}
					}
				}
        
        Set<Class<?>> output = new HashSet<Class<?>>();
        for (Class<?> cls : classes) {
            if (allowAllPackages) {
                output.add(cls);
            } else {
                for (String pkg : acceptablePackages) {
                    // startsWith allows everything within a package
                    // the dots ensures that package siblings are not considered
                    if ((cls.getPackage().getName() + ".").startsWith(pkg + ".")) {
                        output.add(cls);
			break;
                    }
                }
            }
        }
        return output;

	}
}
