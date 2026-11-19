package ar.didgetaller.ui

import android.icu.text.ListFormatter
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import ar.didgetaller.R
import ar.didgetaller.core.ConstructionForm
import ar.didgetaller.core.ConstructionMaterial
import ar.didgetaller.core.ConstructionMaterial.Problem
import ar.didgetaller.core.WoodTerm
import ar.didgetaller.core.wood

internal data class MaterialAttention(val field: String, val attempt: Int)

internal data class MaterialFeedback(
    val problems: Map<String, Problem>,
    val attention: MaterialAttention?,
) {
    val missing get() = problems.filterValues { it == Problem.MISSING }.keys
    val nextField get() = (missing.ifEmpty { problems.keys }).firstOrNull()
}

internal val woodDimensionsLabels = mapOf(
    "stockLength" to WoodTerm.LENGTH, "stockDiameter" to WoodTerm.DIAMETER,
    "stockWidth" to WoodTerm.WIDTH, "stockThickness" to WoodTerm.BOARD_T,
    "stockBlockThickness" to WoodTerm.BLOCK_T,
)

private val materialLabels = mapOf(
    "method" to R.string.wizard_material, "stockKind" to R.string.wizard_wood_type,
    "stockConfirmed" to R.string.wizard_confirm_measurements,
    "pipes" to R.string.wizard_pipes_label, "fittings" to R.string.wizard_fittings_label,
    "pipeInside" to R.string.pvc_inside, "pipeOutside" to R.string.pvc_outside, "pipeLength" to R.string.pvc_stock_length,
    "assembly" to R.string.wizard_assembly_label, "clearance" to R.string.construction_clearance,
    "interval" to R.string.construction_interval, "wall" to R.string.construction_wall,
    "tolerance" to R.string.construction_tolerance,
)

@Composable
internal fun MaterialStatus(feedback: MaterialFeedback) {
    val language = exportLanguage()
    val message = when {
        feedback.missing.isNotEmpty() -> {
            val labels = feedback.missing.map { key ->
                woodDimensionsLabels[key]?.let { language.wood(it) } ?: stringResource(materialLabels.getValue(key))
            }
            val list = ListFormatter.getInstance(LocalConfiguration.current.locales[0]).format(labels)
            stringResource(R.string.wizard_material_missing, list)
        }
        feedback.problems.isNotEmpty() -> stringResource(R.string.wizard_material_review)
        else -> stringResource(R.string.wizard_material_ready)
    }
    Text(message, Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        .testTag("material-status").semantics { liveRegion = LiveRegionMode.Polite },
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
internal fun MaterialFeedback.reveal(field: String): Modifier {
    val requester = remember { BringIntoViewRequester() }
    val request = attention?.takeIf { it.field == field && field in problems }
    LaunchedEffect(request) { if (request != null) requester.bringIntoView() }
    return Modifier.bringIntoViewRequester(requester)
}

@Composable
internal fun MaterialField(
    key: String, label: String, values: Map<String, String>, set: (String, String) -> Unit,
    feedback: MaterialFeedback, multiline: Boolean = false, helper: String? = null, errorText: String? = null,
) {
    val pending = feedback.attention != null && feedback.problems[key] == Problem.MISSING
    val pendingDescription = stringResource(R.string.wizard_material_field_missing)
    val error = errorText.takeIf { feedback.problems[key] == Problem.INVALID }
    Column(Modifier.fillMaxWidth().then(feedback.reveal(key)), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        if (helper != null) Text(helper, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(values.getValue(key), { set(key, it) },
            modifier = Modifier.fillMaxWidth().testTag("construction-$key").semantics {
                contentDescription = label
                if (error != null) error(error)
                if (pending) stateDescription = pendingDescription
            }, singleLine = !multiline, minLines = if (multiline) 3 else 1, maxLines = if (multiline) 6 else 1,
            isError = error != null, colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = if (pending) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            ))
        if (error != null) Text(error, Modifier.testTag("material-error-$key"),
            color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
internal fun ConstructionMethodFields(
    values: Map<String, String>, set: (String, String) -> Unit, saveCatalog: () -> Unit,
    loadCatalog: () -> Unit, writable: Boolean, feedback: MaterialFeedback,
) {
    if (values.getValue("method") == "LAMINATED") Text(stringResource(R.string.wizard_legacy), style = MaterialTheme.typography.bodySmall)
    val names = listOf(R.string.material_wood, R.string.material_pvc)
    FlowRow(feedback.reveal("method"), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ConstructionForm.activeMethods().forEachIndexed { i, method ->
            FilterChip(values.getValue("method") == method.name, { set("method", method.name) },
                label = { Text(stringResource(names[i])) }, modifier = Modifier.testTag("construction-method-${method.name}"))
        }
    }
    when (values.getValue("method")) {
        "SANDWICH" -> WoodStockFields(values, set, feedback)
        "PVC" -> PvcFields(values, set, feedback, writable, saveCatalog, loadCatalog)
    }
    MaterialSettings(values, set, feedback)
}

@Composable
private fun MaterialSettings(values: Map<String, String>, set: (String, String) -> Unit, feedback: MaterialFeedback) {
    var show by rememberSaveable { mutableStateOf(false) }
    TextButton(onClick = { show = !show }) { Text(stringResource(R.string.wizard_plan_settings)) }
    if (show || feedback.problems.keys.any { it in listOf("interval", "wall", "tolerance", "options") }) {
        MaterialField("interval", stringResource(R.string.construction_interval), values, set, feedback,
            errorText = stringResource(R.string.wizard_dimension_range, 50, 500))
        if (values.getValue("method") != "PVC") {
            MaterialField("wall", stringResource(R.string.construction_wall), values, set, feedback,
                errorText = stringResource(R.string.wizard_positive_dimension))
            MaterialField("tolerance", stringResource(R.string.construction_tolerance), values, set, feedback,
                errorText = stringResource(R.string.wizard_nonnegative_dimension))
        }
    }
}
