package net.redpipe.templating.freemarker;

import java.io.IOException;
import java.util.List;

import javax.ws.rs.core.Response.Status;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.ext.web.codec.BodyCodec;
import net.redpipe.engine.core.Server;
import net.redpipe.engine.mail.MockMailer;
import net.redpipe.engine.mail.MockMailer.SentMail;

@RunWith(VertxUnitRunner.class)
public class ApiTest {

	private Server server;
	private WebClient webClient;

	@Before
	public void prepare(TestContext context) throws IOException {
		Async async = context.async();

		server = new Server();
		server.start(//new JsonObject().put("mode", "prod"), 
				TestResource.class)
		.subscribe(() -> {
			webClient = WebClient.create(server.getVertx(),
					new WebClientOptions().setDefaultHost("localhost").setDefaultPort(9000));
			async.complete();
		}, x -> {
			x.printStackTrace();
			context.fail(x);
			async.complete();
		});
	}

	@After
	public void finish(TestContext context) {
		webClient.close();
		Async async = context.async();
		server.close().subscribe(() -> async.complete(),
				x -> {
					context.fail(x); 
					async.complete();
				});
	}

	@Test
	public void checkTemplate(TestContext context) {
		Async async = context.async();

		webClient
		.get("/")
		.as(BodyCodec.string())
		.rxSend()
		.map(r -> {
			System.err.println("body: "+r.body());
			context.assertEquals(200, r.statusCode());
			context.assertEquals("<html>\n" + 
					" <head>\n" + 
					"  <title>my title</title>\n" + 
					" </head>\n" + 
					" <body>my message</body>\n" + 
					"</html>", r.body());
			return r;
		})
		.doOnError(x -> context.fail(x))
		.subscribe(response -> {
			async.complete();
		});
	}

	@Test
	public void checkTemplateWithTemplateExtension(TestContext context) {
		Async async = context.async();

		webClient
		.get("/indexWithTemplateExtension")
		.as(BodyCodec.string())
		.rxSend()
		.map(r -> {
			System.err.println("body: "+r.body());
			context.assertEquals(200, r.statusCode());
			context.assertEquals("<html>\n" + 
					" <head>\n" + 
					"  <title>my title</title>\n" + 
					" </head>\n" + 
					" <body>my message</body>\n" + 
					"</html>", r.body());
			return r;
		})
		.doOnError(x -> context.fail(x))
		.subscribe(response -> {
			async.complete();
		});
	}

	@Test
	public void checkMail(TestContext context) {
		Async async = context.async();

		webClient
		.get("/mail")
		.as(BodyCodec.string())
		.rxSend()
		.map(r -> {
			System.err.println("body: "+r.body());
			context.assertEquals(200, r.statusCode());
			MockMailer mailer = (MockMailer) server.getAppGlobals().getMailer();
			System.err.println("using mailer "+System.identityHashCode(mailer));
			List<SentMail> mails = mailer.getMailsSentTo("foo@example.com");
			context.assertNotNull(mails);
			context.assertEquals(1, mails.size());
			context.assertEquals("<html>\n" + 
					" <head>\n" + 
					"  <title>my title</title>\n" + 
					" </head>\n" + 
					" <body>my message</body>\n" + 
					"</html>", mails.get(0).text);
			context.assertNull(mails.get(0).html);
			return r;
		})
		.doOnError(x -> context.fail(x))
		.subscribe(response -> {
			async.complete();
		});
	}

	@Test
	public void checkMail2(TestContext context) {
		Async async = context.async();

		webClient
		.get("/mail2")
		.as(BodyCodec.string())
		.rxSend()
		.map(r -> {
			System.err.println("body: "+r.body());
			context.assertEquals(200, r.statusCode());
			MockMailer mailer = (MockMailer) server.getAppGlobals().getMailer();
			System.err.println("using mailer "+System.identityHashCode(mailer));
			List<SentMail> mails = mailer.getMailsSentTo("foo@example.com");
			context.assertNotNull(mails);
			context.assertEquals(1, mails.size());
			context.assertEquals("<html>\n" + 
					" <head>\n" + 
					"  <title>my title</title>\n" + 
					" </head>\n" + 
					" <body>my message</body>\n" + 
					"</html>", mails.get(0).html);
			context.assertEquals("## my title ##\n" + 
					"\n" + 
					"my message", mails.get(0).text);
			return r;
		})
		.doOnError(x -> context.fail(x))
		.subscribe(response -> {
			async.complete();
		});
	}

	@Test
	public void checkTemplateNegociationDefault(TestContext context) {
		Async async = context.async();

		webClient
		.get("/nego")
		.as(BodyCodec.string())
		.rxSend()
		.map(r -> {
			context.assertEquals(200, r.statusCode());
			context.assertTrue(("<html>\n" +
					" <head>\n" + 
					"  <title>my title</title>\n" + 
					" </head>\n" + 
					" <body>my message</body>\n" + 
					"</html>").equals(r.body()) ||
					("## my title ##\n" + 
					"\n" + 
					"my mewssage").equals(r.body()));
			context.assertTrue(
					r.getHeader("Content-Type").equals("text/html") ||
					r.getHeader("Content-Type").equals("text/plain")
					);

			return r;
		})
		.doOnError(x -> context.fail(x))
		.subscribe(response -> {
			async.complete();
		});
	}

	@Test
	public void checkTemplateWithHtmlExtension(TestContext context) {
		Async async = context.async();

		webClient
		.get("/negoWithHtmlExtension")
		.as(BodyCodec.string())
		.rxSend()
		.map(r -> {
			context.assertEquals(200, r.statusCode());
			context.assertEquals("<html>\n" + 
					" <head>\n" + 
					"  <title>my title</title>\n" + 
					" </head>\n" + 
					" <body>my message</body>\n" + 
					"</html>", r.body());
			context.assertEquals("text/html", r.getHeader("Content-Type"));
			return r;
		})
		.doOnError(x -> context.fail(x))
		.subscribe(response -> {
			async.complete();
		});
	}

	@Test
	public void checkTemplateWithHtmlAndTemplateExtension(TestContext context) {
		Async async = context.async();

		webClient
		.get("/negoWithHtmlAndTemplateExtension")
		.as(BodyCodec.string())
		.rxSend()
		.map(r -> {
			context.assertEquals(200, r.statusCode());
			context.assertEquals("<html>\n" + 
					" <head>\n" + 
					"  <title>my title</title>\n" + 
					" </head>\n" + 
					" <body>my message</body>\n" + 
					"</html>", r.body());
			context.assertEquals("text/html", r.getHeader("Content-Type"));
			return r;
		})
		.doOnError(x -> context.fail(x))
		.subscribe(response -> {
			async.complete();
		});
	}

	@Test
	public void checkTemplateNegociationText(TestContext context) {
		Async async = context.async();

		webClient
		.get("/nego")
		.putHeader("Accept", "text/plain")
		.as(BodyCodec.string())
		.rxSend()
		.map(r -> {
			context.assertEquals(200, r.statusCode());
			context.assertEquals("## my title ##\n" + 
					"\n" + 
					"my message", r.body());
			context.assertEquals("text/plain", r.getHeader("Content-Type"));
			return r;
		})
		.doOnError(x -> context.fail(x))
		.subscribe(response -> {
			async.complete();
		});
	}

	@Test
	public void checkTemplateNegociationText2(TestContext context) {
		Async async = context.async();

		webClient
		.get("/nego")
		.putHeader("Accept", "text/plain, text/html;q=0.9")
		.as(BodyCodec.string())
		.rxSend()
		.map(r -> {
			context.assertEquals(200, r.statusCode());
			context.assertEquals("## my title ##\n" + 
					"\n" + 
					"my message", r.body());
			context.assertEquals("text/plain", r.getHeader("Content-Type"));
			return r;
		})
		.doOnError(x -> context.fail(x))
		.subscribe(response -> {
			async.complete();
		});
	}

	@Test
	public void checkTemplateNegociationHtml(TestContext context) {
		Async async = context.async();

		webClient
		.get("/nego")
		.putHeader("Accept", "text/html")
		.as(BodyCodec.string())
		.rxSend()
		.map(r -> {
			context.assertEquals(200, r.statusCode());
			context.assertEquals("<html>\n" + 
					" <head>\n" + 
					"  <title>my title</title>\n" + 
					" </head>\n" + 
					" <body>my message</body>\n" + 
					"</html>", r.body());
			context.assertEquals("text/html", r.getHeader("Content-Type"));
			return r;
		})
		.doOnError(x -> context.fail(x))
		.subscribe(response -> {
			async.complete();
		});
	}

	@Test
	public void checkTemplateNegociationHtml2(TestContext context) {
		Async async = context.async();

		webClient
		.get("/nego")
		.putHeader("Accept", "text/html, text/plain;q=0.9")
		.as(BodyCodec.string())
		.rxSend()
		.map(r -> {
			context.assertEquals(200, r.statusCode());
			context.assertEquals("<html>\n" + 
					" <head>\n" + 
					"  <title>my title</title>\n" + 
					" </head>\n" + 
					" <body>my message</body>\n" + 
					"</html>", r.body());
			context.assertEquals("text/html", r.getHeader("Content-Type"));
			return r;
		})
		.doOnError(x -> context.fail(x))
		.subscribe(response -> {
			async.complete();
		});
	}

	@Test
	public void checkTemplateNegociationNotAcceptable(TestContext context) {
		Async async = context.async();

		webClient
		.get("/nego")
		.putHeader("Accept", "text/stef")
		.as(BodyCodec.string())
		.rxSend()
		.map(r -> {
			context.assertEquals(Status.NOT_ACCEPTABLE.getStatusCode(), r.statusCode());
			return r;
		})
		.doOnError(x -> context.fail(x))
		.subscribe(response -> {
			async.complete();
		});
	}

	@Test
	public void checkTemplateNegociationSingleHtml(TestContext context) {
		Async async = context.async();

		webClient
		.get("/single")
		.as(BodyCodec.string())
		.rxSend()
		.map(r -> {
			context.assertEquals(200, r.statusCode());
			context.assertEquals("<html>\n" + 
					" <head>\n" + 
					"  <title>my title</title>\n" + 
					" </head>\n" + 
					" <body>my message</body>\n" + 
					"</html>", r.body());
			context.assertEquals("text/html", r.getHeader("Content-Type"));
			return r;
		})
		.doOnError(x -> context.fail(x))
		.subscribe(response -> {
			async.complete();
		});
	}

	@Test
	public void checkTemplateDefaultName(TestContext context) {
		Async async = context.async();

		webClient
		.get("/defaultTemplate")
		.as(BodyCodec.string())
		.rxSend()
		.map(r -> {
			context.assertEquals(200, r.statusCode());
			context.assertEquals("<html>\n" + 
					" <head>\n" + 
					"  <title>my title</title>\n" + 
					" </head>\n" + 
					" <body>my message</body>\n" + 
					"</html>", r.body());
			context.assertEquals("text/html", r.getHeader("Content-Type"));
			return r;
		})
		.doOnError(x -> context.fail(x))
		.subscribe(response -> {
			async.complete();
		});
	}
}
