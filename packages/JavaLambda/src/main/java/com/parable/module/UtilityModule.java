package com.parable.module;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parable.adapter.PojoMapper;

import dagger.Module;
import dagger.Provides;

@Module
public class UtilityModule {
    
    @Provides
    public PojoMapper getPojoMapper() {
        return new PojoMapper(new ObjectMapper());
    }
}
