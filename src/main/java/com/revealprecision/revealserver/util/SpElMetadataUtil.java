package com.revealprecision.revealserver.util;

import com.revealprecision.revealserver.messaging.message.FormDataEntityTagEvent;
import com.revealprecision.revealserver.messaging.message.FormDataEntityTagValueEvent;
import com.revealprecision.revealserver.messaging.message.MetaDataEvent;
import com.revealprecision.revealserver.messaging.message.TMetadataEvent;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

public class SpElMetadataUtil {

//  public static boolean eq_(TMetadataEvent tMetadataEvent,String tag ,Object value, String dateForDateScopeFields) {
//
//    Optional<MetaDataEvent> metaDataEventSearch;
//    if (dateForDateScopeFields == null) {
//      metaDataEventSearch = tMetadataEvent.getMetaDataEvents().stream()
//          .filter(metaDataEvent -> metaDataEvent.getTag().equals(tag))
//          .findFirst();
//    }else {
//      metaDataEventSearch = tMetadataEvent.getMetaDataEvents().stream()
//          .filter(metaDataEvent -> metaDataEvent.getTag().equals(tag))
//          .filter(
//              metaDataEvent -> !metaDataEvent.isDateScope() || metaDataEvent.getDateForDateScope()
//                  .equals(dateForDateScopeFields))
//          .findFirst();
//    }
//    return
//        metaDataEventSearch.stream().anyMatch(metaDataEvent -> {
//
//          switch (metaDataEvent.getDataType()) {
//            case "integer":
//              return Objects.equals(metaDataEvent.getTagData().getValue().getValueInteger(),
//                  value);
//
//            case "string":
//              return metaDataEvent.getTagData().getValue().getValueString()
//                  .equals(String.valueOf(value));
//
//            case "boolean":
//              return metaDataEvent.getTagData().getValue().getValueBoolean()
//                  .equals( value);
//            case "double":
//              return metaDataEvent.getTagData().getValue().getValueDouble()
//                  .equals( value);
//            case "date":
//              return metaDataEvent.getTagData().getValue().getValueDate()
//                  .equals( LocalDateTime.parse((String)value));
//          }
//          return false;
//        });
//  }

  public static boolean eq_(FormDataEntityTagEvent formDataEntityTagEvent,String tag ,Object value, String date) {

    return  formDataEntityTagEvent.getFormDataEntityTagValueEvents().stream()
          .filter(formDataEntityTagValueEvent -> formDataEntityTagValueEvent.getEntityTagEvent().getTag().equals(tag))
          .anyMatch(metaDataEvent -> Objects.equals(metaDataEvent.getValue(), (String) value));

  }

  public static boolean like_(FormDataEntityTagEvent formDataEntityTagEvent,String tag ,Object value, String date) {

    return  formDataEntityTagEvent.getFormDataEntityTagValueEvents().stream()
        .filter(formDataEntityTagValueEvent -> formDataEntityTagValueEvent.getEntityTagEvent().getTag().equals(tag))
        .anyMatch(metaDataEvent -> ((String) metaDataEvent.getValue()).contains((String) value));
  }

  public static boolean gt_(FormDataEntityTagEvent formDataEntityTagEvent, String tag, Object value, String date) {

    return  formDataEntityTagEvent.getFormDataEntityTagValueEvents().stream()
        .filter(formDataEntityTagValueEvent -> formDataEntityTagValueEvent.getEntityTagEvent().getTag().equals(tag))
        .anyMatch(metaDataEvent -> (Integer.parseInt((String) metaDataEvent.getValue())) > (Integer) value);
  }

  public static boolean lt_(FormDataEntityTagEvent formDataEntityTagEvent, String tag, Object value, String date) {

    return  formDataEntityTagEvent.getFormDataEntityTagValueEvents().stream()
        .filter(formDataEntityTagValueEvent -> formDataEntityTagValueEvent.getEntityTagEvent().getTag().equals(tag))
        .anyMatch(metaDataEvent ->  (Integer.parseInt((String) metaDataEvent.getValue())) < (Integer) value);
  }

  public static boolean le_(FormDataEntityTagEvent formDataEntityTagEvent, String tag, Object value, String date) {

    return  formDataEntityTagEvent.getFormDataEntityTagValueEvents().stream()
        .filter(formDataEntityTagValueEvent -> formDataEntityTagValueEvent.getEntityTagEvent().getTag().equals(tag))
        .anyMatch(metaDataEvent ->  (Integer.parseInt((String) metaDataEvent.getValue())) <= (Integer) value);
  }

  public static boolean ge_(FormDataEntityTagEvent formDataEntityTagEvent, String tag, Object value, String date) {

    return  formDataEntityTagEvent.getFormDataEntityTagValueEvents().stream()
        .filter(formDataEntityTagValueEvent -> formDataEntityTagValueEvent.getEntityTagEvent().getTag().equals(tag))
        .anyMatch(metaDataEvent ->  (Integer.parseInt((String) metaDataEvent.getValue())) >= (Integer) value);
  }

  public static Object get_(FormDataEntityTagEvent formDataEntityTagEvent,String tag, String date) {

    return formDataEntityTagEvent.getFormDataEntityTagValueEvents()
        .stream()
        .filter(
            formDataEntityTagValueEvent -> formDataEntityTagValueEvent.getEntityTagEvent().getTag()
                .equals(tag))
        .map(formDataEntityTagValueEvent -> {
          switch (formDataEntityTagValueEvent.getEntityTagEvent().getValueType()){
            case "integer" :
              return Integer.parseInt((String) formDataEntityTagValueEvent.getValue());
            case "string" :
              return  String.valueOf(formDataEntityTagValueEvent.getValue());
            case "boolean":
              return  (boolean) formDataEntityTagValueEvent.getValue();
          }
         return  formDataEntityTagValueEvent.getValue();
        })
        .findFirst().orElse(null);
  }


}
