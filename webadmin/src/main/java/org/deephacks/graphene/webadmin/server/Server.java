package org.deephacks.graphene.webadmin.server;

import org.deephacks.graphene.webadmin.Application;
import org.deephacks.graphene.webadmin.service.EntityEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class Server {
  public static final String APPLICATION_CONF_PROP = "application.conf";
  public static final String APPLICATION_CONF = System.getProperty(APPLICATION_CONF_PROP);
  private static final Logger log = LoggerFactory.getLogger(Server.class);
  private final Application application = new Application();
  private final RequestHandler requestHandler = application.getRequestHandler();
  private final EntityEndpoint entityEndpoint = application.getEntityEndpoint();
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
      entityEndpoint.setupRoutes(requestHandler);

      application.getVertx().createHttpServer().requestHandler(requestHandler.getRouteMatcher()).listen(8081);
      application.getVertx().createHttpServer().requestHandler(requestHandler.getRouteMatcher()).listen(8081);

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
    if (application.getVertx() != null) {
      application.getVertx().stop();
    }
  }
}
