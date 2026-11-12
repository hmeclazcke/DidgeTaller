package ar.didgetaller.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ar.didgetaller.R
import ar.didgetaller.TallerViewModel
import ar.didgetaller.data.LocalStore
import ar.didgetaller.data.ConstructionStore
import java.util.UUID

/** A stable draft identity resumes the same constructor instead of creating another design tool. */
@Composable
internal fun LaboratoryWizard(vm:TallerViewModel,export:(String,String,ByteArray)->Unit,tool:(Int)->Unit) {
    var id by rememberSaveable {mutableStateOf(
        if(runCatching {ConstructionStore(vm.getApplication<android.app.Application>()).project("inverse-workspace").isNotEmpty()}.getOrDefault(false))"inverse-workspace" else UUID.randomUUID().toString())}
    var open by rememberSaveable {mutableStateOf(true)}
    if(open) {
        val design=remember(id){runCatching {LocalStore(vm.getApplication()).designs(true).firstOrNull {it.id==id}}.getOrNull() ?: LocalStore.Design(id,"",vm.shape)}
        ConstructionWorkspace(design,export,vm.getApplication(),embedded=true) {open=false;vm.reloadDesigns()}
    } else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).testTag("lab-home"),verticalArrangement=Arrangement.spacedBy(12.dp)) {
    Text(stringResource(R.string.wizard_design),style=MaterialTheme.typography.headlineSmall)
    Button(onClick={open=true},modifier=Modifier.fillMaxWidth().testTag("lab-resume-wizard")){Text(stringResource(R.string.wizard_resume))}
    TextButton(onClick={id=UUID.randomUUID().toString();open=true},modifier=Modifier.testTag("lab-new-wizard")){Text(stringResource(R.string.wizard_new))}
    HorizontalDivider()
    Text(stringResource(R.string.wizard_other_tools),style=MaterialTheme.typography.titleSmall)
    Text(stringResource(R.string.wizard_spectrum_help),style=MaterialTheme.typography.bodySmall)
    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        TextButton(onClick={tool(1)}){Text(stringResource(R.string.ui_live_spectrum))}
        TextButton(onClick={tool(2)}){Text(stringResource(R.string.wizard_advanced_search))}
        TextButton(onClick={tool(3)}){Text(stringResource(R.string.ui_guide))}
    }
    }
}
