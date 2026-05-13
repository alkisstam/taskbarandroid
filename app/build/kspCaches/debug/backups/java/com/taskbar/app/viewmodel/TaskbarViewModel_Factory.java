package com.taskbar.app.viewmodel;

import android.content.Context;
import com.taskbar.app.data.AppRepository;
import com.taskbar.app.data.PreferencesRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
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
public final class TaskbarViewModel_Factory implements Factory<TaskbarViewModel> {
  private final Provider<Context> contextProvider;

  private final Provider<AppRepository> appRepositoryProvider;

  private final Provider<PreferencesRepository> prefsRepositoryProvider;

  private TaskbarViewModel_Factory(Provider<Context> contextProvider,
      Provider<AppRepository> appRepositoryProvider,
      Provider<PreferencesRepository> prefsRepositoryProvider) {
    this.contextProvider = contextProvider;
    this.appRepositoryProvider = appRepositoryProvider;
    this.prefsRepositoryProvider = prefsRepositoryProvider;
  }

  @Override
  public TaskbarViewModel get() {
    return newInstance(contextProvider.get(), appRepositoryProvider.get(), prefsRepositoryProvider.get());
  }

  public static TaskbarViewModel_Factory create(Provider<Context> contextProvider,
      Provider<AppRepository> appRepositoryProvider,
      Provider<PreferencesRepository> prefsRepositoryProvider) {
    return new TaskbarViewModel_Factory(contextProvider, appRepositoryProvider, prefsRepositoryProvider);
  }

  public static TaskbarViewModel newInstance(Context context, AppRepository appRepository,
      PreferencesRepository prefsRepository) {
    return new TaskbarViewModel(context, appRepository, prefsRepository);
  }
}
