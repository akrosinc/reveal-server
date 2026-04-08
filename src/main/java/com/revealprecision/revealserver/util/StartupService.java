package com.revealprecision.revealserver.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revealprecision.revealserver.api.v1.dto.response.PopulationResponse;
import com.revealprecision.revealserver.api.v1.dto.response.PopulationResponseData;
import com.revealprecision.revealserver.client.PopulationClient;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.repository.LocationRepository;
import lombok.AllArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
@AllArgsConstructor
public class StartupService {

    private final PopulationClient populationClient;
    private final LocationRepository locationRepository;
    private final ObjectMapper objectMapper;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!populationDataLoaded())
            fetchAndProcessPopulationData();
    }

    public boolean populationDataLoaded() {
        return locationRepository.populationDataExistsForAll();
    }

    public void fetchAndProcessPopulationData() {
        Set<UUID> locationsIds = locationRepository.findAllIdentifiers();
        Flux.fromIterable(locationsIds)
                .flatMap(id -> Mono.delay(Duration.ofMillis(333))
                                .then(getPopulationDataForLocation(id)
                                        .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                                                .filter(throwable -> throwable instanceof WebClientResponseException
                                                        && ((WebClientResponseException) throwable).getStatusCode().is5xxServerError()))),
                        5)
                .then()
                .subscribe();
    }

    public Mono<PopulationResponseData> getPopulationDataForLocation(UUID locationId) {
        Location location = locationRepository.findById(locationId).orElseThrow(
                () -> new NotFoundException(Pair.of(Location.Fields.identifier, locationId),
                        Location.class));
        List<Object> coordinates = location.getGeometry().getCoordinates();
        var coordinatesWithElevation = addElevation(coordinates);
        Mono<PopulationResponse> response = populationClient.getPopulationForLocation(location.getGeometry().getType(), coordinatesWithElevation);
        return response.flatMap(res -> {
            if (res.getResults() != null && !res.getResults().isEmpty()) {
                var population = res.getResults().get(0).getPopulationData();
                JsonNode jsonNode = objectMapper.convertValue(population, JsonNode.class);
                locationRepository.updatePopulationData(locationId, jsonNode);
                return Mono.justOrEmpty(population);
            }
            return Mono.empty();
        });
    }

    @SuppressWarnings("unchecked")
    private List<Object> addElevation(List<Object> coordinates) {
        List<Object> result = new ArrayList<>();

        for (Object item : coordinates) {
            if (item instanceof List) {
                List<Object> nestedList = (List<Object>) item;
                if (!nestedList.isEmpty() && nestedList.get(0) instanceof Number) {
                    List<Double> updatedPoint = new ArrayList<>();
                    for (Object value : nestedList) {
                        if (value instanceof Number) {
                            updatedPoint.add(((Number) value).doubleValue());
                        } else {
                            throw new IllegalArgumentException("Unexpected value in coordinate list: " + value);
                        }
                    }
                    updatedPoint.add(0.0);
                    result.add(updatedPoint);
                } else {
                    result.add(addElevation(nestedList));
                }
            } else {
                throw new IllegalArgumentException("Unexpected non-list item in coordinates: " + item);
            }
        }

        return result;
    }

}
