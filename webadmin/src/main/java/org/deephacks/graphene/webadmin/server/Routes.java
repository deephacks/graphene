package org.deephacks.graphene.webadmin.server;

import org.vertx.java.core.http.HttpServerRequest;

public class Routes {

  public static void main(String[] args) {
    Post p = new Post("ssd") {

      @Override
      public void handle(HttpServerRequest request) {

      }
    };
  }

  public static abstract class Route {
    protected String pattern;
    public Route(String pattern) {
      this.pattern = pattern;
    }
  }


  public static abstract class Post extends Route {

    public Post(String pattern) {
      super(pattern);
    }

    public abstract void handle(HttpServerRequest request);
  }

  public static abstract class Get extends Route {

    public Get(String pattern) {
      super(pattern);
    }

    public abstract void handle(HttpServerRequest request);
  }
}
