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

            // Seleccionamos el mejor proveedor activo (GPS por hardware o Red)
            val provider = if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                LocationManager.GPS_PROVIDER
            } else {
                providers[0]
            }

            // Pedimos una sola actualización instantánea al chip
            locationManager.requestSingleUpdate(provider, object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    // 1. Evaluamos si es una ubicación simulada (Mock) de forma segura y compatible
                    val isMock = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        location.isMock
                    } else {
                        // Corrección: Usamos ?. para evitar el error de nulidad y "mockLocation" como String directo
                        location.extras?.getBoolean("mockLocation", false) == true
                    }

                    // 2. Construimos el reporte detallado del origen de la señal
                    val infoCulpable = "Proveedor: ${location.provider} | Precision: ${location.accuracy}m"

                    callback(isMock, infoCulpable)
                }

                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }, Looper.getMainLooper())

        } catch (e: SecurityException) {
            callback(true, "Error de Permisos: Falta habilitar ubicación en el dispositivo.")
        } catch (e: Exception) {
            callback(false, "CLEAN") // Ante una falla física crítica, no bloqueamos el login
        }
    }
}