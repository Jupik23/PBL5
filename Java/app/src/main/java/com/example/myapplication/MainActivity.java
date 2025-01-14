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

import com.github.anastr.speedviewlib.TubeSpeedometer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MainActivity extends AppCompatActivity {

    private static final String ESP32_DEVICE_NAME = "ESP32test";
    private static final UUID ESP32_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private InputStream inputStream;

    private TubeSpeedometer tachometer1, tachometer2, tachometer3, tachometer4, tachometer5;
    private Button btnConnect, btnSave, btnDisplay, btnReset;
    private TextView tvT1, tvT2, tvT3, tvVoltage, tvCurrent, tvFileContents;

    private final ConcurrentLinkedQueue<String> recentResults = new ConcurrentLinkedQueue<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicjalizacja komponentów UI
        initUI();

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        btnConnect.setOnClickListener(v -> connectToESP32());
        btnSave.setOnClickListener(v -> saveResultsToFile());
        btnDisplay.setOnClickListener(v -> displayResultsFromFile());
        btnReset.setOnClickListener(v -> resetFile());
    }

    /**
     * Inicjalizacja komponentów UI
     */
    private void initUI() {
        tachometer1 = findViewById(R.id.tachometer1);
        tachometer2 = findViewById(R.id.tachometer2);
        tachometer3 = findViewById(R.id.tachometer3);
        tachometer4 = findViewById(R.id.tachometer4);
        tachometer5 = findViewById(R.id.tachometer5);
        btnConnect = findViewById(R.id.btnConnect);
        btnSave = findViewById(R.id.btnSave);
        btnDisplay = findViewById(R.id.btnDisplay);
        btnReset = findViewById(R.id.btnReset);
        tvT1 = findViewById(R.id.tvT1);
        tvT2 = findViewById(R.id.tvT2);
        tvT3 = findViewById(R.id.tvT3);
        tvVoltage = findViewById(R.id.tvVoltage);
        tvCurrent = findViewById(R.id.tvCurrent);
        tvFileContents = findViewById(R.id.tvFileContents);

        // Konfiguracja tachometrów
        configureTachometer(tachometer1, 40);
        configureTachometer(tachometer2, 40);
        configureTachometer(tachometer3, 40);
        configureTachometer(tachometer4, 5);
        configureTachometer(tachometer5, 2);
        tachometer1.setUnit("");
        tachometer2.setUnit("");
        tachometer3.setUnit("");
        tachometer4.setUnit("");
        tachometer5.setUnit("");
    }

    /**
     * Konfiguruje pojedynczy tachometr
     *
     * @param tachometer Tachometr do skonfigurowania
     * @param maxSpeed   Maksymalna prędkość dla tachometru
     */
    private void configureTachometer(TubeSpeedometer tachometer, int maxSpeed) {
        tachometer.setWithTremble(false);
        tachometer.setMaxSpeed(maxSpeed);
    }

    /**
     * Łączy się z urządzeniem ESP32 przez Bluetooth
     */
    @SuppressLint("MissingPermission")
    private void connectToESP32() {
        if (bluetoothAdapter == null) {
            showToast("Bluetooth nie jest obsługiwany");
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            showToast("Proszę włączyć Bluetooth");
            return;
        }

        BluetoothDevice esp32Device = findESP32Device();
        if (esp32Device == null) {
            showToast("ESP32 nie znaleziony");
            return;
        }

        try {
            bluetoothSocket = esp32Device.createRfcommSocketToServiceRecord(ESP32_UUID);
            bluetoothSocket.connect();
            inputStream = bluetoothSocket.getInputStream();
            showToast("Połączono z ESP32");
            new Thread(this::listenForData).start();
        } catch (IOException e) {
            e.printStackTrace();
            showToast("Błąd podczas łączenia");
        }
    }

    /**
     * Szuka sparowanego urządzenia ESP32
     *
     * @return Znalezione urządzenie lub null
     */
    private BluetoothDevice findESP32Device() {
        for (BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
            if (ESP32_DEVICE_NAME.equals(device.getName())) {
                return device;
            }
        }
        return null;
    }

    /**
     * Słucha danych przesyłanych przez Bluetooth
     */
    private void listenForData() {
        byte[] buffer = new byte[1024];
        int bytes;

        while (true) {
            try {
                if (inputStream == null) break;
                bytes = inputStream.read(buffer);
                final String receivedMessage = new String(buffer, 0, bytes).trim();

                if (isValidMessage(receivedMessage)) {
                    processReceivedMessage(receivedMessage);
                }

            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> showToast("Połączenie utracone"));
                break;
            }
        }
    }

    /**
     * Sprawdza, czy wiadomość ma prawidłowy format
     *
     * @param message Wiadomość do sprawdzenia
     * @return True jeśli prawidłowa, false w przeciwnym razie
     */
    private boolean isValidMessage(String message) {
        return message.startsWith("(") && message.endsWith(")") && message.length() > 2;
    }

    /**
     * Przetwarza otrzymaną wiadomość
     *
     * @param message Wiadomość do przetworzenia
     */
    private void processReceivedMessage(String message) {
        String trimmedMessage = message.substring(1, message.length() - 1);
        String[] values = trimmedMessage.split(",");

        if (values.length == 5) {
            try {
                float T1 = Float.parseFloat(values[0]);
                float T2 = Float.parseFloat(values[1]);
                float T3 = Float.parseFloat(values[2]);
                float Voltage = Float.parseFloat(values[3]);
                float Current = Float.parseFloat(values[4]);

                addResultToQueue(T1, T2, T3, Voltage, Current);

                runOnUiThread(() -> updateUI(T1, T2, T3, Voltage, Current));
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Dodaje wynik do kolejki, utrzymując maksymalnie 5 najnowszych wyników
     */
    private void addResultToQueue(float T1, float T2, float T3, float Voltage, float Current) {
        String result = String.format("T1: %.2f, T2: %.2f, T3: %.2f, V: %.2f V, C: %.2f A",
                T1, T2, T3, Voltage, Current);

        if (recentResults.size() == 5) {
            recentResults.poll(); // Usuń najstarszy wynik
        }

        recentResults.add(result);
    }

    /**
     * Aktualizuje interfejs użytkownika z otrzymanymi danymi
     */
    private void updateUI(float T1, float T2, float T3, float Voltage, float Current) {
        tachometer1.speedTo(T1);
        tachometer2.speedTo(T2);
        tachometer3.speedTo(T3);
        tachometer4.speedTo(Voltage);
        tachometer5.speedTo(Current);

        tvT1.setText(String.format("T1: %.2f°C", T1));
        tvT2.setText(String.format("T2: %.2f°C", T2));
        tvT3.setText(String.format("T3: %.2f°C", T3));
        tvVoltage.setText(String.format("Voltage: %.2f V", Voltage));
        tvCurrent.setText(String.format("Current: %.2f A", Current));
    }

    /**
     * Zapisuje wyniki do pliku
     */
    private void saveResultsToFile() {
        if (recentResults.isEmpty()) {
            showToast("Brak wyników do zapisania");
            return;
        }

        File file = new File(getExternalFilesDir(null), "results.txt");

        try (FileWriter writer = new FileWriter(file, false)) { // false nadpisuje plik
            for (String result : recentResults) {
                writer.write(result + "\n");
            }
            showToast("Wyniki zapisane do: " + file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
            showToast("Błąd podczas zapisu");
        }
    }

    /**
     * Wyświetla zawartość pliku w TextView
     */
    private void displayResultsFromFile() {
        File file = new File(getExternalFilesDir(null), "results.txt");

        if (!file.exists()) {
            showToast("Plik nie istnieje");
            return;
        }

        try (Scanner scanner = new Scanner(file)) {
            StringBuilder fileContents = new StringBuilder();

            while (scanner.hasNextLine()) {
                fileContents.append(scanner.nextLine()).append("\n");
            }

            tvFileContents.setText(fileContents.toString());
        } catch (IOException e) {
            e.printStackTrace();
            showToast("Błąd podczas odczytu pliku");
        }
    }

    /**
     * Resetuje zawartość pliku
     */
    private void resetFile() {
        File file = new File(getExternalFilesDir(null), "results.txt");

        try (FileWriter writer = new FileWriter(file, false)) { // false nadpisuje plik
            writer.write(""); // Zapisuje pustą zawartość
            showToast("Plik został zresetowany");
            tvFileContents.setText("Zawartość pliku została zresetowana.");
        } catch (IOException e) {
            e.printStackTrace();
            showToast("Błąd podczas resetowania pliku");
        }
    }

    /**
     * Wyświetla krótkie powiadomienie (Toast)
     *
     * @param message Wiadomość do wyświetlenia
     */
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeBluetoothConnection();
    }

    /**
     * Zamyka połączenie Bluetooth
     */
    private void closeBluetoothConnection() {
        try {
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
