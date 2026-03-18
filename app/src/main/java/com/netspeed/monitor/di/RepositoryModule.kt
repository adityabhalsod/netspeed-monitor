package com.netspeed.monitor.di

import com.netspeed.monitor.data.repository.PreferencesRepositoryImpl
import com.netspeed.monitor.data.repository.SpeedRepositoryImpl
import com.netspeed.monitor.domain.repository.PreferencesRepository
import com.netspeed.monitor.domain.repository.SpeedRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Hilt module that binds repository interfaces to their implementations
// Using abstract bindings for zero-overhead injection at compile time
@Module
@InstallIn(SingletonComponent::class) // Scoped to the application lifetime
abstract class RepositoryModule {

    // Binds the SpeedRepositoryImpl singleton to the SpeedRepository interface
    // Hilt will inject SpeedRepositoryImpl wherever SpeedRepository is requested
    @Binds
    @Singleton
    abstract fun bindSpeedRepository(
        impl: SpeedRepositoryImpl
    ): SpeedRepository

    // Binds the PreferencesRepositoryImpl singleton to the PreferencesRepository interface
    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(
        impl: PreferencesRepositoryImpl
    ): PreferencesRepository
}
