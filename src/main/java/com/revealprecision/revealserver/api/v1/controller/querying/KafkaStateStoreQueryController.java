package com.revealprecision.revealserver.api.v1.controller.querying;

import com.revealprecision.revealserver.messaging.KafkaConstants;
import com.revealprecision.revealserver.messaging.Message;
import com.revealprecision.revealserver.messaging.message.LocationBusinessStatusAggregate;
import com.revealprecision.revealserver.messaging.message.OperationalAreaAggregate;
import com.revealprecision.revealserver.messaging.message.OperationalAreaVisitedCount;
import com.revealprecision.revealserver.messaging.message.PersonBusinessStatusAggregate;
import com.revealprecision.revealserver.messaging.message.TaskAggregate;
import com.revealprecision.revealserver.messaging.message.TaskEvent;
import com.revealprecision.revealserver.props.KafkaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/state-store")
@Slf4j
@RequiredArgsConstructor
public class KafkaStateStoreQueryController<T> {

  private final StreamsBuilderFactoryBean getKafkaStreams;
  private final KafkaProperties kafkaProperties;
  private final KafkaTemplate<String, Message> kafkaTemplate;

  @GetMapping("/personBusinessStatus")
  public void personBusinessStatus() {
    KafkaStreams kafkaStreams = getKafkaStreams.getKafkaStreams();
    ReadOnlyKeyValueStore<String, PersonBusinessStatusAggregate> counts = kafkaStreams.store(
        StoreQueryParameters.fromNameAndType(
            kafkaProperties.getStoreMap().get(KafkaConstants.personBusinessStatus),
            QueryableStoreTypes.keyValueStore())
    );
    iterateThroughStore(counts);
  }

  @GetMapping("/tableOfOperationalAreas")
  public void tableOfOperationalAreas() {
    KafkaStreams kafkaStreams = getKafkaStreams.getKafkaStreams();
    ReadOnlyKeyValueStore<String, OperationalAreaAggregate> counts = kafkaStreams.store(
        StoreQueryParameters.fromNameAndType(
            kafkaProperties.getStoreMap().get(KafkaConstants.tableOfOperationalAreas),
            QueryableStoreTypes.keyValueStore())
    );
    iterateThroughStore(counts);
  }



  @GetMapping("/assignedStructureCountPerParent")
  public void countOfAssignedStructure() {
    KafkaStreams kafkaStreams = getKafkaStreams.getKafkaStreams();
    ReadOnlyKeyValueStore<String, Long> counts = kafkaStreams.store(
        StoreQueryParameters.fromNameAndType(
            kafkaProperties.getStoreMap().get(KafkaConstants.assignedStructureCountPerParent),
            QueryableStoreTypes.keyValueStore())
    );
    iterateThroughStore(counts);
  }

  @GetMapping("/tableOfOperationalAreaHierarchies")
  public void tableOfOperationalAreaHierarchies() {
    KafkaStreams kafkaStreams = getKafkaStreams.getKafkaStreams();
    ReadOnlyKeyValueStore<String, OperationalAreaAggregate> counts = kafkaStreams.store(
        StoreQueryParameters.fromNameAndType(
            kafkaProperties.getStoreMap().get(KafkaConstants.tableOfOperationalAreaHierarchies),
            QueryableStoreTypes.keyValueStore())
    );
    iterateThroughStore(counts);
  }

  @GetMapping("/locationBusinessStatusByPlanParentHierarchy")
  public void locationBusinessStatusByPlanParentHierarchy() {
    KafkaStreams kafkaStreams = getKafkaStreams.getKafkaStreams();
    ReadOnlyKeyValueStore<String, Long> counts = kafkaStreams.store(
        StoreQueryParameters.fromNameAndType(kafkaProperties.getStoreMap()
                .get(KafkaConstants.locationBusinessStatusByPlanParentHierarchy),
            QueryableStoreTypes.keyValueStore())
    );
    iterateThroughStore(counts);
  }

  @GetMapping("/operationalAreaByPlanParentHierarchy")
  public void operationalAreaByPlanParentHierarchy() {
    KafkaStreams kafkaStreams = getKafkaStreams.getKafkaStreams();
    ReadOnlyKeyValueStore<String, OperationalAreaVisitedCount> counts = kafkaStreams.store(
        StoreQueryParameters.fromNameAndType(kafkaProperties.getStoreMap()
                .get(KafkaConstants.operationalAreaByPlanParentHierarchy),
            QueryableStoreTypes.keyValueStore())
    );
    iterateThroughStore(counts);
  }


  @GetMapping("/locationBusinessStatus")
  public void locationBusinessStatus() {
    KafkaStreams kafkaStreams = getKafkaStreams.getKafkaStreams();
    ReadOnlyKeyValueStore<String, LocationBusinessStatusAggregate> counts = kafkaStreams.store(
        StoreQueryParameters.fromNameAndType(
            kafkaProperties.getStoreMap().get(KafkaConstants.locationBusinessStatus),
            QueryableStoreTypes.keyValueStore())
    );
    iterateThroughStore(counts);
  }

  @GetMapping("/taskPlanParent")
  public void taskPlanParent() {
    KafkaStreams kafkaStreams = getKafkaStreams.getKafkaStreams();
    ReadOnlyKeyValueStore<String, TaskEvent> counts = kafkaStreams.store(
        StoreQueryParameters.fromNameAndType(
            kafkaProperties.getStoreMap().get(KafkaConstants.taskPlanParent),
            QueryableStoreTypes.keyValueStore())
    );
    iterateThroughStore(counts);
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
    iterateThroughStore(counts);
  }

  @GetMapping("/taskParentCount")
  public void taskParentCount() {
    KafkaStreams kafkaStreams = getKafkaStreams.getKafkaStreams();
    ReadOnlyKeyValueStore<String, TaskAggregate> counts = kafkaStreams.store(
        StoreQueryParameters.fromNameAndType(
            kafkaProperties.getStoreMap().get(KafkaConstants.taskParent),
            QueryableStoreTypes.keyValueStore())
    );
    iterateThroughStore(counts);
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
    return counts.get(plan.concat("_").concat(parent));
  }

  private void iterateThroughStore(ReadOnlyKeyValueStore<String, ?> counts) {
    KeyValueIterator<String, ?> all = counts.all();
    log.info("Started");
    while (all.hasNext()) {
      KeyValue<String, ?> keyValue = all.next();
      String key = keyValue.key;
      Object value = keyValue.value;
      log.info("key: {} - value: {}", key, value);
    }
    log.info("Ended");
  }

}
