package com.brainrotrpg

import android.app.AppOpsManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UsagePermissionHelperTest {

    @Test
    fun `isModeAllowed returns false when mode is not MODE_ALLOWED`() {
        // AppOpsManager.MODE_DEFAULT (2), MODE_IGNORED (1), MODE_ERRORED (3) all indicate
        // the permission is not granted.
        assertFalse(UsagePermissionHelper.isModeAllowed(AppOpsManager.MODE_DEFAULT))
        assertFalse(UsagePermissionHelper.isModeAllowed(AppOpsManager.MODE_IGNORED))
        assertFalse(UsagePermissionHelper.isModeAllowed(AppOpsManager.MODE_ERRORED))
    }

    @Test
    fun `isModeAllowed returns true when mode is MODE_ALLOWED`() {
        assertTrue(UsagePermissionHelper.isModeAllowed(AppOpsManager.MODE_ALLOWED))
    }
}
