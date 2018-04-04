package net.redpipe.example.wiki.keycloakJooq;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

import javax.script.ScriptException;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.HttpWaitStrategy;
import org.testcontainers.jdbc.ext.ScriptUtils;
import net.redpipe.engine.core.Server;
import net.redpipe.example.wiki.keycloakJooq.AppResource;
import net.redpipe.example.wiki.keycloakJooq.WikiServer;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.rxjava.core.MultiMap;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.client.WebClient;
import io.vertx.rxjava.ext.web.codec.BodyCodec;
import rx.Single;

@RunWith(VertxUnitRunner.class)
public class ApiTest {

	private Server server;
	private WebClient webClient;
	
    @ClassRule
    public static PostgreSQLContainer postgres = new PostgreSQLContainer();
    
    @ClassRule
    public static GenericContainer keyCloak =
    	new GenericContainer("jboss/keycloak:3.3.0.Final")
    	.withEnv("KEYCLOAK_USER", "admin")
    	.withEnv("KEYCLOAK_PASSWORD", "admin")
    	.withExposedPorts(8080).waitingFor(new HttpWaitStrategy());

	@Before
	public void prepare(TestContext context) throws IOException {
		Async async = context.async();

		Single<String> keyCloakSetup = setupKeyCloak();
		keyCloakSetup.flatMap(rsaKey -> {
			System.err.println("Got RSA key: "+rsaKey);
			JsonObject config = new JsonObject().put("db_name", "test")
					.put("db_port", postgres.getFirstMappedPort())
					.put("db_user", postgres.getUsername())
					.put("db_pass", postgres.getPassword())
					.put("db_max_pool_size", 4)
					.put("security_definitions", "classpath:wiki-users.properties")
					.put("scan", new JsonArray().add(AppResource.class.getPackage().getName()))
					.put("keystore", new JsonObject()
							.put("path", "keystore.jceks")
							.put("type", "jceks")
							.put("password", "secret"))
					.put("keycloack", new JsonObject()
							.put("realm", "demo")
							.put("auth-server-url", "http://"+keyCloak.getContainerIpAddress()+":"+keyCloak.getMappedPort(8080)+"/auth")
							.put("ssl-required", "external")
							.put("resource", "vertx")
							.put("credentials", new JsonObject()
									.put("secret", "super-secret"))
							.put("realm-public-key", rsaKey));

			server = new WikiServer();
			return server.start(config);
		})
		.subscribe(v -> {
			webClient = WebClient.create(server.getVertx(),
					new WebClientOptions().setDefaultHost("localhost").setDefaultPort(9000));
			initSql();
			async.complete();
		}, x -> {
			x.printStackTrace();
			context.fail(x);
			async.complete();
		});
	}

	private void initSql() {
		String path = "db.sql";
		try(InputStream script = new FileInputStream(path)){
			String sql = IOUtils.toString(script, StandardCharsets.UTF_8);
	        Properties info = new Properties();
	        info.put("user", postgres.getUsername());
	        info.put("password", postgres.getPassword());
	        Driver driver = new org.postgresql.Driver();
	        try(Connection connection = driver.connect(postgres.getJdbcUrl(), info)){
	        	ScriptUtils.executeSqlScript(connection, path, sql);
			}
		} catch (IOException | SQLException | ScriptException e) {
			throw new RuntimeException(e);
		}
	}
	
	private Single<String> setupKeyCloak() throws IOException {
		String keyCloakUrl = keyCloak.getContainerIpAddress() + ":" + keyCloak.getMappedPort(8080);
		Vertx vertx = Vertx.vertx();
		WebClient webClient = WebClient.create(vertx,
				new WebClientOptions().setDefaultHost(keyCloak.getContainerIpAddress())
					.setDefaultPort(keyCloak.getMappedPort(8080)));
		System.err.println(keyCloakUrl);

		return webClient
		.post("/auth/realms/master/protocol/openid-connect/token")
		.as(BodyCodec.jsonObject())
		.rxSendForm(MultiMap.caseInsensitiveMultiMap()
				.add("username", "admin")
				.add("password", "admin")
				.add("grant_type", "password")
				.add("client_id", "admin-cli"))
		.map(r -> r.body())
		.flatMap(token -> {
			String jwtTokenHeaderValue = "Bearer " + token.getString("access_token");
			System.err.println(token);
			
			/*
			    user.root=w00t,admin
				user.foo=bar,editor,writer
				user.bar=baz,writer
				user.baz=baz
				
				role.admin=*
				role.editor=create,delete,update
				role.writer=update
			 */
			JsonObject rootUser = new JsonObject()
					.put("username", "root")
					.put("enabled", true)
					.put("credentials", new JsonArray()
							.add(new JsonObject()
									.put("type", "password")
									.put("value", "w00t")
									.put("temporary", false)))
					.put("clientRoles", new JsonObject()
							.put("vertx", new JsonArray()
									.add("admin")));
			JsonObject fooUser = new JsonObject()
					.put("username", "foo")
					.put("enabled", true)
					.put("credentials", new JsonArray()
							.add(new JsonObject()
									.put("type", "password")
									.put("value", "bar")
									.put("temporary", false)))
					.put("clientRoles", new JsonObject()
							.put("vertx", new JsonArray()
									.add("editor")
									.add("writer")));
			JsonObject barUser = new JsonObject()
					.put("username", "bar")
					.put("enabled", true)
					.put("credentials", new JsonArray()
							.add(new JsonObject()
									.put("type", "password")
									.put("value", "baz")
									.put("temporary", false)))
					.put("clientRoles", new JsonObject()
							.put("vertx", new JsonArray()
									.add("writer")));
			JsonObject bazUser = new JsonObject()
					.put("username", "baz")
					.put("enabled", true)
					.put("credentials", new JsonArray()
							.add(new JsonObject()
									.put("type", "password")
									.put("value", "baz")
									.put("temporary", false)));
			
			JsonObject client = new JsonObject()
					.put("enabled", true)
					.put("clientId", "vertx")
					.put("protocol", "openid-connect")
					.put("adminUrl", "http://localhost:9000")
					.put("rootUrl", "http://localhost:9000")
					.put("secret", "super-secret")
					.put("access", new JsonObject().put("confidential", true))
					.put("directAccessGrantsEnabled", true)
					.put("redirectUris", new JsonArray().add("http://localhost:9000/callback/*"));
			
			JsonObject realm = new JsonObject();
			realm.put("realm", "demo");
			realm.put("enabled", true);
			realm.put("users", new JsonArray()
					.add(fooUser)
					.add(barUser)
					.add(rootUser)
					.add(bazUser));
			
			JsonObject createRole = new JsonObject().put("name", "create")
					.put("clientRole", true);
			JsonObject deleteRole = new JsonObject().put("name", "delete")
					.put("clientRole", true);
			JsonObject updateRole = new JsonObject().put("name", "update")
					.put("clientRole", true);

			JsonObject editorRole = new JsonObject().put("name", "editor")
					.put("clientRole", true)
					.put("composite", true)
					.put("composites", new JsonObject()
							.put("client", new JsonObject()
									.put("vertx", new JsonArray()
											.add("create")
											.add("delete")
											.add("update"))));
			JsonObject writerRole = new JsonObject().put("name", "writer")
					.put("clientRole", true)
					.put("composite", true)
					.put("composites", new JsonObject()
							.put("client", new JsonObject()
									.put("vertx", new JsonArray()
											.add("create"))));
			JsonObject adminRole = new JsonObject().put("name", "admin")
					.put("clientRole", true)
					.put("composite", true)
					.put("composites", new JsonObject()
							.put("client", new JsonObject()
									.put("vertx", new JsonArray()
											.add("create")
											.add("delete")
											.add("update"))));
			
			JsonArray roles = new JsonArray()
					.add(editorRole)
					.add(writerRole)
					.add(adminRole)
					.add(createRole)
					.add(deleteRole)
					.add(updateRole);

			realm.put("roles", new JsonObject()
					.put("client", new JsonObject()
							.put("vertx", roles))
					);
			realm.put("clients", new JsonArray().add(client));

			return webClient.post("/auth/admin/realms")
					.putHeader("Authorization", jwtTokenHeaderValue)
					.putHeader("Accept", "application/json")
					.as(BodyCodec.jsonObject())
					.rxSendJsonObject(realm)
					.flatMap(response -> {
						return webClient.get("/auth/admin/realms/demo/keys")
								.putHeader("Authorization", jwtTokenHeaderValue)
								.putHeader("Accept", "application/json")
								.as(BodyCodec.jsonObject())
								.rxSend()
								.map(r -> r.body())
								.map(keysResponse -> {
									
									vertx.close();
									for(Object obj : keysResponse.getJsonArray("keys")) {
										JsonObject key = (JsonObject) obj;
										if(key.getString("type").equals("RSA")) {
											return key.getString("publicKey");
										}
									}
									return null;
								});
					});
		});
	}

	@After
	public void finish(TestContext context) {
		webClient.close();
		// FIXME: doesn't use RxJava
		server.close(context.asyncAssertSuccess());
	}

	@Test
	public void playWithApi(TestContext context) {
		Async async = context.async();

		JsonObject page = new JsonObject()
				.put("name", "Sample")
				.put("markdown", "# A page");
		webClient
			.get("/wiki/api/token")
			.putHeader("login", "foo")
			.putHeader("password", "bar")
			.as(BodyCodec.string())
			.rxSendJsonObject(page)
			.map(r -> r.body())
			.flatMap(token -> {
				String jwtTokenHeaderValue = "Bearer " + token;
				return webClient.post("/wiki/api/pages")
						.putHeader("Authorization", jwtTokenHeaderValue)
						.as(BodyCodec.jsonObject())
						.rxSendJsonObject(page)
						.map(r -> r.body())
						.flatMap(response -> {
							context.assertTrue(response.getBoolean("success"));
							return webClient.get("/wiki/api/pages")
									.putHeader("Authorization", jwtTokenHeaderValue)
									.as(BodyCodec.jsonObject())
									.rxSendJsonObject(page)
									.map(r -> r.body());
						}).flatMap(response -> {
							context.assertTrue(response.getBoolean("success"));
							JsonArray array = response.getJsonArray("pages");
							context.assertEquals(1, array.size());
							context.assertNotNull(array.getJsonObject(0).getInteger("id"));
							return webClient.put("/wiki/api/pages/0")
									.putHeader("Authorization", jwtTokenHeaderValue)
									.as(BodyCodec.jsonObject())
									.rxSendJsonObject(new JsonObject()
											.put("id", 0)
											.put("markdown", "Oh Yeah!"))
									.map(r -> r.body());
						}).flatMap(response -> {
							context.assertTrue(response.getBoolean("success"));
							return webClient.delete("/wiki/api/pages/0")
									.putHeader("Authorization", jwtTokenHeaderValue)
									.as(BodyCodec.jsonObject())
									.rxSendJsonObject(new JsonObject()
											.put("id", 0)
											.put("markdown", "Oh Yeah!"))
									.map(r -> r.body());
					});
		}).doOnError(x -> context.fail(x)).subscribe(response -> {
			context.assertTrue(response.getBoolean("success"));
			async.complete();
		});
	}

}
