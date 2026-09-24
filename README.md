# EmoSense

## AI-Driven Multimodal Emotional Signal Analysis System

> **Understanding the emotions we don't always express.**

EmoSense is a Java-based AI application that analyzes emotional signals from **facial expressions and text**. It combines the outputs from both modalities to generate a multimodal emotional insight and provide supportive, non-clinical responses.

The project demonstrates the integration of **Artificial Intelligence, Computer Vision, Natural Language Processing, ONNX-based model inference, database management, and Generative AI** into a single desktop application.

---

## 📌 Project Overview

EmoSense allows users to complete an emotional check-in by:

- Uploading a facial image
- Entering their thoughts or feelings as text
- Using facial input, text input, or both
- Analyzing emotional signals using AI models
- Viewing emotion predictions and confidence values
- Receiving a combined multimodal insight
- Viewing previous check-ins and insights
- Interacting with an AI Companion

EmoSense is designed for **emotional awareness and self-reflection**. It is an academic prototype and does **not provide medical or psychological diagnosis**.

---

## ✨ Key Features

- 🔐 User Sign Up and Sign In
- 📷 Facial Expression Analysis
- 📝 Text Emotion Analysis
- 🧠 Multimodal Emotional Signal Analysis
- 📊 Emotion Confidence Visualization
- 📜 Check-In History
- 📈 Personal Insights
- 💬 AI Companion
- 🗄️ MySQL Database Integration
- 🔄 Offline Fallback Support
- 🛡️ Privacy and Responsible AI Considerations

---

## 🖼️ Screenshots

### 🔐 Sign In

<!-- Add your Sign In screenshot here -->

![Sign In](images/signin.png)

---

### 🏠 Dashboard

<!-- Add your Dashboard screenshot here -->

![Dashboard](images/dashboard.png)

---

### 📝 Emotional Check-In

<!-- Add your Check-In screenshot here -->

![Check-In](images/checkin.png)

---

### 📊 Analysis Results

<!-- Add your Results screenshot here -->

![Results](images/results.png)

---

### 💬 AI Companion

<!-- Add your AI Companion screenshot here -->

![AI Companion](images/companion.png)

---

## 🤖 AI Models Used

### 1. Facial Emotion Recognition

**Model:** `onnx-community/face-emotion-detection-ONNX`

Used to analyze facial expressions from uploaded images.

The model recognizes seven facial emotion classes:

- Angry
- Disgust
- Fear
- Happy
- Sad
- Surprise
- Neutral

---

### 2. Text Emotion Recognition

**Model:** `minuva/MiniLMv2-goemotions-v2-onnx`

Used to identify emotional signals from user-provided text.

The model is based on the **GoEmotions** dataset and provides emotion probabilities that are mapped by EmoSense into broader emotional signals for multimodal analysis.

---

### 3. Face Detection

**Model:** `UltraFace RFB-320`

Used to detect and locate the face in an uploaded image before facial emotion analysis.

---

## 🔄 System Workflow

```text
                    User Check-In
                         │
             ┌───────────┴───────────┐
             │                       │
       Facial Image             Text Input
             │                       │
             ▼                       ▼
      Face Detection          Text Processing
             │                       │
             ▼                       ▼
    Facial Emotion Model    Text Emotion Model
             │                       │
             └───────────┬───────────┘
                         ▼
                Multimodal Analysis
                         │
                         ▼
                Emotional Insight
                         │
             ┌───────────┴───────────┐
             ▼                       ▼
          Results              AI Companion
             │
             ▼
       History & Insights

## 🧩 System Architecture

```text
                    JavaFX User Interface
                             │
                             ▼
                    Application Services
                             │
             ┌───────────────┼───────────────┐
             │               │               │
             ▼               ▼               ▼
       Facial Analysis  Text Analysis   AI Companion
             │               │               │
             ▼               ▼               ▼
          ONNX Model      ONNX Model     Gemini API /
                                            Local Fallback
             │               │
             └───────┬───────┘
                     ▼
              Multimodal Analysis
                     │
                     ▼
                MySQL Database



               

  
