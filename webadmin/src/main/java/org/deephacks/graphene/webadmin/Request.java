package org.deephacks.graphene.webadmin;


import org.deephacks.graphene.webadmin.Request.Path;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Path("dsd")
public class Request {

  public void handle() {

  }

  @Path("root")
  public static class Handler {

    private String hello;

    public Handler(@QueryParam Request request, @PathParam String hello) {
      this.hello = hello;

    }

    @Path("execute1")
    public void execute1(String body) {

    }

    @Path("execute2")
    public void execute2(@QueryParam Request request, @PathParam String hello, String body) {

    }

  }


  @Target({ElementType.TYPE, ElementType.METHOD})
  public static @interface Path {
    public String value();
  }

  @Target(ElementType.PARAMETER)
  public static @interface PathParam {
  }

  @Target(ElementType.PARAMETER)
  public static @interface QueryParam {
  }

  @Target(ElementType.FIELD)
  public static @interface Header {
    public String value();
  }
}
