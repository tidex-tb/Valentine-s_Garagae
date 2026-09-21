package com.example.valentinesgarage.di

import com.example.valentinesgarage.data.auth.FirebaseAuthRepository
import com.example.valentinesgarage.data.firestore.FirestoreGarageRepository
import com.example.valentinesgarage.domain.repository.AuthRepository
import com.example.valentinesgarage.domain.repository.GarageRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton abstract fun bindAuthRepository(implementation: FirebaseAuthRepository): AuthRepository
    @Binds @Singleton abstract fun bindGarageRepository(implementation: FirestoreGarageRepository): GarageRepository
}
