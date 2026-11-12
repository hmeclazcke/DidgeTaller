package ar.didgetaller.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import ar.didgetaller.R
import ar.didgetaller.core.*

@Composable
internal fun ConstructionReadinessLabel(plan:ConstructionPlan,accepted:Boolean) {
    val state=ConstructionJourney.readiness(plan,accepted)
    val l=exportLanguage()
    val text=when(state) {
        ConstructionReadiness.READY->if(plan.options.method==ConstructionMethod.SANDWICH)l.wood(WoodTerm.POSITIVE_MARGIN) else stringResource(R.string.wizard_compatible)
        ConstructionReadiness.MINIMUM->l.wood(WoodTerm.ZERO_MARGIN)
        ConstructionReadiness.INCOMPATIBLE->l.wood(WoodTerm.FAIL)
        ConstructionReadiness.UNCONFIRMED->l.wood(WoodTerm.UNKNOWN)
        ConstructionReadiness.ALTERNATIVE->stringResource(R.string.wizard_alternative_required)
        ConstructionReadiness.ACCEPTED_ALTERNATIVE->stringResource(R.string.wizard_alternative_accepted)
        ConstructionReadiness.LEGACY->stringResource(R.string.wizard_legacy)
    }
    Text(text,modifier=Modifier.testTag("wizard-readiness-${state.name}"))
}
