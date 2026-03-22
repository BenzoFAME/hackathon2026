package org.example.orbit.controller;

import lombok.RequiredArgsConstructor;
import org.example.orbit.EnumsTags.Country;
import org.example.orbit.EnumsTags.ObjectType;
import org.example.orbit.EnumsTags.OrbitType;
import org.example.orbit.ModelDto.PassPredictionDto;
import org.example.orbit.ModelDto.SatPositionDto;
import org.example.orbit.ModelDto.SatelliteCardDto;
import org.example.orbit.ModelDto.SatelliteDto;
import org.example.orbit.Service.PassPredictionService;
import org.example.orbit.Service.SatelliteService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/satellites")
@RequiredArgsConstructor
public class SatelliteController {
    private final SatelliteService satelliteService;
    private final PassPredictionService passPredictionService;

    @GetMapping
    public Page<SatelliteDto> list(@RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "50") int size){
        return satelliteService.getAll(PageRequest.of(page, size));
    }
    @GetMapping("/search")
    public List<SatelliteDto> search(@RequestParam String query){
        return satelliteService.search(query);
    }
    @GetMapping("/filter")
    public List<SatelliteDto> filter(@RequestParam(required = false) Country country,
                                     @RequestParam(required = false) OrbitType orbitType ,
                                     @RequestParam(required = false) ObjectType objectType){
        return satelliteService.filter(country, orbitType, objectType);
    }
    @GetMapping("/{noradId}/position")
    public SatPositionDto position(@PathVariable Integer noradId,
                                   @RequestParam(required = false) String time){
        Instant instant = (time != null) ? Instant.parse(time) : Instant.now();
        return satelliteService.getPosition(noradId, instant);
    }
//    @GetMapping("/{noradId}/track")
//    public List<SatPositionDto> track(@PathVariable Integer noradId){
//        return satelliteService.getOrbitTrack(noradId);
//    }

    @GetMapping("/{noradId}/track")
    public ResponseEntity<Map<String, Object>> getOrbitTrack(@PathVariable Integer noradId) {
        return ResponseEntity.ok(satelliteService.getOrbitTrackGeoJson(noradId));
    }

    @GetMapping("/{noradId}/card")
    public SatelliteCardDto card (@PathVariable Integer noradId){
        return satelliteService.getCard(noradId);
    }

    @GetMapping("/{noradId}/passes")
    public List<PassPredictionDto> passes(@PathVariable Integer noradId,
                                          @RequestParam double lat,
                                          @RequestParam double lon,
                                          @RequestParam(defaultValue = "24") int hours){
        return satelliteService.getPasses(noradId, lat, lon, hours);
    }

    @GetMapping("/passes/batch")
    public ResponseEntity<List<PassPredictionDto>> getBatchPasses(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "2") int hoursAhead) {

        // Обязательно ограничиваем время предсказания
        // иначе без кэша сервер может задуматься очень надолго пу пу пу
        int safeHours = Math.min(hoursAhead, 12);

        return ResponseEntity.ok(passPredictionService.predictBatch(lat, lon, safeHours));
    }
}
