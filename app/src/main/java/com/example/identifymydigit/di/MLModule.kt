package com.example.identifymydigit.di

import android.content.Context
import com.example.identifymydigit.IdentifyDigitRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing ML dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object MLModule {

    /**
     * Provides IdentifyDigitRepository as a Singleton
     * Context is injected using @ApplicationContext
     */
    @Provides
    @Singleton
    fun provideIdentifyDigitRepository(
        @ApplicationContext context: Context
    ): IdentifyDigitRepository {
        return IdentifyDigitRepository(context)
    }
}