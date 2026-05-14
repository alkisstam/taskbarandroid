package com.taskbar.app.service;

import com.taskbar.app.data.AppRepository;
import com.taskbar.app.data.PreferencesRepository;
import com.taskbar.app.data.QuickControlsRepository;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.Provider;
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
public final class OverlayService_MembersInjector implements MembersInjector<OverlayService> {
  private final Provider<AppRepository> appRepositoryProvider;

  private final Provider<PreferencesRepository> prefsRepositoryProvider;

  private final Provider<QuickControlsRepository> quickControlsRepositoryProvider;

  private OverlayService_MembersInjector(Provider<AppRepository> appRepositoryProvider,
      Provider<PreferencesRepository> prefsRepositoryProvider,
      Provider<QuickControlsRepository> quickControlsRepositoryProvider) {
    this.appRepositoryProvider = appRepositoryProvider;
    this.prefsRepositoryProvider = prefsRepositoryProvider;
    this.quickControlsRepositoryProvider = quickControlsRepositoryProvider;
  }

  public static MembersInjector<OverlayService> create(
      Provider<AppRepository> appRepositoryProvider,
      Provider<PreferencesRepository> prefsRepositoryProvider,
      Provider<QuickControlsRepository> quickControlsRepositoryProvider) {
    return new OverlayService_MembersInjector(appRepositoryProvider, prefsRepositoryProvider, quickControlsRepositoryProvider);
  }

  @Override
  public void injectMembers(OverlayService instance) {
    injectAppRepository(instance, appRepositoryProvider.get());
    injectPrefsRepository(instance, prefsRepositoryProvider.get());
    injectQuickControlsRepository(instance, quickControlsRepositoryProvider.get());
  }

  @InjectedFieldSignature("com.taskbar.app.service.OverlayService.appRepository")
  public static void injectAppRepository(OverlayService instance, AppRepository appRepository) {
    instance.appRepository = appRepository;
  }

  @InjectedFieldSignature("com.taskbar.app.service.OverlayService.prefsRepository")
  public static void injectPrefsRepository(OverlayService instance,
      PreferencesRepository prefsRepository) {
    instance.prefsRepository = prefsRepository;
  }

  @InjectedFieldSignature("com.taskbar.app.service.OverlayService.quickControlsRepository")
  public static void injectQuickControlsRepository(OverlayService instance,
      QuickControlsRepository quickControlsRepository) {
    instance.quickControlsRepository = quickControlsRepository;
  }
}
