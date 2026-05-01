/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/ui/DrawerFragment.java
 * Module:  main source-set  (com.android.launcher3.settings.ui)
 * Role:    NEW
 *
 * Tree (settings/ui/):
 *   See AreaFragmentBase.java for the directory's full sibling listing.
 *
 * Purpose:
 *   App drawer settings. Inflates preferences_drawer.xml and lets {@link
 *   com.android.launcher3.settings.controllers.PreferenceControllerRegistry}
 *   wire up every per-key controller.
 *
 * Plan reference: Plans/Migration04/04-settings-ui-modularization.md §2, §5
 */
package com.android.launcher3.settings.ui;

import com.android.launcher3.R;

/** App drawer settings. */
public class DrawerFragment extends AreaFragmentBase {
    @Override protected int xmlRes() { return R.xml.preferences_drawer; }
}
