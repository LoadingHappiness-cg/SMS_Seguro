package com.smsguard.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteEnrichmentConfigTest {

    @Test
    fun activeRequiresAllowedBuildExplicitEnablementAndBaseUrl() {
        assertFalse(
            RemoteEnrichmentConfig(
                enabled = false,
                allowedInBuild = true,
                baseUrl = "https://backend.example",
            ).isActive,
        )

        assertFalse(
            RemoteEnrichmentConfig(
                enabled = true,
                allowedInBuild = false,
                baseUrl = "https://backend.example",
            ).isActive,
        )

        assertFalse(
            RemoteEnrichmentConfig(
                enabled = true,
                allowedInBuild = true,
                baseUrl = "",
            ).isActive,
        )

        assertTrue(
            RemoteEnrichmentConfig(
                enabled = true,
                allowedInBuild = true,
                baseUrl = "https://backend.example",
            ).isActive,
        )
    }

    @Test
    fun traceRequiresAllowedBuildAndExplicitEnablement() {
        assertFalse(
            RemoteEnrichmentConfig(
                enabled = false,
                allowedInBuild = true,
                baseUrl = "https://backend.example",
                traceEnabled = true,
            ).shouldTrace,
        )

        assertFalse(
            RemoteEnrichmentConfig(
                enabled = true,
                allowedInBuild = false,
                baseUrl = "https://backend.example",
                traceEnabled = true,
            ).shouldTrace,
        )

        assertTrue(
            RemoteEnrichmentConfig(
                enabled = true,
                allowedInBuild = true,
                baseUrl = "https://backend.example",
                traceEnabled = true,
            ).shouldTrace,
        )
    }
}
