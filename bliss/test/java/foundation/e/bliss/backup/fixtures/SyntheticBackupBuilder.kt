/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
/*
 * File:    bliss/test/java/foundation/e/bliss/backup/fixtures/SyntheticBackupBuilder.kt
 * Module:  bliss test source-set  (foundation.e.bliss.backup.fixtures)
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/backup/fixtures/):
 *   ├── SyntheticBackupBuilder.kt       — builds a minimal .lawnchairbackup ZIP  ← THIS FILE
 *   └── (resources)/fixtures/synthetic.lawnchairbackup
 *                                       — committed output of this builder
 *
 * Purpose:
 *   Produces a minimal but representative .lawnchairbackup ZIP for use by the
 *   golden importer test. The plan §3.5 prefers a synthetic fixture over an
 *   anonymised user backup — the synthetic version contains no PII and is
 *   reproducible. The output is committed under
 *   bliss/test/resources/fixtures/synthetic.lawnchairbackup so the test can
 *   load it as a classpath resource without rebuilding it.
 *
 * Consumed by:
 *   - foundation.e.bliss.backup.LawnchairImporterGoldenTest
 *   - foundation.e.bliss.backup.SharedPrefsXmlParserTest
 *   - foundation.e.bliss.backup.DataStoreProtoParserTest
 *
 * Plan reference: Plans/Migration04/08-observability-and-testing.md §3.5
 */
package foundation.e.bliss.backup.fixtures

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Builds a synthetic Lawnchair backup ZIP. Run as a JVM `main` to regenerate the committed fixture
 * (rare); the parser tests construct the byte payloads directly via [buildSharedPrefsXml] /
 * [buildDataStoreProto] without invoking `main`.
 */
object SyntheticBackupBuilder {

    /**
     * The canonical synthetic-prefs map, also used directly by the targeted parser tests so a
     * single source of truth describes the expected output. Booleans, ints, longs, floats, strings,
     * and a string-set — one of each.
     */
    val SYNTHETIC_PREFS: Map<String, Any> =
        linkedMapOf(
            "show_icon_labels_on_home_screen" to true,
            "pref_iconSizeFactor" to 1.0f,
            "pref_workspaceColumns" to 4,
            "long_value" to 123456789012L,
            "double_tap_gesture_handler" to "{\"type\":\"openAppDrawer\"}",
            "hidden-app-set" to setOf("com.example.app1", "com.example.app2"),
        )

    fun buildSharedPrefsXml(prefs: Map<String, Any>): ByteArray {
        val sb = StringBuilder()
        sb.append("<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n")
        sb.append("<map>\n")
        for ((k, v) in prefs) {
            when (v) {
                is Boolean -> sb.append("    <boolean name=\"$k\" value=\"$v\" />\n")
                is Int -> sb.append("    <int name=\"$k\" value=\"$v\" />\n")
                is Long -> sb.append("    <long name=\"$k\" value=\"$v\" />\n")
                is Float -> sb.append("    <float name=\"$k\" value=\"$v\" />\n")
                is String -> sb.append("    <string name=\"$k\">${escape(v)}</string>\n")
                is Set<*> -> {
                    sb.append("    <set name=\"$k\">\n")
                    for (e in v) sb.append("        <string>${escape(e.toString())}</string>\n")
                    sb.append("    </set>\n")
                }
                else -> error("Unsupported pref value type: ${v::class}")
            }
        }
        sb.append("</map>\n")
        return sb.toString().toByteArray(StandardCharsets.UTF_8)
    }

    private fun escape(s: String): String =
        s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")

    /**
     * Builds a Jetpack DataStore Preferences protobuf payload. The wire format (see the
     * parseDataStoreProto comment in LawnchairImportHelper):
     *
     * PreferencesProto { map<string, Value> preferences = 1; } Value { oneof { bool=1; float=2;
     * int32=3; int64=4; string=5; double=6; StringSet=7; } }
     */
    fun buildDataStoreProto(prefs: Map<String, Any>): ByteArray {
        val out = ByteArrayOutputStream()
        for ((k, v) in prefs) {
            // Each entry is field 1, wire-type 2 (length-delimited), wrapping a
            // PreferenceMapEntry message:
            //   1: string key (wt 2)
            //   2: Value value (wt 2)
            val entry = ByteArrayOutputStream()
            writeTag(entry, 1, 2)
            writeBytes(entry, k.toByteArray(StandardCharsets.UTF_8))
            val value = encodeValue(v)
            writeTag(entry, 2, 2)
            writeBytes(entry, value)

            writeTag(out, 1, 2)
            writeBytes(out, entry.toByteArray())
        }
        return out.toByteArray()
    }

    private fun encodeValue(v: Any): ByteArray {
        val out = ByteArrayOutputStream()
        when (v) {
            is Boolean -> {
                writeTag(out, 1, 0)
                writeVarint(out, if (v) 1 else 0)
            }
            is Float -> {
                writeTag(out, 2, 5)
                writeFixed32(out, java.lang.Float.floatToIntBits(v))
            }
            is Int -> {
                writeTag(out, 3, 0)
                writeVarint(out, v.toLong())
            }
            is Long -> {
                writeTag(out, 4, 0)
                writeVarint(out, v)
            }
            is String -> {
                writeTag(out, 5, 2)
                writeBytes(out, v.toByteArray(StandardCharsets.UTF_8))
            }
            is Double -> {
                writeTag(out, 6, 1)
                writeFixed64(out, java.lang.Double.doubleToLongBits(v))
            }
            is Set<*> -> {
                // StringSet { repeated string strings = 1; }
                val set = ByteArrayOutputStream()
                for (s in v) {
                    writeTag(set, 1, 2)
                    writeBytes(set, s.toString().toByteArray(StandardCharsets.UTF_8))
                }
                writeTag(out, 7, 2)
                writeBytes(out, set.toByteArray())
            }
            else -> error("Unsupported value type: ${v::class}")
        }
        return out.toByteArray()
    }

    private fun writeTag(out: ByteArrayOutputStream, fieldNumber: Int, wireType: Int) {
        writeVarint(out, ((fieldNumber.toLong() shl 3) or wireType.toLong()))
    }

    private fun writeVarint(out: ByteArrayOutputStream, valueIn: Long) {
        var value = valueIn
        while (true) {
            if (value and 0x7FL.inv() == 0L) {
                out.write(value.toInt())
                return
            }
            out.write(((value and 0x7F) or 0x80).toInt())
            value = value ushr 7
        }
    }

    private fun writeFixed32(out: ByteArrayOutputStream, value: Int) {
        out.write(value and 0xFF)
        out.write((value ushr 8) and 0xFF)
        out.write((value ushr 16) and 0xFF)
        out.write((value ushr 24) and 0xFF)
    }

    private fun writeFixed64(out: ByteArrayOutputStream, value: Long) {
        for (i in 0..7) out.write(((value ushr (i * 8)) and 0xFFL).toInt())
    }

    private fun writeBytes(out: ByteArrayOutputStream, bytes: ByteArray) {
        writeVarint(out, bytes.size.toLong())
        out.write(bytes)
    }

    /**
     * Build the full .lawnchairbackup ZIP. The launcher.db entry, if [dbBytes] is null, is omitted
     * — the importer treats a missing DB as "no layout import", which is fine for parser-level
     * tests. The golden test (when un-ignored in phase 02) will pass real SQLite bytes here.
     */
    fun buildBackupZip(
        prefs: Map<String, Any> = SYNTHETIC_PREFS,
        dbBytes: ByteArray? = null,
    ): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            putEntry(zip, "com.android.launcher3.prefs.xml", buildSharedPrefsXml(prefs))
            putEntry(zip, "preferences.preferences_pb", buildDataStoreProto(prefs))
            if (dbBytes != null) putEntry(zip, "launcher.db", dbBytes)
        }
        return out.toByteArray()
    }

    private fun putEntry(zip: ZipOutputStream, name: String, bytes: ByteArray) {
        val entry = ZipEntry(name)
        zip.putNextEntry(entry)
        zip.write(bytes)
        zip.closeEntry()
    }

    /**
     * Regenerate the committed fixture. Invoke via: ./gradlew
     * :compileBlissWithQuickstepDebugUnitTestKotlin java -cp <classpath>
     * foundation.e.bliss.backup.fixtures.SyntheticBackupBuilderKt
     */
    @JvmStatic
    fun main(args: Array<String>) {
        val outDir = File(args.getOrNull(0) ?: "bliss/test/resources/fixtures").apply { mkdirs() }
        val outFile = File(outDir, "synthetic.lawnchairbackup")
        outFile.writeBytes(buildBackupZip())
        println("Wrote ${outFile.length()} bytes to $outFile")
    }

    /**
     * Entry point for the audit01/09 workspace-import golden test. Returns a fluent builder that
     * accumulates synthetic Lawnchair `favorites` rows and finishes with
     * [Builder.buildLauncherDbBytes], which materialises an actual SQLite blob suitable for handing
     * to [foundation.e.bliss.backup.workspace.WorkspaceImporter.run].
     *
     * NOTE: this path requires Android SQLite (Robolectric or device); calling it from a plain JVM
     * `main` will fail at runtime. Used only by JVM unit tests run via Robolectric.
     */
    @JvmStatic fun builder(): Builder = Builder()

    /**
     * Mutable accumulator for synthetic Lawnchair workspace items. Mirrors the column set produced
     * by Lawnchair (see WorkspaceImporter / FavoritesCursor.PROJECTION) so the importer reads valid
     * rows. Defaults match the simplest workspace-icon case (ITEM_TYPE_APPLICATION, span=1×1, no
     * widget, no icon BLOB).
     */
    class Builder {
        private val rows = mutableListOf<ContentValues>()
        private var nextId = 1L

        @JvmOverloads
        fun withWorkspaceItem(
            screen: Int,
            cellX: Int,
            cellY: Int,
            packageName: String,
            cls: String = "$packageName.MainActivity",
            container: Int = -100,
            spanX: Int = 1,
            spanY: Int = 1,
            itemType: Int = 0,
            rank: Int = 0,
            title: String = packageName.substringAfterLast('.'),
        ): Builder {
            val cv = ContentValues()
            cv.put("_id", nextId++)
            cv.put("title", title)
            cv.put(
                "intent",
                "#Intent;action=android.intent.action.MAIN;category=android.intent.category.LAUNCHER" +
                    ";launchFlags=0x10200000;component=$packageName/$cls;end",
            )
            cv.put("container", container)
            cv.put("screen", screen)
            cv.put("cellX", cellX)
            cv.put("cellY", cellY)
            cv.put("spanX", spanX)
            cv.put("spanY", spanY)
            cv.put("itemType", itemType)
            cv.putNull("appWidgetProvider")
            cv.put("rank", rank)
            cv.putNull("icon")
            rows.add(cv)
            return this
        }

        /**
         * Materialise the accumulated rows into a SQLite database file matching the Lawnchair
         * `launcher.db` schema (only the columns FavoritesCursor.PROJECTION reads), and return its
         * raw bytes.
         */
        fun buildLauncherDbBytes(): ByteArray {
            val tmp = Files.createTempFile("synthetic_launcher_", ".db").toFile()
            tmp.delete()
            val db = SQLiteDatabase.openOrCreateDatabase(tmp, null)
            try {
                db.execSQL(
                    "CREATE TABLE favorites (" +
                        "_id INTEGER PRIMARY KEY, " +
                        "title TEXT, " +
                        "intent TEXT, " +
                        "container INTEGER, " +
                        "screen INTEGER, " +
                        "cellX INTEGER, " +
                        "cellY INTEGER, " +
                        "spanX INTEGER, " +
                        "spanY INTEGER, " +
                        "itemType INTEGER, " +
                        "appWidgetProvider TEXT, " +
                        "rank INTEGER, " +
                        "icon BLOB)"
                )
                db.beginTransaction()
                try {
                    for (cv in rows) {
                        db.insertOrThrow("favorites", null, cv)
                    }
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
            } finally {
                db.close()
            }
            val bytes = tmp.readBytes()
            tmp.delete()
            return bytes
        }
    }
}
