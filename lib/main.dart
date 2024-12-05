import 'package:flutter/material.dart';
import 'package:smsapp/sms.dart';
import 'package:permission_handler/permission_handler.dart';


void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(MyApp());
}





class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {

  @override
  void initState() {
    requestPermissions();
    super.initState();
  }

  Future<void> requestPermissions() async {
    await [
      Permission.sms,
    ].request();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      home: SmsListPage(),
    );
  }
}

