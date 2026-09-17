package com.aldo.event_pass.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aldo.event_pass.entity.Pago;

public interface PagoRepository extends JpaRepository<Pago, Long>{
    
}
