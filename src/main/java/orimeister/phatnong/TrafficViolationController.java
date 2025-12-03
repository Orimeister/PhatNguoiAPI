package orimeister.phatnong;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@CrossOrigin(origins="*")
@RequestMapping("/api/traffic-violations")
public class TrafficViolationController {
    private final TrafficViolationService service;
    public TrafficViolationController(TrafficViolationService service) {
        this.service = service;
    }

    @GetMapping("/{plate}")
    public ResponseEntity<List<TrafficViolation>> getViolations(
            @PathVariable String plate,
            @RequestParam String vehicleType) {
        try {
            List<TrafficViolation> violations = service.getTrafficViolations(plate, vehicleType);
            return ResponseEntity.ok(violations);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }



}
