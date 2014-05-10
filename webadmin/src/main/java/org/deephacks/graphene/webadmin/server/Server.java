package org.deephacks.graphene.webadmin.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.deephacks.graphene.webadmin.TestEntity;
import org.deephacks.graphene.webadmin.TestEntityBuilder;
import org.deephacks.graphene.webadmin.service.EntityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VertxFactory;
import org.vertx.java.core.http.RouteMatcher;

import java.io.File;
import java.util.UUID;

public class Server {
  public static final String APPLICATION_CONF_PROP = "application.conf";
  public static final String APPLICATION_CONF = System.getProperty(APPLICATION_CONF_PROP);
  private static final Logger log = LoggerFactory.getLogger(Server.class);
  private Vertx vertx = VertxFactory.newVertx();
  private ObjectMapper mapper = new ObjectMapper();
  private RouteMatcher routeMatcher = new RouteMatcher();
  private RequestHandler requestHandler = new RequestHandler(mapper, routeMatcher);
  private EntityService entityService = new EntityService();

  public static void main(String[] args) {
    new Server().startup();
  }

  public void startup() {
    try {
      if (APPLICATION_CONF != null && !APPLICATION_CONF.isEmpty()) {
        File conf = new File(APPLICATION_CONF);
        if (conf.exists()) {
          System.setProperty(APPLICATION_CONF_PROP, conf.getAbsolutePath());
        }
      }
      requestHandler.get("/list/:cls", req -> {
        try {
          return entityService.list(Class.forName(req.params().get("cls")));
        } catch (ClassNotFoundException e) {
          throw new RuntimeException(e);
        }
      }).post("/put/:cls", req -> {
        TestEntity entity = new TestEntityBuilder().withId(UUID.randomUUID().toString()).withName(UUID.randomUUID().toString()).build();
        entityService.put(entity);
        return null;
      });

      vertx.createHttpServer().requestHandler(requestHandler.getRouteMatcher()).listen(8081);
      vertx.createHttpServer().requestHandler(requestHandler.getRouteMatcher()).listen(8081);

      ShutdownHook.install(new Thread("ShutdownHook") {
        @Override
        public void run() {
          shutdown();
        }
      });
      Thread.currentThread().join();
    } catch (InterruptedException e) {
      // Ok, exit application and let ShutdownHook clean up!
    } catch (Exception e) {
      log.error("Exception during startup", e);
      shutdown();
      throw new RuntimeException(e);
    }
  }

  public void shutdown() {
    if (vertx != null) {
      vertx.stop();
    }
  }
}
