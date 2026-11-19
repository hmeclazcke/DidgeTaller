package ar.didgetaller.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ar.didgetaller.R
import ar.didgetaller.core.ConstructionCatalog
import ar.didgetaller.core.ConstructionForm
import ar.didgetaller.core.ConstructionMaterial.Problem
import ar.didgetaller.core.PvcCutDraft
import ar.didgetaller.core.SinglePipeDraft
import ar.didgetaller.core.WorkshopPlans
import ar.didgetaller.core.dimension

@Composable
internal fun PvcFields(values: Map<String, String>, set: (String, String) -> Unit, feedback: MaterialFeedback,
    writable: Boolean, saveCatalog: () -> Unit, loadCatalog: () -> Unit) {
    var advanced by rememberSaveable { mutableStateOf(false) }
    val pipe = SinglePipeDraft.read(values.getValue("pipes"))
    val cuts = PvcCutDraft.read(values.getValue("assembly"))
    val rawIssue = feedback.attention?.field in listOf("pipes", "fittings", "clearance", "options")
    LaunchedEffect(feedback.attention) { if (rawIssue) advanced = true }
    Text(stringResource(R.string.pvc_title), style = MaterialTheme.typography.titleMedium)
    if (advanced) PvcCatalogFields(values, set, feedback, writable, saveCatalog, loadCatalog)
    else GuidedPvcFields(values, set, feedback, pipe, cuts)
    TextButton(onClick = { advanced = !advanced }, modifier = Modifier.testTag("pvc-catalog-editor")) {
        Text(stringResource(if (advanced) R.string.pvc_back_to_measurements else R.string.pvc_catalog_advanced))
    }
}

@Composable
private fun GuidedPvcFields(values: Map<String, String>, set: (String, String) -> Unit, feedback: MaterialFeedback,
    pipe: SinglePipeDraft?, cuts: List<PvcCutDraft>?) {
    if (pipe != null && cuts?.size == 1 && values.getValue("fittings").isBlank()) {
        SinglePipeFields(pipe, values, set, feedback)
        return
    }
    val pipes = remember(values["pipes"]) {
        runCatching { ConstructionCatalog.pipes(values.getValue("pipes")) }.getOrDefault(emptyMap())
    }
    val fittings = remember(values["fittings"]) {
        runCatching { ConstructionCatalog.connectors(values.getValue("fittings")) }.getOrDefault(emptyMap())
    }
    if (pipes.isEmpty() || cuts == null) {
        Text(stringResource(R.string.pvc_catalog_review), style = MaterialTheme.typography.bodySmall)
        return
    }
    Text(stringResource(R.string.pvc_choose_stock), style = MaterialTheme.typography.bodySmall)
    PvcAssembly(cuts, pipes, fittings.keys.toList(), feedback) { set("assembly", PvcCutDraft.records(it)) }
}

@Composable
private fun SinglePipeFields(pipe: SinglePipeDraft, values: Map<String, String>, set: (String, String) -> Unit, feedback: MaterialFeedback) {
    val fields = pipe.fields()
    val edit: (String, String) -> Unit = { key, value ->
        val measurement = value.replace(',', '.').replace('\n', ' ').take(40)
        set("pipes", SinglePipeDraft.fromFields(fields + (key to measurement)).record())
    }
    Text(stringResource(R.string.pvc_measure_help), style = MaterialTheme.typography.bodySmall)
    MaterialField("pipeInside", stringResource(R.string.pvc_inside), fields, edit, feedback,
        errorText = stringResource(R.string.wizard_dimension_range, 15, 220))
    MaterialField("pipeOutside", stringResource(R.string.pvc_outside), fields, edit, feedback,
        errorText = stringResource(R.string.pvc_outer_error))
    MaterialField("pipeLength", stringResource(R.string.pvc_stock_length), fields, edit, feedback,
        errorText = stringResource(R.string.wizard_dimension_range, 500, 10000))
    SinglePipeCut(pipe, values, set, feedback)
}

@Composable
private fun SinglePipeCut(pipe: SinglePipeDraft, values: Map<String, String>, set: (String, String) -> Unit, feedback: MaterialFeedback) {
    val design = remember(values["geometry"]) { runCatching { ConstructionForm.shape(values) }.getOrNull() } ?: return
    val profile = design.controlPoints()
    if (profile.any { it.diameterMm != profile.first().diameterMm }) {
        Text(stringResource(R.string.pvc_straight_limit), style = MaterialTheme.typography.bodySmall)
    }
    val cut = pipe.cutFor(design.lengthMm) ?: return
    val selected = values["assembly"] == cut
    val previous = PvcCutDraft.read(values.getValue("assembly"))?.singleOrNull()
    Column(feedback.reveal("assembly")) {
        if (previous?.length?.isNotBlank() == true) Text(stringResource(R.string.pvc_selected_cut, previous.length))
        if (!selected) TextButton(onClick = { set("assembly", cut) }, modifier = Modifier.testTag("pvc-use-pipe")) {
            Text(stringResource(R.string.pvc_use_pipe, cut.substringAfter(',').substringBefore(',')))
        }
        if (feedback.problems["assembly"] == Problem.INVALID) {
            Text(stringResource(R.string.wizard_invalid_assembly), color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun PvcAssembly(cuts: List<PvcCutDraft>, pipes: Map<String, WorkshopPlans.Pipe>, fittings: List<String>,
    feedback: MaterialFeedback, change: (List<PvcCutDraft>) -> Unit) {
    Column(Modifier.fillMaxWidth().then(feedback.reveal("assembly")), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.wizard_assembly_label), style = MaterialTheme.typography.titleSmall)
        cuts.forEachIndexed { index, cut ->
            PvcSegment(index, cut, pipes, fittings.takeUnless { index == cuts.lastIndex }) { updated ->
                change(cuts.mapIndexed { i, row -> if (i == index) updated else row })
            }
        }
        if (feedback.problems["assembly"] == Problem.INVALID) {
            Text(stringResource(R.string.wizard_invalid_assembly), color = MaterialTheme.colorScheme.error)
        }
        FlowRow {
            TextButton(onClick = { change(cuts + PvcCutDraft()) }, enabled = fittings.isNotEmpty() && cuts.size < 100,
                modifier = Modifier.testTag("pvc-add-segment")) { Text(stringResource(R.string.pvc_add_segment)) }
            if (cuts.size > 1) TextButton(onClick = { change(cuts.dropLast(1)) }) { Text(stringResource(R.string.pvc_remove_segment)) }
        }
        if (fittings.isEmpty()) Text(stringResource(R.string.pvc_fitting_needed), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun PvcSegment(index: Int, cut: PvcCutDraft, pipes: Map<String, WorkshopPlans.Pipe>, fittings: List<String>?, change: (PvcCutDraft) -> Unit) {
    val labels = pipes.mapValues { (_, pipe) -> stringResource(R.string.pvc_catalog_pipe, pipe.id,
        exportLanguage().dimension(pipe.insideMm), pipe.stockLengthMm) }
    Text(stringResource(R.string.pvc_segment, index + 1), style = MaterialTheme.typography.labelLarge)
    ComponentPicker("pvc-pipe-$index", cut.pipe, labels, stringResource(R.string.pvc_choose_pipe)) { change(cut.copy(pipe = it)) }
    MaterialField("cut-$index", stringResource(R.string.pvc_cut_length), mapOf("cut-$index" to cut.length),
        { _, length -> change(cut.copy(length = length.replace(',', '.').replace('\n', ' ').take(16))) }, MaterialFeedback(emptyMap(), null))
    if (fittings != null) {
        Text(stringResource(R.string.pvc_next_fitting), style = MaterialTheme.typography.labelLarge)
        ComponentPicker("pvc-fitting-$index", cut.nextFitting, fittings.associateWith { it },
            stringResource(R.string.pvc_choose_fitting)) { change(cut.copy(nextFitting = it)) }
    }
}

@Composable
private fun ComponentPicker(tag: String, selected: String, choices: Map<String, String>, placeholder: String, select: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.testTag(tag)) { Text(choices[selected] ?: placeholder) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            choices.forEach { (id, label) ->
                DropdownMenuItem(text = { Text(label) }, onClick = { select(id); open = false }, modifier = Modifier.testTag("$tag-$id"))
            }
        }
    }
}

@Composable
private fun PvcCatalogFields(values: Map<String, String>, set: (String, String) -> Unit, feedback: MaterialFeedback,
    writable: Boolean, saveCatalog: () -> Unit, loadCatalog: () -> Unit) {
    var format by rememberSaveable { mutableStateOf(false) }
    FlowRow {
        TextButton(onClick = loadCatalog) { Text(stringResource(R.string.construction_load_catalog)) }
        TextButton(onClick = saveCatalog, enabled = writable, modifier = Modifier.testTag("construction-save-catalog")) { Text(stringResource(R.string.construction_save_catalog)) }
        TextButton(onClick = { format = !format }) { Text(stringResource(R.string.wizard_pvc_format)) }
    }
    if (format) Text(stringResource(R.string.construction_schema), style = MaterialTheme.typography.bodySmall)
    MaterialField("pipes", stringResource(R.string.wizard_pipes_label), values, set, feedback, multiline = true,
        helper = stringResource(R.string.wizard_pipes_help), errorText = stringResource(R.string.wizard_invalid_pipes))
    MaterialField("fittings", stringResource(R.string.wizard_fittings_label), values, set, feedback, multiline = true,
        helper = stringResource(R.string.wizard_fittings_help), errorText = stringResource(R.string.wizard_invalid_fittings))
    MaterialField("assembly", stringResource(R.string.wizard_assembly_label), values, set, feedback, multiline = true,
        helper = stringResource(R.string.wizard_assembly_help), errorText = stringResource(R.string.wizard_invalid_assembly))
    MaterialField("clearance", stringResource(R.string.construction_clearance), values, set, feedback,
        errorText = stringResource(R.string.wizard_nonnegative_dimension))
}
