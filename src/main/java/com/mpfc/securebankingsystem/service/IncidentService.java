package com.mpfc.securebankingsystem.service;

import com.mpfc.securebankingsystem.entity.Incident;
import com.mpfc.securebankingsystem.repo.IncidentRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class IncidentService {
    private final IncidentRepository repo;

    public IncidentService(IncidentRepository repo) {
        this.repo = repo;
    }

    public void recordIncident(String username, String eventType, String details) {
        Incident i = new Incident();
        i.setUsername(username);
        i.setEventType(eventType);
        i.setDetails(details);
        i.setTimestamp(Instant.now());
        repo.save(i);
    }

    public void acknowledge(Long id) {
        repo.findById(id).ifPresent(i -> { i.setAcknowledged(true); repo.save(i); });
    }
}
