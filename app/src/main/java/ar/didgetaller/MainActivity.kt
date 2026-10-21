package ar.didgetaller

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import ar.didgetaller.ui.TallerApp

class MainActivity:ComponentActivity() {
    private lateinit var vm:TallerViewModel
    private var permissionForSession=true

    private val micPermission=registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        vm.onPermissionResult(granted,permissionForSession)
    }
    private val saveDocument=registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val target=result.data?.data
        val bytes=vm.pendingExport
        val file=vm.pendingExportFile
        vm.pendingExport=null;vm.pendingExportFile=null
        if(result.resultCode==RESULT_OK && target!=null && (bytes!=null || file!=null)) {
            lifecycleScope.launch {
                runCatching {withContext(Dispatchers.IO){contentResolver.openOutputStream(target)?.use {output ->
                    if(file!=null)file.inputStream().buffered().use {it.copyTo(output)} else output.write(bytes!!)
                } ?: error("Destino inválido")}}
                    .onSuccess {vm.notify("Archivo exportado.")}
                    .onFailure {vm.notify("No se pudo guardar: ${it.message}")}
            }
        }
    }

    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        WindowCompat.getInsetsController(window,window.decorView).isAppearanceLightStatusBars=false
        WindowCompat.getInsetsController(window,window.decorView).isAppearanceLightNavigationBars=false
        vm=ViewModelProvider(this)[TallerViewModel::class.java]
        setContent {
            TallerApp(vm,
                startMeasurement = {
                    if(ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)
                        vm.startSession()
                    else {permissionForSession=true;vm.onPermissionRequest();micPermission.launch(Manifest.permission.RECORD_AUDIO)}
                },
                toggleMic = {
                    if(vm.listening)vm.microphoneStop()
                    else if(ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED){
                        vm.microphoneStart()
                    } else {permissionForSession=false;vm.onPermissionRequest();micPermission.launch(Manifest.permission.RECORD_AUDIO)}
                },
                exportFile = {name,mime,bytes ->
                    vm.pendingExportFile=null;vm.pendingExport=bytes
                    val intent=Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type=mime
                        putExtra(Intent.EXTRA_TITLE,name)
                    }
                    saveDocument.launch(intent)
                },
                exportRecording = {item ->
                    vm.pendingExport=null;vm.pendingExportFile=vm.sessionFile(item)
                    saveDocument.launch(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE);type="audio/wav";putExtra(Intent.EXTRA_TITLE,"${item.name}.wav")
                    })
                }
            )
        }
    }
    override fun onResume(){super.onResume()
        if(::vm.isInitialized && ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)vm.permissionAvailable()
    }
    override fun onStop(){vm.onAppStopped(isChangingConfigurations);super.onStop()}
}
