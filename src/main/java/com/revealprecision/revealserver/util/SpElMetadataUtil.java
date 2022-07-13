package com.revealprecision.revealserver.util;

import com.revealprecision.revealserver.messaging.message.MetaDataEvent;
import com.revealprecision.revealserver.messaging.message.TMetadataEvent;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

public class SpElMetadataUtil {

  public static boolean eq_(TMetadataEvent tMetadataEvent,String tag ,Object value, String dateForDateScopeFields) {

    Optional<MetaDataEvent> metaDataEventSearch;
    if (dateForDateScopeFields == null) {
      metaDataEventSearch = tMetadataEvent.getMetaDataEvents().stream()
          .filter(metaDataEvent -> metaDataEvent.getTag().equals(tag))
          .findFirst();
    }else {
      metaDataEventSearch = tMetadataEvent.getMetaDataEvents().stream()
          .filter(metaDataEvent -> metaDataEvent.getTag().equals(tag))
          .filter(
              metaDataEvent -> !metaDataEvent.isDateScope() || metaDataEvent.getDateForDateScope()
                  .equals(dateForDateScopeFields))
          .findFirst();
    }
    return
        metaDataEventSearch.stream().anyMatch(metaDataEvent -> {

          switch (metaDataEvent.getDataType()) {
            case "integer":
              return Objects.equals(metaDataEvent.getTagData().getValue().getValueInteger(),
                  value);

            case "string":
              return metaDataEvent.getTagData().getValue().getValueString()
                  .equals(String.valueOf(value));

            case "boolean":
              return metaDataEvent.getTagData().getValue().getValueBoolean()
                  .equals( value);
            case "double":
              return metaDataEvent.getTagData().getValue().getValueDouble()
                  .equals( value);
            case "date":
              return metaDataEvent.getTagData().getValue().getValueDate()
                  .equals( LocalDateTime.parse((String)value));
          }
          return false;
        });

  }



  public static boolean like_(TMetadataEvent tMetadataEvent,String tag ,Object value) {

    return tMetadataEvent.getMetaDataEvents().stream()
        .filter(metaDataEvent -> metaDataEvent.getTag().equals(tag))
        .findFirst().stream().anyMatch(metaDataEvent -> {

          if ("string".equals(metaDataEvent.getDataType())) {
            return metaDataEvent.getTagData().getValue().getValueString()
                .contains(String.valueOf(value));
          }
          return false;
        });
  }

  public static boolean gt_(TMetadataEvent tMetadataEvent, String tag, Object value, String dateForDateScopeFields) {

    Optional<MetaDataEvent> metaDataEventSearch;
    if (dateForDateScopeFields == null) {
      metaDataEventSearch = tMetadataEvent.getMetaDataEvents().stream()
          .filter(metaDataEvent -> metaDataEvent.getTag().equals(tag))
          .findFirst();
    }else {
      metaDataEventSearch = tMetadataEvent.getMetaDataEvents().stream()
          .filter(metaDataEvent -> metaDataEvent.getTag().equals(tag))
          .filter(
              metaDataEvent -> !metaDataEvent.isDateScope() || metaDataEvent.getDateForDateScope()
                  .equals(dateForDateScopeFields))
          .findFirst();
    }


    return metaDataEventSearch.stream().anyMatch(metaDataEvent -> {
          switch (metaDataEvent.getDataType()) {
            case "integer":
              return metaDataEvent.getTagData().getValue().getValueInteger() >
                  (Integer) value;
            case "double":
              return metaDataEvent.getTagData().getValue().getValueDouble() >
                  (Double) value;
            case "date":
              return metaDataEvent.getTagData().getValue().getValueDate()
                  .isBefore(LocalDateTime.parse((String)value));
          }
          return false;
        });
  }

  public static boolean lt_(TMetadataEvent tMetadataEvent, String tag, Object value) {

    return tMetadataEvent.getMetaDataEvents().stream()
        .filter(metaDataEvent -> metaDataEvent.getTag().equals(tag))
        .findFirst().stream().anyMatch(metaDataEvent -> {
          switch (metaDataEvent.getDataType()) {
            case "integer":
              return metaDataEvent.getTagData().getValue().getValueInteger() <
                  (Integer) value;
            case "double":
              return metaDataEvent.getTagData().getValue().getValueDouble() <
                  (Double) value;
            case "date":
              return metaDataEvent.getTagData().getValue().getValueDate()
                  .isAfter( LocalDateTime.parse((String)value));
          }
          return false;
        });
  }

  public static boolean le_(TMetadataEvent tMetadataEvent, String tag, Object value) {

    return tMetadataEvent.getMetaDataEvents().stream()
        .filter(metaDataEvent -> metaDataEvent.getTag().equals(tag))
        .findFirst().stream().anyMatch(metaDataEvent -> {
          switch (metaDataEvent.getDataType()) {
            case "integer":
              return metaDataEvent.getTagData().getValue().getValueInteger() <=
                  (Integer) value;
            case "double":
              return metaDataEvent.getTagData().getValue().getValueDouble() <=
                  (Double) value;
            case "date":
              return metaDataEvent.getTagData().getValue().getValueDate()
                  .isAfter( LocalDateTime.parse((String)value)) || metaDataEvent.getTagData().getValue().getValueDate().equals(LocalDateTime.parse((String)value));
          }
          return false;
        });
  }

  public static boolean ge_(TMetadataEvent tMetadataEvent, String tag, Object value) {

    return tMetadataEvent.getMetaDataEvents().stream()
        .filter(metaDataEvent -> metaDataEvent.getTag().equals(tag))
        .findFirst().stream().anyMatch(metaDataEvent -> {
          switch (metaDataEvent.getDataType()) {
            case "integer":
              return metaDataEvent.getTagData().getValue().getValueInteger() >=
                  (Integer) value;
            case "double":
              return metaDataEvent.getTagData().getValue().getValueDouble() >=
                  (Double) value;
            case "date":
              return metaDataEvent.getTagData().getValue().getValueDate()
                  .isBefore( LocalDateTime.parse((String)value)) || metaDataEvent.getTagData().getValue().getValueDate().equals(LocalDateTime.parse((String)value));
          }
          return false;
        });
  }

  public static Object get_(TMetadataEvent tMetadataEvent,String tag, String dateForDateScopeFields) {

    Optional<MetaDataEvent> metaDataEventSearch;
    if (dateForDateScopeFields == null) {
      metaDataEventSearch = tMetadataEvent.getMetaDataEvents().stream()
          .filter(metaDataEvent -> metaDataEvent.getTag().equals(tag))
          .findFirst();
    }else {
      metaDataEventSearch = tMetadataEvent.getMetaDataEvents().stream()
          .filter(metaDataEvent -> metaDataEvent.getTag().equals(tag))
          .filter(
              metaDataEvent -> !metaDataEvent.isDateScope() || metaDataEvent.getDateForDateScope()
                  .equals(dateForDateScopeFields))
          .findFirst();
    }

    if (metaDataEventSearch.isPresent()){

      MetaDataEvent metaDataEvent = metaDataEventSearch.get();

      switch (metaDataEvent.getDataType()) {
        case "integer":
          return metaDataEvent.getTagData().getValue().getValueInteger();
        case "string":
          return metaDataEvent.getTagData().getValue().getValueString();
        case "boolean":
          return metaDataEvent.getTagData().getValue().getValueBoolean();
        case "double":
          return metaDataEvent.getTagData().getValue().getValueBoolean();
        case "date":
          return metaDataEvent.getTagData().getValue().getValueDate();
      }
      return false;

    } else {
      return null;
    }
  }


}
