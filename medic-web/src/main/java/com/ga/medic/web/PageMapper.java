package com.ga.medic.web;

import com.ga.medic.web.dto.PageResponse;
import org.springframework.data.domain.Page;

import java.util.function.Function;

/** Maps a Spring Page to a PageResponse. */
public class PageMapper {

    /** Maps each row and keeps page info (number, size, totals). */
    public <E, D> PageResponse<D> toResponse(Page<E> page, Function<E, D> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
