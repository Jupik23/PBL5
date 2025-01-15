package com.example.myapplication;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.anastr.speedviewlib.TubeSpeedometer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.Vector;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String ESP32_DEVICE_NAME = "ESP32test";
    private static final UUID ESP32_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private InputStream inputStream;

    private TubeSpeedometer tachometer1, tachometer2, tachometer3, tachometer4, tachometer5;
    private Button btnConnect, btnSave, btnDisplay, btnReset;
    private TextView tvT1, tvT2, tvT3, tvVoltage, tvCurrent, tvFileContents;

    private final Vector<String> recentResults = new Vector<>();
    private boolean isSaved = false; // Flaga do kontrolowania możliwości zapisu

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

        // Usunięcie jednostek z tachometrów
        removeUnitsFromTachometers();
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
     * Usuwa wyświetlanie jednostki z wszystkich tachometrów
     */
    private void removeUnitsFromTachometers() {
        tachometer1.setUnit("");
        tachometer2.setUnit("");
        tachometer3.setUnit("");
        tachometer4.setUnit("");
        tachometer5.setUnit("");
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
            Log.e(TAG, "Błąd podczas łączenia z ESP32", e);
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
                Log.d(TAG, "Otrzymano wiadomość: " + receivedMessage);

                if (receivedMessage.startsWith("(") && receivedMessage.endsWith(")")) {
                    String trimmedMessage = receivedMessage.substring(1, receivedMessage.length() - 1);
                    String[] values = trimmedMessage.split(",");

                    if (values.length == 5) {
                        try {
                            float T1 = Float.parseFloat(values[0]);
                            float T2 = Float.parseFloat(values[1]);
                            float T3 = Float.parseFloat(values[2]);
                            float Voltage = Float.parseFloat(values[3]);
                            float Current = Float.parseFloat(values[4]);

                            addResultToVector(T1, T2, T3, Voltage, Current);

                            runOnUiThread(() -> {
                                tachometer1.speedTo(T1); // Przekazywanie wartości do tachografu
                                tachometer2.speedTo(T2);
                                tachometer3.speedTo(T3);
                                tachometer4.speedTo(Voltage);
                                tachometer5.speedTo(Current);

                                tvT1.setText(String.format("T1: %.2f°C", T1));
                                tvT2.setText(String.format("T2: %.2f°C", T2));
                                tvT3.setText(String.format("T3: %.2f°C", T3));
                                tvVoltage.setText(String.format("Voltage: %.2f V", Voltage));
                                tvCurrent.setText(String.format("Current: %.2f A", Current));

                                // Resetowanie flagi zapisu po otrzymaniu nowych danych
                                isSaved = false;
                                btnSave.setEnabled(true);
                            });
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "Błąd parsowania danych", e);
                        }
                    } else {
                        Log.w(TAG, "Nieprawidłowa liczba wartości w wiadomości");
                    }
                }

            } catch (IOException e) {
                Log.e(TAG, "Błąd podczas odczytu danych", e);
                runOnUiThread(() -> Toast.makeText(this, "Connection lost", Toast.LENGTH_SHORT).show());
                break;
            }
        }
    }

    /**
     * Dodaje wynik do wektora, utrzymując maksymalnie 5 najnowszych wyników
     */
    private void addResultToVector(float T1, float T2, float T3, float Voltage, float Current) {
        String result = String.format("T1: %.2f°C, T2: %.2f°C, T3: %.2f°C, V: %.2f V, C: %.2f A",
                T1, T2, T3, Voltage, Current);

        if (recentResults.size() == 5) {
            recentResults.remove(0); // Usuń najstarszy wynik
        }

        recentResults.add(result); // Dodaj nowy wynik
    }

    /**
     * Zapisuje wyniki do pliku
     */
    private void saveResultsToFile() {
        if (recentResults.isEmpty()) {
            showToast("Brak wyników do zapisania");
            return;
        }

        if (isSaved) {
            showToast("Dane zostały już zapisane");
            return;
        }

        File file = new File(getExternalFilesDir(null), "results.txt");

        try (FileWriter writer = new FileWriter(file, true)) { // true oznacza dopisywanie do pliku
            for (String result : recentResults) {
                writer.write(result + "\n");
            }
            Toast.makeText(this, "Wyniki zapisane do: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
            isSaved = true;
            btnSave.setEnabled(false); // Wyłącz przycisk zapisu po zapisaniu
            Log.d(TAG, "Dane zostały zapisane do pliku");

            // Czyszczenie wektora po zapisaniu danych
            recentResults.clear();
            Log.d(TAG, "Wektor recentResults został wyczyszczony po zapisie");
        } catch (IOException e) {
            Log.e(TAG, "Błąd podczas zapisu do pliku", e);
            Toast.makeText(this, "Błąd podczas zapisu", Toast.LENGTH_SHORT).show();
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
            scanner.close();

            // Wyświetl dane w TextView
            tvFileContents.setText(fileContents.toString());
            Log.d(TAG, "Zawartość pliku została wyświetlona");
        } catch (IOException e) {
            Log.e(TAG, "Błąd podczas odczytu pliku", e);
            showToast("Błąd podczas odczytu pliku");
        }
    }

    /**
     * Resetuje zawartość pliku oraz wektora wyników
     */
    private void resetFile() {
        File file = new File(getExternalFilesDir(null), "results.txt");

        try (FileWriter writer = new FileWriter(file, false)) { // false nadpisuje plik
            writer.write(""); // Zapisuje pustą zawartość
            Toast.makeText(this, "Plik został zresetowany", Toast.LENGTH_SHORT).show();

            // Aktualizuj TextView po zresetowaniu
            tvFileContents.setText("Zawartość pliku została zresetowana.");

            // Czyść wektor wyników
            recentResults.clear();

            // Resetuj flagę zapisu i wyłącz przycisk zapisu
            isSaved = false;
            btnSave.setEnabled(false);

            Log.d(TAG, "Plik i wektor wyników zostały zresetowane");
        } catch (IOException e) {
            Log.e(TAG, "Błąd podczas resetowania pliku", e);
            Toast.makeText(this, "Błąd podczas resetowania pliku", Toast.LENGTH_SHORT).show();
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
                Log.d(TAG, "Połączenie Bluetooth zostało zamknięte");
            }
        } catch (IOException e) {
            Log.e(TAG, "Błąd podczas zamykania połączenia Bluetooth", e);
        }
    }
}
