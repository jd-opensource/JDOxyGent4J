package com.jd.oxygent.oxybank.core.model.annotation;

import java.util.List;

import lombok.Data;

/**
 * Annotation platform common response models.
 */
public class CommonModel {

    @Data
    public static class APIResponse<T> {

        /**
         * Response status code
         */
        private int code = 200;

        /**
         * Response message
         */
        private String msg = "success";

        /**
         * Response data
         */
        private T data;
    }

    @Data
    public static class DataListResponse<T> {

        /**
         * Total record count
         */
        private int total;

        /**
         * Current page number
         */
        private int page;

        /**
         * Items per page
         */
        private int pageSize;

        /**
         * Data list
         */
        private List<T> items;
    }
}