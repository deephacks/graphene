package org.deephacks.graphene.webadmin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.deephacks.graphene.webadmin.server.Endpoint;
import org.deephacks.graphene.webadmin.server.RequestHandler;
import org.deephacks.graphene.webadmin.server.WebException;

import java.io.IOException;
import java.util.List;

public class EntityEndpoint implements Endpoint {
  private final EntityService entityService;
  private final ObjectMapper mapper;

  public EntityEndpoint(ObjectMapper mapper, EntityService entityService) {
    this.entityService = entityService;
    this.mapper = mapper;
  }


  public void setupRoutes(RequestHandler handler) throws Exception {
    handler.get("/list/:cls", req -> {
      Class<?> cls = readClass(req.params().get("cls"));
      List<?> list = entityService.list(cls);
      return mapper.writeValueAsBytes(list);
    }).post("/put/:cls", req -> {
      final Class<?> cls = readClass(req.params().get("cls"));

      req.dataHandler(buffer -> {
        try {
          entityService.put(mapper.readValue(buffer.getBytes(), cls));
        } catch (IOException e) {
          throw new RuntimeException(e);
        }

      });
      return null;
    });
  }

  private Class<?> readClass(String className) {
    try {
      return Class.forName(className);
    } catch (ClassNotFoundException e) {
      throw new WebException(HttpResponseStatus.NOT_FOUND);
    }
  }
}
