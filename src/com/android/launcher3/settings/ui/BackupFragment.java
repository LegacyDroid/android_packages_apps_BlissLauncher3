/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    src/com/android/launcher3/settings/ui/BackupFragment.java
 * Module:  main source-set  (com.android.launcher3.settings.ui)
 * Role:    NEW
 *
 * Tree (settings/ui/):
 *   See AreaFragmentBase.java for the directory's full sibling listing.
 *
 * Purpose:
 *   Backup / restore / Lawnchair-import screen. Unlike the other area
 *   fragments, this one also registers three ActivityResultLaunchers
 *   (CreateDocument + 2 OpenDocument) and parks them in BackupLaunchers
 *   for the corresponding click controllers to fire on user tap. The
 *   actual "what happens after the user picks a file" callbacks live here
 *   because they need the fragment Context (Toasts, model.forceReload()).
 *
 * Plan reference: Plans/Migration04/04-settings-ui-modularization.md §7.2
 */
package com.android.launcher3.settings.ui;

import android.content.Context;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import android.os.Bundle;
import android.widget.Toast;

import com.android.launcher3.LauncherAppState;
import com.android.launcher3.R;
import com.android.launcher3.settings.controllers.backup.BackupLaunchers;
import com.android.launcher3.util.Executors;

import foundation.e.bliss.backup.BackupRestoreHelper;
import foundation.e.bliss.backup.ImportResult;
import foundation.e.bliss.backup.LawnchairImportHelper;

import java.io.InputStream;
import java.io.OutputStream;

/** Backup / restore / Lawnchair-import screen. */
public class BackupFragment extends AreaFragmentBase {

    private ActivityResultLauncher<String> mBackupLauncher;
    private ActivityResultLauncher<String[]> mRestoreLauncher;
    private ActivityResultLauncher<String[]> mLawnchairImportLauncher;

    @Override protected int xmlRes() { return R.xml.preferences_backup; }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        mBackupLauncher = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("application/zip"),
                uri -> {
                    if (uri == null) return;
                    Context ctx = getContext();
                    if (ctx == null) return;
                    try {
                        OutputStream os = ctx.getContentResolver().openOutputStream(uri);
                        if (os == null) return;
                        BackupRestoreHelper.createBackup(ctx, os, success -> {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> Toast.makeText(ctx,
                                        success ? R.string.backup_success : R.string.backup_failed,
                                        Toast.LENGTH_SHORT).show());
                            }
                        });
                    } catch (Exception e) {
                        Toast.makeText(ctx, R.string.backup_failed, Toast.LENGTH_SHORT).show();
                    }
                });

        mRestoreLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri == null) return;
                    Context ctx = getContext();
                    if (ctx == null) return;
                    try {
                        InputStream is = ctx.getContentResolver().openInputStream(uri);
                        if (is == null) return;
                        boolean success = BackupRestoreHelper.restoreFromBackup(ctx, is);
                        Toast.makeText(ctx,
                                success ? R.string.restore_success : R.string.restore_failed,
                                Toast.LENGTH_SHORT).show();
                        if (success) {
                            LauncherAppState.getInstance(ctx).getModel().forceReload();
                        }
                    } catch (Exception e) {
                        Toast.makeText(ctx, R.string.restore_failed, Toast.LENGTH_SHORT).show();
                    }
                });

        mLawnchairImportLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    Context ctx = getContext();
                    if (ctx == null) return;
                    if (uri == null) {
                        Toast.makeText(ctx, "No file selected", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    final InputStream is;
                    try {
                        is = ctx.getContentResolver().openInputStream(uri);
                    } catch (Exception e) {
                        Toast.makeText(ctx, "Could not open file", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (is == null) {
                        Toast.makeText(ctx, "Could not open file", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    final Context appCtx = ctx.getApplicationContext();
                    Toast.makeText(appCtx, "Importing Lawnchair backup\u2026",
                            Toast.LENGTH_SHORT).show();
                    Executors.MODEL_EXECUTOR.execute(() -> {
                        ImportResult result;
                        try (InputStream stream = is) {
                            result = LawnchairImportHelper.importFromLawnchair(appCtx, stream);
                        } catch (Exception e) {
                            final String msg = "Import error: " + e.getMessage();
                            Executors.MAIN_EXECUTOR.execute(() ->
                                    Toast.makeText(appCtx, msg, Toast.LENGTH_LONG).show());
                            return;
                        }
                        final String toastMsg = result.success
                                ? result.message
                                : getString(R.string.lawnchair_import_failed);
                        final boolean reload = result.success;
                        Executors.MAIN_EXECUTOR.execute(() -> {
                            Toast.makeText(appCtx, toastMsg, Toast.LENGTH_LONG).show();
                            if (reload) {
                                LauncherAppState.getInstance(appCtx).getModel().forceReload();
                            }
                        });
                    });
                });

        BackupLaunchers.setBackupLauncher(mBackupLauncher);
        BackupLaunchers.setRestoreLauncher(mRestoreLauncher);
        BackupLaunchers.setLawnchairImportLauncher(mLawnchairImportLauncher);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        BackupLaunchers.clear();
        super.onDestroy();
    }
}
