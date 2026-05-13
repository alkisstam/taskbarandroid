package com.taskbar.app.viewmodel;

import android.content.Context;
import com.taskbar.app.data.AppRepository;
import com.taskbar.app.data.PreferencesRepository;
import com.taskbar.app.data.QuickControlsRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class AppMenuViewModel_Factory implements Factory<AppMenuViewModel> {
  private final Provider<Context> contextProvider;

  private final Provider<AppRepository> appRepositoryProvider;

  private final Provider<PreferencesRepository> prefsRepositoryProvider;

  private final Provider<QuickControlsRepository> quickControlsProvider;

  public AppMenuViewModel_Factory(Provider<Context> contextProvider,
      Provider<AppRepository> appRepositoryProvider,
      Provider<PreferencesRepository> prefsRepositoryProvider,
      Provider<QuickControlsRepository> quickControlsProvider) {
    this.contextProvider = contextProvider;
    this.appRepositoryProvider = appRepositoryProvider;
    this.prefsRepositoryProvider = prefsRepositoryProvider;
    this.quickControlsProvider = quickControlsProvider;
  }

  @Override
  public AppMenuViewModel get() {
    return newInstance(contextProvider.get(), appRepositoryProvider.get(), prefsRepositoryProvider.get(), quickControlsProvider.get());
  }

  public static AppMenuViewModel_Factory create(javax.inject.Provider<Context> contextProvider,
      javax.inject.Provider<AppRepository> appRepositoryProvider,
      javax.inject.Provider<PreferencesRepository> prefsRepositoryProvider,
      javax.inject.Provider<QuickControlsRepository> quickControlsProvider) {
    return new AppMenuViewModel_Factory(Providers.asDaggerProvider(contextProvider), Providers.asDaggerProvider(appRepositoryProvider), Providers.asDaggerProvider(prefsRepositoryProvider), Providers.asDaggerProvider(quickControlsProvider));
  }

  public static AppMenuViewModel_Factory create(Provider<Context> contextProvider,
      Provider<AppRepository> appRepositoryProvider,
      Provider<PreferencesRepository> prefsRepositoryProvider,
      Provider<QuickControlsRepository> quickControlsProvider) {
    return new AppMenuViewModel_Factory(contextProvider, appRepositoryProvider, prefsRepositoryProvider, quickControlsProvider);
  }

  public static AppMenuViewModel newInstance(Context context, AppRepository appRepository,
      PreferencesRepository prefsRepository, QuickControlsRepository quickControls) {
    return new AppMenuViewModel(context, appRepository, prefsRepository, quickControls);
  }
}
