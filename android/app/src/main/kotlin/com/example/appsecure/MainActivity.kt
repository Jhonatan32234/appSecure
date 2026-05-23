package com.example.appsecure // Mantén tu paquete original aquí

import android.view.WindowManager
import android.os.Bundle
import io.flutter.embedding.android.FlutterActivity

class MainActivity: FlutterActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Activa la bandera de seguridad para toda la aplicación
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}