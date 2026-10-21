package ar.didgetaller.ui

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
import ar.didgetaller.core.SessionAnalysis
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
private val sections=listOf("Medir","Laboratorio","Biblioteca")
private val symbols=listOf("♪","◇","▣")

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TallerApp(vm:TallerViewModel, startMeasurement:()->Unit, toggleMic:()->Unit,
              exportFile:(String,String,ByteArray)->Unit,
              navController:NavHostController=rememberNavController(),
              exportRecording:(RecordingStore.Session)->Unit={}) {
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
                if(destination.route!=TallerRoutes.Viewer && vm.viewerOriginalOrientation!=null){
                    hostActivity?.requestedOrientation=vm.viewerOriginalOrientation!!;vm.viewerOriginalOrientation=null
                }
                vm.onDestinationChanged(if(destination.route in listOf(TallerRoutes.Session,TallerRoutes.Viewer))args?.getString("id") else null)
            }
            navController.addOnDestinationChangedListener(listener)
            onDispose {navController.removeOnDestinationChangedListener(listener)}
        }
        LaunchedEffect(vm.message) {if(vm.message.isNotBlank()) {snackbars.showSnackbar(vm.message,withDismissAction=true);vm.clearMessage()}}
        LaunchedEffect(vm.undoEntry) {vm.undoEntry?.let {entry ->
            val result=snackbars.showSnackbar("En la papelera",actionLabel="Deshacer",withDismissAction=true,duration=SnackbarDuration.Long)
            if(vm.undoEntry==entry){if(result==SnackbarResult.ActionPerformed)vm.undoDelete() else vm.clearUndo()}
        }}
        Scaffold(
            containerColor=background,
            topBar={
                if(!inViewer && (inDetail || !vm.fullScreen)) TopAppBar(
                    title={
                        if(inDetail)Text(if(inTool)listOf("Diseñar","Espectro","Buscar","Guía")[vm.labTab] else if(route==TallerRoutes.Compare)"Comparar sesiones" else "Sesión")
                        else Column {
                            Text(sections[section],fontSize=24.sp,fontWeight=FontWeight.Bold)
                            Text("DIDGE TALLER",fontSize=11.sp,color=muted,letterSpacing=1.sp)
                        }
                    },
                    navigationIcon={if(inDetail)IconButton(onClick={navController.popBackStack()}) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack,contentDescription=if(inTool)"Volver al Laboratorio" else "Volver a Sesiones")
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
                                SessionPhase.IDLE -> "Iniciar medición"
                                SessionPhase.CAPTURING -> "Detener y guardar"
                                SessionPhase.SAVING -> "Guardando sesión…"
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
                    DestinationContent("measure-screen") {MeasureScreen(vm)}
                }
                composable(TallerRoutes.Lab) {
                    DestinationContent("lab-home") {LabHome {index->vm.labTab=index;navController.navigate(TallerRoutes.tool(index))}}
                }
                composable(TallerRoutes.LabTool,arguments=listOf(navArgument("tool"){type=NavType.IntType})) {tool ->
                    val index=tool.arguments?.getInt("tool") ?: 0
                    LaunchedEffect(index){vm.labTab=index}
                    Column {
                        ScrollableTabRow(selectedTabIndex=index,edgePadding=0.dp,containerColor=background){
                            listOf("Diseñar","Espectro","Buscar","Guía").forEachIndexed {i,label->Tab(selected=index==i,onClick={
                                if(i!=index)navController.navigate(TallerRoutes.tool(i)){popUpTo(TallerRoutes.Lab);launchSingleTop=true}
                            },text={Text(label,maxLines=1)})}
                        }
                        key(index){DestinationContent("lab-screen"){when(index){0->DesignerScreen(vm,exportFile);1->SpectrumScreen(vm,toggleMic);2->EvolutionScreen(vm);else->GuideScreen()}}}
                    }
                }
                composable(TallerRoutes.Library) {
                    Column {
                        PrimaryTabRow(selectedTabIndex=vm.libraryTab,containerColor=background){
                            listOf("Sesiones","Diseños").forEachIndexed {i,label->Tab(selected=vm.libraryTab==i,onClick={vm.libraryTab=i},text={Text(label)})}
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
                        if(item!=null)SessionDetail(vm,item,exportRecording,onBack={navController.popBackStack()},onExpand={navController.navigate(TallerRoutes.viewer(item.id,it))})
                        else Text("Esta sesión ya no está disponible.",color=muted)
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
private fun MeasureScreen(vm:TallerViewModel) {
    RecordingSettings(vm)
    if(vm.permissionDenied){
        val context=androidx.compose.ui.platform.LocalContext.current
        OutlinedButton(onClick={context.startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:${context.packageName}")))}){Text("Configurar permiso de micrófono")}
    }
    val capturing=vm.sessionPhase==SessionPhase.CAPTURING
    val hz=if(capturing)vm.filteredHz else 0.0
    val note=Music.note(hz).split(" · ")
    val status=when {
        vm.requestingPermission -> "Esperando permiso de micrófono"
        vm.permissionDenied -> "Permiso denegado · habilitalo en Ajustes"
        vm.microphoneError!=null -> "Error de captura · ${vm.microphoneError}"
        vm.sessionPhase==SessionPhase.SAVING -> "Analizando y guardando la sesión"
        !capturing -> "Listo para medir"
        vm.reading.rms<0.005 -> "Escuchando · sin señal"
        hz<=0 -> "Detectando · señal baja o inestable"
        vm.stableReading -> "Nota estable"
        else -> "Detectando nota"
    }
    var elapsed by remember(capturing) {mutableIntStateOf(0)}
    LaunchedEffect(capturing,vm.sessionStartedAtMs) {
        while(capturing) {
            elapsed=((android.os.SystemClock.elapsedRealtime()-vm.sessionStartedAtMs)/1000).toInt().coerceAtMost(vm.recordingLimitSeconds)
            kotlinx.coroutines.delay(250)
        }
    }
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
        Text(if(capturing)"SESIÓN EN CURSO" else "MEDICIÓN ACÚSTICA",Modifier.weight(1f),
            color=if(capturing)orange else muted,fontSize=12.sp,fontWeight=FontWeight.Bold,letterSpacing=1.sp)
        TextButton(onClick={vm.fullScreen=!vm.fullScreen}) {Text(if(vm.fullScreen)"Vista normal" else "Vista amplia")}
    }
    Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(28.dp),color=cardColor) {
        Column(Modifier.background(Brush.verticalGradient(listOf(Color(0xFF1D3839),cardColor)))
            .padding(horizontal=20.dp,vertical=if(vm.fullScreen)28.dp else 20.dp),
            horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                Text("Fundamental estimada",Modifier.weight(1f),color=muted,fontSize=12.sp)
                if(capturing)Text("●  ${"%02d:%02d".format(elapsed/60,elapsed%60)}",color=orange,
                    fontWeight=FontWeight.Bold,fontSize=14.sp)
            }
            BoxWithConstraints(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center) {
                val size=(maxWidth.value*(if(vm.fullScreen)0.37f else 0.31f)).coerceIn(74f,155f).sp
                Column(horizontalAlignment=Alignment.CenterHorizontally) {
                    Text(if(hz>0)note.first().uppercase(Locale.getDefault()) else "—",fontSize=size,
                        fontWeight=FontWeight.Bold,lineHeight=size,color=ink,maxLines=1)
                    Text(if(hz>0)note.getOrElse(1){""} else "Nota pendiente",color=blue,
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
        Panel("LECTURA EN VIVO",if(vm.stableReading)"La frecuencia se sostiene entre lecturas." else "La nota aún puede cambiar.") {
            if(hz>0) {
                val cents=Music.cents(hz)
                Text("${if(cents>0) "+" else ""}$cents cents respecto a ${note.getOrElse(1){"la nota cercana"}}",
                    color=ink,fontSize=13.sp)
                CentMeter(cents)
            } else Text("Tocá una nota sostenida. El silencio se conserva como pausa en el gráfico.",color=muted,fontSize=13.sp)
            if(!vm.fullScreen && vm.sessionTimeline.size>2)
                SessionGraph(vm.sessionTimeline,elapsed*1000L,Modifier.fillMaxWidth().height(94.dp),compact=true)
        }
    } else if(!vm.fullScreen) {
        Text("Una sola acción inicia la medición y el WAV. Al detener, la sesión se guarda en Biblioteca.",
            color=muted,fontSize=14.sp,lineHeight=21.sp)
    }
    if(!vm.fullScreen) TextButton(onClick={vm.setRange(vm.maxHz==140)},modifier=Modifier.fillMaxWidth(),enabled=!capturing) {
        Text(if(vm.maxHz==140)"Rango 35–140 Hz · ampliar a 220" else "Rango 35–220 Hz · volver a 140")
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
                         compact:Boolean=false,cursorMs:Int?=null) {
    val valid=points.filter {it.usable}
    val bottom=((valid.minOfOrNull {it.hz!!} ?: 55.0)-4.0).coerceAtLeast(0.0)
    val top=(valid.maxOfOrNull {it.hz!!} ?: 85.0)+4.0
    Box(modifier) {
    Canvas(Modifier.fillMaxSize().background(Color(0xFF10262C),RoundedCornerShape(12.dp)).padding(8.dp)) {
        val width=size.width;val height=size.height
        val span=durationMs.coerceAtLeast(points.lastOrNull()?.timeMs ?: 1L).coerceAtLeast(1L)
        fun x(point:SessionAnalysis.Point)=(point.timeMs.toFloat()/span*width).coerceIn(0f,width)
        fun y(point:SessionAnalysis.Point)=((top-point.hz!!)/(top-bottom)*height).toFloat().coerceIn(0f,height)
        for(i in 0..4)drawLine(Color(0xFF29434A),Offset(0f,height*i/4),Offset(width,height*i/4),strokeWidth=1f)
        var previous:SessionAnalysis.Point?=null
        points.forEach {point ->
            if(point.usable) {
                val before=previous
                if(before!=null && point.timeMs-before.timeMs<=1200) {
                    val difference=abs(1200.0*log2(point.hz!!/before.hz!!))
                    val color=when {
                        point.confidence<0.80 || difference>60 -> orange
                        difference>25 -> Color(0xFFF0D58A)
                        else -> accent
                    }
                    drawLine(color,Offset(x(before),y(before)),Offset(x(point),y(point)),
                        strokeWidth=if(compact)3f else 4f,cap=StrokeCap.Round)
                } else if(!compact)drawCircle(accent,4f,Offset(x(point),y(point)))
                previous=point
            } else previous=null
        }
        cursorMs?.let {time ->
            val at=(time.toFloat()/span*width).coerceIn(0f,width)
            drawLine(blue,Offset(at,0f),Offset(at,height),strokeWidth=3f)
        }
    }
    if(!compact && valid.isNotEmpty()) {
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
    Panel("ESPECTRO EN VIVO","Componentes de 0 a 600 Hz. Un pico no es necesariamente un toot.") {
        SignalGraph(vm.reading.spectrum,GraphKind.Spectrum,Modifier.fillMaxWidth().height(260.dp))
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            Text("0 Hz",color=muted,fontSize=11.sp);Text("300 Hz",color=muted,fontSize=11.sp)
            Text("600 Hz",color=muted,fontSize=11.sp)
        }
        Text(if(vm.filteredHz>0 && vm.listening) "Drone: ${Music.note(vm.filteredHz)} · ${hzLabel(vm.filteredHz)}"
        else "Activá el micrófono y tocá una nota grave.",color=accent,fontSize=14.sp)
    }
    Button(onClick=toggleMic,modifier=Modifier.fillMaxWidth().height(54.dp)) {
        Text(if(vm.listening)"■ Detener análisis" else "● Activar análisis")
    }
    Text("Este análisis en vivo no graba. Para guardar audio y lecturas juntos, iniciá una sesión desde Medir.",
        color=muted,fontSize=12.sp)
    Panel("Fundamental, armónicos y toots") {
        Text("Los picos del gráfico son componentes del sonido. Los toots son modos de ejecución " +
            "que debés tocar por separado para medirlos. El pico más alto no tiene por qué ser la fundamental.",
            color=muted,fontSize=13.sp)
    }
}

@Composable
private fun DesignerScreen(vm:TallerViewModel,export:(String,String,ByteArray)->Unit) {
    var draft by remember(vm.shape) {mutableStateOf(vm.shape)}
    var showSaveDialog by remember {mutableStateOf(false)}
    var designName by remember {mutableStateOf("")}
    Panel("TALLER DE DISEÑO","Predicción educativa 1D. No reemplaza CADSD, DidgeLab ni una prueba real.") {
        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            listOf("PVC","Cónico","Campana").forEachIndexed {index,label ->
                OutlinedButton(onClick={vm.applyPreset(index)},contentPadding=PaddingValues(horizontal=12.dp),
                    modifier=Modifier.heightIn(min=48.dp)) {Text(label,maxLines=1)}
            }
        }
        BoreGraph(draft,Modifier.fillMaxWidth().height(165.dp))
        Text("0 mm · boquilla ${draft.mouthMm} mm → ${draft.lengthMm} mm · campana ${draft.bellMm} mm",
            color=muted,fontSize=12.sp)
        Text("Perfil interior esquemático; no es un plano de fabricación.",color=muted,fontSize=11.sp)
    }
    Panel("PUNTOS DEL CONDUCTO","Editá la forma interior. El primer y último punto son los extremos.") {
        val nodes=draft.controlPoints()
        nodes.forEachIndexed { index,node ->
            Text("Punto ${index+1} / ${nodes.size}",fontWeight=FontWeight.Medium,color=accent,fontSize=12.sp)
            if(index!=0 && index!=nodes.lastIndex) {
                val start=nodes[index-1].positionMm+30
                val end=nodes[index+1].positionMm-30
                if(end>start) DimensionControl("Posición",node.positionMm,start..end,"mm",
                    onChange={ value ->
                        val next=nodes.toMutableList();next[index]=next[index].copy(positionMm=value)
                        draft=draft.withControlPoints(next)
                    },onCommit={vm.updateShape(draft)})
            }
            DimensionControl("Diámetro",node.diameterMm.roundToInt(),20..170,"mm",
                onChange={ value ->
                    val next=nodes.toMutableList();next[index]=next[index].copy(diameterMm=value.toDouble())
                    draft=draft.withControlPoints(next)
                },onCommit={vm.updateShape(draft)})
            if(index!=0 && index!=nodes.lastIndex && nodes.size>2) {
                TextButton(onClick={
                    val next=nodes.toMutableList(); next.removeAt(index)
                    draft=draft.withControlPoints(next);vm.updateShape(draft)
                }) {Text("Quitar este punto",fontSize=11.sp)}
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
        }, enabled=nodes.size<16,modifier=Modifier.fillMaxWidth()) {Text("+ Agregar punto de control")}
    }
    Panel("DIMENSIONES GENERALES","Todos los valores están en milímetros. Los puntos personalizados se conservan.") {
        DimensionControl("Longitud",draft.lengthMm,900..2200,"mm",onChange={draft=resizeProfile(draft,it)},onCommit={vm.updateShape(draft)})
        DimensionControl("Boquilla",draft.mouthMm,20..70,"mm",onChange={draft=changeDiameter(draft,0,it)},onCommit={vm.updateShape(draft)})
        DimensionControl("Diámetro intermedio",draft.middleMm,20..110,"mm",onChange={draft=changeDiameter(draft,1,it)},onCommit={vm.updateShape(draft)})
        DimensionControl("Campana",draft.bellMm,25..170,"mm",onChange={draft=changeDiameter(draft,2,it)},onCommit={vm.updateShape(draft)})
    }
    Panel("RESONANCIAS ESTIMADAS","Modelo 1D con pérdidas y radiación aproximadas. Picos ≠ toots garantizados.") {
        if(vm.simBusy)LinearProgressIndicator(Modifier.fillMaxWidth())
        val result=vm.simulation
        if(result!=null){
            SignalGraph(result.impedance,GraphKind.Impedance,Modifier.fillMaxWidth().height(160.dp))
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                Text("30 Hz",color=muted,fontSize=11.sp);Text("340 Hz",color=muted,fontSize=11.sp)
                Text("650 Hz",color=muted,fontSize=11.sp)
            }
            if(result.frequenciesHz.isEmpty())Text("No se encontraron resonancias en el rango.",color=muted)
            else result.frequenciesHz.take(6).forEachIndexed {index,hz->
                Text("${if(index==0)"Resonancia grave estimada" else "Resonancia ${index+1}"}: ${Music.note(hz)} · ${hzLabel(hz)}",fontSize=13.sp,color=accent,lineHeight=21.sp)
            }
        }else Text("Calculando…",color=muted)
        Text(vm.measuredComparison(),color=muted,fontSize=12.sp)
    }
    FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        Button(onClick={showSaveDialog=true}) {Text("Guardar diseño",fontSize=12.sp)}
        OutlinedButton(onClick={export("didge-perfil.csv","text/csv",vm.exportProfile())}) {
            Text("Exportar CSV",fontSize=12.sp)
        }
    }
    if(showSaveDialog)AlertDialog(onDismissRequest={showSaveDialog=false},
        title={Text("Guardar diseño")},
        text={OutlinedTextField(value=designName,onValueChange={designName=it},label={Text("Nombre")},singleLine=true)},
        confirmButton={TextButton(onClick={vm.saveDesign(designName);showSaveDialog=false;designName=""}){Text("Guardar")}},
        dismissButton={TextButton(onClick={showSaveDialog=false}){Text("Cancelar")}})
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
    Panel("DISEÑO INVERSO", "Buscamos una geometría a partir de tres frecuencias objetivo, como en DidgeLab.") {
        Text("1. Elegí las resonancias deseadas. 2. Tocá Buscar. 3. Compará y probá el resultado.",
            fontSize=13.sp,color=muted)
        val target=vm.optimizationTarget
        TargetSlider("Fundamental / drone",target.droneHz,45f..125f) {vm.updateTarget(drone=it)}
        TargetSlider("Segunda resonancia",target.secondHz,120f..350f) {vm.updateTarget(second=it)}
        TargetSlider("Tercera resonancia",target.thirdHz,190f..510f) {vm.updateTarget(third=it)}
        Text("Estas son resonancias de entrada estimadas, no notas que el intérprete pueda tocar necesariamente.",
            color=orange,fontSize=12.sp)
    }
    Panel("EVOLUCIÓN DE FORMAS","Búsqueda experimental offline: 16 generaciones de 14 candidatos.") {
        if(vm.optimizationActive) {
            LinearProgressIndicator(progress={vm.optimizationGeneration / 16f},modifier=Modifier.fillMaxWidth())
            Text("Generación ${vm.optimizationGeneration} / 16",color=accent)
            OutlinedButton(onClick=vm::cancelOptimization,modifier=Modifier.fillMaxWidth()) {Text("Cancelar búsqueda")}
        }else {
            Button(onClick=vm::optimize,modifier=Modifier.fillMaxWidth()) {Text("Buscar geometría que se aproxime")}
        }
        val best=vm.optimizationBest
        if(best==null) Text("Todavía no se generó ningún candidato.",color=muted,fontSize=13.sp)
        else {
            Text("Perfil original",fontWeight=FontWeight.SemiBold)
            BoreGraph(vm.shape,Modifier.fillMaxWidth().height(110.dp))
            Text("Candidato optimizado",fontWeight=FontWeight.SemiBold)
            Text("Mejor error ponderado: ${String.format(Locale.US,"%.1f",best.fitness)} cents",color=accent)
            Text("Largo: ${best.shape.lengthMm} mm · Boquilla: ${best.shape.mouthMm} mm · Campana: ${best.shape.bellMm} mm",
                color=muted,fontSize=12.sp)
            Text(best.peaks.take(4).joinToString(" · ") { hzLabel(it) },color=muted,fontSize=12.sp)
            BoreGraph(best.shape,Modifier.fillMaxWidth().height(165.dp))
            Button(onClick=vm::applyOptimization,modifier=Modifier.fillMaxWidth(),enabled=!vm.optimizationActive) {
                Text("Aplicar perfil al Diseñador")
            }
        }
    }
    Panel("ALCANCES Y REPRODUCIBILIDAD") {
        Text("Búsqueda evolutiva implementada en Kotlin para este proyecto. No integra las fuentes Python de DidgeLab; " +
            "usa ecuaciones clásicas de propagación 1D, aproximaciones de pérdidas y una función de error por notas.",
            color=muted,fontSize=13.sp)
        Text("La búsqueda es reproducible con la semilla fija. Su precisión debe validarse con mediciones físicas.",
            color=orange,fontSize=12.sp)
    }
}

@Composable
private fun TargetSlider(title:String,value:Double,range:ClosedFloatingPointRange<Float>,onChange:(Double)->Unit) {
    Text(title,color=muted,fontSize=13.sp)
    Text("${Music.note(value)} · ${hzLabel(value)}",color=accent,fontSize=13.sp)
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
    var oldestFirst by remember {mutableStateOf(false)}
    var selected by remember {mutableStateOf<LocalStore.Measurement?>(null)}
    var compared by remember {mutableStateOf<LocalStore.Measurement?>(null)}
    var delete by remember {mutableStateOf<LocalStore.Measurement?>(null)}
    HorizontalDivider(color=Color(0xFF334D51))
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Lecturas antiguas",fontWeight=FontWeight.SemiBold)
            Text("${vm.measurements.size} mediciones sin audio",color=muted,fontSize=12.sp)
        }
        TextButton(onClick={oldestFirst=!oldestFirst}) {Text(if(oldestFirst)"Más antiguas" else "Más recientes")}
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
        modifier=Modifier.fillMaxWidth(),enabled=vm.measurements.isNotEmpty()) {Text("Exportar CSV")}
    selected?.let {item ->
        AlertDialog(onDismissRequest={selected=null},title={Text(item.note)},
            text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("${hzLabel(item.hz)} · ${item.cents} cents")
                Text(item.date)
                Text("Un cent es 1/100 de semitono. Valor positivo: más agudo que la nota teórica.")
                compared?.let {other -> if(other.id!=item.id) Text(
                    "Comparada con ${other.note}: ${String.format(spanish,"%+.1f",item.hz-other.hz)} Hz")}
            }},confirmButton={TextButton(onClick={compared=item;selected=null}){Text("Comparar")}},
            dismissButton={Row {
                TextButton(onClick={selected=null;delete=item}) {Text("Eliminar")}
                TextButton(onClick={selected=null}) {Text("Cerrar")}
            }})
    }
    delete?.let {item -> ConfirmDelete("medición",onConfirm={vm.deleteMeasurement(item);delete=null},onDismiss={delete=null})}
}

@Composable
private fun SessionsLibrary(vm:TallerViewModel,onOpen:(RecordingStore.Session)->Unit) {
    Text("Audio y análisis guardados en este teléfono",color=muted,fontSize=13.sp)
    if(vm.sessions.isEmpty())Panel("Todavía no hay sesiones") {
        Text("Abrí Medir y tocá Iniciar medición. Al detenerla, va a aparecer acá.",color=muted)
    }
    vm.sessions.forEach {item ->key(item.id) {
        DismissCard(onDelete={vm.deleteSession(item)},tag="swipe-${item.id}") {
            Surface(color=cardColor,shape=RoundedCornerShape(18.dp),modifier=Modifier.fillMaxWidth().testTag("session-${item.id}").clickable {onOpen(item)}) {
                Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    var menu by remember {mutableStateOf(false)}
                    val generated="Sesión ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm",Locale.getDefault()).format(java.util.Date(item.createdAt))}"
                    val customName=item.name!=generated
                    Row(verticalAlignment=Alignment.CenterVertically){
                        Text(if(customName)item.name else item.summary?.let {Music.note(it.typicalHz)} ?: "Sesión sin nota",Modifier.weight(1f),style=MaterialTheme.typography.titleMedium,maxLines=2,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        Box {
                            IconButton(onClick={menu=true}){Icon(Icons.Default.MoreVert,"Opciones de la sesión ${item.name}")}
                            DropdownMenu(expanded=menu,onDismissRequest={menu=false}){
                                DropdownMenuItem(text={Text("Mover a papelera")},onClick={menu=false;vm.deleteSession(item)})
                            }
                        }
                    }
                    FlowRow(horizontalArrangement=Arrangement.spacedBy(12.dp)){
                        if(customName && item.summary!=null)Text(Music.note(item.summary.typicalHz),style=MaterialTheme.typography.bodyLarge)
                        Text(item.summary?.let {hzLabel(it.typicalHz)} ?: "Nota no determinada",color=if(item.summary!=null)accent else muted,style=if(item.summary!=null)MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium)
                    }
                    if(item.timeline.isNotEmpty())SessionGraph(item.timeline,item.durationMs,Modifier.fillMaxWidth().height(48.dp),compact=true)
                    Text("${sessionDate(item.createdAt)} · ${formatDuration(item.durationMs)}",color=muted,style=MaterialTheme.typography.bodySmall)
                    Text(item.summary?.let {"Estabilidad ${stabilityLabel(it.stabilityPercent)}"} ?: "WAV disponible para analizar",color=orange,style=MaterialTheme.typography.bodySmall)
                    if(item.legacy && item.name!=(if(item.recovered)"Audio recuperado" else "Grabación anterior"))Text(if(item.recovered)"Audio recuperado" else "Grabación anterior",color=muted,style=MaterialTheme.typography.bodySmall)
                }
            }
        }
    }}
}

@Composable
private fun SessionDetail(vm:TallerViewModel,item:RecordingStore.Session,
                          export:(RecordingStore.Session)->Unit,onBack:()->Unit,onExpand:(AcousticPlot)->Unit) {
    var rename by remember(item.id) {mutableStateOf(false)}
    var name by remember(item.id) {mutableStateOf(item.name)}
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
        Text(item.name,style=MaterialTheme.typography.titleMedium,maxLines=2,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        Text(item.summary?.let {Music.note(it.typicalHz)} ?: "Nota no determinada",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
        Text(item.summary?.let {hzLabel(it.typicalHz)} ?: "Sin frecuencia confiable",color=accent,style=MaterialTheme.typography.headlineSmall)
        Text("${sessionDate(item.createdAt)} · ${formatDuration(item.durationMs)}",color=muted,style=MaterialTheme.typography.bodySmall)
        Text(item.summary?.let {"Estabilidad ${stabilityLabel(it.stabilityPercent)}"} ?: "Audio disponible para analizar",color=orange,style=MaterialTheme.typography.bodySmall)
        TextButton(onClick={more=!more}){Text(if(more)"Menos información" else "Detalles de la sesión")}
        if(more){
            Text(item.name,style=MaterialTheme.typography.bodyMedium)
            Text("%.1f MB · WAV local".format(java.util.Locale.forLanguageTag("es-AR"),item.sizeBytes/1024.0/1024.0),style=MaterialTheme.typography.bodySmall)
            item.summary?.let {summary->
                Text("${summary.stabilityPercent}% de las lecturas pertenecen a la nota predominante. Rango habitual: ${hzLabel(summary.lowHz)}–${hzLabel(summary.highHz)} · ${summary.validPoints} lecturas útiles.",style=MaterialTheme.typography.bodySmall)
            } ?: Text("No hay suficientes lecturas asociadas. Podés analizar el WAV sin modificarlo.",style=MaterialTheme.typography.bodySmall)
        }
    }
    Panel("Audio") {
        FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Button(onClick={vm.togglePlayback(item)},modifier=Modifier.heightIn(min=50.dp)) {
                Text(if(vm.playingId==item.id && !vm.playbackPaused)"Pausar" else "Reproducir")
            }
            OutlinedButton(onClick={vm.restartPlayback(item)},modifier=Modifier.heightIn(min=50.dp)) {Text("Reiniciar")}
        }
        if(vm.playingId==item.id) Text("${formatDuration(cursor.toLong())} / ${formatDuration(item.durationMs)}",
            color=blue,fontSize=12.sp)
        FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            TextButton(onClick={export(item)}) {Text("Exportar WAV")}
            TextButton(onClick={rename=true}) {Text("Renombrar")}
            TextButton(onClick={delete=true}) {Text("Borrar")}
        }
    }
    SessionExplorer(vm,item,cursor.toLong(),onExpand)
    SessionRelations(vm,item)
    if(rename)AlertDialog(onDismissRequest={rename=false},title={Text("Renombrar sesión")},
        text={OutlinedTextField(value=name,onValueChange={name=it},label={Text("Nombre")},singleLine=true)},
        confirmButton={TextButton(onClick={vm.renameSession(item,name);rename=false},enabled=name.isNotBlank()){Text("Guardar")}},
        dismissButton={TextButton(onClick={rename=false}){Text("Cancelar")}})
    if(delete)ConfirmDelete("sesión",onConfirm={vm.deleteSession(item);delete=false;onBack()},onDismiss={delete=false})
}

private fun sessionDate(time:Long)=java.text.SimpleDateFormat("dd MMM · HH:mm",spanish).format(java.util.Date(time))
private fun formatDuration(timeMs:Long)="%02d:%02d".format(timeMs/60000,(timeMs/1000)%60)
private fun stabilityLabel(percent:Int)=when {
    percent>=75 -> "alta"
    percent>=45 -> "media"
    else -> "baja"
}

@Composable
private fun DesignsLibrary(vm:TallerViewModel,onOpen:(LocalStore.Design)->Unit) {
    var delete by remember {mutableStateOf<LocalStore.Design?>(null)}
    Text("Perfiles interiores guardados",color=muted)
    if(vm.designs.isEmpty())Text("Aún no guardaste diseños.",color=muted)
    vm.designs.forEach {item ->key(item.id) {
        DismissCard(onDelete={vm.deleteDesign(item)},tag="swipe-design-${item.id}") {
        Panel(item.name) {
            Text("${item.shape.lengthMm} mm de longitud · ${item.shape.controlPoints().size} puntos · " +
                "boquilla ${item.shape.mouthMm} mm · campana ${item.shape.bellMm} mm",color=muted,fontSize=12.sp)
            FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                TextButton(onClick={onOpen(item)}) {Text("Abrir")}
                TextButton(onClick={vm.duplicateDesign(item)}) {Text("Duplicar")}
                TextButton(onClick={delete=item}) {Text("Eliminar")}
            }
        }
    }
        }
    }
    delete?.let {item -> ConfirmDelete("diseño",onConfirm={vm.deleteDesign(item);delete=null},onDismiss={delete=null})}
}

@Composable
private fun ConfirmDelete(what:String,onConfirm:()->Unit,onDismiss:()->Unit) {
    AlertDialog(onDismissRequest=onDismiss,title={Text("Eliminar $what")},text={Text(if(what=="medición")"Esta acción no se puede deshacer." else "Se moverá a la papelera. Podés restaurarlo hasta que decidas vaciarla.")},
        confirmButton={TextButton(onClick=onConfirm){Text("Eliminar")}},
        dismissButton={TextButton(onClick=onDismiss){Text("Cancelar")}})
}

@Composable
private fun GuideScreen() {
    Panel("APRENDER SIN SABER MÚSICA") {
        Text("Nota fundamental",fontWeight=FontWeight.Bold,color=accent)
        Text("El grave que conseguís con la vibración habitual de los labios. Por ejemplo, Re2 ≈ 73,4 Hz.",color=muted,fontSize=13.sp)
        HorizontalDivider()
        Text("Armónicos",fontWeight=FontWeight.Bold,color=accent)
        Text("Frecuencias que componen el timbre de una nota. No son lo mismo que los toots.",color=muted,fontSize=13.sp)
        HorizontalDivider()
        Text("Toots",fontWeight=FontWeight.Bold,color=accent)
        Text("Modos más agudos que tocás ajustando los labios. Medilos individualmente.",color=muted,fontSize=13.sp)
        HorizontalDivider()
        Text("Contrapresión",fontWeight=FontWeight.Bold,color=accent)
        Text("La resistencia que sentís al tocar. Un espectro no alcanza para predecir esa sensación.",color=muted,fontSize=13.sp)
    }
    Panel("DISEÑAR UN INSTRUMENTO") {
        Text("Probá primero el preset PVC, luego Cónico y Campana. " +
            "Mové una dimensión por vez y compará los picos de impedancia. " +
            "Los resultados son estimaciones; medí prototipos para calibrar el modelo.",color=muted,fontSize=13.sp)
        Text("Para tallar uno de madera, empezá con madera seca, probá el conducto antes de pegarlo y no uses fuego dentro de casa.",
            color=muted,fontSize=13.sp)
    }
    Panel("PRIVACIDAD Y ALCANCES") {
        Text("No hay acceso a Internet, publicidad, cuentas ni envío de audio. " +
            "Medir pide permiso de micrófono al iniciar y guarda cada sesión como WAV privado al detener. Exportar crea una copia donde elijas.",
            color=muted,fontSize=13.sp)
        Text("La simulación de transmisión acústica y la búsqueda evolutiva son implementaciones Kotlin propias, " +
            "inspiradas en las ideas públicas de DidgeLab y CADSD, no sus motores originales.",
            color=orange,fontSize=13.sp)
    }
}
