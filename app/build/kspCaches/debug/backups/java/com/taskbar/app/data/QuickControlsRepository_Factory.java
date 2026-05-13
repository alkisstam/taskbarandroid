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
public final class QuickControlsRepository_Factory implements Factory<QuickControlsRepository> {
  private final Provider<Context> contextProvider;

  private QuickControlsRepository_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public QuickControlsRepository get() {
    return newInstance(contextProvider.get());
  }

  public static QuickControlsRepository_Factory create(Provider<Context> contextProvider) {
    return new QuickControlsRepository_Factory(contextProvider);
  }

  public static QuickControlsRepository newInstance(Context context) {
    return new QuickControlsRepository(context);
  }
}
