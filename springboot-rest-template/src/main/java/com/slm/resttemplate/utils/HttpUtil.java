package com.slm.resttemplate.utils;

import com.alibaba.fastjson.util.ParameterizedTypeImpl;
import com.slm.resttemplate.enums.ResultStatus;
import com.slm.resttemplate.model.ApiResponse;
import com.slm.resttemplate.model.InMemoryResource;
import com.slm.resttemplate.model.TradeEntity;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class HttpUtil {

    private final RestTemplate restTemplate;

    public <R, T> T request(HttpMethod httpMethod, String url, Map<String, String> headerParam, R requestBody, T responseType) {
        HttpHeaders headers = new HttpHeaders();
        headerParam.forEach(headers::add);
        RequestEntity<R> requestEntity = new RequestEntity<R>(requestBody, headers, httpMethod, URI.create(url));
        ResponseEntity<ApiResponse<T>> exchange = restTemplate.exchange(requestEntity, getReference((Class<T>) responseType.getClass()));
        return Objects.requireNonNull(exchange.getBody()).getData();
    }

    private <T> ParameterizedTypeReference<ApiResponse<T>> getReference(Class<T> clazz) {
        return ParameterizedTypeReference.forType(new ParameterizedTypeImpl(new Type[] {clazz}, null, ApiResponse.class));
    }

    public void uploadFile(HttpMethod httpMethod, String url, Map<String, String> extraParam, InputStream is, String fileName) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        extraParam.forEach(headers::add);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>(1);
        File file = new File(Objects.requireNonNull(fileName));
        FileUtils.copyInputStreamToFile(is, file);
        FileSystemResource resource = new FileSystemResource(file);
        form.add("file", resource);
        RequestEntity<MultiValueMap<String, Object>> requestEntity = new RequestEntity<>(form, headers, httpMethod, URI.create(url));
        ResponseEntity<ApiResponse> exchange = restTemplate.exchange(requestEntity, ApiResponse.class);
        if (ResultStatus.SUCCESS.getCode() != Objects.requireNonNull(exchange.getBody()).getCode()) {
            throw new RuntimeException("上传文件异常:".concat(exchange.getBody().getMessage()));
        }
    }

    public void uploadFile2(HttpMethod httpMethod, String url, Map<String, String> extraParam, InputStream is, String filename) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        extraParam.forEach(headers::add);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>(1);   //需要MultiValueMap数据结构
        InMemoryResource imr = new InMemoryResource(is.readAllBytes(), "", filename, System.currentTimeMillis());
        form.add("file", imr);
        RequestEntity<MultiValueMap<String, Object>> requestEntity = new RequestEntity<>(form, headers, httpMethod, URI.create(url));
        ResponseEntity<ApiResponse> exchange = restTemplate.exchange(requestEntity, ApiResponse.class);
        if (ResultStatus.SUCCESS.getCode() != Objects.requireNonNull(exchange.getBody()).getCode()) {
            throw new RuntimeException("上传文件异常:".concat(exchange.getBody().getMessage()));
        }
    }

    public byte[] downloadFile(HttpMethod httpMethod, String url, Map<String, String> extraParam) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        extraParam.forEach(headers::add);
        RequestEntity<MultiValueMap<String, Object>> requestEntity = new RequestEntity<>(headers, httpMethod, URI.create(url));
        ResponseEntity<byte[]> exchange = restTemplate.exchange(requestEntity, byte[].class);
        return exchange.getBody();
    }

}
