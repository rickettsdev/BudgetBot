package com.parable.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class PojoMapper {
    private final ObjectMapper mapper;

    public <T> T getObject(String json, TypeReference<T> reference) throws JsonProcessingException {
        return mapper.readValue(json, reference);
    }
}
