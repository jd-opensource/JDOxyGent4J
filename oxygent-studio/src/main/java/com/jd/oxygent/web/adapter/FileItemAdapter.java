package com.jd.oxygent.web.adapter;

import org.apache.tomcat.util.http.fileupload.FileItem;
import org.apache.tomcat.util.http.fileupload.FileItemHeaders;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;

public class FileItemAdapter implements FileItem {
    private MultipartFile multipartFile;

    public FileItemAdapter(MultipartFile file) {
        this.multipartFile = file;
    }

    @Override
    public void delete() {

    }

    @Override
    public byte[] get() {
        return new byte[0];
    }

    @Override
    public String getContentType() {
        return multipartFile.getContentType();
    }

    @Override
    public String getFieldName() {
        return "file";
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return multipartFile.getInputStream();
    }

    @Override
    public String getName() {
        return multipartFile.getOriginalFilename();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return null;
    }

    @Override
    public long getSize() {
        return multipartFile.getSize();
    }

    @Override
    public String getString() {
        return "";
    }

    @Override
    public String getString(String s) throws UnsupportedEncodingException {
        return "";
    }

    @Override
    public boolean isFormField() {
        return true;
    }

    @Override
    public boolean isInMemory() {
        return false;
    }

    @Override
    public void setFieldName(String s) {

    }

    @Override
    public void setFormField(boolean b) {

    }

    @Override
    public void write(File file) throws Exception {

    }

    @Override
    public FileItemHeaders getHeaders() {
        return null;
    }

    @Override
    public void setHeaders(FileItemHeaders fileItemHeaders) {

    }
}
