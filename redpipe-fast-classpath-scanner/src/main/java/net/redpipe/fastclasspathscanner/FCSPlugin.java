package net.redpipe.fastclasspathscanner;

import java.lang.reflect.Modifier;

import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.plugins.server.vertx.VertxResteasyDeployment;

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import io.reactivex.Completable;
import io.vertx.core.json.JsonArray;
import net.redpipe.engine.core.AppGlobals;
import net.redpipe.engine.spi.Plugin;

public class FCSPlugin extends Plugin {
	@Override
	public Completable deployToResteasy(VertxResteasyDeployment deployment) {
		return Completable.defer(() -> {
			JsonArray packages = AppGlobals.get().getConfig().getJsonArray("scan");
			if(packages == null) {
				System.err.println("Not scanning any packages, please specify the 'scan' array of packages in configuration");
			}else {
				String[] packagesToScan = (String[]) packages.getList().toArray(new String[packages.size()+1]);
				packagesToScan[packagesToScan.length-1] = "net.redpipe.engine";
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
			return super.deployToResteasy(deployment);
		});
	}
}
