package ar.didgetaller.ui

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

internal object TallerRoutes {
    const val Measure = "measure"
    const val MeasureWide = "measure/wide"
    const val Lab = "lab"
    const val Library = "library"
    const val Compare = "compare"
    const val Session = "session/{id}"
    const val Viewer = "viewer/{id}/{plot}"
    const val LabTool = "lab/tool/{tool}"
    val sections = listOf(Measure, Lab, Library)
    fun session(id: String) = "session/$id"
    fun viewer(id:String,plot:AcousticPlot)="viewer/$id/${plot.name}"
    fun tool(index:Int)="lab/tool/$index"
}

/** Keep each main section's saved UI state, without saving an abandoned audio detail. */
internal fun NavHostController.navigateToSection(index: Int) {
    while(currentDestination?.route in listOf(TallerRoutes.MeasureWide,TallerRoutes.Session,TallerRoutes.Viewer,TallerRoutes.Compare,TallerRoutes.LabTool)) popBackStack()
    if (currentDestination?.route == TallerRoutes.sections[index]) return
    navigate(TallerRoutes.sections[index]) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
