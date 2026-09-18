package com.creditlens.backend.api.dto;

import java.util.List;

public record PageDto<T>(List<T> items, int page, int size, long totalItems, int totalPages) {

    public PageDto {
        items = List.copyOf(items);
        if (page < 0 || size < 1 || totalItems < 0 || totalPages < 0) {
            throw new IllegalArgumentException("page metadata must not be negative and size must be positive");
        }
    }
}
