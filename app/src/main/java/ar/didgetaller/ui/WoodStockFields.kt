package ar.didgetaller.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import ar.didgetaller.core.*

/** One set of persisted inputs owns both candidate constraints and the workshop stock assessment. */
@Composable
internal fun WoodStockFields(values:Map<String,String>,set:(String,String)->Unit) {
    val l=exportLanguage()
    Text(l.wood(WoodTerm.STOCK),style=MaterialTheme.typography.titleMedium)
    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        listOf("UNSPECIFIED" to WoodTerm.NONE,"LOG" to WoodTerm.LOG,"BOARDS" to WoodTerm.BOARDS,"BLOCK" to WoodTerm.BLOCK).forEach {(key,term)->
            FilterChip(values.getValue("stockKind")==key,{set("stockKind",key)},label={Text(l.wood(term))},modifier=Modifier.testTag("stock-kind-$key"))
        }
    }
    if(values.getValue("stockKind")=="UNSPECIFIED")return
    WoodInput("stockLength",WoodTerm.LENGTH,values,set)
    if(values.getValue("stockKind")=="LOG") {
        WoodInput("stockDiameter",WoodTerm.DIAMETER,values,set)
    } else {
        WoodInput("stockWidth",WoodTerm.WIDTH,values,set)
        if(values.getValue("stockKind")=="BOARDS")WoodInput("stockThickness",WoodTerm.BOARD_T,values,set)
        else WoodInput("stockBlockThickness",WoodTerm.BLOCK_T,values,set)
    }
    var expanded by remember {mutableStateOf(false)}
    TextButton(onClick={expanded=!expanded},modifier=Modifier.testTag("stock-allowances")){Text(l.wood(WoodTerm.LOSS))}
    if(expanded)WoodPreparationFields(values,set)
    Row(Modifier.fillMaxWidth()) {
        Checkbox(values.getValue("stockConfirmed")=="true",{set("stockConfirmed",it.toString())},modifier=Modifier.testTag("stock-confirmed"))
        Text(l.wood(WoodTerm.CONFIRM),Modifier.weight(1f),style=MaterialTheme.typography.bodySmall)
    }
    Text(l.wood(WoodTerm.ASSUMPTION),style=MaterialTheme.typography.bodySmall)
    val fit=runCatching {
        val o=ConstructionForm.options(values)
        WoodFit.assess(ConstructionForm.shape(values),o.woodStock,o.wallMm,o.radialToleranceMm)
    }.getOrNull()
    if(fit!=null)WoodFitMessage(fit) else Text(l.wood(WoodTerm.INPUT_ERROR),color=MaterialTheme.colorScheme.error)
}

@Composable
private fun WoodPreparationFields(v:Map<String,String>,set:(String,String)->Unit) {
    if(v.getValue("stockKind")=="LOG") {
        WoodInput("stockSections",WoodTerm.SECTIONS,v,set,true)
        WoodInput("stockBark",WoodTerm.BARK,v,set)
        WoodInput("stockExterior",WoodTerm.EXTERIOR,v,set)
    }
    if(v.getValue("stockKind")!="BOARDS")WoodInput("stockKerf",WoodTerm.KERF,v,set)
    WoodInput("stockFace",WoodTerm.FACE,v,set)
    WoodInput("stockTrim",WoodTerm.TRIM,v,set)
    WoodInput("stockUncertainty",WoodTerm.UNCERTAINTY,v,set)
}

@Composable
private fun WoodInput(key:String,term:WoodTerm,v:Map<String,String>,set:(String,String)->Unit,multi:Boolean=false) {
    OutlinedTextField(v.getValue(key),{set(key,it)},label={Text(exportLanguage().wood(term))},singleLine=!multi,
        minLines=if(multi)3 else 1,maxLines=if(multi)6 else 1,modifier=Modifier.fillMaxWidth().testTag("construction-$key"))
}

@Composable
internal fun WoodFitMessage(fit:WoodAssessment) {
    val l=exportLanguage()
    Text(WoodPresentation.status(fit,l),color=if(fit.status==WoodFitStatus.DOES_NOT_FIT)MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        modifier=Modifier.testTag("stock-fit-${fit.status.name}"))
    Text(WoodPresentation.detail(fit,l),style=MaterialTheme.typography.bodySmall)
}
