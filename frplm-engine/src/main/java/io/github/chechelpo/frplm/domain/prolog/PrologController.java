package io.github.chechelpo.frplm.domain.prolog;

import io.github.chechelpo.frplm.utils.prolog.PrologSourceValidator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs.API_BASE;

@RestController
@RequestMapping(API_BASE + "/prolog")
public class PrologController {

    private final PrologSourceValidator prologSourceValidator;

    public PrologController(PrologSourceValidator prologSourceValidator) {
        this.prologSourceValidator = prologSourceValidator;
    }

    @PostMapping("/validate")
    public ResponseEntity<PrologSourceValidator.ValidationResult> validate(
            @RequestBody ValidationRequest request
    ) {
        System.out.println("Hit");
        PrologSourceValidator.ValidationResult result = prologSourceValidator.validate(request.source());
        return ResponseEntity.ok(result);
    }

    public record ValidationRequest(String source) {}
}

