package com.slm.seata.order.utils;

import com.alibaba.fastjson.util.ParameterizedTypeImpl;
import com.slm.seata.order.enums.ResultStatus;
import com.slm.seata.order.model.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Type;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class HttpClient {

    private final RestTemplate restTemplate;

    public <R, T> ApiResponse<T> request(HttpMethod httpMethod, String url, Map<String, String> headerParam, R requestBody, Class<T> clazz) {
        log.debug("请求 {} {}", httpMethod.name(), url);
        log.debug("header {}", headerParam);
        log.debug("body {}", requestBody);
        HttpHeaders headers = new HttpHeaders();
        if (headerParam != null && !headerParam.isEmpty()) {
            headerParam.forEach(headers::add);
        }
        RequestEntity<R> requestEntity = new RequestEntity<>(requestBody, headers, httpMethod, URI.create(url));
        LocalDateTime start = LocalDateTime.now();
        ResponseEntity<ApiResponse<T>> exchange = restTemplate.exchange(requestEntity, getReference((clazz)));
        LocalDateTime end = LocalDateTime.now();
        ApiResponse<T> body = exchange.getBody();
        if (!HttpStatus.OK.equals(exchange.getStatusCode())) {
            log.error("接口调用异常：{}", body);
            throw new RuntimeException("接口调用异常");
        }
        log.debug("return {}", Objects.isNull(body) ? Strings.EMPTY : body.getData());
        log.debug("use time {}ms", ChronoUnit.MILLIS.between(start, end));
        return body;
    }

    private <T> ParameterizedTypeReference<ApiResponse<T>> getReference(Class<T> clazz) {
        Type[] type;
        if (clazz == null) {
            type = new Type[0];
        } else {
            type = new Type[] {clazz};
        }
        return ParameterizedTypeReference.forType(new ParameterizedTypeImpl(type, null, ApiResponse.class));
    }

}
