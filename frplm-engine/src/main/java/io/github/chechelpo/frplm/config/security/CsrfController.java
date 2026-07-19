package io.github.chechelpo.frplm.config.security;

import org.springframework.http.MediaType;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/security")
public final class CsrfController {

    @GetMapping(
            value = "/csrf",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public CsrfResponse csrf(CsrfToken csrfToken) {
        return new CsrfResponse(
                csrfToken.getHeaderName(),
                csrfToken.getToken()
        );
    }

    public record CsrfResponse(
            String headerName,
            String token
    ) {
    }
}