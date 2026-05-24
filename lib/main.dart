import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:permission_handler/permission_handler.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'Login Ultra Seguro',
      theme: ThemeData(
        primarySwatch: Colors.blue,
        useMaterial3: true,
      ),
      home: const LoginScreen(),
    );
  }
}

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});
  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  static const platform = MethodChannel('com.example.seguridad/gps');

  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();

  bool _isChecking = true;
  bool _isFakeGpsDetected = false;
  String _culpableInfo = ""; // Aquí guardaremos lo que descubra el código nativo

  @override
  void initState() {
    super.initState();
    _verificarEntornoSeguro();
  }

  Future<void> _verificarEntornoSeguro() async {
    setState(() {
      _isChecking = true;
    });

    // 1. Solicitar permisos de ubicación en Flutter
    var status = await Permission.location.request();

    if (status.isGranted) {
      try {
        // 2. Invocar el canal nativo (Ahora nos devuelve un String)
        final String resultadoAndroid = await platform.invokeMethod('checkFakeGPS');

        setState(() {
          if (resultadoAndroid == "CLEAN") {
            _isFakeGpsDetected = false;
          } else {
            _isFakeGpsDetected = true;
            _culpableInfo = resultadoAndroid; // Guardamos los datos técnicos del software sospechoso
          }
          _isChecking = false;
        });
      } on PlatformException catch (e) {
        setState(() {
          _isFakeGpsDetected = true;
          _culpableInfo = "Error en el canal nativo: ${e.message}";
          _isChecking = false;
        });
      }
    } else {
      setState(() {
        _isFakeGpsDetected = true;
        _culpableInfo = "Permiso de ubicación denegado por el usuario.";
        _isChecking = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    // 1. Pantalla de carga mientras se consulta al hardware
    if (_isChecking) {
      return const Scaffold(
        body: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              CircularProgressIndicator(),
              SizedBox(height: 16),
              Text('Auditando integridad del GPS...'),
            ],
          ),
        ),
      );
    }

    // 2. PANTALLA DE BLOQUEO DE SEGURIDAD (Muestra el culpable en pantalla)
    if (_isFakeGpsDetected) {
      return Scaffold(
        backgroundColor: Colors.red.shade900,
        body: Padding(
          padding: const EdgeInsets.all(32.0),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Icon(Icons.gpp_bad_rounded, size: 90, color: Colors.white),
              const SizedBox(height: 24),
              const Text(
                'VIOLACIÓN DE SEGURIDAD',
                style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: Colors.white),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 16),
              const Text(
                'Se ha detectado una alteración activa en los servicios de geolocalización de este dispositivo.',
                style: TextStyle(fontSize: 15, color: Colors.white70),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 24),

              // --- CONTENEDOR AUDITOR QUE MUESTRA EL ERROR ---
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: Colors.black.withOpacity(0.3),
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(color: Colors.white30),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'REPORTE TÉCNICO DEL DIAGNÓSTICO:',
                      style: TextStyle(color: Colors.amber, fontWeight: FontWeight.bold, fontSize: 12),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      _culpableInfo,
                      style: const TextStyle(color: Colors.white, fontFamily: 'monospace', fontSize: 13),
                    ),
                  ],
                ),
              ),

              const SizedBox(height: 32),
              ElevatedButton.icon(
                onPressed: _verificarEntornoSeguro,
                icon: const Icon(Icons.autorenew_rounded),
                label: const Text('Volver a auditar entorno'),
                style: ElevatedButton.styleFrom(
                  backgroundColor: Colors.white,
                  foregroundColor: Colors.red.shade900,
                  padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
                ),
              )
            ],
          ),
        ),
      );
    }

    // 3. INTERFAZ DE LOGIN NORMAL (Si pasa los controles de seguridad)
    return Scaffold(
      backgroundColor: Colors.white,
      body: Center(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24.0),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Icon(Icons.shield_rounded, size: 80, color: Colors.blue),
              const SizedBox(height: 16),
              const Text(
                'Acceso Seguro Verificado',
                textAlign: TextAlign.center,
                style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold),
              ),
              const Text(
                'Dispositivo libre de modificaciones de GPS',
                textAlign: TextAlign.center,
                style: TextStyle(color: Colors.green, fontWeight: FontWeight.w500),
              ),
              const SizedBox(height: 32),
              TextField(
                controller: _emailController,
                decoration: const InputDecoration(
                  labelText: 'Correo Electrónico',
                  border: OutlineInputBorder(),
                  prefixIcon: Icon(Icons.email_outlined),
                ),
              ),
              const SizedBox(height: 16),
              TextField(
                controller: _passwordController,
                obscureText: true,
                decoration: const InputDecoration(
                  labelText: 'Contraseña',
                  border: OutlineInputBorder(),
                  prefixIcon: Icon(Icons.lock_outline),
                ),
              ),
              const SizedBox(height: 24),
              ElevatedButton(
                onPressed: () {},
                style: ElevatedButton.styleFrom(padding: const EdgeInsets.symmetric(vertical: 16)),
                child: const Text('Ingresar'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}