package com.revealprecision.revealserver.api.v1.controller.querying;

import com.revealprecision.revealserver.messaging.KafkaConstants;
import com.revealprecision.revealserver.messaging.Message;
import com.revealprecision.revealserver.messaging.message.LocationAssigned;
import com.revealprecision.revealserver.messaging.message.LocationBusinessStatus;
import com.revealprecision.revealserver.messaging.message.OperationalAreaVisitedCount;
import com.revealprecision.revealserver.messaging.message.PersonBusinessStatus;
import com.revealprecision.revealserver.messaging.message.TaskAggregate;
import com.revealprecision.revealserver.messaging.message.TaskEvent;
import com.revealprecision.revealserver.props.KafkaProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/state-store")
@Slf4j
public class KafkaStateStoreQueryController {

  @Autowired
  StreamsBuilderFactoryBean getKafkaStreams;

  @Autowired
  KafkaProperties kafkaProperties;

  @Autowired
  KafkaTemplate<String, Message> kafkaTemplate;

  @GetMapping("/personBusinessStatus")
  public void personBusinessStatus() {
    KafkaStreams kafkaStreams = getKafkaStreams.getKafkaStreams();
    ReadOnlyKeyValueStore<String, PersonBusinessStatus> counts = kafkaStreams.store(
        StoreQueryParameters.fromNameAndType(
            kafkaProperties.getStoreMap().get(KafkaConstants.personBusinessStatus),
            QueryableStoreTypes.keyValueStore())
    );
    KeyValueIterator<String, PersonBusinessStatus> all = counts.all();
    log.info("Started");
    while (all.hasNext()) {
      KeyValue<String, PersonBusinessStatus> keyValue = all.next();
      String key = keyValue.key;
      PersonBusinessStatus value = keyValue.value;
      log.info("key: {} - value: {}", key, value);
    }
    log.info("Ended");
//    return counts.get(parentId);
  }


  @GetMapping("/personBusinessStatusByPlanParentHierarchy")
  public void personBusinessStatusByPlanParentHierarchy() {
    KafkaStreams kafkaStreams = getKafkaStreams.getKafkaStreams();
    ReadOnlyKeyValueStore<String, Long> counts = kafkaStreams.store(
        StoreQueryParameters.fromNameAndType(kafkaProperties.getStoreMap()
                .get(KafkaConstants.personBusinessStatusByPlanParentHierarchy),
            QueryableStoreTypes.keyValueStore())
    );
    KeyValueIterator<String, Long> all = counts.all();
    log.info("Started");
    while (all.hasNext()) {
      KeyValue<String, Long> keyValue = all.next();
      String key = keyValue.key;
      Long value = keyValue.value;
      log.info("key: {} - value: {}", key, value);
    }
    log.info("Ended");
//    return counts.get(parentId);
  }


  @GetMapping("/tableOfOperationalAreas")
  public void tableOfOperationalAreas() {
    KafkaStreams kafkaStreams = getKafkaStreams.getKafkaStreams();
    ReadOnlyKeyValueStore<String, LocationAssigned> counts = kafkaStreams.store(
        StoreQueryParameters.fromNameAndType(
            kafkaProperties.getStoreMap().get(KafkaConstants.tableOfOperationalAreas),
            QueryableStoreTypes.keyValueStore())
    );
    KeyValueIterator<String, LocationAssigned> all = counts.all();
    log.info("Started");
    while (all.hasNext()) {
      KeyValue<String, LocationAssigned> keyValue = all.next();
      String key = keyValue.key;
      LocationAssigned value = keyValue.value;
      log.info("key: {} - value: {}", key, value);
    }
    log.info("Ended");
//    return counts.get(parentId);
  }

  @GetMapping("/locationBusinessStatusByPlanParentHierarchy/{parentId}")
  public Long locationBusinessStatusByPlanParentHierarchy(@PathVariable String parentId) {
    KafkaStreams kafkaStreams = getKafkaStreams.getKafkaStreams();
    ReadOnlyKeyValueStore<String, Long> counts = kafkaStreams.store(
        StoreQueryParameters.fromNameAndType(kafkaProperties.getStoreMap()
                .get(KafkaConstants.locationBusinessStatusByPlanParentHierarchy),
            QueryableStoreTypes.keyValueStore())
    );
    KeyValueIterator<String, Long> all = counts.all();
    log.info("Started");
    while (all.hasNext()) {
      KeyValue<String, Long> keyValue = all.next();
      String key = keyValue.key;
      Long value = keyValue.value;
      log.info("key: {} - value: {}", key, value);
    }
    log.info("Ended");
    return counts.get(parentId);
  }

  @GetMapping("/assignedStructureCountPerParent")
  public void countOfAssignedStructure() {
    KafkaStreams kafkaStreams = getKafkaStreams.getKafkaStreams();
    ReadOnlyKeyValueStore<String, Long> counts = kafkaStreams.store(
        StoreQueryParameters.fromNameAndType(
            kafkaProperties.getStoreMap().get(KafkaConstants.assignedStructureCountPerParent),
            QueryableStoreTypes.keyValueStore())
    );
    KeyValueIterator<String, Long> all = counts.all();
    log.info("Started");
    while (all.hasNext()) {
      KeyValue<String, Long> keyValue = all.next();
      String key = keyValue.key;
      Long value = keyValue.value;
      log.info("key: {} - value: {}", key, value);
    }
    log.info("Ended");
//    return counts.get(parentId);
  }


  @GetMapping("/tableOfOperationalAreaHierarchies")
  public void tableOfOperationalAreaHierarchies() {
    KafkaStreams kafkaStreams = getKafkaStreams.getKafkaStreams();
    ReadOnlyKeyValueStore<String, OperationalAreaVisitedCount> counts = kafkaStreams.store(
        StoreQueryParameters.fromNameAndType(
            kafkaProperties.getStoreMap().get(KafkaConstants.tableOfOperationalAreaHierarchies),
            QueryableStoreTypes.keyValueStore())
    );
    KeyValueIterator<String, OperationalAreaVisitedCount> all = counts.all();
    log.info("Started");
    while (all.hasNext()) {
      KeyValue<String, OperationalAreaVisitedCount> keyValue = all.next();
      String key = keyValue.key;
      OperationalAreaVisitedCount value = keyValue.value;
      log.info("key: {} - value: {}", key, value);
    }
    log.info("Ended");
  }

  @GetMapping("/locationBusinessStatusByPlanParentHierarchy")
  public void locationBusinessStatusByPlanParentHierarchy() {
    KafkaStreams kafkaStreams = getKafkaStreams.getKafkaStreams();
    ReadOnlyKeyValueStore<String, Long> counts = kafkaStreams.store(
        StoreQueryParameters.fromNameAndType(kafkaProperties.getStoreMap()
                .get(KafkaConstants.locationBusinessStatusByPlanParentHierarchy),
            QueryableStoreTypes.keyValueStore())
    );
    KeyValueIterator<String, Long> all = counts.all();
    log.info("Started");
    while (all.hasNext()) {
      KeyValue<String, Long> keyValue = all.next();
      String key = keyValue.key;
      Long value = keyValue.value;
      log.info("key: {} - value: {}", key, value);
    }
    log.info("Ended");
  }

  @GetMapping("/locationBusinessStatus")
  public void locationBusinessStatus() {
    KafkaStreams kafkaStreams = getKafkaStreams.getKafkaStreams();
    ReadOnlyKeyValueStore<String, LocationBusinessStatus> counts = kafkaStreams.store(
        StoreQueryParameters.fromNameAndType(
            kafkaProperties.getStoreMap().get(KafkaConstants.locationBusinessStatus),
            QueryableStoreTypes.keyValueStore())
    );
    KeyValueIterator<String, LocationBusinessStatus> all = counts.all();
    log.info("Started");
    while (all.hasNext()) {
      KeyValue<String, LocationBusinessStatus> keyValue = all.next();
      String key = keyValue.key;
      LocationBusinessStatus value = keyValue.value;
      log.info("key: {} - value: {}", key, value);
    }
    log.info("Ended");
  }

  @GetMapping("/taskPlanParent")
  public void taskPlanParent() {
    KafkaStreams kafkaStreams = getKafkaStreams.getKafkaStreams();
    ReadOnlyKeyValueStore<String, TaskEvent> counts = kafkaStreams.store(
        StoreQueryParameters.fromNameAndType(
            kafkaProperties.getStoreMap().get(KafkaConstants.taskPlanParent),
            QueryableStoreTypes.keyValueStore())
    );
    KeyValueIterator<String, TaskEvent> all = counts.all();
    log.info("Started");
    while (all.hasNext()) {
      KeyValue<String, TaskEvent> keyValue = all.next();
      String key = keyValue.key;
      TaskEvent value = keyValue.value;
      log.info("key: {} - value: {}", key, value);
    }
    log.info("Ended");
//    return counts.get(parentId);
  }

  @GetMapping("/taskPlanParent/{task}/{plan}/{parent}")
  public TaskEvent taskPlanParent(@PathVariable("task") String task,
      @PathVariable("plan") String plan, @PathVariable("parent") String parent) {
    KafkaStreams kafkaStreams = getKafkaStreams.getKafkaStreams();
    ReadOnlyKeyValueStore<String, TaskEvent> counts = kafkaStreams.store(
        StoreQueryParameters.fromNameAndType(
            kafkaProperties.getStoreMap().get(KafkaConstants.taskPlanParent),
            QueryableStoreTypes.keyValueStore())
    );
    KeyValueIterator<String, TaskEvent> all = counts.all();
//    log.info("Started");
//    while (all.hasNext()) {
//      KeyValue<String, TaskEvent> keyValue = all.next();
//      String key = keyValue.key;
//      TaskEvent value = keyValue.value;
//      log.info("key: {} - value: {}",key,value);
//    }
//    log.info("Ended");
    return counts.get(task.concat("_").concat(plan).concat("_").concat(parent));
  }

  @GetMapping("/taskParent")
  public void taskParent() {
    KafkaStreams kafkaStreams = getKafkaStreams.getKafkaStreams();
    ReadOnlyKeyValueStore<String, TaskAggregate> counts = kafkaStreams.store(
        StoreQueryParameters.fromNameAndType(
            kafkaProperties.getStoreMap().get(KafkaConstants.taskParent),
            QueryableStoreTypes.keyValueStore())
    );
    KeyValueIterator<String, TaskAggregate> all = counts.all();
    log.info("Started");
    while (all.hasNext()) {
      KeyValue<String, TaskAggregate> keyValue = all.next();
      String key = keyValue.key;
      TaskAggregate value = keyValue.value;
      log.info("key: {} - value: {}", key, value);
    }
    log.info("Ended");
//    return counts.get(parentId);
  }

  @GetMapping("/taskParentCount")
  public void taskParentCount() {
    KafkaStreams kafkaStreams = getKafkaStreams.getKafkaStreams();
    ReadOnlyKeyValueStore<String, TaskAggregate> counts = kafkaStreams.store(
        StoreQueryParameters.fromNameAndType(
            kafkaProperties.getStoreMap().get(KafkaConstants.taskParent),
            QueryableStoreTypes.keyValueStore())
    );
    KeyValueIterator<String, TaskAggregate> all = counts.all();
    log.info("Started");
    while (all.hasNext()) {
      KeyValue<String, TaskAggregate> keyValue = all.next();
      String key = keyValue.key;
      TaskAggregate value = keyValue.value;
      log.info("key: {} - value: {}", key, value.getTaskIds().size());
    }
    log.info("Ended");
//    return counts.get(parentId);
  }


  @GetMapping("/taskParent/{plan}/{parent}")
  public TaskAggregate taskParent(@PathVariable("plan") String plan,
      @PathVariable("parent") String parent) {
    KafkaStreams kafkaStreams = getKafkaStreams.getKafkaStreams();
    ReadOnlyKeyValueStore<String, TaskAggregate> counts = kafkaStreams.store(
        StoreQueryParameters.fromNameAndType(
            kafkaProperties.getStoreMap().get(KafkaConstants.taskParent),
            QueryableStoreTypes.keyValueStore())
    );
    KeyValueIterator<String, TaskAggregate> all = counts.all();
    return counts.get(plan.concat("_").concat(parent));
  }
}
