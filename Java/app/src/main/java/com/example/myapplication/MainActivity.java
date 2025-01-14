package com.example.myapplication;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String ESP32_DEVICE_NAME = "ESP32test"; // ESP32 Bluetooth name
    private static final UUID ESP32_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // Standard UUID for Bluetooth SPP

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private InputStream inputStream;

    private Button btnConnect;
    private TextView gauge1, gauge2, gauge3, gauge4, tvOutput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnConnect = findViewById(R.id.btnConnect);
        gauge1 = findViewById(R.id.gauge1);
        gauge2 = findViewById(R.id.gauge2);
        gauge3 = findViewById(R.id.gauge3);
        gauge4 = findViewById(R.id.gauge4);
        tvOutput = findViewById(R.id.tvOutput);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        btnConnect.setOnClickListener(v -> connectToESP32());
    }

    @SuppressLint("MissingPermission")
    private void connectToESP32() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Please enable Bluetooth", Toast.LENGTH_SHORT).show();
            return;
        }

        BluetoothDevice esp32Device = null;
        for (BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
            if (ESP32_DEVICE_NAME.equals(device.getName())) {
                esp32Device = device;
                break;
            }
        }

        if (esp32Device == null) {
            Toast.makeText(this, "ESP32 not found", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            bluetoothSocket = esp32Device.createRfcommSocketToServiceRecord(ESP32_UUID);
            bluetoothSocket.connect();

            inputStream = bluetoothSocket.getInputStream();

            Toast.makeText(this, "Connected to ESP32", Toast.LENGTH_SHORT).show();

            // Start a thread to listen for incoming data
            new Thread(this::listenForData).start();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Connection failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void listenForData() {
        byte[] buffer = new byte[1024];
        int bytes;

        while (true) {
            try {
                if (inputStream == null) break;

                bytes = inputStream.read(buffer);
                final String receivedMessage = new String(buffer, 0, bytes).trim();

                // Check if the received message matches the expected format
                if (receivedMessage.startsWith("(") && receivedMessage.endsWith(")")) {
                    String trimmedMessage = receivedMessage.substring(1, receivedMessage.length() - 1); // Remove '(' and ')'
                    String[] values = trimmedMessage.split(",");

                    if (values.length == 5) {
                        try {
                            // Parse the floating-point values
                            float T1 = Float.parseFloat(values[0]);
                            float T2 = Float.parseFloat(values[1]);
                            float T3 = Float.parseFloat(values[2]);
                            float Voltage = Float.parseFloat(values[3]);
                            float Current = Float.parseFloat(values[4]);

                            // Update the UI with parsed data
                            runOnUiThread(() -> {
                                gauge1.setText(String.format("T1: %.1f °C", T1));
                                gauge2.setText(String.format("T2: %.1f °C", T2));
                                gauge3.setText(String.format("T3: %.1f °C", T3));
                                gauge4.setText(String.format("Voltage: %.2f V", Voltage));
                                tvOutput.setText(String.format("Current: %.2f A", Current));
                            });
                        } catch (NumberFormatException e) {
                            e.printStackTrace(); // Handle parsing errors
                        }
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Connection lost", Toast.LENGTH_SHORT).show());
                break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
