package ar.didgetaller.core

enum class WoodTerm(val en:String,val es:String) {
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
    FITS("Fits the stated dimensional envelope","Cabe en la envolvente dimensional indicada"), FAIL("DOES NOT FIT this wood","NO CABE en esta madera"), UNKNOWN("Fit not confirmed","Ajuste sin confirmar"),
    POSITION("Limiting position","Posición limitante"), MARGIN("Clearance reserve","Margen dimensional"),
    LIMIT_LENGTH("Insufficient usable length","Largo útil insuficiente"), LIMIT_WIDTH("Insufficient prepared width","Ancho preparado insuficiente"), LIMIT_DEPTH("Insufficient half thickness","Espesor de mitad insuficiente"),
    LIMIT_LOG("Insufficient usable log envelope","Envolvente útil del tronco insuficiente"), LIMIT_UNKNOWN("Measurements or full-length bounds are incomplete","Faltan medidas o límites de todo el largo"),
    OVERVIEW("Instrument and starting wood","Instrumento y madera inicial"), CARVING("Interior · halves A and B","Interior · mitades A y B"),
    RAW("Starting stock · before preparation","Madera inicial · antes de preparar"), CAVITY("White: final air passage","Blanco: conducto de aire final"),
    BOUND("Log outline: declared minimum envelope","Tronco: envolvente mínima declarada"), PROFILE_BOUND("Log outline: interpolated measured bounds","Tronco: límites medidos interpolados"),
    UNMEASURED("Required envelope only · available wood unknown","Solo envolvente requerida · madera disponible desconocida"),
    PREPARED("Prepared half","Mitad preparada"), PLANE("Joining plane","Plano de unión"),
    BORE("Bore diameter","Diámetro interior"), DEPTH("Groove depth","Profundidad del canal"), WALL("Remaining wall","Pared restante"), SIDE("Side wall","Pared lateral"),
    HALF_T("Half thickness","Espesor de mitad"),
    HALF_WIDTH("Half width","Ancho de mitad"), MINIMUM("Theoretical minimum per half","Mínimo teórico por mitad"),
    CONSTRAINT("Chosen wall constraint","Pared elegida como restricción"), NO_SAFETY("A geometric fit is not a wood-strength or instrument-safety certification.","El ajuste geométrico no certifica resistencia de la madera ni seguridad del instrumento."),
    PASSIVE("Estimated passive resonance","Resonancia pasiva estimada"), TARGET("Acoustic target","Objetivo acústico"), NOTE_LIMIT("Played pitch is not guaranteed","La nota tocada no está garantizada"),
    DETAIL("Full measurements and component data: CSV / SVG","Medidas completas y piezas: CSV / SVG"), SCALE("Reduced view · dimensions govern","Vista reducida · mandan las cotas"),
    A("Half A","Mitad A"), B("Half B · mirrored","Mitad B · espejada"), LEGEND("Warm wood: available material · white: cavity","Madera cálida: material disponible · blanco: cavidad"),
    PREP_UNKNOWN("No reliable prepared outline at this station","Sin contorno preparado fiable en esta posición")
}
fun WorkshopLanguage.wood(term:WoodTerm)=if(this==WorkshopLanguage.SPANISH)term.es else term.en

object WoodPresentation {
    fun number(value:Double)=if(kotlin.math.abs(value-kotlin.math.round(value))<1e-8)"%.0f".format(java.util.Locale.ROOT,value) else WorkshopDocument.number(value)
    fun kind(stock:WoodStock?)=when(stock) {null->WoodTerm.NONE;is WoodStock.Log->WoodTerm.LOG;is WoodStock.Timber->if(stock.twoBoards)WoodTerm.BOARDS else WoodTerm.BLOCK}
    fun status(fit:WoodAssessment,l:WorkshopLanguage)=l.wood(when(fit.status) {WoodFitStatus.FITS->WoodTerm.FITS;WoodFitStatus.DOES_NOT_FIT->WoodTerm.FAIL;WoodFitStatus.UNCONFIRMED->WoodTerm.UNKNOWN})
    fun reason(fit:WoodAssessment,l:WorkshopLanguage)=l.wood(when(fit.reason) {WoodLimit.LENGTH->WoodTerm.LIMIT_LENGTH;WoodLimit.WIDTH->WoodTerm.LIMIT_WIDTH;WoodLimit.HALF_THICKNESS->WoodTerm.LIMIT_DEPTH;WoodLimit.LOG_ENVELOPE->WoodTerm.LIMIT_LOG;WoodLimit.MEASUREMENTS->WoodTerm.LIMIT_UNKNOWN})
    fun detail(fit:WoodAssessment,l:WorkshopLanguage):String {
        val where="${l.wood(WoodTerm.POSITION)}: ${number(fit.positionMm)} mm"
        return (if(fit.status==WoodFitStatus.FITS)"" else reason(fit,l)+". ")+where+(fit.marginMm?.let {" · "+l.wood(WoodTerm.MARGIN)+": "+number(it)+" mm"} ?: "")
    }
}
