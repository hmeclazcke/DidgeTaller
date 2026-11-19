package ar.didgetaller.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ar.didgetaller.R
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ar.didgetaller.core.*

/** One set of persisted inputs owns both candidate constraints and the workshop stock assessment. */
@Composable
internal fun WoodStockFields(values:Map<String,String>,set:(String,String)->Unit,feedback:MaterialFeedback) {
    val l=exportLanguage()
    Text(l.wood(WoodTerm.STOCK),style=MaterialTheme.typography.titleMedium)
    FlowRow(feedback.reveal("stockKind"), horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        listOf("UNSPECIFIED" to WoodTerm.NONE,"LOG" to WoodTerm.LOG,"BOARDS" to WoodTerm.BOARDS,"BLOCK" to WoodTerm.BLOCK).forEach {(key,term)->
            FilterChip(values.getValue("stockKind")==key,{set("stockKind",key)},label={Text(l.wood(term))},modifier=Modifier.testTag("stock-kind-$key"))
        }
    }
    val dimensions = ConstructionMaterial.woodDimensions(values["stockKind"])
    if (dimensions.isEmpty()) return
    val help = when (values["stockKind"]) {
        "LOG" -> R.string.wood_material_log_help
        "BOARDS" -> R.string.wood_material_boards_help
        else -> R.string.wood_material_block_help
    }
    Text(stringResource(help), style = MaterialTheme.typography.bodySmall)
    dimensions.forEach { key ->
        MaterialField(key, l.wood(woodDimensionsLabels.getValue(key)), values, set, feedback,
            errorText = stringResource(R.string.wizard_dimension_range, 1, ConstructionMaterial.maximumDimension(key)))
    }
    ConfirmWoodMeasurements(values, set, feedback, ready = dimensions.none { it in feedback.problems })
    val fit = if (feedback.problems.isEmpty()) runCatching {
        val o=ConstructionForm.options(values)
        WoodFit.assess(ConstructionForm.shape(values),o.woodStock,o.wallMm,o.radialToleranceMm)
    }.getOrNull() else null
    if (fit != null) WoodFitMessage(fit)
    WoodPreparation(values, set, feedback)
}

@Composable
private fun WoodPreparation(values: Map<String, String>, set: (String, String) -> Unit, feedback: MaterialFeedback) {
    val l = exportLanguage()
    var expanded by remember {mutableStateOf(false)}
    TextButton(onClick={expanded=!expanded},modifier=Modifier.testTag("stock-allowances").then(feedback.reveal("options"))){Text(stringResource(R.string.wood_material_advanced))}
    if (expanded || "options" in feedback.problems) WoodPreparationFields(values,set,feedback)
    if ("options" in feedback.problems) Text(stringResource(R.string.wizard_material_invalid), color=MaterialTheme.colorScheme.error)
    if(expanded) Text(l.wood(WoodTerm.ASSUMPTION),style=MaterialTheme.typography.bodySmall)
}

@Composable
private fun ConfirmWoodMeasurements(values: Map<String, String>, set: (String, String) -> Unit, feedback: MaterialFeedback, ready: Boolean) {
    val label = exportLanguage().wood(WoodTerm.CONFIRM)
    Row(Modifier.fillMaxWidth().then(feedback.reveal("stockConfirmed"))) {
        Checkbox(values.getValue("stockConfirmed")=="true",{set("stockConfirmed",it.toString())},enabled=ready,modifier=Modifier.testTag("stock-confirmed").semantics {contentDescription=label})
        val pending = feedback.attention != null && "stockConfirmed" in feedback.missing
        Text(label,Modifier.weight(1f),style=MaterialTheme.typography.bodySmall,
            color = if (pending) MaterialTheme.colorScheme.primary else LocalContentColor.current)
    }
}

@Composable
private fun WoodPreparationFields(v:Map<String,String>,set:(String,String)->Unit,feedback:MaterialFeedback) {
    val l = exportLanguage()
    val fields = buildMap {
        if (v.getValue("stockKind") == "LOG") {
            put("stockSections", WoodTerm.SECTIONS)
            put("stockBark", WoodTerm.BARK)
            put("stockExterior", WoodTerm.EXTERIOR)
        }
        if (v.getValue("stockKind") != "BOARDS") put("stockKerf", WoodTerm.KERF)
        put("stockFace", WoodTerm.FACE)
        put("stockTrim", WoodTerm.TRIM)
        put("stockUncertainty", WoodTerm.UNCERTAINTY)
    }
    fields.forEach { (key, term) -> MaterialField(key, l.wood(term), v, set, feedback, multiline = key == "stockSections") }
}

@Composable
internal fun WoodFitMessage(fit:WoodAssessment) {
    val l=exportLanguage()
    Text(WoodPresentation.status(fit,l),color=if(fit.status==WoodFitStatus.DOES_NOT_FIT)MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        modifier=Modifier.testTag("stock-fit-${fit.status.name}"))
    Text(WoodPresentation.detail(fit,l),style=MaterialTheme.typography.bodySmall)
}
