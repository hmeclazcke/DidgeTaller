package ar.didgetaller.ui

import android.content.Context
import ar.didgetaller.R

/** Bridge existing string-only diagnostics without changing protected audio/core contracts.
 * Translation text lives only in Android resources; unknown platform details remain verbatim.
 */
internal fun Context.localizedDiagnostic(message: String): String {
    for (rule in diagnosticRules) {
        val match = rule.regex.matchEntire(message) ?: continue
        val arguments = match.groupValues.drop(1).map { localizedDiagnostic(it) }.toTypedArray()
        if (rule.plural) {
            val count = arguments.firstOrNull()?.toIntOrNull() ?: return message
            return resources.getQuantityString(rule.resource, count, count)
        }
        return getString(rule.resource, *arguments)
    }
    // Music.hzText is also used by persisted/exported data and retains its canonical format.
    val frequency = canonicalFrequency.matchEntire(message)?.groupValues?.get(1)?.toDoubleOrNull()
    return if (frequency != null) String.format(resources.configuration.locales[0], "%.1f Hz", frequency) else message
}

private val canonicalFrequency = Regex("([-+]?\\d+(?:\\.\\d+)?) Hz")
private data class DiagnosticRule(val source: String, val resource: Int, val plural: Boolean = false) {
    val regex = Regex(source.split(Regex("\\{\\d+\\}")).joinToString("(.*?)") { Regex.escape(it) }, RegexOption.DOT_MATCHES_ALL)
}

private val diagnosticRules = listOf(
    DiagnosticRule("Destino inválido", R.string.diagnostic_invalid_destination),
    DiagnosticRule("Archivo exportado.", R.string.diagnostic_file_exported),
    DiagnosticRule("No se pudo guardar: {1}", R.string.diagnostic_could_not_save),
    DiagnosticRule("Para medir hace falta permiso de micrófono. Podés concederlo desde Ajustes.", R.string.diagnostic_measurement_requires_microphone_permission_you_can_grant_it_in_setting),
    DiagnosticRule("Error de comparación.", R.string.diagnostic_comparison_error),
    DiagnosticRule("No se pudo restaurar: {1}", R.string.diagnostic_could_not_restore),
    DiagnosticRule("Papelera vaciada.", R.string.diagnostic_trash_emptied),
    DiagnosticRule("No se pudo completar el vaciado: {1}", R.string.diagnostic_could_not_finish_emptying_trash),
    DiagnosticRule("No se capturó audio. Intentá de nuevo.", R.string.diagnostic_no_audio_was_captured_try_again),
    DiagnosticRule("Sesión guardada en Biblioteca.", R.string.diagnostic_session_saved_in_library),
    DiagnosticRule("No se pudo guardar la sesión: {1}", R.string.diagnostic_could_not_save_the_session),
    DiagnosticRule("Se recuperaron {1} grabaciones interrumpidas. Están en Biblioteca.", R.plurals.recovered_recording_count, plural = true),
    DiagnosticRule("Error al leer el WAV.", R.string.diagnostic_error_reading_the_wav),
    DiagnosticRule("Análisis cancelado.", R.string.diagnostic_analysis_canceled),
    DiagnosticRule("No se pudo iniciar la grabación.", R.string.diagnostic_could_not_start_recording),
    DiagnosticRule("Escribí un nombre para el diseño.", R.string.diagnostic_enter_a_name_for_the_design),
    DiagnosticRule("Diseño guardado.", R.string.diagnostic_design_saved),
    DiagnosticRule("No se pudo duplicar: {1}", R.string.diagnostic_could_not_duplicate),
    DiagnosticRule("No se pudo mover a la papelera: {1}", R.string.diagnostic_could_not_move_to_trash),
    DiagnosticRule("No se pudo borrar: {1}", R.string.diagnostic_could_not_delete),
    DiagnosticRule("No se pudo renombrar: {1}", R.string.diagnostic_could_not_rename),
    DiagnosticRule("No se pudo abrir el WAV: {1}", R.string.diagnostic_could_not_open_the_wav),
    DiagnosticRule("Error de simulación: {1}", R.string.diagnostic_simulation_error),
    DiagnosticRule("Búsqueda terminada: probá el diseño encontrado.", R.string.diagnostic_search_finished_try_the_resulting_design),
    DiagnosticRule("Error de búsqueda: {1}", R.string.diagnostic_search_error),
    DiagnosticRule("Búsqueda cancelada. Podés aplicar el mejor resultado parcial.", R.string.diagnostic_search_canceled_you_can_apply_the_best_partial_result),
    DiagnosticRule("Perfil de la búsqueda aplicado. Guardalo para conservarlo.", R.string.diagnostic_search_profile_applied_save_it_to_keep_it),
    DiagnosticRule("Perfil inválido: {1}", R.string.diagnostic_invalid_profile),
    DiagnosticRule("Medí tu didge y simulá su geometría para comparar.", R.string.diagnostic_measure_your_didge_and_simulate_its_geometry_to_compare),
    DiagnosticRule("Medido {1} · Modelo {2} · Desvío {3} cents (aprox.)", R.string.diagnostic_measured_model_difference_cents_approx),
    DiagnosticRule("El micrófono no admite audio PCM a 44,1 kHz.", R.string.diagnostic_the_microphone_does_not_support_pcm_audio_at_44_1_khz),
    DiagnosticRule("No hay permiso de micrófono.", R.string.diagnostic_microphone_permission_is_missing),
    DiagnosticRule("No se pudo inicializar el micrófono: {1}", R.string.diagnostic_could_not_initialize_the_microphone),
    DiagnosticRule("Micrófono no disponible.", R.string.diagnostic_microphone_unavailable),
    DiagnosticRule("No se pudo comenzar: {1}", R.string.diagnostic_could_not_start),
    DiagnosticRule("No se pudo grabar: {1}", R.string.diagnostic_could_not_record),
    DiagnosticRule("No se pudo cerrar el WAV: {1}. La captura queda pendiente de recuperación.", R.string.diagnostic_could_not_close_the_wav_the_capture_is_pending_recovery),
    DiagnosticRule("No se pudo liberar el micrófono.", R.string.recording_release_failed),
    DiagnosticRule("Error al leer el micrófono ({1}).", R.string.diagnostic_microphone_read_error),
    DiagnosticRule("Falló la captura: {1}", R.string.diagnostic_capture_failed),
    DiagnosticRule("No se pudo reproducir: {1}", R.string.diagnostic_could_not_play_audio),
    DiagnosticRule("Audio {1}/{2}", R.string.diagnostic_audio),
    DiagnosticRule("Visualización cancelada.", R.string.diagnostic_visualization_canceled),
    DiagnosticRule("Longitud fuera de rango", R.string.diagnostic_length_out_of_range),
    DiagnosticRule("No se encontraron resonancias en el rango.", R.string.diagnostic_no_resonances_found_in_the_range),
    DiagnosticRule("No es un WAV RIFF válido.", R.string.diagnostic_not_a_valid_riff_wav),
    DiagnosticRule("WAV incompleto o dañado.", R.string.diagnostic_incomplete_or_damaged_wav),
    DiagnosticRule("El WAV tiene un bloque truncado.", R.string.diagnostic_the_wav_has_a_truncated_chunk),
    DiagnosticRule("Formato WAV ambiguo o incompleto.", R.string.diagnostic_ambiguous_or_incomplete_wav_format),
    DiagnosticRule("Se admite WAV PCM de 16 bits, mono o estéreo.", R.string.diagnostic_16_bit_pcm_wav_mono_or_stereo_is_supported),
    DiagnosticRule("Frecuencia de muestreo o alineación WAV inválida.", R.string.diagnostic_invalid_wav_sample_rate_or_frame_alignment),
    DiagnosticRule("WAV con varios bloques de audio no admitido.", R.string.diagnostic_wav_files_with_multiple_audio_chunks_are_not_supported),
    DiagnosticRule("Falta el relleno de un bloque WAV.", R.string.diagnostic_missing_wav_chunk_padding),
    DiagnosticRule("El WAV no contiene audio PCM completo.", R.string.diagnostic_the_wav_contains_no_complete_pcm_audio),
    DiagnosticRule("El archivo de captura ya existe.", R.string.diagnostic_the_capture_file_already_exists),
    DiagnosticRule("Espacio insuficiente. Se conserva el audio capturado.", R.string.diagnostic_not_enough_storage_space_captured_audio_is_preserved),
    DiagnosticRule("Se alcanzó el tamaño máximo de WAV.", R.string.diagnostic_the_maximum_wav_size_was_reached),
    DiagnosticRule("Captura parcial no compatible.", R.string.diagnostic_unsupported_partial_capture),
    DiagnosticRule("Captura parcial demasiado grande.", R.string.diagnostic_partial_capture_is_too_large),
    DiagnosticRule("Rango de fundamental demasiado alto para este muestreo.", R.string.diagnostic_fundamental_range_is_too_high_for_this_sample_rate),
    DiagnosticRule("Demasiadas ventanas: seleccioná un fragmento menor o aumentá el presupuesto de análisis.", R.string.diagnostic_too_many_windows_select_a_shorter_excerpt_or_increase_the_analysis_bud),
    DiagnosticRule("Análisis cancelado", R.string.diagnostic_analysis_canceled_d0d4d),
    DiagnosticRule("El WAV terminó antes del intervalo solicitado.", R.string.diagnostic_the_wav_ended_before_the_requested_interval),
    DiagnosticRule("Demasiadas lecturas: seleccioná un fragmento menor.", R.string.diagnostic_too_many_readings_select_a_shorter_excerpt),
    DiagnosticRule("No se pudo conservar la captura.", R.string.diagnostic_could_not_preserve_the_capture),
    DiagnosticRule("Captura parcial sin muestras completas.", R.string.diagnostic_partial_capture_has_no_complete_samples),
    DiagnosticRule("No se pudo conservar el WAV", R.string.diagnostic_could_not_preserve_the_wav),
    DiagnosticRule("No se encontró el WAV", R.string.diagnostic_wav_not_found),
    DiagnosticRule("No se pudo eliminar el audio; permanece en la papelera.", R.string.diagnostic_could_not_delete_the_audio_it_remains_in_trash),
    DiagnosticRule("Identificador inválido", R.string.diagnostic_invalid_identifier),
    DiagnosticRule("Check failed.", R.string.diagnostic_check_failed),
    DiagnosticRule("Failed requirement.", R.string.diagnostic_invalid_value),
)
