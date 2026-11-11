package com.ga.medic.web.dto;

import java.util.List;

/** Shared paginated API response. */
public record PageResponse<T>(
        List<T> content,
        int page, int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {
}
