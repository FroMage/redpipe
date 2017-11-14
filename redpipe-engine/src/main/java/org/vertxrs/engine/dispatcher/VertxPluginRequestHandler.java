package org.vertxrs.engine.dispatcher;

import java.io.IOException;
import java.util.List;

import org.jboss.resteasy.core.SynchronousDispatcher;
import org.jboss.resteasy.plugins.server.embedded.SecurityDomain;
import org.jboss.resteasy.plugins.server.vertx.RequestDispatcher;
import org.jboss.resteasy.plugins.server.vertx.VertxHttpRequest;
import org.jboss.resteasy.plugins.server.vertx.VertxHttpResponse;
import org.jboss.resteasy.plugins.server.vertx.VertxUtil;
import org.jboss.resteasy.plugins.server.vertx.i18n.LogMessages;
import org.jboss.resteasy.plugins.server.vertx.i18n.Messages;
import org.jboss.resteasy.specimpl.ResteasyHttpHeaders;
import org.jboss.resteasy.spi.Failure;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.jboss.resteasy.spi.ResteasyUriInfo;
import org.vertxrs.engine.core.AppGlobals;
import org.vertxrs.engine.spi.Plugin;

import io.netty.buffer.ByteBufInputStream;
import io.vertx.core.Handler;
import io.vertx.rxjava.core.Context;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.http.HttpServerRequest;
import io.vertx.rxjava.core.http.HttpServerResponse;

public class VertxPluginRequestHandler implements Handler<HttpServerRequest>
{

   private final Vertx vertx;
   protected final RequestDispatcher dispatcher;
   private final String servletMappingPrefix;
   private AppGlobals appGlobals;

   public VertxPluginRequestHandler(Vertx vertx, ResteasyDeployment deployment, String servletMappingPrefix, SecurityDomain domain, List<Plugin> plugins)
   {
      this.vertx = vertx;
      this.dispatcher = new PluginRequestDispatcher((SynchronousDispatcher) deployment.getDispatcher(), deployment.getProviderFactory(), domain, plugins);
      this.servletMappingPrefix = servletMappingPrefix;
      appGlobals = AppGlobals.get();
   }

   public VertxPluginRequestHandler(Vertx vertx, ResteasyDeployment deployment, String servletMappingPrefix, List<Plugin> plugins)
   {
      this(vertx, deployment, servletMappingPrefix, null, plugins);
   }

   public VertxPluginRequestHandler(Vertx vertx, ResteasyDeployment deployment, List<Plugin> plugins)
   {
      this(vertx, deployment, "", plugins);
   }

   @Override
   public void handle(HttpServerRequest request)
   {
      request.bodyHandler(buff -> {
         Context ctx = vertx.getOrCreateContext();
         ResteasyUriInfo uriInfo = VertxUtil.extractUriInfo(request.getDelegate(), servletMappingPrefix);
         ResteasyHttpHeaders headers = VertxUtil.extractHttpHeaders(request.getDelegate());
         HttpServerResponse response = request.response();
         VertxHttpResponse vertxResponse = new VertxHttpResponse(response.getDelegate(), dispatcher.getProviderFactory(), request.method());
         VertxHttpRequest vertxRequest = new VertxHttpRequest(ctx.getDelegate(), headers, uriInfo, request.rawMethod(), dispatcher.getDispatcher(), vertxResponse, false);
         if (buff.length() > 0)
         {
            ByteBufInputStream in = new ByteBufInputStream(buff.getDelegate().getByteBuf());
            vertxRequest.setInputStream(in);
         }

         try
         {
        	AppGlobals.set(appGlobals);
        	appGlobals.injectGlobals();
            dispatcher.service(ctx.getDelegate(), request.getDelegate(), response.getDelegate(), vertxRequest, vertxResponse, true);
         } catch (Failure e1)
         {
            vertxResponse.setStatus(e1.getErrorCode());
         } catch (Exception ex)
         {
            vertxResponse.setStatus(500);
            LogMessages.LOGGER.error(Messages.MESSAGES.unexpected(), ex);
         }
         finally 
         {
        	 AppGlobals.set(null);
         }
         if (!vertxRequest.getAsyncContext().isSuspended())
         {
            try
            {
               vertxResponse.finish();
            } catch (IOException e)
            {
               e.printStackTrace();
            }
         }
      });
   }
}
