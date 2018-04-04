package net.redpipe.cdi;

import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.InitialContextFactoryBuilder;
import javax.naming.spi.NamingManager;
import javax.validation.ValidatorFactory;
import javax.ws.rs.container.CompletionCallback;

import org.jboss.resteasy.cdi.CdiInjectorFactory;
import org.jboss.resteasy.cdi.ResteasyCdiExtension;
import org.jboss.resteasy.plugins.server.vertx.VertxResteasyDeployment;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.weld.bean.builtin.BeanManagerProxy;
import org.jboss.weld.context.bound.BoundRequestContext;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.vertx.VertxExtension;
import net.redpipe.engine.core.AppGlobals;
import net.redpipe.engine.spi.Plugin;
import net.redpipe.engine.spi.RunnableWithException;

import io.vertx.rxjava.core.Vertx;
import rx.Single;

public class CdiPlugin extends Plugin {

	private Weld weld;

	@Override
	public Single<Void> init() {
		// Setup the Vertx-CDI integration
		VertxExtension vertxExtension = CDI.current().select(VertxExtension.class).get();
		BeanManager beanManager = CDI.current().getBeanManager();
		// has to be done in a blocking thread
		Vertx vertx = AppGlobals.get().getVertx();
		return vertx.rxExecuteBlocking(future -> {
			vertxExtension.registerConsumers(vertx.getDelegate(), BeanManagerProxy.unwrap(beanManager).event());
			future.complete();
		});
	}

	@Override
	public Single<Void> deployToResteasy(VertxResteasyDeployment deployment) {
		ResteasyCdiExtension cdiExtension = CDI.current().select(ResteasyCdiExtension.class).get();
		deployment.setActualResourceClasses(cdiExtension.getResources());
		deployment.setInjectorFactoryClass(CdiInjectorFactory.class.getName());
		deployment.getActualProviderClasses().addAll(cdiExtension.getProviders());
		return Single.just(null);
	}

	@Override
	public Single<Void> shutdown() {
		weld.shutdown();
		return super.shutdown();
	}
	
	@Override
	public Single<Void> preInit() {
		// CDI
		weld = new Weld();
		weld.addExtension(new VertxExtension());
		weld.initialize();

		// Set up Resteasy to build BV with CDI
		try {
			NamingManager.setInitialContextFactoryBuilder(new InitialContextFactoryBuilder() {
				@Override
				public InitialContextFactory createInitialContextFactory(Hashtable<?, ?> environment) throws NamingException {
					return new InitialContextFactory() {

						@Override
						public Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {
							Context ctx = new InitialContext(){
								@Override
								public Object lookup(String name) throws NamingException {
									if(name.equals("java:comp/ValidatorFactory"))
										return CDI.current().select(ValidatorFactory.class).get();
									return null;
								}
							};
							return ctx;
						}
					};
				}
			});
		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return Single.just(null);
	}

	@Override
	public void aroundRequest(HttpRequest req, RunnableWithException<IOException> continuation) throws IOException {
        BoundRequestContext cdiContext = CDI.current().select(BoundRequestContext.class).get();
        Map<String,Object> contextMap = new HashMap<String,Object>();
        cdiContext.associate(contextMap);
        cdiContext.activate();
        try {
        	// FIXME: associate CDI thread context on thread change, like Resteasy context?
        	continuation.run();
        }finally {
    		if(req.getAsyncContext().isSuspended()) {
    			req.getAsyncContext().getAsyncResponse().register((CompletionCallback)(t) -> {
        			cdiContext.invalidate();
        			cdiContext.deactivate();
        			cdiContext.dissociate(contextMap);
    			});
    		}else {
    			cdiContext.invalidate();
    			cdiContext.deactivate();
    			cdiContext.dissociate(contextMap);
    		}		
        }
	}
}
