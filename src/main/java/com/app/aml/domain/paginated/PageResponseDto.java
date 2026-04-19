package com.app.aml.domain.paginated;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * A generic data transfer object for paginated API responses.
 * Standardizes the JSON envelope for all lists across the platform.
 *
 * @param <T> The type of the content elements (usually a DTO)
 */
public record PageResponseDto<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean isLast
) {
    /**
     * Factory method to convert a Spring Data Page directly into our standardized DTO.
     * * @param springPage The raw Page object from the repository
     * @param <T> The type of the elements
     * @return A clean PageResponseDto
     */
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

    /**
     * Factory method that maps the internal entities to DTOs on the fly.
     * This is the most common use case in your Service layer.
     *
     * @param springPage The raw Page of database entities
     * @param mapper The function to convert Entity -> DTO
     * @param <T> The target DTO type
     * @param <U> The source Entity type
     * @return A clean PageResponseDto containing the mapped DTOs
     */
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