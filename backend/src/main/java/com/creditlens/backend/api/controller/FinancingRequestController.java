package com.creditlens.backend.api.controller;

import com.creditlens.backend.api.dto.CreateFinancingRequestRequestDto;
import com.creditlens.backend.api.dto.FinancingRequestDto;
import com.creditlens.backend.api.mapper.FinancingRequestApiMapper;
import com.creditlens.backend.application.CreateFinancingRequestResult;
import com.creditlens.backend.application.CreateFinancingRequestUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/financing-requests")
public class FinancingRequestController {

    private final CreateFinancingRequestUseCase createFinancingRequest;
    private final FinancingRequestApiMapper mapper;

    public FinancingRequestController(
            CreateFinancingRequestUseCase createFinancingRequest,
            FinancingRequestApiMapper mapper
    ) {
        this.createFinancingRequest = createFinancingRequest;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<FinancingRequestDto> create(
            @Valid @RequestBody CreateFinancingRequestRequestDto request
    ) {
        CreateFinancingRequestResult result = createFinancingRequest.execute(mapper.toCommand(request));
        FinancingRequestDto response = mapper.toDto(result.financingRequest());
        if (!result.created()) {
            return ResponseEntity.ok(response);
        }

        URI location = URI.create("/api/v1/financing-requests/" + response.id());
        return ResponseEntity.created(location).body(response);
    }
}
