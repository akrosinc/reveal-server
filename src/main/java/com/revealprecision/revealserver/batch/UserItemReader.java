package com.revealprecision.revealserver.batch;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;

public class UserItemReader implements ItemReader {

  @Override
  public Object read()
      throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
    return null;
  }
}
