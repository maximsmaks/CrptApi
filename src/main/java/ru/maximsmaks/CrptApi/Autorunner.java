package ru.maximsmaks.CrptApi;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@Slf4j
@Component
public class Autorunner implements CommandLineRunner {

    public static final int LIMIT_REQUESTS_PER_MINUTE = 10;
    public static final int NUMOF_REQUESTS = 20;

    @Override
    public void run(String... args) throws Exception {
        val request = createRequest();
        val signature = "some-signature";

        val crptApi = new CrptApi(TimeUnit.MINUTES, LIMIT_REQUESTS_PER_MINUTE);

        val startAt = System.currentTimeMillis();
        log.info("started at {}", LocalDateTime.now());
        IntStream
            .range(0, NUMOF_REQUESTS)
            .parallel()
            .forEach(i -> {
                    val response = crptApi.sendCreateDocument(request, signature);
                    log.info("response from CrptApi: {}", response);
                }
            );
        log.info("finished at {}", LocalDateTime.now());
        val finishAt = System.currentTimeMillis();

        log.info(
            "{} requests proceed by {} sec (limit: {} requests/minute, actial: {})",
            NUMOF_REQUESTS,
            (finishAt - startAt) / 1_000,
            LIMIT_REQUESTS_PER_MINUTE,
            NUMOF_REQUESTS / ((finishAt - startAt) / 1_000)
        );
    }

    private CrptApi.CrptApiSendDocumentRequest createRequest() {
        return CrptApi.CrptApiSendDocumentRequest
            .builder()
            .description(
                CrptApi.CrptApiSendDocumentRequest.Description
                    .builder()
                    .participantInn("participantInn")
                    .build()
            )
            .doc_id("")
            .importRequest(true)
            .doc_status("")
            .doc_type("")
            .owner_inn("")
            .participant_inn("")
            .producer_inn("")
            .production_date(LocalDate.now())
            .production_type("")
            .products(
                List.of(
                    CrptApi.CrptApiSendDocumentRequest.Product
                        .builder()
                        .certificate_document("")
                        .certificate_document_date(LocalDate.now())
                        .certificate_document_number("")
                        .owner_inn("")
                        .producer_inn("")
                        .production_date(LocalDate.now())
                        .tnved_code("")
                        .uit_code("")
                        .uitu_cod("")
                        .build()
                )
            )
            .reg_date(LocalDate.now())
            .reg_number("")
            .build();
    }

}
