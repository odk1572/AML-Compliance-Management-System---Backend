package com.app.aml.paginated;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

public record PageResponseDto<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean isLast
) {

    public static <T> PageResponseDto<T> of(Page<T> springPage) {
        return new PageResponseDto<>(
                springPage.getContent(),
                springPage.getNumber(),
                springPage.getSize(),
                springPage.getTotalElements(),
                springPage.getTotalPages(),
                springPage.isLast()
        );
    }

    public static <T, U> PageResponseDto<T> of(Page<U> springPage, Function<U, T> mapper) {
        List<T> mappedContent = springPage.getContent().stream()
                .map(mapper)
                .toList();

        return new PageResponseDto<>(
                mappedContent,
                springPage.getNumber(),
                springPage.getSize(),
                springPage.getTotalElements(),
                springPage.getTotalPages(),
                springPage.isLast()
        );
    }
}