package com.example.appsecure // Asegúrate de mantener tu paquete real aquí

import android.view.WindowManager
import android.os.Bundle
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity: FlutterActivity() {
    private val CHANNEL = "com.example.seguridad/gps"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Mantenemos la protección de captura de pantalla de la práctica 1
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            if (call.method == "checkFakeGPS") {
                // Ejecutamos la verificación en tiempo real
                checkMockLocationLive { isMock, infoCulpable ->
                    if (isMock) {
                        // Si es falso, le mandamos a Flutter el nombre del culpable
                        result.success(infoCulpable)
                    } else {
                        // Si está limpio, mandamos la palabra clave "CLEAN"
                        result.success("CLEAN")
                    }
                }
            } else {
                result.notImplemented()
            }
        }
    }

    // Estrategia en vivo que analiza la telemetría actual y el proveedor
    private fun checkMockLocationLive(callback: (Boolean, String) -> Unit) {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        try {
            val providers = locationManager.getProviders(true)
            if (providers.isEmpty()) {
                callback(false, "CLEAN")
                return
            }

            val handler = android.os.Handler(android.os.Looper.getMainLooper())
            var hasResponded = false

            // El Listener ahora procesará las respuestas de cualquier proveedor que despierte primero
            val locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    val isMock = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        location.isMock
                    } else {
                        location.extras?.getBoolean("mockLocation", false) == true
                    }

                    // SI DETECTA FAKE GPS: Cortamos de inmediato y mandamos la alerta (prioridad máxima)
                    if (isMock) {
                        if (!hasResponded) {
                            hasResponded = true
                            handler.removeCallbacksAndMessages(null)
                            locationManager.removeUpdates(this)
                            val infoCulpable = "Proveedor: ${location.provider} (SIMULADO) | Precision: ${location.accuracy}m"
                            callback(true, infoCulpable)
                        }
                        return
                    }

                    // SI NO ES MOCK: Esperamos un momento por si el otro proveedor (el GPS) sí trae la simulación.
                    // Si ya es el último recurso o la precisión es alta, cerramos de forma limpia.
                    if (!hasResponded && (location.provider == LocationManager.GPS_PROVIDER || !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))) {
                        hasResponded = true
                        handler.removeCallbacksAndMessages(null)
                        locationManager.removeUpdates(this)
                        callback(false, "CLEAN")
                    }
                }

                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            // TIMEOUT DE EMERGENCIA: Si tras 3.5 segundos nadie confirma Fake GPS, dejamos pasar al usuario
            handler.postDelayed({
                if (!hasResponded) {
                    hasResponded = true
                    locationManager.removeUpdates(locationListener)

                    // Auditoría rápida de respaldo sobre el caché del GPS que es donde se oculta el Fake GPS
                    val lastGpsLoc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    if (lastGpsLoc != null) {
                        val isMock = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) lastGpsLoc.isMock else lastGpsLoc.extras?.getBoolean("mockLocation", false) == true
                        if (isMock) {
                            callback(true, "Respaldo Cache GPS -> Detectado Simulador Pasivo")
                            return@postDelayed
                        }
                    }
                    callback(false, "CLEAN")
                }
            }, 3500)

            // ESCUCHA MULTITAREA: Nos suscribimos a la Red Y al Satélite simultáneamente
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, locationListener, android.os.Looper.getMainLooper())
            }
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, locationListener, android.os.Looper.getMainLooper())
            }

        } catch (e: SecurityException) {
            callback(true, "Error de Permisos: Falta habilitar ubicación en el dispositivo.")
        } catch (e: Exception) {
            callback(false, "CLEAN")
        }
    }
}