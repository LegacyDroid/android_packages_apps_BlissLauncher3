package com.android.launcher3;

import com.android.launcher3.model.AllAppsList;
import com.android.launcher3.model.BgDataModel;
import com.android.launcher3.model.ModelTaskController;

@FunctionalInterface
public interface ModelUpdateTask {
    void execute(ModelTaskController taskController, BgDataModel dataModel, AllAppsList apps);

    default void setIgnoreLoaded(boolean ignore) {
        // default no-op
    }

    default boolean isIgnoreLoaded() {
        return false;
    }
}
