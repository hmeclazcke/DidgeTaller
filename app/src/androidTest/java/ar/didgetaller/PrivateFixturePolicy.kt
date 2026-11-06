package ar.didgetaller

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume.assumeTrue

/** File presence never grants permission to inspect protected recordings. */
internal fun requirePrivateFixtures(allow:String?=InstrumentationRegistry.getArguments().getString("allowPrivateFixtures")) {
    assumeTrue("Private fixtures require explicit allowPrivateFixtures=true and user authorization",allow=="true")
}
