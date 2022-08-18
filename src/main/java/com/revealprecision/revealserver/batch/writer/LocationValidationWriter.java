package com.revealprecision.revealserver.batch.writer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revealprecision.revealserver.batch.dto.LocationValidationDTO;
import com.revealprecision.revealserver.enums.BulkEntryStatus;
import com.revealprecision.revealserver.persistence.es.LocationElastic;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.batch.item.file.transform.LineAggregator;
import org.springframework.batch.item.support.AbstractFileItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

@StepScope
public class LocationValidationWriter extends AbstractFileItemWriter<LocationValidationDTO> {

  protected LineAggregator<LocationValidationDTO> lineAggregator;
  @Autowired
  private RestHighLevelClient client;

  private SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
  private ObjectMapper mapper = new ObjectMapper();

  public LocationValidationWriter(String fileName) {
    String exportFilePath = "data/" + fileName + ".csv";
    Resource exportFileResource = new FileSystemResource(exportFilePath);
    setResource(exportFileResource);
    setLineAggregator(getDelimitedLineAggregator());
    this.setExecutionContextName(ClassUtils.getShortName(LocationValidationWriter.class));
  }

  @Override
  protected String doWrite(List<? extends LocationValidationDTO> items) {
    BoolQueryBuilder boolQuery = new BoolQueryBuilder();

    StringBuilder lines = new StringBuilder();
    List<LocationValidationDTO> withErrors = items.stream().filter(el -> el.getStatus().equals(BulkEntryStatus.FAILED)).collect(
        Collectors.toList());
    Set<String> hashes = items.stream().filter(el -> el.getStatus().equals(BulkEntryStatus.SUCCESSFUL)).map(LocationValidationDTO::getHashValue).collect(
        Collectors.toSet());
    boolQuery.must(QueryBuilders.termsQuery("hashValue", hashes));
    sourceBuilder.query(boolQuery);
    sourceBuilder.size(100);
    SearchRequest searchRequest = new SearchRequest("location");
    searchRequest.source(sourceBuilder);
    try {
      SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
      for(SearchHit hit : searchResponse.getHits().getHits()) {
        LocationElastic locationElastic = mapper.readValue(hit.getSourceAsString(), LocationElastic.class);
        withErrors.add(new LocationValidationDTO(locationElastic.getName(), BulkEntryStatus.FAILED, locationElastic.getHashValue(), "Already exist"));
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    for (LocationValidationDTO item : withErrors) {
      lines.append(this.lineAggregator.aggregate(item)).append(this.lineSeparator);
    }
    return lines.toString();
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.notNull(lineAggregator, "A LineAggregator must be provided.");
    if (append) {
      shouldDeleteIfExists = false;
    }
  }

  public void setLineAggregator(LineAggregator<LocationValidationDTO> lineAggregator) {
    this.lineAggregator = lineAggregator;
  }

  public DelimitedLineAggregator<LocationValidationDTO> getDelimitedLineAggregator() {
    BeanWrapperFieldExtractor<LocationValidationDTO> beanWrapperFieldExtractor = new BeanWrapperFieldExtractor<LocationValidationDTO>();
    beanWrapperFieldExtractor.setNames(new String[] {"name", "error"});

    DelimitedLineAggregator<LocationValidationDTO> delimitedLineAggregator = new DelimitedLineAggregator<LocationValidationDTO>();
    delimitedLineAggregator.setDelimiter(",");
    delimitedLineAggregator.setFieldExtractor(beanWrapperFieldExtractor);
    return delimitedLineAggregator;

  }
}
