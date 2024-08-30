package com.slm.mybatis.typeHandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

@Slf4j
public class JSONHandler extends AbstractJsonTypeHandler {

    private static final ObjectMapper mapper = new ObjectMapper();

    public JSONHandler(Class<?> classType) {
        super(classType);
    }

    @Override
    protected Object parse(String jsonStr) {
        if (StringUtils.isBlank(jsonStr)) {
            return null;
        }
        try {
            return mapper.readValue(jsonStr, classType);
        } catch (JsonProcessingException e) {
            log.error("parse failed, jsonStr={}", jsonStr, e);
            return null;
        }
    }

    @Override
    protected String toJson(Object obj) {
        if (Objects.isNull(obj)) {
            return null;
        }
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Object to json string failed!" + obj, e);
        }
    }

}
