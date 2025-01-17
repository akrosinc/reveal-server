package com.revealprecision.revealserver.client;

import com.revealprecision.revealserver.api.v1.dto.request.PopulationRequest;
import com.revealprecision.revealserver.api.v1.dto.response.PopulationResponse;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@Slf4j
public class PopulationClient {

    private final WebClient webClient;


    public PopulationClient(
                            WebClient.Builder builder,
                            @Value(value = "${webClient.population.baseUrl}") String baseUrl,
                            @Value(value = "${webClient.population.token}") String token,
                            @Value(value = "${webClient.population.ds-name}") String dsName,
                            @Value(value = "${webClient.population.ds-year}") String dsYear
    ) {
        builder.baseUrl(baseUrl);
        builder.defaultHeader("Content-Type", "application/json");
        builder.defaultHeader("Authorization", "Token " + token);
        builder.defaultHeader("DS-NAME", dsName);
        builder.defaultHeader("DS-YEAR", dsYear);
        this.webClient = builder
                .build();
    }

    public Mono<PopulationResponse> getPopulationForLocation(String polygonType, List<Object> locationCoordinates) {
        return webClient.post()
                .uri("/api/geodata/population/")
                .bodyValue(new PopulationRequest(polygonType, locationCoordinates))
                .exchangeToMono(response -> {
                    String contentType = response.headers().contentType()
                            .map(MediaType::toString)
                            .orElse("unknown");
                    if (contentType.equals(MediaType.APPLICATION_JSON_VALUE)) {
                        return response.bodyToMono(PopulationResponse.class);
                    } else {
                        return response.bodyToMono(String.class)
                                .doOnNext(body -> log.warn("Body {}", body))
                                .then(Mono.empty());
                    }
                })
                .doOnError(err ->
                        log.error("Population Client's API failed: {}", err.getMessage())
                );

    }
}
