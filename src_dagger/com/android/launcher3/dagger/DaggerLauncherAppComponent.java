package com.android.launcher3.dagger;

import android.content.Context;
import com.android.launcher3.util.DaggerSingletonTracker;
import com.android.launcher3.util.DaggerSingletonTracker_Factory;
import com.android.launcher3.dagger.LauncherAppComponent;
import com.android.quickstep.logging.SettingsChangeLogger;

import dagger.internal.DaggerGenerated;
import dagger.internal.DoubleCheck;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import javax.annotation.processing.Generated;

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
    "cast"
})
public final class DaggerLauncherAppComponent {
  private DaggerLauncherAppComponent() {
  }

  public static LauncherAppComponent.Builder builder() {
    return new Builder();
  }

  private static final class Builder implements LauncherAppComponent.Builder {
    private Context appContext;

    @Override
    public Builder appContext(Context context) {
      this.appContext = Preconditions.checkNotNull(context);
      return this;
    }

    @Override
    public LauncherAppComponent build() {
      Preconditions.checkBuilderRequirement(appContext, Context.class);
      return new LauncherAppComponentImpl(appContext);
    }
  }

  private static final class LauncherAppComponentImpl implements LauncherAppComponent {
    private final LauncherAppComponentImpl launcherAppComponentImpl = this;

    private Provider<DaggerSingletonTracker> daggerSingletonTrackerProvider;

    private Context context;

    private LauncherAppComponentImpl(Context appContextParam) {
      initialize(appContextParam);
      context = appContextParam;
    }

    @SuppressWarnings("unchecked")
    private void initialize(final Context appContextParam) {
      this.daggerSingletonTrackerProvider = DoubleCheck.provider(DaggerSingletonTracker_Factory.create());
    }

    @Override
    public DaggerSingletonTracker getDaggerSingletonTracker() {
      return daggerSingletonTrackerProvider.get();
    }

    @Override
    public SettingsChangeLogger getSettingsChangeLogger() {
      return SettingsChangeLogger.INSTANCE.get(context);
    }
  }
}
