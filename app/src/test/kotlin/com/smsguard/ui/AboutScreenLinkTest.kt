package com.smsguard.ui

import com.smsguard.R
import org.junit.Assert.assertEquals
import org.junit.Test

class AboutScreenLinkTest {

    @Test
    fun aboutLinkItems_returnsOnlyAllowedAboutLinks() {
        assertEquals(
            listOf(
                AboutLinkItem(
                    titleRes = R.string.about_link_privacy_title,
                    subtitleRes = R.string.about_link_privacy_subtitle,
                    urlRes = R.string.about_url_privacy,
                ),
                AboutLinkItem(
                    titleRes = R.string.about_link_github_title,
                    subtitleRes = R.string.about_link_github_subtitle,
                    urlRes = R.string.about_url_source,
                ),
            ),
            aboutLinkItems(),
        )
    }
}
