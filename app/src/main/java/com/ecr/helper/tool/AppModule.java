package com.ecr.helper.tool;

import com.ecr.helper.app_constants_config_info.AppConfig;


import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class AppModule {

    @Provides
    @Singleton
    public AppConfig provideAppConfig() {
        return new AppConfig();
    }


}
