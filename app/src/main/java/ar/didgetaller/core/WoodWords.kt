package ar.didgetaller.core

enum class WoodTerm(val en:String,val es:String) {
    DEPTH_SHORT("Groove","Canal"), STOCK_T("Thickness of each board","Espesor por tabla"), BLOCK_SIZE("Total block thickness","Espesor total del bloque"), MIN_D("Minimum outside diameter","Diámetro exterior mínimo"), MEASURED_D("Measured outside diameters","Diámetros exteriores medidos"),
    INSTRUMENT_LENGTH("Instrument length","Largo del instrumento"), MOUTH("Mouthpiece internal diameter","Diámetro interior de la boquilla"), BELL("Bell internal diameter","Diámetro interior de campana"),
    TABLE_POSITION("From the mouthpiece","Desde la boquilla"), TABLE_DEPTH("Groove depth per half","Profundidad por mitad"), TABLE_REMAINING("Remaining wall thickness","Espesor restante"),
    CARVING_TABLE("Carving measurements · millimetres","Medidas de ahuecado · milímetros"), SELECTED("Selected stations; complete measurements in CSV","Puntos seleccionados; medidas completas en CSV"),
    TABLE_NOTE("Depth is measured from the joining plane; remaining wood is below the groove.","La profundidad se mide desde el plano de unión; la madera restante queda debajo del canal."),
    LOG_NOTE("Log: minimum wall estimated from the stated diameters, assuming a centred bore.","Tronco: pared mínima estimada con los diámetros indicados, suponiendo el canal centrado."),
    PREPARATION_NOTE("Dimensions allow for the stated preparation losses.","Las medidas descuentan las pérdidas de preparación indicadas."),
    UNCONFIRMED("Not confirmed","Por confirmar"), MIN_REMAINING("Smallest remaining wall","Menor espesor de pared"), AT_BELL("at the bell","en la campana"), AT_MOUTH("at the mouthpiece","en la boquilla"), FROM_MOUTH("from the mouthpiece","desde la boquilla"),
    RESERVE("Extra carving allowance","Reserva de rebaje"), PARALLEL("Rectangular stock: parallel outside edges","Madera rectangular: bordes exteriores paralelos"),
    SHORT_LENGTH("Length","Largo"), SHORT_WIDTH("width","ancho"), SHORT_THICKNESS("thickness","espesor"),
    GROOVE("Groove","Canal"), SHORT_WALL("Wall","Pared"),
    BEFORE("Bore before station","Interior antes de la posición"), AFTER("Bore after station","Interior después de la posición"),
    NAME("Instrument name","Nombre del instrumento"), INPUT_ERROR("Check wood dimensions and allowances","Revisá las medidas y pérdidas de la madera"),
    STOCK("Available wood","Madera disponible"), NONE("Not measured yet","Sin medir todavía"),
    LOG("Natural log","Tronco natural"), BOARDS("Two separate boards","Dos tablas separadas"), BLOCK("Block to split","Bloque para partir"),
    LENGTH("Available length (mm)","Largo disponible (mm)"), DIAMETER("Minimum measured outer diameter (mm)","Diámetro exterior mínimo medido (mm)"),
    WIDTH("Available width (mm)","Ancho disponible (mm)"), BOARD_T("Thickness of each board (mm)","Espesor de cada tabla (mm)"), BLOCK_T("Total block thickness (mm)","Espesor total del bloque (mm)"),
    LOSS("Preparation allowances","Pérdidas de preparación"), KERF("Blade kerf (mm)","Ancho de corte de sierra (mm)"), FACE("Loss per prepared face (mm)","Pérdida por cara preparada (mm)"),
    TRIM("End trim, each end (mm)","Recorte en cada extremo (mm)"), BARK("Radial bark thickness (mm)","Espesor radial de corteza (mm)"),
    EXTERIOR("Radial exterior preparation loss (mm)","Pérdida radial de preparación exterior (mm)"), UNCERTAINTY("Radial uncertainty reserve (mm)","Reserva radial por incertidumbre (mm)"),
    SECTIONS("Optional log measurements: distance, minimum diameter (mm)","Medidas opcionales del tronco: distancia, diámetro mínimo (mm)"),
    CONFIRM("These are my measured bounds along a straight, centred, sound piece","Son mis medidas límite de una pieza recta, centrada y sin defectos conocidos"),
    ASSUMPTION("Initial values are examples. Use the smaller dimensions of both boards. A log needs conservative bounds between measurements; curvature, cracks, knots or ovality may prevent confirmation.","Los valores iniciales son ejemplos. Usá las menores dimensiones de ambas tablas. El tronco necesita límites conservadores entre mediciones; curvatura, grietas, nudos u ovalidad pueden impedir confirmarlo."),
    ZERO_MARGIN("Geometric minimum: no additional allowance","Mínimo geométrico: sin margen adicional"),
    POSITIVE_MARGIN("Fits geometrically, with dimensional allowance","Cabe geométricamente, con margen dimensional"),
    JOIN_WIDTH("Width at joining plane","Ancho en la unión"), LOG_DEPTH("Plane to outside (calculated)","Plano al exterior (calculado)"),
    FITS("The available wood has the required dimensions","La madera disponible tiene las medidas necesarias"), FAIL("DOES NOT FIT this wood","NO CABE en esta madera"), UNKNOWN("Wood measurements still need confirmation","Falta confirmar las medidas de la madera"),
    POSITION("Limiting position","Posición limitante"), MARGIN("Clearance reserve","Margen dimensional"),
    LIMIT_LENGTH("Insufficient usable length","Largo útil insuficiente"), LIMIT_WIDTH("Insufficient prepared width","Ancho preparado insuficiente"), LIMIT_DEPTH("Insufficient half thickness","Espesor de mitad insuficiente"),
    LIMIT_LOG("The usable log diameter is too small","El diámetro útil del tronco no alcanza"), LIMIT_UNKNOWN("More measurements are needed to check the full length","Faltan medidas para comprobar todo el largo"),
    OVERVIEW("Instrument and available wood","Instrumento y madera disponible"), CARVING("Interior and carving dimensions","Interior y medidas de ahuecado"),
    RAW("Available wood","Madera disponible"), CAVITY("White: final air passage","Blanco: conducto de aire final"),
    BOUND("Log outline: stated minimum diameter","Contorno del tronco: diámetro mínimo indicado"), PROFILE_BOUND("Log outline estimated from the measurements","Contorno del tronco aproximado a partir de las medidas"),
    UNMEASURED("Wood dimensions have not been supplied","Faltan las medidas de la madera"),
    PREPARED("Prepared half","Mitad preparada"), PLANE("Joining plane","Plano de unión"),
    BORE("Internal bore diameter","Diámetro interior"), DEPTH("Groove depth","Profundidad del canal"), WALL("Remaining wall","Pared restante"), SIDE("Side wall","Pared lateral"),
    HALF_T("Half thickness","Espesor de mitad"),
    HALF_WIDTH("Half width","Ancho de mitad"), MINIMUM("Minimum per half, before preparation","Mínimo por mitad, sin pérdidas"),
    CONSTRAINT("Minimum wall thickness","Espesor mínimo de pared"), NO_SAFETY("Wood condition and grain still need assessment.","La resistencia depende de la madera y su estado."),
    PASSIVE("Estimated passive resonance","Resonancia pasiva estimada"), TARGET("Target note","Nota objetivo"), NOTE_LIMIT("The target note is not a guarantee of the played note.","La nota objetivo no garantiza la nota al tocar."),
    DETAIL("Full measurements and component data: CSV / SVG","Medidas completas y piezas: CSV / SVG"), SCALE("Longitudinal view · scale","Vista longitudinal · escala"),
    A("Half A","Mitad A"), B("Half B · mirrored","Mitad B · espejada"), LEGEND("Wood in colour; the white area is the air passage","Madera en color; el área blanca es el conducto de aire"),
    PREP_UNKNOWN("No reliable prepared outline at this station","Sin contorno preparado fiable en esta posición")
}
fun WorkshopLanguage.wood(term:WoodTerm)=if(this==WorkshopLanguage.SPANISH)term.es else term.en

object WoodPresentation {
    fun minimum(value:Double)=WorkshopDocument.minimum(value).removeSuffix(".0")
    fun number(value:Double)=if(kotlin.math.abs(value-kotlin.math.round(value))<1e-8)"%.0f".format(java.util.Locale.ROOT,value) else WorkshopDocument.number(value)
    fun kind(stock:WoodStock?)=when(stock) {null->WoodTerm.NONE;is WoodStock.Log->WoodTerm.LOG;is WoodStock.Timber->if(stock.twoBoards)WoodTerm.BOARDS else WoodTerm.BLOCK}
    fun status(fit:WoodAssessment,l:WorkshopLanguage)=l.wood(when(fit.status) {WoodFitStatus.FITS->WoodTerm.FITS;WoodFitStatus.DOES_NOT_FIT->WoodTerm.FAIL;WoodFitStatus.UNCONFIRMED->WoodTerm.UNKNOWN})
    fun reason(fit:WoodAssessment,l:WorkshopLanguage)=l.wood(when(fit.reason) {WoodLimit.LENGTH->WoodTerm.LIMIT_LENGTH;WoodLimit.WIDTH->WoodTerm.LIMIT_WIDTH;WoodLimit.HALF_THICKNESS->WoodTerm.LIMIT_DEPTH;WoodLimit.LOG_ENVELOPE->WoodTerm.LIMIT_LOG;WoodLimit.MEASUREMENTS->WoodTerm.LIMIT_UNKNOWN})
    fun detail(fit:WoodAssessment,l:WorkshopLanguage):String {
        val where="${l.wood(WoodTerm.POSITION)}: ${number(fit.positionMm)} mm"
        return (if(fit.status==WoodFitStatus.FITS)"" else reason(fit,l)+". ")+where+(fit.marginMm?.let {" · "+l.wood(WoodTerm.MARGIN)+": "+number(it)+" mm"} ?: "")
    }
}

/** Print formatting only; numeric CSV and acoustic calculations remain locale independent. */
internal fun WorkshopLanguage.dimension(value:Double):String = WoodPresentation.number(value).let {
    if(this==WorkshopLanguage.SPANISH)it.replace('.',',') else it
}
internal fun WorkshopLanguage.location(x:Double,length:Double):String = when(x) {
    0.0->wood(WoodTerm.AT_MOUTH)
    length->wood(WoodTerm.AT_BELL)
    else->dimension(x)+" mm "+wood(WoodTerm.FROM_MOUTH)
}
