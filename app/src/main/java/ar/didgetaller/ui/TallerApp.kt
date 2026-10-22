package ar.didgetaller.ui

import ar.didgetaller.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ar.didgetaller.TallerViewModel
import ar.didgetaller.SessionPhase
import ar.didgetaller.data.LocalStore
import ar.didgetaller.data.RecordingStore
import ar.didgetaller.core.AcousticModel
import ar.didgetaller.core.Music
import ar.didgetaller.core.InstrumentProfile
import ar.didgetaller.core.SessionAnalysis
import ar.didgetaller.core.PlotScales
import ar.didgetaller.core.PitchTrace
import kotlin.math.*
import java.util.Locale

private val ink=Color(0xFFEDF5F1)
private val muted=Color(0xFFAFC6C2)
private val accent=Color(0xFF8FE0BD)
private val orange=Color(0xFFE9BD87)
private val blue=Color(0xFF91C8DC)
private val background=Color(0xFF0D171B)
private val cardColor=Color(0xFF192D31)
private val appColors=darkColorScheme(
    primary=accent,onPrimary=background,secondary=orange,onSecondary=background,
    background=background,onBackground=ink,surface=cardColor,onSurface=ink,
    surfaceVariant=Color(0xFF263D42),onSurfaceVariant=muted,
    secondaryContainer=Color(0xFF2C4B49),onSecondaryContainer=ink,
    primaryContainer=Color(0xFF234A40),onPrimaryContainer=ink,
    tertiary=blue,onTertiary=background,tertiaryContainer=Color(0xFF23414D),onTertiaryContainer=ink,
    inverseSurface=Color(0xFFD7E7E1),inverseOnSurface=background,inversePrimary=Color(0xFF245F49)
)
private val symbols=listOf("♪","◇","▣")

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TallerApp(vm:TallerViewModel, startMeasurement:()->Unit, toggleMic:()->Unit,
              exportFile:(String,String,ByteArray)->Unit,
              navController:NavHostController=rememberNavController(),
              exportRecording:(RecordingStore.Session)->Unit={}) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sections=listOf(stringResource(R.string.ui_measure),stringResource(R.string.ui_laboratory),stringResource(R.string.ui_library))
    MaterialTheme(colorScheme=appColors) {
        val snackbars=remember {SnackbarHostState()}
        val hostActivity=androidx.compose.ui.platform.LocalContext.current.activity()
        val entry by navController.currentBackStackEntryAsState()
        val route=entry?.destination?.route
        val inViewer=route==TallerRoutes.Viewer
        val inTool=route==TallerRoutes.LabTool
        val inDetail=route==TallerRoutes.Session || route==TallerRoutes.Compare || inViewer || inTool
        val section=if(inTool)1 else if(inDetail)2 else TallerRoutes.sections.indexOf(route).coerceAtLeast(0)
        // The same destination change handles system Back, the toolbar arrow and section changes.
        DisposableEffect(navController,vm) {
            val listener=NavController.OnDestinationChangedListener { _,destination,args ->
                vm.fullScreen=destination.route==TallerRoutes.MeasureWide
                if(destination.route!=TallerRoutes.Viewer && vm.viewerOriginalOrientation!=null){
                    hostActivity?.requestedOrientation=vm.viewerOriginalOrientation!!;vm.viewerOriginalOrientation=null
                }
                vm.onDestinationChanged(if(destination.route in listOf(TallerRoutes.Session,TallerRoutes.Viewer))args?.getString("id") else null,
                    measurement=destination.route in listOf(TallerRoutes.Measure,TallerRoutes.MeasureWide))
            }
            navController.addOnDestinationChangedListener(listener)
            onDispose {navController.removeOnDestinationChangedListener(listener)}
        }
        LaunchedEffect(vm.message) {if(vm.message.isNotBlank()) {snackbars.showSnackbar(context.localizedDiagnostic(vm.message),withDismissAction=true);vm.clearMessage()}}
        LaunchedEffect(route){if(inDetail)snackbars.currentSnackbarData?.dismiss()}
        LaunchedEffect(vm.undoEntry) {vm.undoEntry?.let {entry ->
            val result=snackbars.showSnackbar(context.getString(R.string.ui_in_trash),actionLabel=context.getString(R.string.ui_undo),withDismissAction=true,duration=SnackbarDuration.Long)
            if(vm.undoEntry==entry){if(result==SnackbarResult.ActionPerformed)vm.undoDelete() else vm.clearUndo()}
        }}
        Scaffold(
            containerColor=background,
            topBar={
                if(!inViewer && (inDetail || !vm.fullScreen)) TopAppBar(
                    title={
                        if(inDetail)Text(if(inTool)listOf(stringResource(R.string.ui_design),stringResource(R.string.ui_spectrum),stringResource(R.string.ui_search),stringResource(R.string.ui_guide))[vm.labTab] else if(route==TallerRoutes.Compare)stringResource(R.string.ui_compare_sessions) else stringResource(R.string.ui_session_3cb186))
                        else Column {
                            Text(sections[section],fontSize=24.sp,fontWeight=FontWeight.Bold)
                            Text(stringResource(R.string.ui_didge_taller),fontSize=11.sp,color=muted,letterSpacing=1.sp)
                        }
                    },
                    navigationIcon={if(inDetail)IconButton(onClick={navController.popBackStack()}) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack,contentDescription=if(inTool)context.getString(R.string.ui_back_to_laboratory) else context.getString(R.string.ui_back_to_sessions))
                    }},
                    colors=TopAppBarDefaults.topAppBarColors(containerColor=background)
                )
            },
            bottomBar={
                if(!inViewer)Column {
                    if(section==0) Row(Modifier.fillMaxWidth().background(background)
                        .then(if(vm.fullScreen)Modifier.navigationBarsPadding() else Modifier)
                        .padding(horizontal=16.dp,vertical=8.dp)) {
                        Button(onClick={if(vm.sessionPhase==SessionPhase.CAPTURING)vm.finishSession() else startMeasurement()},
                            modifier=Modifier.fillMaxWidth().heightIn(min=60.dp),
                            enabled=!vm.requestingPermission && vm.sessionPhase!=SessionPhase.SAVING,
                            colors=ButtonDefaults.buttonColors(containerColor=if(vm.sessionPhase==SessionPhase.CAPTURING)orange else accent)) {
                            Text(when(vm.sessionPhase){
                                SessionPhase.IDLE -> context.getString(R.string.ui_start_measurement)
                                SessionPhase.CAPTURING -> context.getString(R.string.ui_stop_and_save)
                                SessionPhase.SAVING -> context.getString(R.string.ui_saving_session)
                            },textAlign=TextAlign.Center,fontWeight=FontWeight.Bold,fontSize=17.sp)
                        }
                    }
                    if(!vm.fullScreen)NavigationBar(modifier=Modifier.navigationBarsPadding(),containerColor=cardColor,tonalElevation=0.dp) {
                        sections.forEachIndexed { idx,title ->
                            NavigationBarItem(
                                selected=section==idx,
                                onClick={navController.navigateToSection(idx)},
                                icon={Text(symbols[idx],fontSize=21.sp)},
                                label={Text(title,maxLines=1)},
                                alwaysShowLabel=true
                            )
                        }
                    }
                }
            },contentWindowInsets=if(inViewer)WindowInsets(0,0,0,0) else ScaffoldDefaults.contentWindowInsets,
            snackbarHost={SnackbarHost(snackbars)}
        ) { padding ->
            NavHost(navController,startDestination=TallerRoutes.Measure,modifier=Modifier.padding(padding).fillMaxSize()) {
                composable(TallerRoutes.Measure) {
                    DestinationContent("measure-screen") {MeasureScreen(vm){navController.navigate(TallerRoutes.MeasureWide){launchSingleTop=true}}}
                }
                composable(TallerRoutes.MeasureWide) {
                    DestinationContent("measure-wide-screen") {MeasureScreen(vm){navController.popBackStack()}}
                }
                composable(TallerRoutes.Lab) {
                    DestinationContent("lab-home") {LabHome {index->vm.labTab=index;navController.navigate(TallerRoutes.tool(index))}}
                }
                composable(TallerRoutes.LabTool,arguments=listOf(navArgument("tool"){type=NavType.IntType})) {tool ->
                    val index=tool.arguments?.getInt("tool") ?: 0
                    LaunchedEffect(index){vm.labTab=index}
                    Column {
                        ScrollableTabRow(selectedTabIndex=index,edgePadding=0.dp,containerColor=background){
                            listOf(context.getString(R.string.ui_design),context.getString(R.string.ui_spectrum),context.getString(R.string.ui_search),context.getString(R.string.ui_guide)).forEachIndexed {i,label->Tab(selected=index==i,onClick={
                                if(i!=index)navController.navigate(TallerRoutes.tool(i)){popUpTo(TallerRoutes.Lab);launchSingleTop=true}
                            },text={Text(context.presetLabel(label),maxLines=1)})}
                        }
                        key(index){DestinationContent("lab-screen"){when(index){0->DesignerScreen(vm,exportFile);1->SpectrumScreen(vm,toggleMic);2->EvolutionScreen(vm);else->GuideScreen()}}}
                    }
                }
                composable(TallerRoutes.Library) {
                    Column {
                        PrimaryTabRow(selectedTabIndex=vm.libraryTab,containerColor=background){
                            listOf(context.getString(R.string.ui_sessions),context.getString(R.string.ui_designs)).forEachIndexed {i,label->Tab(selected=vm.libraryTab==i,onClick={vm.libraryTab=i},text={Text(label)})}
                        }
                        key(vm.libraryTab){DestinationContent("library-list"){
                            LibraryScreen(vm,exportFile,
                                onOpen={navController.navigate(TallerRoutes.session(it.id))},
                                onOpenDesign={vm.selectDesign(it);navController.navigateToSection(1);navController.navigate(TallerRoutes.tool(0))},
                                onCompare={navController.navigate(TallerRoutes.Compare)})
                        }}
                    }
                }
                composable(TallerRoutes.Viewer,arguments=listOf(navArgument("id"){type=NavType.StringType},navArgument("plot"){type=NavType.StringType})){viewer ->
                    val item=vm.sessions.firstOrNull {it.id==viewer.arguments?.getString("id")}
                    if(item!=null)ImmersiveViewer(vm,item,viewer.arguments?.getString("plot"),onBack={navController.popBackStack()})
                }
                composable(TallerRoutes.Compare) {DestinationContent("comparison-screen"){ComparisonScreen(vm)}}
                composable(TallerRoutes.Session,arguments=listOf(navArgument("id") {type=NavType.StringType})) { detail ->
                    val item=vm.sessions.firstOrNull {it.id==detail.arguments?.getString("id")}
                    DestinationContent("session-detail") {
                        if(item!=null)SessionDetail(vm,item,exportRecording,onBack={navController.popBackStack()},onExpand={vm.explorer(item.id).plot=it;navController.navigate(TallerRoutes.viewer(item.id,it))})
                        else Text(stringResource(R.string.ui_this_session_is_no_longer_available),color=muted)
                    }
                }
            }
        }
    }
}

/** Each NavBackStackEntry saves its own scroll offset through rememberScrollState. */
@Composable
private fun DestinationContent(tag:String,content:@Composable ColumnScope.()->Unit) {
    Column(Modifier.fillMaxSize().testTag(tag).verticalScroll(rememberScrollState())
        .padding(horizontal=16.dp,vertical=10.dp),verticalArrangement=Arrangement.spacedBy(16.dp),content=content)
}

@Composable
private fun Panel(title:String?=null,subtitle:String?=null,content:@Composable ColumnScope.()->Unit) {
    Surface(shape=RoundedCornerShape(20.dp),color=cardColor,modifier=Modifier.fillMaxWidth(),tonalElevation=0.dp) {
        Column(Modifier.padding(17.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            if(title!=null) Text(title,fontSize=17.sp,fontWeight=FontWeight.SemiBold,color=ink)
            if(subtitle!=null) Text(subtitle,fontSize=12.sp,color=muted)
            content()
        }
    }
}

@Composable
private fun MeasureScreen(vm:TallerViewModel,onPresentationToggle:()->Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var instruments by remember {mutableStateOf(false)}
    Box {
        TextButton(onClick={instruments=true},enabled=!vm.listening && vm.sessionPhase==SessionPhase.IDLE,modifier=Modifier.testTag("instrument-selector")){
            Text(stringResource(R.string.ui_instrument, context.instrumentLabel(vm.instrument)))
        }
        DropdownMenu(expanded=instruments,onDismissRequest={instruments=false}){
            InstrumentProfile.entries.forEach {profile->DropdownMenuItem(text={Text(context.instrumentLabel(profile))},onClick={vm.selectInstrument(profile);instruments=false})}
        }
    }
    RecordingSettings(vm)
    if(vm.permissionDenied){
        val context=androidx.compose.ui.platform.LocalContext.current
        OutlinedButton(onClick={context.startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:${context.packageName}")))}){Text(stringResource(R.string.ui_configure_microphone_permission))}
    }
    val capturing=vm.sessionPhase==SessionPhase.CAPTURING
    val hz=if(capturing)vm.filteredHz else 0.0
    val note=listOf(context.noteBase(hz), Music.pitch(hz)?.note.orEmpty())
    val status=when {
        vm.requestingPermission -> context.getString(R.string.ui_waiting_for_microphone_permission)
        vm.permissionDenied -> context.getString(R.string.ui_permission_denied_enable_it_in_settings)
        vm.microphoneError!=null -> context.getString(R.string.ui_capture_error, context.localizedDiagnostic(vm.microphoneError.orEmpty()))
        vm.sessionPhase==SessionPhase.SAVING -> context.getString(R.string.ui_analyzing_and_saving_session)
        !capturing -> context.getString(R.string.ui_ready_to_measure)
        vm.reading.rms<0.005 -> context.getString(R.string.ui_listening_no_signal)
        hz<=0 -> context.getString(R.string.ui_signal_present_uncertain_frequency)
        vm.stableReading -> context.getString(R.string.ui_stable_note)
        else -> context.getString(R.string.ui_detecting_note)
    }
    var elapsed by remember(capturing) {mutableIntStateOf(0)}
    LaunchedEffect(capturing,vm.sessionStartedAtMs) {
        while(capturing) {
            elapsed=((android.os.SystemClock.elapsedRealtime()-vm.sessionStartedAtMs)/1000).toInt().coerceAtMost(vm.recordingLimitSeconds)
            kotlinx.coroutines.delay(250)
        }
    }
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
        Text(if(capturing)stringResource(R.string.ui_session_in_progress) else stringResource(R.string.ui_acoustic_measurement),Modifier.weight(1f),
            color=if(capturing)orange else muted,fontSize=12.sp,fontWeight=FontWeight.Bold,letterSpacing=1.sp)
        TextButton(onClick=onPresentationToggle) {Text(if(vm.fullScreen)stringResource(R.string.ui_normal_view) else stringResource(R.string.ui_wide_view))}
    }
    Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(28.dp),color=cardColor) {
        Column(Modifier.background(Brush.verticalGradient(listOf(Color(0xFF1D3839),cardColor)))
            .padding(horizontal=20.dp,vertical=if(vm.fullScreen)28.dp else 20.dp),
            horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                Text(stringResource(R.string.ui_fundamental_base_note),Modifier.weight(1f),color=muted,fontSize=12.sp)
                if(capturing)Text("●  ${"%02d:%02d".format(elapsed/60,elapsed%60)}",color=orange,
                    fontWeight=FontWeight.Bold,fontSize=14.sp)
            }
            BoxWithConstraints(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center) {
                val label=if(hz>0)note.first().uppercase(Locale.getDefault()) else "—"
                val preferred=(maxWidth.value*(if(vm.fullScreen)0.37f else 0.31f)).coerceIn(74f,155f)
                val measurer=androidx.compose.ui.text.rememberTextMeasurer()
                val density=androidx.compose.ui.platform.LocalDensity.current
                val width=measurer.measure(label,style=androidx.compose.ui.text.TextStyle(fontSize=preferred.sp,fontWeight=FontWeight.Bold),softWrap=false).size.width
                val size=(preferred*(with(density){maxWidth.toPx()}/width.coerceAtLeast(1)).coerceAtMost(1f)).sp
                Column(horizontalAlignment=Alignment.CenterHorizontally) {
                    Text(label,fontSize=size,
                        fontWeight=FontWeight.Bold,lineHeight=size,color=ink,maxLines=1)
                    Text(if(hz>0)note.getOrElse(1){""} else stringResource(R.string.ui_note_pending),color=blue,
                        fontSize=if(vm.fullScreen)26.sp else 20.sp,fontWeight=FontWeight.Medium)
                }
            }
            Text(hzLabel(hz),fontSize=if(vm.fullScreen)46.sp else 36.sp,
                fontWeight=FontWeight.Bold,color=accent)
            Text(status,color=if(vm.microphoneError!=null || vm.permissionDenied)orange else muted,
                textAlign=TextAlign.Center,fontSize=15.sp)
        }
    }
    if(capturing) {
        Panel(context.getString(R.string.ui_live_reading),if(vm.stableReading)context.getString(R.string.ui_the_frequency_holds_between_readings) else context.getString(R.string.ui_the_note_may_still_change)) {
            if(hz>0) {
                val cents=Music.cents(hz)
                Text(stringResource(R.string.ui_periodicity_26b035, vm.reading.confidence),color=muted,fontSize=12.sp)
                Text(stringResource(R.string.ui_cents_relative_to, if(cents>0) "+" else "", cents, note.getOrElse(1){stringResource(R.string.ui_the_nearby_note)}),
                    color=ink,fontSize=13.sp)
                CentMeter(cents)
            } else Text(stringResource(R.string.ui_play_a_sustained_note_silence_is_preserved_as_a_pause_in_the_chart),color=muted,fontSize=13.sp)
            if(!vm.fullScreen && vm.sessionTimeline.size>2)
                SessionGraph(vm.sessionTimeline,elapsed*1000L,Modifier.fillMaxWidth().height(94.dp),compact=true,instrument=vm.instrument)
        }
    } else if(!vm.fullScreen) {
        Text(stringResource(R.string.ui_one_action_starts_measurement_and_wav_recording_stopping_saves_the_session_in),
            color=muted,fontSize=14.sp,lineHeight=21.sp)
    }
    if(!vm.fullScreen && vm.instrument==InstrumentProfile.DIDGERIDOO) TextButton(onClick={vm.setRange(vm.maxHz==140)},modifier=Modifier.fillMaxWidth(),enabled=!capturing) {
        Text(if(vm.maxHz==140)stringResource(R.string.ui_range_35_140_hz_extend_to_220) else stringResource(R.string.ui_range_35_220_hz_return_to_140))
    }
}

@Composable
private fun CentMeter(cents:Int) {
    Canvas(Modifier.fillMaxWidth().height(45.dp)) {
        val y=size.height/2
        drawLine(Color(0xFF5D7776),Offset(12f,y),Offset(size.width-12f,y),strokeWidth=7f,cap=StrokeCap.Round)
        drawLine(accent,Offset(size.width/2,y-13f),Offset(size.width/2,y+13f),strokeWidth=3f)
        val p=(cents.coerceIn(-50,50)+50)/100f
        val x=12f+p*(size.width-24f)
        drawLine(orange,Offset(x,y-15f),Offset(x,y+15f),strokeWidth=7f,cap=StrokeCap.Round)
    }
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
        Text("−50",color=muted,fontSize=12.sp);Text("0",color=muted,fontSize=12.sp)
        Text("+50 cents",color=muted,fontSize=12.sp)
    }
}

@Composable
private fun SessionGraph(points:List<SessionAnalysis.Point>,durationMs:Long,modifier:Modifier=Modifier,
                         compact:Boolean=false,cursorMs:Int?=null,instrument:InstrumentProfile=InstrumentProfile.DIDGERIDOO,tag:String="pitch-summary") {
    val range=remember(points,instrument){PlotScales.overview(points,instrument)}
    val trace=remember(points,instrument){PitchTrace.build(points,instrument)}
    val bottom=range.window.low
    val top=range.window.high
    Box(modifier) {
    Canvas(Modifier.fillMaxSize().testTag(tag).semantics {this[PlotWindow]=ar.didgetaller.core.PlotViewport(ar.didgetaller.core.AxisWindow(0.0,durationMs.coerceAtLeast(2).toDouble()),range.window)}.background(Color(0xFF10262C),RoundedCornerShape(12.dp)).padding(8.dp)) {
        val width=size.width;val height=size.height
        val span=durationMs.coerceAtLeast(2L)
        fun x(point:SessionAnalysis.Point)=point.timeMs.toFloat()/span*width
        fun y(point:SessionAnalysis.Point)=((top-point.hz!!)/(top-bottom)*height).toFloat()
        for(i in 0..4)drawLine(Color(0xFF29434A),Offset(0f,height*i/4),Offset(width,height*i/4),strokeWidth=1f)
        clipRect(0f,0f,width,height){trace.forEach {sample ->
            val point=sample.point
            if(!sample.uncertain){
                val before=sample.previous?.let {trace[it].point}
                if(before!=null) {
                    drawLine(pitchColor(sample.tone),Offset(x(before),y(before)),Offset(x(point),y(point)),
                        strokeWidth=if(compact)3f else 4f,cap=StrokeCap.Round,
                        pathEffect=if(sample.bridged)androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(5f,5f)) else null)
                } else drawCircle(pitchColor(sample.tone),if(compact)2f else 4f,Offset(x(point),y(point)))
            }
        }}
        cursorMs?.let {time ->
            val at=(time.toFloat()/span*width).coerceIn(0f,width)
            drawLine(blue,Offset(at,0f),Offset(at,height),strokeWidth=3f)
        }
    }
    if(!compact && range.valid>0) {
        Text(hzLabel(top),Modifier.align(Alignment.TopStart).padding(10.dp),
            color=muted,fontSize=10.sp)
        Text(hzLabel(bottom),Modifier.align(Alignment.BottomStart).padding(10.dp),
            color=muted,fontSize=10.sp)
    }
    }
}

private enum class GraphKind {Wave,Spectrum,Impedance}

@Composable
private fun SignalGraph(values:List<Float>,kind:GraphKind,modifier:Modifier=Modifier) {
    Canvas(modifier.background(Color(0xFF102125),RoundedCornerShape(12.dp)).padding(10.dp)) {
        val w=size.width;val h=size.height
        for(j in 1..3)drawLine(Color(0xFF294249),Offset(0f,j*h/4),Offset(w,j*h/4),strokeWidth=1f)
        if(values.isEmpty())return@Canvas
        if(kind==GraphKind.Wave){
            val step=w/values.size
            values.forEachIndexed { i,value ->
                val a=value.coerceIn(0f,1f)*h*0.43f
                drawLine(accent,Offset((i+0.5f)*step,h/2-a),Offset((i+0.5f)*step,h/2+a),
                    strokeWidth=step.coerceAtLeast(1f)*0.64f,cap=StrokeCap.Round)
            }
        }else{
            val line=Path()
            values.forEachIndexed { i,value ->
                val x=i*w/(values.size-1).coerceAtLeast(1)
                val y=h-(value.coerceIn(0f,1f)*h)
                if(i==0)line.moveTo(x,y) else line.lineTo(x,y)
            }
            drawPath(line,if(kind==GraphKind.Impedance)orange else accent,style=Stroke(2.5f))
        }
    }
}

@Composable
private fun SpectrumScreen(vm:TallerViewModel,toggleMic:()->Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Panel(context.getString(R.string.ui_live_spectrum),context.getString(R.string.ui_components_from_0_to_600_hz_a_peak_is_not_necessarily_a_toot)) {
        SignalGraph(vm.reading.spectrum,GraphKind.Spectrum,Modifier.fillMaxWidth().height(260.dp))
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            Text("0 Hz",color=muted,fontSize=11.sp);Text("300 Hz",color=muted,fontSize=11.sp)
            Text("600 Hz",color=muted,fontSize=11.sp)
        }
        Text(if(vm.filteredHz>0 && vm.listening) stringResource(R.string.ui_drone, context.noteLabel(vm.filteredHz), hzLabel(vm.filteredHz))
        else context.getString(R.string.ui_enable_the_microphone_and_play_a_low_note),color=accent,fontSize=14.sp)
    }
    Button(onClick=toggleMic,modifier=Modifier.fillMaxWidth().height(54.dp)) {
        Text(if(vm.listening)stringResource(R.string.ui_stop_analysis) else stringResource(R.string.ui_start_analysis))
    }
    Text(stringResource(R.string.ui_this_live_analysis_does_not_record_to_save_audio_and_readings_together_start_a),
        color=muted,fontSize=12.sp)
    Panel(context.getString(R.string.ui_fundamental_harmonics_and_toots)) {
        Text(stringResource(R.string.ui_chart_peaks_are_sound_components_toots_are_playing_modes) +
            context.getString(R.string.ui_that_you_need_to_play_separately_to_measure_them_the_highest_peak_is_not_neces),
            color=muted,fontSize=13.sp)
    }
}

@Composable
private fun DesignerScreen(vm:TallerViewModel,export:(String,String,ByteArray)->Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var draft by remember(vm.shape) {mutableStateOf(vm.shape)}
    var showSaveDialog by remember {mutableStateOf(false)}
    var designName by remember {mutableStateOf("")}
    Panel(context.getString(R.string.ui_design_workshop),context.getString(R.string.ui_educational_1d_prediction_does_not_replace_cadsd_didgelab_or_a_real_test)) {
        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            listOf("PVC","Cónico","Campana").forEachIndexed {index,label ->
                OutlinedButton(onClick={vm.applyPreset(index)},contentPadding=PaddingValues(horizontal=12.dp),
                    modifier=Modifier.heightIn(min=48.dp)) {Text(context.presetLabel(label),maxLines=1)}
            }
        }
        BoreGraph(draft,Modifier.fillMaxWidth().height(165.dp))
        Text(stringResource(R.string.ui_0_mm_mouthpiece_mm_mm_bell_mm, draft.mouthMm, draft.lengthMm, draft.bellMm),
            color=muted,fontSize=12.sp)
        Text(stringResource(R.string.ui_schematic_bore_profile_not_a_construction_drawing),color=muted,fontSize=11.sp)
    }
    Panel(context.getString(R.string.ui_bore_points),context.getString(R.string.ui_edit_the_bore_shape_the_first_and_last_points_are_the_endpoints)) {
        val nodes=draft.controlPoints()
        nodes.forEachIndexed { index,node ->
            Text(stringResource(R.string.ui_point, index+1, nodes.size),fontWeight=FontWeight.Medium,color=accent,fontSize=12.sp)
            if(index!=0 && index!=nodes.lastIndex) {
                val start=nodes[index-1].positionMm+30
                val end=nodes[index+1].positionMm-30
                if(end>start) DimensionControl(context.getString(R.string.ui_position),node.positionMm,start..end,"mm",
                    onChange={ value ->
                        val next=nodes.toMutableList();next[index]=next[index].copy(positionMm=value)
                        draft=draft.withControlPoints(next)
                    },onCommit={vm.updateShape(draft)})
            }
            DimensionControl(context.getString(R.string.ui_diameter),node.diameterMm.roundToInt(),20..170,"mm",
                onChange={ value ->
                    val next=nodes.toMutableList();next[index]=next[index].copy(diameterMm=value.toDouble())
                    draft=draft.withControlPoints(next)
                },onCommit={vm.updateShape(draft)})
            if(index!=0 && index!=nodes.lastIndex && nodes.size>2) {
                TextButton(onClick={
                    val next=nodes.toMutableList(); next.removeAt(index)
                    draft=draft.withControlPoints(next);vm.updateShape(draft)
                }) {Text(stringResource(R.string.ui_remove_this_point),fontSize=11.sp)}
            }
            HorizontalDivider(color=Color(0xFF334D51))
        }
        OutlinedButton(onClick={
            if (nodes.size<16) {
                val widest=nodes.zipWithNext().withIndex().maxByOrNull {(_,pair) ->
                    pair.second.positionMm-pair.first.positionMm
                }?.index ?: 0
                val a=nodes[widest]; val b=nodes[widest+1]
                val middle=AcousticModel.BorePoint((a.positionMm+b.positionMm)/2,(a.diameterMm+b.diameterMm)/2)
                val next=nodes.toMutableList();next.add(widest+1,middle)
                draft=draft.withControlPoints(next);vm.updateShape(draft)
            }
        }, enabled=nodes.size<16,modifier=Modifier.fillMaxWidth()) {Text(stringResource(R.string.ui_add_control_point))}
    }
    Panel(context.getString(R.string.ui_overall_dimensions),context.getString(R.string.ui_all_values_are_in_millimeters_custom_points_are_preserved)) {
        DimensionControl(context.getString(R.string.ui_length),draft.lengthMm,900..2200,"mm",onChange={draft=resizeProfile(draft,it)},onCommit={vm.updateShape(draft)})
        DimensionControl(context.getString(R.string.ui_mouthpiece),draft.mouthMm,20..70,"mm",onChange={draft=changeDiameter(draft,0,it)},onCommit={vm.updateShape(draft)})
        DimensionControl(context.getString(R.string.ui_middle_diameter),draft.middleMm,20..110,"mm",onChange={draft=changeDiameter(draft,1,it)},onCommit={vm.updateShape(draft)})
        DimensionControl(context.getString(R.string.ui_bell),draft.bellMm,25..170,"mm",onChange={draft=changeDiameter(draft,2,it)},onCommit={vm.updateShape(draft)})
    }
    Panel(context.getString(R.string.ui_estimated_resonances),context.getString(R.string.ui_1d_model_with_approximate_losses_and_radiation_peaks_guaranteed_toots)) {
        if(vm.simBusy)LinearProgressIndicator(Modifier.fillMaxWidth())
        val result=vm.simulation
        if(result!=null){
            SignalGraph(result.impedance,GraphKind.Impedance,Modifier.fillMaxWidth().height(160.dp))
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                Text("30 Hz",color=muted,fontSize=11.sp);Text("340 Hz",color=muted,fontSize=11.sp)
                Text("650 Hz",color=muted,fontSize=11.sp)
            }
            if(result.frequenciesHz.isEmpty())Text(stringResource(R.string.ui_no_resonances_found_in_the_range),color=muted)
            else result.frequenciesHz.take(6).forEachIndexed {index,hz->
                Text("${if(index==0)stringResource(R.string.ui_estimated_low_resonance) else stringResource(R.string.ui_resonance, index+1)}: ${context.noteLabel(hz)} · ${hzLabel(hz)}",fontSize=13.sp,color=accent,lineHeight=21.sp)
            }
        }else Text(stringResource(R.string.ui_calculating),color=muted)
        Text(context.localizedDiagnostic(vm.measuredComparison()),color=muted,fontSize=12.sp)
    }
    FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        Button(onClick={showSaveDialog=true}) {Text(stringResource(R.string.ui_save_design),fontSize=12.sp)}
        OutlinedButton(onClick={export("didge-perfil.csv","text/csv",vm.exportProfile())}) {
            Text(stringResource(R.string.ui_export_csv),fontSize=12.sp)
        }
    }
    if(showSaveDialog)AlertDialog(onDismissRequest={showSaveDialog=false},
        title={Text(stringResource(R.string.ui_save_design))},
        text={OutlinedTextField(value=designName,onValueChange={designName=it},label={Text(stringResource(R.string.ui_name))},singleLine=true)},
        confirmButton={TextButton(onClick={vm.saveDesign(designName);showSaveDialog=false;designName=""}){Text(stringResource(R.string.ui_save))}},
        dismissButton={TextButton(onClick={showSaveDialog=false}){Text(stringResource(R.string.ui_cancel))}})
}

private fun resizeProfile(shape:AcousticModel.Shape,length:Int):AcousticModel.Shape {
    if(shape.points.isEmpty())return shape.copy(lengthMm=length)
    val nodes=shape.points.mapIndexed {index,p ->
        p.copy(positionMm=when(index){0->0;shape.points.lastIndex->length
            else ->(p.positionMm.toDouble()*length/shape.lengthMm).roundToInt()})
    }
    return shape.copy(lengthMm=length,points=nodes)
}
private fun changeDiameter(shape:AcousticModel.Shape,which:Int,value:Int):AcousticModel.Shape {
    if(shape.points.isEmpty())return when(which){0->shape.copy(mouthMm=value);1->shape.copy(middleMm=value);else->shape.copy(bellMm=value)}
    val nodes=shape.points.toMutableList()
    val index=when(which){0->0;1->nodes.size/2;else->nodes.lastIndex}
    nodes[index]=nodes[index].copy(diameterMm=value.toDouble())
    return shape.withControlPoints(nodes)
}

@Composable
private fun DimensionControl(label:String,value:Int,range:IntRange,unit:String,
                             onChange:(Int)->Unit,onCommit:()->Unit){
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
        Text(label,Modifier.weight(1f).padding(end=12.dp),fontSize=13.sp,color=muted)
        Text("$value $unit",fontSize=13.sp,fontWeight=FontWeight.Bold,color=ink)
    }
    Slider(value=value.toFloat(),onValueChange={onChange(it.roundToInt())},
        valueRange=range.first.toFloat()..range.last.toFloat(),onValueChangeFinished=onCommit)
}

@Composable
private fun BoreGraph(shape:AcousticModel.Shape,modifier:Modifier=Modifier) {
    Canvas(modifier.background(Color(0xFF102125),RoundedCornerShape(12.dp)).padding(16.dp)) {
        val w=size.width;val h=size.height
        val magnification=(h*0.77f)/(200f)
        val upper=Path();val lower=Path()
        repeat(121){idx ->
            val pos=shape.lengthMm*idx/120
            val diameter=shape.diameter(pos).toFloat()
            val x=idx/120f*w
            val r=diameter*magnification/2
            if(idx==0){upper.moveTo(x,h/2-r);lower.moveTo(x,h/2+r)}
            else{upper.lineTo(x,h/2-r);lower.lineTo(x,h/2+r)}
        }
        drawPath(upper,accent,style=Stroke(3f))
        drawPath(lower,accent,style=Stroke(3f))
        drawLine(muted,Offset(0f,h/2),Offset(w,h/2),strokeWidth=1f)
        drawLine(orange,Offset(0f,h/2-shape.mouthMm*magnification/2),Offset(0f,h/2+shape.mouthMm*magnification/2),strokeWidth=3f)
        drawLine(orange,Offset(w,h/2-shape.bellMm*magnification/2),Offset(w,h/2+shape.bellMm*magnification/2),strokeWidth=3f)
        shape.controlPoints().forEach {point ->
            val x=point.positionMm.toFloat()/shape.lengthMm*w
            drawCircle(orange,radius=5f,center=Offset(x,h/2-point.diameterMm.toFloat()*magnification/2))
        }
    }
}

@Composable
private fun EvolutionScreen(vm:TallerViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Panel(context.getString(R.string.ui_inverse_design), context.getString(R.string.ui_search_for_a_geometry_from_three_target_frequencies_as_in_didgelab)) {
        Text(stringResource(R.string.ui_1_choose_the_desired_resonances_2_tap_search_3_compare_and_test_the_result),
            fontSize=13.sp,color=muted)
        val target=vm.optimizationTarget
        TargetSlider(context.getString(R.string.ui_fundamental_drone),target.droneHz,45f..125f) {vm.updateTarget(drone=it)}
        TargetSlider(context.getString(R.string.ui_second_resonance),target.secondHz,120f..350f) {vm.updateTarget(second=it)}
        TargetSlider(context.getString(R.string.ui_third_resonance),target.thirdHz,190f..510f) {vm.updateTarget(third=it)}
        Text(stringResource(R.string.ui_these_are_estimated_input_resonances_not_necessarily_notes_that_the_performer),
            color=orange,fontSize=12.sp)
    }
    Panel(context.getString(R.string.ui_evolving_shapes),context.getString(R.string.ui_experimental_offline_search_16_generations_of_14_candidates)) {
        if(vm.optimizationActive) {
            LinearProgressIndicator(progress={vm.optimizationGeneration / 16f},modifier=Modifier.fillMaxWidth())
            Text(stringResource(R.string.ui_generation_16, vm.optimizationGeneration),color=accent)
            OutlinedButton(onClick=vm::cancelOptimization,modifier=Modifier.fillMaxWidth()) {Text(stringResource(R.string.ui_cancel_search))}
        }else {
            Button(onClick=vm::optimize,modifier=Modifier.fillMaxWidth()) {Text(stringResource(R.string.ui_find_an_approximate_geometry))}
        }
        val best=vm.optimizationBest
        if(best==null) Text(stringResource(R.string.ui_no_candidates_generated_yet),color=muted,fontSize=13.sp)
        else {
            Text(stringResource(R.string.ui_original_profile),fontWeight=FontWeight.SemiBold)
            BoreGraph(vm.shape,Modifier.fillMaxWidth().height(110.dp))
            Text(stringResource(R.string.ui_optimized_candidate),fontWeight=FontWeight.SemiBold)
            Text(stringResource(R.string.ui_best_weighted_error_cents, String.format(displayLocale,"%.1f",best.fitness)),color=accent)
            Text(stringResource(R.string.ui_length_mm_mouthpiece_mm_bell_mm, best.shape.lengthMm, best.shape.mouthMm, best.shape.bellMm),
                color=muted,fontSize=12.sp)
            Text(best.peaks.take(4).joinToString(" · ") { hzLabel(it) },color=muted,fontSize=12.sp)
            BoreGraph(best.shape,Modifier.fillMaxWidth().height(165.dp))
            Button(onClick=vm::applyOptimization,modifier=Modifier.fillMaxWidth(),enabled=!vm.optimizationActive) {
                Text(stringResource(R.string.ui_apply_profile_to_designer))
            }
        }
    }
    Panel(context.getString(R.string.ui_scope_and_reproducibility)) {
        Text(stringResource(R.string.ui_evolutionary_search_implemented_in_kotlin_for_this_project_it_does_not_include) +
            context.getString(R.string.ui_it_uses_classical_1d_propagation_equations_approximate_losses_and_a_pitch_base),
            color=muted,fontSize=13.sp)
        Text(stringResource(R.string.ui_the_search_is_reproducible_with_a_fixed_seed_its_accuracy_must_be_validated_wi),
            color=orange,fontSize=12.sp)
    }
}

@Composable
private fun TargetSlider(title:String,value:Double,range:ClosedFloatingPointRange<Float>,onChange:(Double)->Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Text(title,color=muted,fontSize=13.sp)
    Text("${context.noteLabel(value)} · ${hzLabel(value)}",color=accent,fontSize=13.sp)
    Slider(value=value.toFloat().coerceIn(range.start,range.endInclusive),
        onValueChange={onChange(it.toDouble())},valueRange=range)
}

@Composable
private fun LibraryScreen(vm:TallerViewModel,export:(String,String,ByteArray)->Unit,
                          onOpen:(RecordingStore.Session)->Unit,onOpenDesign:(LocalStore.Design)->Unit,onCompare:()->Unit) {
    LibraryTools(vm,onCompare)
    when(vm.libraryTab) {
        0 -> {
            SessionsLibrary(vm,onOpen=onOpen)
            if(vm.measurements.isNotEmpty())MeasurementsLibrary(vm,export)
        }
        else -> DesignsLibrary(vm,onOpenDesign)
    }
}

@Composable
private fun MeasurementsLibrary(vm:TallerViewModel,export:(String,String,ByteArray)->Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var oldestFirst by remember {mutableStateOf(false)}
    var selected by remember {mutableStateOf<LocalStore.Measurement?>(null)}
    var compared by remember {mutableStateOf<LocalStore.Measurement?>(null)}
    var delete by remember {mutableStateOf<LocalStore.Measurement?>(null)}
    HorizontalDivider(color=Color(0xFF334D51))
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.ui_legacy_readings),fontWeight=FontWeight.SemiBold)
            Text(pluralStringResource(R.plurals.measurement_count, vm.measurements.size, vm.measurements.size),color=muted,fontSize=12.sp)
        }
        TextButton(onClick={oldestFirst=!oldestFirst}) {Text(if(oldestFirst)stringResource(R.string.ui_oldest_first) else stringResource(R.string.ui_newest_first))}
    }
    val sorted=if(oldestFirst)vm.measurements.asReversed() else vm.measurements
    sorted.forEach {item ->
        Surface(color=cardColor,shape=RoundedCornerShape(16.dp),modifier=Modifier.fillMaxWidth().clickable {selected=item}) {
            Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(item.note,fontSize=22.sp,fontWeight=FontWeight.SemiBold)
                    Text(item.date,color=muted,fontSize=12.sp)
                }
                Column(horizontalAlignment=Alignment.End) {
                    Text(hzLabel(item.hz),color=accent,fontWeight=FontWeight.Bold)
                    Text("${if(item.cents>0)"+" else ""}${item.cents} cents",color=muted,fontSize=12.sp)
                }
            }
        }
    }
    OutlinedButton(onClick={export("didge-mediciones.csv","text/csv",vm.exportMeasurements())},
        modifier=Modifier.fillMaxWidth(),enabled=vm.measurements.isNotEmpty()) {Text(stringResource(R.string.ui_export_csv))}
    selected?.let {item ->
        AlertDialog(onDismissRequest={selected=null},title={Text(item.note)},
            text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("${hzLabel(item.hz)} · ${item.cents} cents")
                Text(item.date)
                Text(stringResource(R.string.ui_one_cent_is_1_100_of_a_semitone_a_positive_value_is_higher_than_the_theoretica))
                compared?.let {other -> if(other.id!=item.id) Text(
                    context.getString(R.string.ui_compared_with_hz, other.note, String.format(displayLocale,"%+.1f",item.hz-other.hz)))}
            }},confirmButton={TextButton(onClick={compared=item;selected=null}){Text(stringResource(R.string.ui_compare))}},
            dismissButton={Row {
                TextButton(onClick={selected=null;delete=item}) {Text(stringResource(R.string.ui_delete))}
                TextButton(onClick={selected=null}) {Text(stringResource(R.string.ui_close))}
            }})
    }
    delete?.let {item -> ConfirmDelete("medición",onConfirm={vm.deleteMeasurement(item);delete=null},onDismiss={delete=null})}
}

@Composable
private fun SessionsLibrary(vm:TallerViewModel,onOpen:(RecordingStore.Session)->Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Text(stringResource(R.string.ui_audio_and_analysis_stored_on_this_phone),color=muted,fontSize=13.sp)
    if(vm.sessions.isEmpty())Panel(context.getString(R.string.ui_no_sessions_yet)) {
        Text(stringResource(R.string.ui_open_measure_and_tap_start_measurement_when_you_stop_the_session_will_appear_h),color=muted)
    }
    vm.sessions.forEach {item ->key(item.id) {
        DismissCard(onDelete={vm.deleteSession(item)},tag="swipe-${item.id}") {
            Surface(color=cardColor,shape=RoundedCornerShape(18.dp),modifier=Modifier.fillMaxWidth().testTag("session-${item.id}").clickable {onOpen(item)}) {
                Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    var menu by remember {mutableStateOf(false)}
                    // Compare the original stored default name; names are data, not translated UI labels.
                    val generated="Sesión ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm",Locale.getDefault()).format(java.util.Date(item.createdAt))}"
                    val customName=item.name!=generated
                    Row(verticalAlignment=Alignment.CenterVertically){
                        Text(if(customName)context.sessionName(item) else item.summary?.let {context.noteLabel(it.typicalHz)} ?: if(item.timeline.any {item.instrument.accepts(it)})stringResource(R.string.ui_changing_notes) else stringResource(R.string.ui_session_without_a_note),Modifier.weight(1f),style=MaterialTheme.typography.titleMedium,maxLines=2,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        Box {
                            IconButton(onClick={menu=true}){Icon(Icons.Default.MoreVert,context.getString(R.string.ui_options_for_session, context.sessionName(item)))}
                            DropdownMenu(expanded=menu,onDismissRequest={menu=false}){
                                DropdownMenuItem(text={Text(stringResource(R.string.ui_move_to_trash))},onClick={menu=false;vm.deleteSession(item)})
                            }
                        }
                    }
                    FlowRow(horizontalArrangement=Arrangement.spacedBy(12.dp)){
                        if(customName && item.summary!=null)Text(context.noteLabel(item.summary.typicalHz),style=MaterialTheme.typography.bodyLarge)
                        Text(item.summary?.let {hzLabel(it.typicalHz)} ?: if(item.timeline.any {item.instrument.accepts(it)})stringResource(R.string.ui_changing_notes) else stringResource(R.string.ui_note_not_determined),color=if(item.summary!=null)accent else muted,style=if(item.summary!=null)MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium)
                    }
                    if(item.timeline.isNotEmpty())SessionGraph(item.timeline,item.durationMs,Modifier.fillMaxWidth().height(48.dp),compact=true,instrument=item.instrument,tag="summary-${item.id}")
                    Text("${context.instrumentLabel(item.instrument)} · ${sessionDate(item.createdAt)} · ${formatDuration(item.durationMs)}",color=muted,style=MaterialTheme.typography.bodySmall)
                    Text(item.summary?.let {stringResource(R.string.ui_stability_ff3f29, stabilityLabel(it.stabilityPercent))} ?: stringResource(R.string.ui_wav_available_for_analysis),color=orange,style=MaterialTheme.typography.bodySmall)
                    if(item.legacy && context.sessionName(item)!=(if(item.recovered)context.getString(R.string.ui_recovered_audio) else context.getString(R.string.ui_legacy_recording)))Text(if(item.recovered)stringResource(R.string.ui_recovered_audio) else stringResource(R.string.ui_legacy_recording),color=muted,style=MaterialTheme.typography.bodySmall)
                }
            }
        }
    }}
}

@Composable
private fun SessionDetail(vm:TallerViewModel,item:RecordingStore.Session,
                          export:(RecordingStore.Session)->Unit,onBack:()->Unit,onExpand:(AcousticPlot)->Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var rename by remember(item.id) {mutableStateOf(false)}
    var name by remember(item.id) {mutableStateOf(context.sessionName(item))}
    var delete by remember(item.id) {mutableStateOf(false)}
    var cursor by remember(item.id) {mutableIntStateOf(0)}
    LaunchedEffect(vm.playingId,vm.playbackPaused,item.id) {
        while(vm.playingId==item.id) {
            cursor=vm.playbackPositionMs()
            kotlinx.coroutines.delay(100)
        }
        if(vm.playingId!=item.id)cursor=0
    }
    var more by rememberSaveable(item.id){mutableStateOf(false)}
    Panel {
        Text(context.sessionName(item),style=MaterialTheme.typography.titleMedium,maxLines=2,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        Text(item.summary?.let {context.noteLabel(it.typicalHz)} ?: if(item.timeline.any {item.instrument.accepts(it)})stringResource(R.string.ui_changing_notes) else stringResource(R.string.ui_note_not_determined),style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
        Text(item.summary?.let {hzLabel(it.typicalHz)} ?: if(item.timeline.any {item.instrument.accepts(it)})stringResource(R.string.ui_view_changes_in_fundamental) else stringResource(R.string.ui_no_reliable_frequency),color=accent,style=MaterialTheme.typography.headlineSmall)
        Text("${context.instrumentLabel(item.instrument)} · ${sessionDate(item.createdAt)} · ${formatDuration(item.durationMs)}",color=muted,style=MaterialTheme.typography.bodySmall)
        Text(item.summary?.let {stringResource(R.string.ui_stability_ff3f29, stabilityLabel(it.stabilityPercent))} ?: stringResource(R.string.ui_audio_available_for_analysis),color=orange,style=MaterialTheme.typography.bodySmall)
        TextButton(onClick={more=!more}){Text(if(more)stringResource(R.string.ui_less_information) else stringResource(R.string.ui_session_details))}
        if(more){
            Text(context.sessionName(item),style=MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.ui_mb_local_wav, item.sizeBytes/1024.0/1024.0),style=MaterialTheme.typography.bodySmall)
            item.summary?.let {summary->
                Text(pluralStringResource(R.plurals.session_summary_readings, summary.validPoints, summary.stabilityPercent, hzLabel(summary.lowHz), hzLabel(summary.highHz), summary.validPoints),style=MaterialTheme.typography.bodySmall)
            } ?: Text(if(item.timeline.any {item.instrument.accepts(it)})stringResource(R.string.ui_there_are_different_notes_with_no_predominant_note_explore_the_time_series) else stringResource(R.string.ui_not_enough_associated_readings_you_can_analyze_the_wav_without_modifying_it),style=MaterialTheme.typography.bodySmall)
        }
    }
    SessionExplorer(vm,item,cursor.toLong(),onExpand) {
        FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Button(onClick={vm.togglePlayback(item)},modifier=Modifier.heightIn(min=50.dp)) {
                Text(if(vm.playingId==item.id && !vm.playbackPaused)stringResource(R.string.ui_pause) else stringResource(R.string.ui_play))
            }
            OutlinedButton(onClick={vm.restartPlayback(item)},modifier=Modifier.heightIn(min=50.dp)) {Text(stringResource(R.string.ui_restart))}
            Box(Modifier.heightIn(min=50.dp),contentAlignment=Alignment.Center) {
                Text(stringResource(R.string.session_playback_time, formatDuration(cursor.toLong()), formatDuration(item.durationMs)),
                    color=blue,fontSize=12.sp)
            }
        }
    }
    Panel(context.getString(R.string.ui_audio)) {
        FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            TextButton(onClick={export(item)}) {Text(stringResource(R.string.ui_export_wav))}
            TextButton(onClick={rename=true}) {Text(stringResource(R.string.ui_rename))}
            TextButton(onClick={delete=true}) {Text(stringResource(R.string.ui_delete_d7b82b))}
        }
    }
    SessionRelations(vm,item)
    if(rename)AlertDialog(onDismissRequest={rename=false},title={Text(stringResource(R.string.ui_rename_session))},
        text={OutlinedTextField(value=name,onValueChange={name=it},label={Text(stringResource(R.string.ui_name))},singleLine=true)},
        confirmButton={TextButton(onClick={vm.renameSession(item,name);rename=false},enabled=name.isNotBlank()){Text(stringResource(R.string.ui_save))}},
        dismissButton={TextButton(onClick={rename=false}){Text(stringResource(R.string.ui_cancel))}})
    if(delete)ConfirmDelete("sesión",onConfirm={vm.deleteSession(item);delete=false;onBack()},onDismiss={delete=false})
}

private fun sessionDate(time:Long)=java.text.SimpleDateFormat("dd MMM · HH:mm",displayLocale).format(java.util.Date(time))
private fun formatDuration(timeMs:Long)="%02d:%02d".format(timeMs/60000,(timeMs/1000)%60)
@Composable
private fun stabilityLabel(percent:Int)=when {
    percent>=75 -> stringResource(R.string.ui_high)
    percent>=45 -> stringResource(R.string.ui_medium)
    else -> stringResource(R.string.ui_low)
}

@Composable
private fun DesignsLibrary(vm:TallerViewModel,onOpen:(LocalStore.Design)->Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var delete by remember {mutableStateOf<LocalStore.Design?>(null)}
    Text(stringResource(R.string.ui_saved_bore_profiles),color=muted)
    if(vm.designs.isEmpty())Text(stringResource(R.string.ui_no_saved_designs_yet),color=muted)
    vm.designs.forEach {item ->key(item.id) {
        DismissCard(onDelete={vm.deleteDesign(item)},tag="swipe-design-${item.id}") {
        Panel(item.name) {
            Text(stringResource(R.string.ui_mm_long_points, item.shape.lengthMm, item.shape.controlPoints().size) +
                context.getString(R.string.ui_mouthpiece_mm_bell_mm, item.shape.mouthMm, item.shape.bellMm),color=muted,fontSize=12.sp)
            FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                TextButton(onClick={onOpen(item)}) {Text(stringResource(R.string.ui_open))}
                TextButton(onClick={vm.duplicateDesign(item)}) {Text(stringResource(R.string.ui_duplicate))}
                TextButton(onClick={delete=item}) {Text(stringResource(R.string.ui_delete))}
            }
        }
    }
        }
    }
    delete?.let {item -> ConfirmDelete("diseño",onConfirm={vm.deleteDesign(item);delete=null},onDismiss={delete=null})}
}

@Composable
private fun ConfirmDelete(what:String,onConfirm:()->Unit,onDismiss:()->Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    AlertDialog(onDismissRequest=onDismiss,title={Text(stringResource(R.string.ui_delete_18e9c0, context.categoryLabel(what)))},text={Text(if(what=="medición")stringResource(R.string.ui_this_action_cannot_be_undone) else stringResource(R.string.ui_it_will_be_moved_to_trash_you_can_restore_it_until_you_choose_to_empty_the_tra))},
        confirmButton={TextButton(onClick=onConfirm){Text(stringResource(R.string.ui_delete))}},
        dismissButton={TextButton(onClick=onDismiss){Text(stringResource(R.string.ui_cancel))}})
}

@Composable
private fun GuideScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    Panel(context.getString(R.string.ui_learn_without_music_theory)) {
        Text(stringResource(R.string.ui_fundamental_note),fontWeight=FontWeight.Bold,color=accent)
        Text(stringResource(R.string.ui_the_low_note_produced_by_your_usual_lip_vibration_for_example_d2_73_4_hz),color=muted,fontSize=13.sp)
        HorizontalDivider()
        Text(stringResource(R.string.ui_harmonics),fontWeight=FontWeight.Bold,color=accent)
        Text(stringResource(R.string.ui_frequencies_that_make_up_a_note_s_timbre_they_are_different_from_toots),color=muted,fontSize=13.sp)
        HorizontalDivider()
        Text(stringResource(R.string.ui_toots),fontWeight=FontWeight.Bold,color=accent)
        Text(stringResource(R.string.ui_higher_modes_played_by_adjusting_your_lips_measure_them_individually),color=muted,fontSize=13.sp)
        HorizontalDivider()
        Text(stringResource(R.string.ui_back_pressure),fontWeight=FontWeight.Bold,color=accent)
        Text(stringResource(R.string.ui_the_resistance_you_feel_while_playing_a_spectrum_alone_cannot_predict_that_sen),color=muted,fontSize=13.sp)
    }
    Panel(context.getString(R.string.ui_design_an_instrument)) {
        Text(stringResource(R.string.ui_try_the_pvc_preset_first_then_conical_and_bell) +
            context.getString(R.string.ui_change_one_dimension_at_a_time_and_compare_impedance_peaks) +
            context.getString(R.string.ui_results_are_estimates_measure_prototypes_to_calibrate_the_model),color=muted,fontSize=13.sp)
        Text(stringResource(R.string.ui_to_carve_a_wooden_instrument_start_with_dry_wood_test_the_bore_before_gluing_a),
            color=muted,fontSize=13.sp)
    }
    Panel(context.getString(R.string.ui_privacy_and_scope)) {
        Text(stringResource(R.string.ui_no_internet_access_advertising_accounts_or_audio_uploads) +
            context.getString(R.string.ui_measure_requests_microphone_permission_when_starting_and_saves_each_session_as),
            color=muted,fontSize=13.sp)
        Text(stringResource(R.string.ui_the_acoustic_transmission_simulation_and_evolutionary_search_are_original_kotl) +
            context.getString(R.string.ui_inspired_by_the_public_ideas_of_didgelab_and_cadsd_not_their_original_engines),
            color=orange,fontSize=13.sp)
    }
}
