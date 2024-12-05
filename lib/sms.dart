import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:smsapp/sms-detail.dart';

class SmsListPage extends StatefulWidget {
  @override
  _SmsListPageState createState() => _SmsListPageState();
}

class _SmsListPageState extends State<SmsListPage> {
  static const platform = MethodChannel('sms.channel');
  Map<String, List<Map<String, String>>> groupedSms = {};

  Future<void> getSms() async {
    try {
      final List<dynamic> result = await platform.invokeMethod('getSms');
      final smsList = result.map((e) => Map<String, String>.from(e)).toList();

      final Map<String, List<Map<String, String>>> grouped = {};
      for (var sms in smsList) {
        final address = sms['address'] ?? "No Address";
        if (!grouped.containsKey(address)) {
          grouped[address] = [];
        }
        grouped[address]!.add(sms);
      }

      setState(() {
        groupedSms = grouped;
      });
    } on PlatformException catch (e) {
      print("Error: '${e.message}'");
    }
  }

  @override
  void initState() {
    super.initState();
    getSms();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text("SMS List"),
      ),
      body: groupedSms.isEmpty
          ? Center(child: CircularProgressIndicator())
          : ListView.builder(
        itemCount: groupedSms.keys.length,
        itemBuilder: (context, index) {
          final address = groupedSms.keys.elementAt(index);
          return ListTile(
            title: Text(address),
            subtitle: Text("${groupedSms[address]!.length} messages"),
            onTap: () {
              Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (context) => SmsDetailPage(
                    address: address,
                    smsList: groupedSms[address]!,
                  ),
                ),
              );
            },
          );
        },
      ),
    );
  }
}