package com.woowa.bus.alert.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataBusAlertHistoryRepository extends JpaRepository<BusAlertHistoryJpaEntity, Long> {
}
