# PBL5
<h1>Overview</h1>
<p>ESP32 Bluetooth Data Logger is an Android application designed to connect to an ESP32 device via Bluetooth, receive real-time sensor data, display the data using customizable speedometers, and log the results into a file for future reference. The app ensures data integrity by allowing each set of results to be saved only once, preventing duplication and maintaining clean logs.</p>
<h1>Features</h1>
<li>Bluetooth Connectivity: Easily connect to your ESP32 device to receive sensor data.</li>
<li>Real-Time Data Visualization: Display temperature, voltage, and current readings using TubeSpeedometer.</li>
<li>Data Logging: Save up to the latest 5 data entries to a file for future reference.</li>
<li>Data Management: View saved data logs and reset logs to clear old entries.</li>
<li>User-Friendly Interface: Intuitive and responsive UI for seamless interaction.</li></p>
<h1>Installation</h1>
<h3>Prerequisites</h3>
<li>Android Studio</li>
<li>Android Device or Emulator</li>
<li>ESP32</li>
<h3>Steps</h3>
<ol>Clone Repo</ol>
<li>git clone https://github.com/jupik23/PBL5.git</li>
<ol>Open in Android Studio</ol>
<li>Click on Open an existing Android Studio project.</li>
<li>Navigate to the cloned repository folder and select it.</li>
<ol>Build the Project</ol>
<li>Ensure all dependencies are resolved.</li>
<li>Click on Build > Make Project or press Ctrl+F9.</li>
<ol>Run the App</ol>
<li>Connect your Android device via USB or set up an emulator.</li>
<li>Click on Run > Run 'app' or press the Run button.</li>

<h1>Additional Notes</h1>
<h3>Permissions</h3>
<p>Ensure that your AndroidManifest.xml includes the necessary permissions for Bluetooth connectivity. Since you're using getExternalFilesDir(null), you do not need to request WRITE_EXTERNAL_STORAGE or READ_EXTERNAL_STORAGE permissions.</p>
