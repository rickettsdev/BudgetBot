package com.parable.component;

import javax.inject.Singleton;

import com.parable.App;
import com.parable.module.AWSModule;
import com.parable.module.CommandModule;
import com.parable.module.ObserverModule;
import com.parable.module.UtilityModule;

import dagger.Component;

@Singleton
@Component(modules = {AWSModule.class, CommandModule.class, ObserverModule.class, UtilityModule.class})
public interface AppComponent {
    void inject(App app);
}
