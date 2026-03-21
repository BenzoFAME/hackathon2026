package org.example.orbit.controller;

import lombok.RequiredArgsConstructor;
import org.example.orbit.Service.TleLoaderService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final TleLoaderService tleLoaderService;
    @PostMapping("/reload")
    public Map<String , Object> reload(){
        int count = tleLoaderService.loadActiveSatellites();
        return Map.of("status", "ok"
                , "loaded", count, "timestamp"
                , Instant.now().toString());
    }
}
