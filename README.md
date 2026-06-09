# BeSafe – Campus Safety Mobile Application

## Overview

BeSafe is a real-time campus safety mobile application designed to improve emergency communication between students, campus security, and administrators. It provides fast SOS alerts, incident reporting, SafeWalk navigation, and real-time safety monitoring using location-based services and intelligent risk analysis.

The system aims to reduce emergency response time and create a safer university environment through instant communication and geolocation tracking.

---

##  Project Objectives

- Improve campus emergency response time
- Provide real-time SOS alert system
- Enable location-based incident reporting
- Enhance communication between students and security
- Identify high-risk campus zones using data analysis

---

##  Target Users

- Students (primary users)
- Campus Security Personnel
- University Administrators
- Visitors and staff

---

##  Key Features

### Student Features
- SOS emergency alert system
- SafeWalk navigation assistance
- Incident reporting with images
- Real-time location sharing
- Safety notifications

### Security Features
- Live emergency incident feed
- Dispatch and response management
- Incident tracking and updates
- Mark incidents as resolved
- Real-time monitoring dashboard

### Admin Features
- User management
- System analytics dashboard
- Campus-wide safety broadcasts
- Incident overview reports

---

##  Intelligent Features

- Risk Zone generation using spatial clustering
- Real-time incident-based heat mapping
- Location-aware emergency routing

---

##  System Architecture

- **Frontend:** Android (Java, Android SDK)
- **Backend:** Supabase (PostgreSQL, Authentication, Storage)
- **Communication:** REST API using Retrofit
- **Real-time updates:** Supabase Realtime Database

---

##  Technologies Used

- Java (Android Development)
- Android SDK
- Supabase (Backend-as-a-Service)
- PostgreSQL
- Google Maps SDK
- Google Directions API
- Google FusedLocationProvider API
- Material Design Components
- Glide (Image Loading)
- OneSignal (Push Notifications)

---

##  APIs & Services

- Google Maps SDK – Campus mapping and visualization
- Google Directions API – SafeWalk routing
- FusedLocationProvider – GPS tracking
- OneSignal – Push notifications and alerts

---

##  Architecture

- MVVM architecture pattern
- Modular package structure
- RESTful API integration
- Role-Based Access Control (Student / Security / Admin)

---

##  Testing

- Integration testing with Supabase backend
- Role-Based Access Control (RBAC) testing
- Performance testing using pagination optimization
- Real-time SOS and reporting validation

---

##  Scrum Methodology

### Sprint 1
- Authentication and login system

### Sprint 2
- Emergency SOS and dispatch workflow

### Sprint 3
- Profile management and media uploads

---

##  Project Structure

- Activities: Login, Register, Alerts, Reports
- Fragments: Map, Reports, Dashboard
- Models: Data transfer objects
- Network: Retrofit API services

---

##  Conclusion

BeSafe is a smart campus safety system that improves emergency response times through real-time SOS alerts, location tracking, and structured communication between students and security personnel. The system uses intelligent mapping and role-based access control to ensure efficient and secure incident management.

---

##  Developer

Thobani Alex Shezi  
Advanced Diploma in ICT  
Durban University of Technology
