package com.revealprecision.revealserver.client;

import com.revealprecision.revealserver.api.v1.dto.request.PopulationRequest;
import com.revealprecision.revealserver.api.v1.dto.response.PopulationResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class PopulationClient {

    private final WebClient webClient;

    public PopulationClient(WebClient.Builder builder) {
        this.webClient = builder.baseUrl("https://populationexplorer.com")
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Authorization", "Token 270c931c147557035071cc118841e4d528d11ffe")
                .defaultHeader("DS-NAME", "landscan")
                .defaultHeader("DS-YEAR", "2016")
                .build();
    }

    public Mono<PopulationResponse> getPopulationForLocation(List<Object> locationCoordinates) {
        return webClient.post()
                .uri("/api/geodata/population/")
                .bodyValue(new PopulationRequest("Polygon", locationCoordinates))
                .retrieve()
                .onStatus(
                        HttpStatus::is4xxClientError,
                        clientResponse -> Mono.error(new RuntimeException("Invalid request to PopEx API"))
                )
                .onStatus(
                        HttpStatus::is5xxServerError,
                        clientResponse -> Mono.error(new RuntimeException("PopEx service is unavailable"))
                )
                .bodyToMono(PopulationResponse.class);

    }
}
