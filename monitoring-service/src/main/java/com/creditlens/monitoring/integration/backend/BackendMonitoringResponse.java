package com.creditlens.monitoring.integration.backend;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record BackendMonitoringResponse(
    List<BackendMonitoringItem> items, int page, int size, long totalItems, int totalPages) {}
