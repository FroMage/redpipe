package org.mygroup.vertxrs.security;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.annotation.Priority;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.util.AnnotationLiteral;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.mygroup.vertxrs.Config;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystemException;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jwt.JWT;

@Priority(0)
@PreMatching
@Provider
public class JWTUtilProvider implements ContainerRequestFilter {

	private volatile JWT jwt;
	@Context
	private Vertx vertx;

	private JWT getJWT() {
		// FIXME: clearly this belongs in the init but we're initialised before we set the global vertx and config
		// so something is horribly wrong there
		if(jwt == null){
			synchronized(this){
				if(jwt == null){
					JsonObject config = CDI.current().select(JsonObject.class, new AnnotationLiteral<Config>() {}).get();
					JsonObject keyStoreOptions = config.getJsonObject("keystore");

					// attempt to load a Key file
					try {
						if (keyStoreOptions != null) {
							KeyStore ks = KeyStore.getInstance(keyStoreOptions.getString("type"));

							final Buffer keystore = vertx.fileSystem().readFileBlocking(keyStoreOptions.getString("path"));

							try (InputStream in = new ByteArrayInputStream(keystore.getBytes())) {
								ks.load(in, keyStoreOptions.getString("password").toCharArray());
							}

							this.jwt = new JWT(ks, keyStoreOptions.getString("password").toCharArray());
						}else{
							this.jwt = null;
						}
					}catch(KeyStoreException | IOException | FileSystemException | NoSuchAlgorithmException | CertificateException e){
						throw new RuntimeException(e);
					}
				}
			}
		}
		return jwt;
	}

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		ResteasyProviderFactory.pushContext(JWT.class, getJWT());
	}

}
