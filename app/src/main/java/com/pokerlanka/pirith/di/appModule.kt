package com.pokerlanka.pirith.di

import androidx.room.Room
import com.pokerlanka.pirith.data.local.PirithDatabase
import com.pokerlanka.pirith.data.preferences.UserPreferencesRepository
import com.pokerlanka.pirith.data.repository.PirithRepository
import com.pokerlanka.pirith.data.repository.PirithRepositoryImpl
import com.pokerlanka.pirith.ui.home.HomeViewModel
import com.pokerlanka.pirith.ui.reader.ReaderViewModel
import com.pokerlanka.pirith.ui.settings.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Database
    single {
        Room.databaseBuilder(
            androidContext(),
            PirithDatabase::class.java,
            PirithDatabase.DATABASE_NAME
        )
            .createFromAsset("databases/pirith.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    // DAOs
    single { get<PirithDatabase>().suttaDao() }
    single { get<PirithDatabase>().verseDao() }

    // Repositories
    single { UserPreferencesRepository(androidContext()) }
    single<PirithRepository> { PirithRepositoryImpl(get(), get(), get()) }

    // ViewModels
    viewModel { HomeViewModel(get()) }
    viewModel { ReaderViewModel(get(), get()) }
    viewModel { SettingsViewModel(get()) }
}
