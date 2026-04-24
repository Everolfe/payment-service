package com.github.everolfe.paymentservice.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;



@Component
@RequiredArgsConstructor
@Slf4j
public class RandomNumberClient {

    private final RestClient restClient = RestClient.create();

    public Integer getRandomNumber() {
        try {
            String externalApiUrl = "https://www.random.org/integers/?num=1&min=1&max=100&col=1&base=10&format=plain&rnd=new";
            String response = restClient.get()
                    .uri(externalApiUrl)
                    .header("User-Agent", "PaymentService/1.0")
                    .retrieve()
                    .body(String.class);

            if (response != null) {
                return Integer.parseInt(response.trim());
            }
        } catch (Exception e) {
            log.error("External API failed, using fallback random: {}", e.getMessage());
        }

        return 0;
    }
}
