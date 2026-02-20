# 📱 PhoneMotionDataAndroidApp

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)
![WebSockets](https://img.shields.io/badge/WebSockets-010101?style=for-the-badge&logo=socket.io&logoColor=white)
![Android Studio](https://img.shields.io/badge/Android%20Studio-3DDC84?style=for-the-badge&logo=android-studio&logoColor=white)

**PhoneMotionDataAndroidApp** is a native Android application developed to collect and log motion sensor data from mobile devices. It leverages the phone's built-in sensors (e.g., accelerometer, gyroscope) to gather real-time movement data and send it to a server. The app was developed and used as part of my engineering thesis, which focused on a game utilizing real-time player movement. It demonstrates my understanding of Android development, hardware sensors, real-time data collection, server communication. The project can be applied to fitness tracking, motion analysis, data science, or machine learning projects related to human movement.

---

## 🎯 Purpose

This project was developed and used as part of my engineering thesis, which focused on a game utilizing real-time player movement. It demonstrates my understanding of Android development, hardware sensors, real-time data collection. The project can be applied to motion analysis, data science, or machine learning projects related to human movement.

---

## ✨ Features

- 📲 **Live Sensor Data Capture**  
  Collects real-time data from multiple phone sensors such as:
  - Accelerometer
  - Gyroscope
  - Magnetometer
  - Linear acceleration

- ⏱ **Start/Stop Recording**  
  Simple controls to start and stop data collection at any time.

- 📁 **Data Communication**  
  Logged data is transfered to a computer through a WebSocket.

- 🧪 **Experimental Use Cases**  
  Suitable for motion recognition, physical activity classification, or academic research.

---

## 🛠️ Technologies Used

- **Language**: Java / Kotlin  
- **Platform**: Android SDK  
- **APIs**: Android SensorManager, WebScoket
- **Tools**: Android Studio, 

---

## 🚀 How to Run the App

1. **Clone the repository:**
   ```bash
   git clone https://github.com/arturzelek1/PhoneMotionDataAndroidApp.git
   cd PhoneMotionDataAndroidApp
2. **Open in Android Studio**
- Open as an existing Android project.
3. **Connect an Android device**
4. **Run project**
5. **Use app:**
- Start motion tracking
- Track live motion data
## 🧠 What I Learned
- Working with Android's sensor APIs

- Managing foreground/background services

- Writing data to external storage safely

- Building a responsive and efficient mobile UI
