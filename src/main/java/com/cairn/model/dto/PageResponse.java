package com.cairn.model.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Generic wrapper for all paginated list responses in Cairn.
 *
 * <p>WHY: Enforces SDE Standard #2 (Pagination). Intercepts Spring Data's bloated Page object and
 * strips it down to a clean, enterprise-grade wrapper without verbose metadata.
 *
 * @param <T> The type of the DTO contained in the page
 */
public record PageResponse<T>(
    List<T> data,
    int pageNumber,
    int pageSize,
    long totalElements,
    int totalPages,
    boolean isLast) {

  /**
   * Creates a PageResponse from a Spring Data Page.
   *
   * @param page the Spring Data Page
   * @param <T> the type parameter
   * @return a clean PageResponse DTO
   */
  public static <T> PageResponse<T> of(Page<T> page) {
    return new PageResponse<>(
        page.getContent(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages(),
        page.isLast());
  }
}
