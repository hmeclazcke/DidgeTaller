package ar.didgetaller

import org.junit.Assert.fail
import org.junit.AssumptionViolatedException
import org.junit.Test

class PrivateFixturePolicyTest {
    @Test fun absentOrNonExplicitPermissionSkipsBeforeAnyFileAccess() {
        for(value in listOf(null,"false","TRUE","1","")) {
            try {requirePrivateFixtures(value);fail("Protected fixtures must require explicit permission")}
            catch(expected:AssumptionViolatedException) { /* No recording is inspected. */ }
        }
    }

    @Test fun explicitPermissionAllowsTheSeparatelyAuthorizedFixtureCheck() {
        requirePrivateFixtures("true")
    }
}
