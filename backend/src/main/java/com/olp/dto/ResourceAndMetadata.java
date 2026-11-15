package com.olp.dto;

import org.springframework.core.io.Resource;

/**
 * Holder for a streaming file Resource with HTTP metadata (FR-I17).
 */
public class ResourceAndMetadata {

    private final Resource resource;
    private final String filename;
    private final String contentType;
    private final Long contentLength;

    public ResourceAndMetadata(Resource resource, String filename, String contentType, Long contentLength) {
        this.resource = resource;
        this.filename = filename;
        this.contentType = contentType != null && !contentType.isBlank() ? contentType : "application/octet-stream";
        this.contentLength = contentLength;
    }

    public Resource getResource() {
        return resource;
    }

    public String getFilename() {
        return filename != null ? filename : "material";
    }

    public String getContentType() {
        return contentType;
    }

    public Long getContentLength() {
        return contentLength;
    }
}
