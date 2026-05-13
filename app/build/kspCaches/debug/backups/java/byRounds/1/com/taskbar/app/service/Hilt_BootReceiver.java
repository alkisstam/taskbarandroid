package com.taskbar.app.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.CallSuper;
import dagger.hilt.android.internal.OnReceiveBytecodeInjectionMarker;
import dagger.hilt.android.internal.managers.BroadcastReceiverComponentManager;
import dagger.hilt.internal.UnsafeCasts;
import java.lang.Object;
import java.lang.Override;
import javax.annotation.processing.Generated;

/**
 * A generated base class to be extended by the @dagger.hilt.android.AndroidEntryPoint annotated class. If using the Gradle plugin, this is swapped as the base class via bytecode transformation.
 */
@OnReceiveBytecodeInjectionMarker
@Generated("dagger.hilt.android.processor.internal.androidentrypoint.BroadcastReceiverGenerator")
public abstract class Hilt_BootReceiver extends BroadcastReceiver {
  private volatile boolean injected = false;

  private final Object injectedLock = new Object();

  @Override
  @CallSuper
  public void onReceive(Context context, Intent intent) {
    inject(context);
  }

  protected void inject(Context context) {
    if (!injected) {
      synchronized (injectedLock) {
        if (!injected) {
          ((BootReceiver_GeneratedInjector) BroadcastReceiverComponentManager.generatedComponent(context)).injectBootReceiver(UnsafeCasts.<BootReceiver>unsafeCast(this));
          injected = true;
        }
      }
    }
  }
}
