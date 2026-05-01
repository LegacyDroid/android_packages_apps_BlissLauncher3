/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    bliss-compat/src/main/kotlin/foundation/e/bliss/compat/ReflectionGate.kt
 * Module:  :bliss-compat  (foundation.e.bliss.compat)
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/compat/):
 *   ├── BuildCompat.kt           — SDK_INT gates with @ChecksSdkIntAtLeast
 *   ├── ReflectionGate.kt        — single point of reflection (this file)  ← THIS FILE
 *   ├── platform/                — runtime-API shims (PrivateSpaceFlags, DisplayId, …)
 *   ├── desktop/                 — DesktopMode shims
 *   └── quickstep/               — QuickStep / ATM shims (compiled in withQuickstep flavour)
 *
 * Purpose:
 *   Caches Class.forName / getMethod lookups for compat shims that need to
 *   speculatively probe runtime APIs (e.g. android.multiuser.Flags). The
 *   only file in the codebase allowed to call Class.forName / getMethod —
 *   a future Lint rule (Migration05) will enforce this.
 *
 * Consumed by:
 *   - foundation.e.bliss.compat.platform.PrivateSpaceFlagsCompat
 *   - foundation.e.bliss.compat.quickstep.ActivityTaskManagerCompat
 *   - foundation.e.bliss.compat.quickstep.QuickStepContractCompat
 *
 * Plan reference: Plans/Migration04/01-compat-platform.md §3.2
 */
package foundation.e.bliss.compat

import java.lang.reflect.Method

/**
 * The single point of reflection in the codebase. Every shim that needs to
 * speculatively resolve a method or class on the running framework calls
 * through here. Caches the result; never throws to callers.
 *
 * Do not add any other Class.forName / getMethod calls anywhere in
 * BlissLauncher3 source. Lint rule (Migration05) will enforce this.
 */
object ReflectionGate {

    /** Returns true iff `className` is loadable on this framework. */
    @JvmStatic
    fun classExists(className: String): Boolean = lookupClass(className) != null

    /** Returns the `Method` if it exists with the given parameter types,
     *  otherwise null. Result is cached per (class, name, params) tuple. */
    @JvmStatic
    fun lookupMethod(
        className: String,
        methodName: String,
        vararg parameterTypes: Class<*>
    ): Method? {
        val key = Key(className, methodName, parameterTypes.toList())
        // getOrPut won't memoise null misses, so do the put ourselves.
        if (methodCache.containsKey(key)) return methodCache[key]
        val resolved: Method? = try {
            lookupClass(className)?.getMethod(methodName, *parameterTypes)
        } catch (t: Throwable) {
            null
        }
        methodCache[key] = resolved
        return resolved
    }

    /** Convenience: invoke a static no-arg method that returns Boolean.
     *  Returns null if the class/method/return type doesn't match. */
    @JvmStatic
    fun invokeStaticBoolean(className: String, methodName: String): Boolean? {
        val m = lookupMethod(className, methodName) ?: return null
        return try {
            m.invoke(null) as? Boolean
        } catch (t: Throwable) {
            null
        }
    }

    private fun lookupClass(name: String): Class<*>? {
        if (classCache.containsKey(name)) return classCache[name]
        val resolved: Class<*>? = try {
            Class.forName(name)
        } catch (t: Throwable) {
            null
        }
        classCache[name] = resolved
        return resolved
    }

    private val classCache = mutableMapOf<String, Class<*>?>()
    private val methodCache = mutableMapOf<Key, Method?>()
    private data class Key(val cls: String, val method: String, val params: List<Class<*>>)
}
