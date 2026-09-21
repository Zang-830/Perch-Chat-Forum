package com.campus.community.file.storage;

import java.io.InputStream;

public interface ImageStorage {
    void store(String objectName, byte[] content, String contentType) throws Exception;

    InputStream open(String objectName) throws Exception;

    void delete(String objectName);
}
