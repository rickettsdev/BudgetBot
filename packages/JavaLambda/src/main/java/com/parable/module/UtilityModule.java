package com.parable.module;

import com.fasterxml.jackson.databind.ObjectMapper;

import dagger.Module;
import dagger.Provides;

@Module
public class UtilityModule {

    @Provides
    public ObjectMapper getObjectMapper() {
        return new ObjectMapper();
    }
}
