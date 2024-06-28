package ru.maximsmaks.CrptApi;

import io.github.bucket4j.Bucket;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.time.Duration.ofMinutes;

public class CrptApi {

    private interface CrptApiUri {
        /**
         * Для проверки ограничений запросов с использованием локального контроллера:
         */
        // String BASE = "http://localhost:8080/";
        String BASE = "https://ismp.crpt.ru/api/v3/lk/";
        String DOCUMENT_CREATE = "documents/create";
    }

    private final Logger logger = LoggerFactory.getLogger(CrptApi.class);

    private final Bucket bucket;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        val requestsLimitPerMinute =
            ((double) requestLimit) / TimeUnit.MINUTES.convert(1, timeUnit);
        bucket = Bucket.builder()
            .addLimit(
                limit -> limit
                    .capacity(requestLimit)
                    .refillGreedy((long) requestsLimitPerMinute, ofMinutes(1))
                    .initialTokens(0)
            )
            .build();
    }

    public CrptApiSendDocumentResponse sendCreateDocument(
        final CrptApiSendDocumentRequest request,
        final String signature
    ) {
        try {
            acquireSendPermission();
            return doCreateDocumentRequest(request);
        } catch (GenericRequestException e) {
            logger.error("request failed", e);
            return null;
        }
    }

    private void acquireSendPermission() {
        while (true) {
            try {
                bucket.asBlocking().consume(1);
                return;
            } catch (InterruptedException e) {
                logger.error("acquire send permission interrupted", e);
            }
        }
    }

    private CrptApiSendDocumentResponse doCreateDocumentRequest(
        final CrptApiSendDocumentRequest crptApiSendDocumentRequest
    ) throws GenericRequestException {
        try {
            val response = WebClient
                .builder()
                //.filters(exchangeFilterFunctions -> {
                //    exchangeFilterFunctions.add(logRequest());
                //    exchangeFilterFunctions.add(logResponse());
                //})
                .baseUrl(CrptApiUri.BASE)
                .build()
                .post()
                .uri(CrptApiUri.DOCUMENT_CREATE)
                //.headers(h -> h.setBearerAuth(token))
                .body(BodyInserters.fromValue(crptApiSendDocumentRequest))
                .retrieve()
                .onStatus(
                    HttpStatusCode::is4xxClientError,
                    clientResponse -> clientResponse.bodyToMono(String.class)
                        .flatMap(body -> {
                            logger.error(body);
                            return Mono.error(new CreateDocumentRequestException(body));
                        })
                )
                .onStatus(
                    HttpStatusCode::is5xxServerError,
                    clientResponse -> clientResponse.bodyToMono(String.class)
                        .flatMap(body -> {
                            logger.error(body);
                            return Mono.error(new CreateDocumentRequestException(body));
                        })
                )
                .bodyToMono(String.class)
                //.log()
                .block();
            return CrptApiSendDocumentResponse
                .builder()
                .response(response)
                .build();
        } catch (Exception e) {
            logger.error("doCreateDocumentRequest() failed", e);
            throw new GenericRequestException(e);
        }
    }

    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            logger.info(
                " ********** API CLIENT REQUEST **********\n{} url: {}\nbody: {}",
                clientRequest.method(),
                clientRequest.url(),
                clientRequest.body()
            );
            return Mono.just(clientRequest);
        });
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            val status = clientResponse.statusCode();
            val body = clientResponse.bodyToMono(String.class);
            if (status != null && !(status.is4xxClientError() || status.is5xxServerError())) {
                logger.info(
                    "********** API CLIENT RESPONSE **********\nstatus code: {}\nbody: {}",
                    status.value(),
                    body
                );
            }
            return Mono.just(clientResponse);
        });
    }

    public static class CreateDocumentRequestException extends Exception {
        public CreateDocumentRequestException(
            String message
        ) {
            super("Can't send create document request: " + message);
        }
    }

    public static class GenericRequestException extends Exception {
        public GenericRequestException(
            final Throwable cause
        ) {
            super(cause);
        }
    }

    @Value
    @Builder
    public static class CrptApiSendDocumentRequest {
        Description description;
        String doc_id;
        String doc_status;
        String doc_type;
        //109
        Boolean importRequest;
        String owner_inn;
        String participant_inn;
        String producer_inn;
        LocalDate production_date;
        String production_type;
        List<Product> products;
        LocalDate reg_date;
        String reg_number;

        @Value
        @Builder
        @Jacksonized
        public static class Description {
            String participantInn;
        }

        @Value
        @Builder
        @Jacksonized
        public static class Product {
            String certificate_document;
            LocalDate certificate_document_date;
            String certificate_document_number;
            String owner_inn;
            String producer_inn;
            LocalDate production_date;
            String tnved_code;
            String uit_code;
            String uitu_cod;
        }
    }

    @Value
    @Builder
    @AllArgsConstructor
    public static class CrptApiSendDocumentResponse {
        String response;
    }

}
