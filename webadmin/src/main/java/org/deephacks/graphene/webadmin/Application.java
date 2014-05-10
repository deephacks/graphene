package org.deephacks.graphene.webadmin;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.deephacks.graphene.webadmin.server.RequestHandler;
import org.deephacks.graphene.webadmin.service.EntityEndpoint;
import org.deephacks.graphene.webadmin.service.EntityService;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VertxFactory;
import org.vertx.java.core.http.RouteMatcher;

public class Application {

  private Vertx vertx = VertxFactory.newVertx();
  private ObjectMapper mapper = new ObjectMapper();
  private RouteMatcher routeMatcher = new RouteMatcher();
  private RequestHandler requestHandler = new RequestHandler(mapper, routeMatcher);
  private EntityService entityService = new EntityService();
  private EntityEndpoint entityEndpoint = new EntityEndpoint(entityService, requestHandler);

  public Vertx getVertx() {
    return vertx;
  }

  public ObjectMapper getMapper() {
    return mapper;
  }

  public RouteMatcher getRouteMatcher() {
    return routeMatcher;
  }

  public RequestHandler getRequestHandler() {
    return requestHandler;
  }

  public EntityService getEntityService() {
    return entityService;
  }

  public EntityEndpoint getEntityEndpoint() {
    return entityEndpoint;
  }
}
