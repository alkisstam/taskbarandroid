package com.taskbar.app.service;

import com.taskbar.app.data.PreferencesRepository;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;

@QualifierMetadata
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
public final class BootReceiver_MembersInjector implements MembersInjector<BootReceiver> {
  private final Provider<PreferencesRepository> prefsRepositoryProvider;

  public BootReceiver_MembersInjector(Provider<PreferencesRepository> prefsRepositoryProvider) {
    this.prefsRepositoryProvider = prefsRepositoryProvider;
  }

  public static MembersInjector<BootReceiver> create(
      Provider<PreferencesRepository> prefsRepositoryProvider) {
    return new BootReceiver_MembersInjector(prefsRepositoryProvider);
  }

  public static MembersInjector<BootReceiver> create(
      javax.inject.Provider<PreferencesRepository> prefsRepositoryProvider) {
    return new BootReceiver_MembersInjector(Providers.asDaggerProvider(prefsRepositoryProvider));
  }

  @Override
  public void injectMembers(BootReceiver instance) {
    injectPrefsRepository(instance, prefsRepositoryProvider.get());
  }

  @InjectedFieldSignature("com.taskbar.app.service.BootReceiver.prefsRepository")
  public static void injectPrefsRepository(BootReceiver instance,
      PreferencesRepository prefsRepository) {
    instance.prefsRepository = prefsRepository;
  }
}
