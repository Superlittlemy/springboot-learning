package com.slm.resttemplate.model;

import org.springframework.core.io.ByteArrayResource;

import java.io.IOException;

public class InMemoryResource extends ByteArrayResource {

    private String filename;
    private long lastModified;

    public InMemoryResource(byte[] byteArray, String description, String filename, long lastModified) {
        super(byteArray, description);
        this.filename = filename;
        this.lastModified = lastModified;
    }

    @Override
    public long lastModified() throws IOException {
        return lastModified;
    }

    @Override
    public String getFilename() {
        return filename;
    }
}
