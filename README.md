# Fitness Tracker Android App

The **Fitness Tracker** app is an Android application designed to help users track their physical activity, including steps taken, calories burned, and distance traveled. The app integrates with Firebase for real-time database syncing and push notifications, and features a user-friendly interface with animations and robust step tracking functionalities.

---

## Features

- **Authentication & Authorization:**
  - User login and registration system with secure authentication via Firebase Authentication.
  
- **Animations:**
  - Smooth animations to enhance the user experience throughout the app implemented with Lottie.
  
- **Step Tracking:**
  - Tracks steps taken by the user in real-time (Using device's sensors).
  - Displays the total steps, calories burned, and distance traveled.
  - Calculations made in the code following internationally accepted values for distance, calories and number of steps.

- **Database Implementations:**
  - Firebase Realtime Database for real-time data syncing.
  - Firestore Database for structured and scalable data management.
  
- **Settings Implementations:**
  - Allows users to customize their preferences, including units (e.g., metric vs imperial), notification preferences, and more.

- **Notification Service:**
  - Sends notifications to users for milestones(**in development**), achievements(**in development**), updates(**in development**) and daily steps.

- **Step Tracking Service:**
  - Background service to continuously track steps, even when the app is closed.
  - Continuous integration with Firestore in order to save data and access it in real time.
  - In communication with notification service to notify the user.

---

## Requirements for Launching the Kotlin App in Android Studio

### Prerequisites

1. **Android Studio:**
   - Make sure you have the latest stable version of **Android Studio** installed. The app targets **SDK 35**, so your Android Studio should support at least that version. You can download it from [here](https://developer.android.com/studio).

2. **Java Development Kit (JDK):**
   - The project requires **JDK 17** for compatibility. Please ensure you have **JDK 17** installed on your machine.
   - Download **JDK 17** from the official [Oracle website](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html) or use an alternative like [OpenJDK](https://adoptopenjdk.net/).

3. **Android SDK:**
   - The app requires the **SDK 35** to build and run. You can manage SDK versions through the **SDK Manager** in Android Studio. Ensure that you have **SDK 35** installed.

4. **Kotlin:**
   - The project uses **Kotlin** for development. Android Studio should already have Kotlin support enabled by default. The project is configured with **JVM Target 17**, so ensure you have the correct Kotlin version installed.

5. **Gradle:**
   - The project uses **Gradle** as the build system. Ensure your Gradle version is compatible with the Android Studio version you are using. If needed, Android Studio will prompt you to update the Gradle version.

6. **Firebase Account:**
   - Set up a **Firebase project** in the [Firebase Console](https://console.firebase.google.com/).
   - Add Firebase services (Firestore, Realtime Database, Firebase Authentication, etc.) to your app. Download the `google-services.json` file from the Firebase Console and place it in the `app/` directory of your project.

---

### Additional Requirements Based on the `build.gradle` Configuration

1. **Minimum SDK Version:**
   - The app has a **minSdkVersion** of **26** (Android 8.0 Oreo). Ensure that the devices you are targeting run at least Android 8.0 or higher.

2. **Target SDK Version:**
   - The app targets **SDK 35**. Ensure your project is using the correct Android SDK and the devices you are testing on run at least **API Level 35**.

3. **Java Version Compatibility:**
   - The app uses **Java 17** as the **source** and **target** compatibility version. Ensure that **JDK 17** is correctly installed and Android Studio is configured to use it.

4. **Kotlin Configuration:**
   - The project is set to use **JVM Target 17** for Kotlin, ensuring compatibility with Java 17. Make sure your Kotlin plugin is up-to-date and supports this configuration.

---

### Getting Started

1. **Clone the Repository:**

   Clone the repository to your local machine:

   ```bash
   git clone https://github.com/jordanovvvv/fitnesstracker.git

