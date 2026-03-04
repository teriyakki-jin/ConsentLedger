package com.consentledger.global.dto;

import lombok.Getter;

@Getter
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final String error;
    private final Meta meta;

    private ApiResponse(boolean success, T data, String error, Meta meta) {
        this.success = success;
        this.data = data;
        this.error = error;
        this.meta = meta;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, null);
    }

    public static <T> ApiResponse<T> ok(T data, Meta meta) {
        return new ApiResponse<>(true, data, null, meta);
    }

    public static <T> ApiResponse<T> error(String errorMessage) {
        return new ApiResponse<>(false, null, errorMessage, null);
    }

    @Getter
    public static class Meta {
        private final long total;
        private final int page;
        private final int limit;

        public Meta(long total, int page, int limit) {
            this.total = total;
            this.page = page;
            this.limit = limit;
        }
    }
}
