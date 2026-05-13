package com.taskbar.app.data;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
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
public final class PreferencesRepository_Factory implements Factory<PreferencesRepository> {
  private final Provider<Context> contextProvider;

  private PreferencesRepository_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public PreferencesRepository get() {
    return newInstance(contextProvider.get());
  }

  public static PreferencesRepository_Factory create(Provider<Context> contextProvider) {
    return new PreferencesRepository_Factory(contextProvider);
  }

  public static PreferencesRepository newInstance(Context context) {
    return new PreferencesRepository(context);
  }
}
