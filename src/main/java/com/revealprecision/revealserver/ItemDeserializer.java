package com.revealprecision.revealserver;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.revealprecision.revealserver.persistence.domain.Geometry;
import java.io.IOException;
import java.util.List;

public class ItemDeserializer extends StdDeserializer<Geometry> {

  public ItemDeserializer() {
    this(null);
  }

  public ItemDeserializer(Class<?> vc) {
    super(vc);
  }

  @Override
  public Geometry deserialize(JsonParser jp, DeserializationContext ctxt)
      throws IOException, JsonProcessingException {
    JsonNode node = jp.getCodec().readTree(jp);
    ObjectMapper mapper = new ObjectMapper();
    String type = node.get("type").asText();
//    GeometryType geoType = LookupUtil.lookup(GeometryType.class, type);
    ObjectReader reader = mapper.readerFor(new TypeReference<List<Object>>() {
    });
    List<Object> coordinates = reader.readValue(node.get("coordinates"));
    Geometry geometry =  new Geometry(type, coordinates);
    return geometry;
  }
}