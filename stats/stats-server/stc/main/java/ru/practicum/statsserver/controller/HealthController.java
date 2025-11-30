package stc.main.java.ru.practicum.statsserver.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Collections.singletonMap("status", "OK");
    }

    @GetMapping("/")
    public Map<String, String> home() {
        return Collections.singletonMap("message", "Stats Service is running");
    }
}