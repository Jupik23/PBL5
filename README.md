# PBL5

<h1>Overview</h1>
<p>
    ESP32 Bluetooth Data Logger is an Android application designed to connect to an ESP32 device via Bluetooth, receive real-time sensor data, display the data using customizable speedometers, and log the results into a file for future reference. The app ensures data integrity by allowing each set of results to be saved only once, preventing duplication and maintaining clean logs.
</p>

<h1>Features</h1>
<ul>
    <li>Bluetooth Connectivity: Easily connect to your ESP32 device to receive sensor data.</li>
    <li>Real-Time Data Visualization: Display temperature, voltage, and current readings using TubeSpeedometer.</li>
    <li>Data Logging: Save up to the latest 5 data entries to a file for future reference.</li>
    <li>Data Management: View saved data logs and reset logs to clear old entries.</li>
    <li>User-Friendly Interface: Intuitive and responsive UI for seamless interaction.</li>
</ul>

<h1>Installation</h1>

<h3>Prerequisites</h3>
<ul>
    <li>Android Studio</li>
    <li>Android Device or Emulator</li>
    <li>ESP32</li>
</ul>

<h3>Steps</h3>
<ol>
    <li><b>Clone Repo</b>
        <ul>
            <li>Run the following command: <code>git clone https://github.com/jupik23/PBL5.git</code></li>
        </ul>
    </li>
    <li><b>Open in Android Studio</b>
        <ul>
            <li>Click on <i>Open an existing Android Studio project.</i></li>
            <li>Navigate to the cloned repository folder and select it.</li>
        </ul>
    </li>
    <li><b>Build the Project</b>
        <ul>
            <li>Ensure all dependencies are resolved.</li>
            <li>Click on <i>Build > Make Project</i> or press <code>Ctrl+F9</code>.</li>
        </ul>
    </li>
    <li><b>Run the App</b>
        <ul>
            <li>Connect your Android device via USB or set up an emulator.</li>
            <li>Click on <i>Run > Run 'app'</i> or press the Run button.</li>
        </ul>
    </li>
</ol>

<h1>Additional Notes</h1>

<h3>Permissions</h3>
<p>
    Ensure that your <code>AndroidManifest.xml</code> includes the necessary permissions for Bluetooth connectivity. Since you're using <code>getExternalFilesDir(null)</code>, you do not need to request <code>WRITE_EXTERNAL_STORAGE</code> or <code>READ_EXTERNAL_STORAGE</code> permissions.
</p>
