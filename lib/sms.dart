import 'package:flutter/material.dart';

class SmsListPage extends StatelessWidget {
  const SmsListPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text("Sms"),
      ),
      body: Center(child: Text('Welcome user'),)
    );
  }
}
