package com.revealprecision.revealserver.util;

import com.revealprecision.revealserver.messaging.message.FormDataEntityTagEvent;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SpElMetadataUtil {

  public static boolean eq_(FormDataEntityTagEvent formDataEntityTagEvent, String tag,
      Object value) {
//TODO: solve for date
    boolean b = formDataEntityTagEvent.getFormDataEntityTagValueEvents().stream()
        .filter(
            formDataEntityTagValueEvent -> formDataEntityTagValueEvent.getEntityTagEvent().getTag()
                .equals(tag))
        .anyMatch(metaDataEvent -> Objects.equals(metaDataEvent.getValue(), value));
    log.trace("eq_ request for {} = {}", tag, b);
    return b;

  }

  public static boolean like_(FormDataEntityTagEvent formDataEntityTagEvent, String tag,
      Object value) {
//TODO: solve for date
    boolean b = formDataEntityTagEvent.getFormDataEntityTagValueEvents().stream()
        .filter(
            formDataEntityTagValueEvent -> formDataEntityTagValueEvent.getEntityTagEvent().getTag()
                .equals(tag))
        .anyMatch(metaDataEvent -> ((String) metaDataEvent.getValue()).contains((String) value));
    log.trace("like_ request for {} = {}", tag, b);
    return b;
  }

  public static boolean gt_(FormDataEntityTagEvent formDataEntityTagEvent, String tag,
      Object value) {
//TODO: solve for date
    boolean b = formDataEntityTagEvent.getFormDataEntityTagValueEvents().stream()
        .filter(
            formDataEntityTagValueEvent -> formDataEntityTagValueEvent.getEntityTagEvent().getTag()
                .equals(tag))
        .anyMatch(metaDataEvent ->
            ((metaDataEvent.getValue() instanceof Integer) ? (Integer) metaDataEvent.getValue()
                : (Integer.parseInt((String) metaDataEvent.getValue())))
                > (Integer) value);
    log.trace("gt_ request for {} = {}", tag, b);
    return b;
  }

  public static boolean lt_(FormDataEntityTagEvent formDataEntityTagEvent, String tag,
      Object value) {
//TODO: solve for date
    boolean b = formDataEntityTagEvent.getFormDataEntityTagValueEvents().stream()
        .filter(
            formDataEntityTagValueEvent -> formDataEntityTagValueEvent.getEntityTagEvent().getTag()
                .equals(tag))
        .anyMatch(metaDataEvent -> (Integer.parseInt((String) metaDataEvent.getValue()))
            < (Integer) value);
    log.trace("lt_ request for {} = {}", tag, b);
    return b;
  }

  public static boolean le_(FormDataEntityTagEvent formDataEntityTagEvent, String tag,
      Object value) {
//TODO: solve for date
    boolean b = formDataEntityTagEvent.getFormDataEntityTagValueEvents().stream()
        .filter(
            formDataEntityTagValueEvent -> formDataEntityTagValueEvent.getEntityTagEvent().getTag()
                .equals(tag))
        .anyMatch(metaDataEvent -> (Integer.parseInt((String) metaDataEvent.getValue()))
            <= (Integer) value);

    log.trace("le_ request for {} = {}", tag, b);
    return b;
  }

  public static boolean ge_(FormDataEntityTagEvent formDataEntityTagEvent, String tag,
      Object value) {
//TODO: solve for date
    boolean b = formDataEntityTagEvent.getFormDataEntityTagValueEvents().stream()
        .filter(
            formDataEntityTagValueEvent -> formDataEntityTagValueEvent.getEntityTagEvent().getTag()
                .equals(tag))
        .anyMatch(metaDataEvent -> (Integer.parseInt((String) metaDataEvent.getValue()))
            >= (Integer) value);

    log.trace("ge_ request for {} = {}", tag, b);
    return b;
  }

  public static Object get_(FormDataEntityTagEvent formDataEntityTagEvent, String tag) {
//TODO: solve for date
    return formDataEntityTagEvent.getFormDataEntityTagValueEvents()
        .stream()
        .filter(
            formDataEntityTagValueEvent -> formDataEntityTagValueEvent.getEntityTagEvent().getTag()
                .equals(tag))
        .map(formDataEntityTagValueEvent ->
            EntityTagEventUtil.getEntityTagEventValue(
                formDataEntityTagValueEvent.getEntityTagEvent(),
                formDataEntityTagValueEvent.getValue())
        )
        .findFirst().orElse(null);
  }


}
