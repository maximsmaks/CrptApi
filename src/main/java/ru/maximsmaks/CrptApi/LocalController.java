package ru.maximsmaks.CrptApi;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class LocalController {

    @PostMapping("documents/create")
    public ResponseEntity<CrptApi.CrptApiSendDocumentResponse> process(
        @RequestBody CrptApi.CrptApiSendDocumentRequest request
    ) {
        return new ResponseEntity<>(
            CrptApi.CrptApiSendDocumentResponse
                .builder()
                .response("some response")
                .build(),
            HttpStatus.OK
        );
    }

}
