package com.example.blueprintai.di

import com.example.blueprintai.data.ChatRepository
import com.example.blueprintai.data.FolderRepository
import com.example.blueprintai.domain.repository.IChatRepository
import com.example.blueprintai.domain.repository.IFolderRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepository): IChatRepository

    @Binds
    @Singleton
    abstract fun bindFolderRepository(impl: FolderRepository): IFolderRepository
}
