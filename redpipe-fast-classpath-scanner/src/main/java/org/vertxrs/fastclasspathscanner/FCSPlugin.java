package org.vertxrs.fastclasspathscanner;

import java.lang.reflect.Modifier;

import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.plugins.server.vertx.VertxResteasyDeployment;
import org.vertxrs.engine.core.AppGlobals;
import org.vertxrs.engine.spi.Plugin;

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import io.vertx.core.json.JsonArray;
import rx.Single;

public class FCSPlugin extends Plugin {
	@Override
	public Single<Void> deployToResteasy(VertxResteasyDeployment deployment) {
		JsonArray packages = AppGlobals.get().getConfig().getJsonArray("scan");
		if(packages == null) {
			System.err.println("Not scanning any packages, please specify the 'scan' array of packages in configuration");
		}else {
			String[] packagesToScan = (String[]) packages.getList().toArray(new String[packages.size()+1]);
			packagesToScan[packagesToScan.length-1] = "org.vertxrs.engine";
			new FastClasspathScanner(packagesToScan)
				.matchClassesWithAnnotation(Path.class, klass -> {
					if(!Modifier.isAbstract(klass.getModifiers()))
						deployment.getActualResourceClasses().add(klass);
				})
				.matchClassesWithAnnotation(Provider.class, klass -> {
					if(!Modifier.isAbstract(klass.getModifiers()))
						deployment.getActualProviderClasses().add(klass);
				})
				.scan();
		}
		return Single.just(null);
	}
}
