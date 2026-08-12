package pl.tomaszko.s03e04.web;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.tomaszko.s03e04.service.CitySearchService;
import pl.tomaszko.s03e04.service.ItemsSearchService;
import pl.tomaszko.s03e04.service.OutputConstraintValidator;
import pl.tomaszko.s03e04.service.SearchOutcome;

@RestController
@RequestMapping("/api")
public class SearchController {

    private final CitySearchService citySearchService;
    private final ItemsSearchService itemsSearchService;
    private final OutputConstraintValidator outputConstraintValidator;

    public SearchController(
            CitySearchService citySearchService,
            ItemsSearchService itemsSearchService,
            OutputConstraintValidator outputConstraintValidator) {
        this.citySearchService = citySearchService;
        this.itemsSearchService = itemsSearchService;
        this.outputConstraintValidator = outputConstraintValidator;
    }

    @PostMapping("/city")
    public ResponseEntity<SearchResponse> searchByCity(@Valid @RequestBody SearchRequest request) {
        if (isInvalidParams(request.getParams())) {
            return badParams();
        }
        return toResponse(citySearchService.search(request.getParams().toString()));
    }

    @PostMapping("/items")
    public ResponseEntity<SearchResponse> searchByItems(@Valid @RequestBody SearchRequest request) {
        if (isInvalidParams(request.getParams())) {
            return badParams();
        }
        return toResponse(itemsSearchService.search(request.getParams().toString()));
    }

    private ResponseEntity<SearchResponse> toResponse(SearchOutcome outcome) {
        HttpStatus status = outcome.infrastructureError() ? HttpStatus.INTERNAL_SERVER_ERROR : HttpStatus.OK;
        return ResponseEntity.status(status).body(new SearchResponse(outcome.output()));
    }

    private ResponseEntity<SearchResponse> badParams() {
        String output = outputConstraintValidator.enforce("params must be a non-empty JSON object");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new SearchResponse(output));
    }

    private static boolean isInvalidParams(JsonNode params) {
        return params == null || params.isNull() || !params.isObject() || params.isEmpty();
    }
}
