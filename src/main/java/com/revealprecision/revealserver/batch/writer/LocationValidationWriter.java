package com.revealprecision.revealserver.batch.writer;

import com.revealprecision.revealserver.batch.dto.LocationValidationDTO;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

@StepScope
public class LocationValidationWriter extends FlatFileItemWriter<LocationValidationDTO> {

  public LocationValidationWriter(String fileName) {
    String exportFilePath = "data/" + fileName + ".csv";
    Resource exportFileResource = new FileSystemResource(exportFilePath);
    setResource(exportFileResource);
    setLineAggregator(getDelimitedLineAggregator());
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
