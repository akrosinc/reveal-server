package com.revealprecision.revealserver.batch.processor;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.revealprecision.revealserver.api.v1.dto.request.LocationRequest;
import com.revealprecision.revealserver.batch.dto.LocationValidationDTO;
import com.revealprecision.revealserver.enums.BulkEntryStatus;
import com.revealprecision.revealserver.util.ElasticModelUtil;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import lombok.RequiredArgsConstructor;
//import org.elasticsearch.client.RestHighLevelClient;
//import org.elasticsearch.common.geo.GeoShapeUtils;
//import org.elasticsearch.common.geo.GeometryParser;
//import org.elasticsearch.common.geo.ShapeRelation;
//import org.elasticsearch.common.xcontent.DeprecationHandler;
//import org.elasticsearch.common.xcontent.NamedXContentRegistry;
//import org.elasticsearch.common.xcontent.XContentParser;
//import org.elasticsearch.common.xcontent.json.JsonXContent;
//import org.elasticsearch.geometry.Geometry;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;
//
//@RequiredArgsConstructor
//@StepScope
//@Component
public class LocationValidationItemProcessor {//implements ItemProcessor<LocationRequest, LocationValidationDTO> {

//  private final RestHighLevelClient client;
//
//  @Override
//  public LocationValidationDTO process(LocationRequest item) throws Exception {
//    Gson gson = new Gson();
//    JsonObject obj =(JsonObject) gson.toJsonTree(item);
//    GeometryParser parser = new GeometryParser(true, true ,true);
//    XContentParser contentParser = JsonXContent.jsonXContent.createParser(NamedXContentRegistry.EMPTY, DeprecationHandler.THROW_UNSUPPORTED_OPERATION,obj.getAsJsonObject("geometry").toString());
//    contentParser.nextToken();
//    Geometry geometry = parser.parse(contentParser);
//
//    try{
//      GeoShapeUtils.toLuceneGeometry(item.getProperties().getName(), null, geometry, ShapeRelation.INTERSECTS);
//        return setHash(item);
//    }catch (Exception e) {
//      if(!e.getMessage().startsWith("Cannot determine orientation")) { //TODO: investigate why is this throwing, maybe version of smth
//        return new LocationValidationDTO(item.getProperties().getName(), BulkEntryStatus.FAILED, null, e.getMessage());
//      }
//    }
//    return null;
//  }
//
//  private LocationValidationDTO setHash(LocationRequest item) throws NoSuchAlgorithmException, IOException {
//    MessageDigest digest = MessageDigest.getInstance("SHA-256");
//    String val = ElasticModelUtil.bytesToHex(digest.digest(item
//        .getGeometry()
//        .toString()
//        .getBytes(StandardCharsets.UTF_8)));
//    return new LocationValidationDTO(item.getProperties().getName(), BulkEntryStatus.SUCCESSFUL, val, null);
//  }
}
