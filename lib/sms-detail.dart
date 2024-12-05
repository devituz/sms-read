
import 'package:flutter/material.dart';

class SmsDetailPage extends StatelessWidget {
  final String address;
  final List<Map<String, String>> smsList;

  const SmsDetailPage({required this.address, required this.smsList});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text("SMS from: $address"),
      ),
      body: ListView.builder(
        itemCount: smsList.length,
        itemBuilder: (context, index) {
          final sms = smsList[index];
          return ListTile(
            title: Text(sms['body'] ?? "No Body"),
            subtitle: Text("Date: ${sms['date'] ?? 'Unknown'}"),
          );
        },
      ),
    );
  }
}