package com.encurtador.infrastructure.adapter.in.web;

import com.encurtador.domain.exception.InvalidManagementKeyException;
import com.encurtador.domain.port.in.CreateLinkCommand;
import com.encurtador.domain.port.in.CreateLinkUseCase;
import com.encurtador.domain.port.in.CreatedLink;
import com.encurtador.domain.port.in.GetLinkStatsUseCase;
import com.encurtador.domain.port.in.LinkStats;
import com.encurtador.domain.port.in.RedirectLinkUseCase;
import com.encurtador.infrastructure.adapter.in.web.dto.CreateLinkRequest;
import com.encurtador.infrastructure.adapter.in.web.dto.CreatedLinkResponse;
import com.encurtador.infrastructure.adapter.in.web.dto.LinkStatsResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
public class LinkController {

    private final CreateLinkUseCase createLinkUseCase;
    private final RedirectLinkUseCase redirectLinkUseCase;
    private final GetLinkStatsUseCase getLinkStatsUseCase;

    public LinkController(CreateLinkUseCase createLinkUseCase,
                          RedirectLinkUseCase redirectLinkUseCase,
                          GetLinkStatsUseCase getLinkStatsUseCase) {
        this.createLinkUseCase = createLinkUseCase;
        this.redirectLinkUseCase = redirectLinkUseCase;
        this.getLinkStatsUseCase = getLinkStatsUseCase;
    }

    @PostMapping("/api/links")
    public ResponseEntity<CreatedLinkResponse> createLink(@RequestBody CreateLinkRequest request,
                                                         HttpServletRequest servletRequest) {
        String baseUrl = ServletUriComponentsBuilder.fromContextPath(servletRequest).build().toUriString();
        CreateLinkCommand command = new CreateLinkCommand(
                request.targetUrl(),
                request.code(),
                request.expiresAtUtc(),
                baseUrl
        );

        CreatedLink created = createLinkUseCase.create(command);
        URI location = URI.create("/" + created.code());
        CreatedLinkResponse response = new CreatedLinkResponse(
                created.shortUrl(),
                created.code(),
                created.managementKey()
        );

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{code}")
    public ResponseEntity<Void> redirect(@PathVariable String code) {
        String targetUrl = redirectLinkUseCase.getTargetUrlAndIncrementCount(code);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(targetUrl))
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.EXPIRES, "0")
                .build();
    }

    @GetMapping("/api/links/{code}/stats")
    public ResponseEntity<LinkStatsResponse> getStats(
            @PathVariable String code,
            @RequestHeader(value = "X-Management-Key", required = false) String managementKey) {

        if (managementKey == null || managementKey.isBlank()) {
            throw new InvalidManagementKeyException("Chave de gerenciamento não informada.");
        }

        LinkStats stats = getLinkStatsUseCase.getStats(code, managementKey);
        LinkStatsResponse response = new LinkStatsResponse(
                stats.code(),
                stats.targetUrl(),
                stats.createdAtUtc(),
                stats.expiresAtUtc(),
                stats.accessCount()
        );

        return ResponseEntity.ok(response);
    }
}
