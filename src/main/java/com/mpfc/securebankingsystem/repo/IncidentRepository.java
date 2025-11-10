package com.mpfc.securebankingsystem.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mpfc.securebankingsystem.entity.Incident;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

}
