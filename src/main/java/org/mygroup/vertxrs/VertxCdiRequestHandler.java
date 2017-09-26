package org.mygroup.vertxrs;

import java.io.IOException;

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

import io.netty.buffer.ByteBufInputStream;
import io.vertx.core.Handler;
import io.vertx.rxjava.core.Context;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.http.HttpServerRequest;
import io.vertx.rxjava.core.http.HttpServerResponse;

public class VertxCdiRequestHandler implements Handler<HttpServerRequest>
{

   private final Vertx vertx;
   protected final RequestDispatcher dispatcher;
   private final String servletMappingPrefix;

   public VertxCdiRequestHandler(Vertx vertx, ResteasyDeployment deployment, String servletMappingPrefix, SecurityDomain domain)
   {
      this.vertx = vertx;
      this.dispatcher = new CdiRequestDispatcher((SynchronousDispatcher) deployment.getDispatcher(), deployment.getProviderFactory(), domain);
      this.servletMappingPrefix = servletMappingPrefix;
   }

   public VertxCdiRequestHandler(Vertx vertx, ResteasyDeployment deployment, String servletMappingPrefix)
   {
      this(vertx, deployment, servletMappingPrefix, null);
   }

   public VertxCdiRequestHandler(Vertx vertx, ResteasyDeployment deployment)
   {
      this(vertx, deployment, "");
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
            dispatcher.service(ctx.getDelegate(), request.getDelegate(), response.getDelegate(), vertxRequest, vertxResponse, true);
         } catch (Failure e1)
         {
            vertxResponse.setStatus(e1.getErrorCode());
         } catch (Exception ex)
         {
            vertxResponse.setStatus(500);
            LogMessages.LOGGER.error(Messages.MESSAGES.unexpected(), ex);
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
