package com.revealprecision.revealserver.batch.processor;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.revealprecision.revealserver.api.v1.dto.request.LocationRequest;
import com.revealprecision.revealserver.batch.dto.LocationValidationDTO;
import com.revealprecision.revealserver.util.ElasticModelUtil;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import lombok.RequiredArgsConstructor;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.geo.GeoShapeUtils;
import org.elasticsearch.common.geo.GeometryParser;
import org.elasticsearch.common.geo.ShapeRelation;
import org.elasticsearch.common.xcontent.DeprecationHandler;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.geometry.Geometry;
import org.elasticsearch.geometry.utils.GeographyValidator;
import org.elasticsearch.geometry.utils.StandardValidator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@StepScope
@Component
public class LocationValidationItemProcessor implements ItemProcessor<LocationRequest, LocationValidationDTO> {

  private final RestHighLevelClient client;

  @Override
  public LocationValidationDTO process(LocationRequest item) throws Exception {
    Gson gson = new Gson();
    JsonObject obj =(JsonObject) gson.toJsonTree(item);
    GeometryParser parser = new GeometryParser(true, true ,true);
    XContentParser contentParser = JsonXContent.jsonXContent.createParser(NamedXContentRegistry.EMPTY, DeprecationHandler.THROW_UNSUPPORTED_OPERATION,obj.getAsJsonObject("geometry").toString());
    contentParser.nextToken();
    Geometry geometry = parser.parse(contentParser);
    GeographyValidator validator = new GeographyValidator(true);
    validator.validate(geometry);
    StandardValidator validator1 = new StandardValidator(true);
    validator1.validate(geometry);

    try{
      GeoShapeUtils.toLuceneGeometry(item.getProperties().getName(), null, geometry, ShapeRelation.INTERSECTS);
      if(alreadyExist(item)) {
        return new LocationValidationDTO(item.getProperties().getName(), "Already exist");
      }
    }catch (Exception e) {
      if(!e.getMessage().startsWith("Cannot determine orientation")) { //TODO: investigate why is this throwing, maybe version of smth
        return new LocationValidationDTO(item.getProperties().getName(), e.getMessage());
      }
    }
    return null;
  }

  private boolean alreadyExist(LocationRequest item) throws NoSuchAlgorithmException, IOException {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
    sourceBuilder.size(10000);
    String val = ElasticModelUtil.bytesToHex(digest.digest(item
        .getGeometry()
        .toString()
        .getBytes(StandardCharsets.UTF_8)));
    sourceBuilder.query(QueryBuilders.termQuery("hashValue", val));
    SearchRequest searchRequest = new SearchRequest("location");
    searchRequest.source(sourceBuilder);
    SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
    if(searchResponse.getHits().getTotalHits().value > 0)
      return true;
    return false;
  }
}
