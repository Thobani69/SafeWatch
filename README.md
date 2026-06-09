# SafeWatch

## Overview

SafeWatch is a real-time emergency reporting and dispatch platform designed to improve community safety by connecting residents directly with the appropriate emergency responders.

The system enables residents to report emergencies using GPS location data, while responders receive automated alerts based on incident type, area code, and specialization. SafeWatch streamlines emergency response through intelligent routing, live communication, escalation protocols, and administrative analytics.

## Mission

To provide eThekwini Municipality with a real-time intelligence and emergency dispatch platform that directly connects community residents to the exact responders who can help them.

## Features

### Resident Features

* User registration and authentication
* Emergency incident reporting
* GPS location pinning
* Anonymous crime reporting
* Incident tracking
* Communication with responders
* Community incident calendar

### Responder Features

* Secure login system
* Duty Lock (Clock In / Clock Out)
* Area-specific incident feed
* Incident dispatch management
* Live communication logs
* Internal responder notes
* Emergency escalation protocol

### Administrator Features

* System-wide analytics dashboard
* Incident management and moderation
* Bulk responder account creation
* Emergency email broadcasts
* PDF report generation
* Community activity monitoring

## Core Functionalities

### Smart Dispatch System

SafeWatch automatically routes incident alerts to the correct responders based on:

* Incident category
* Area code
* Responder specialization
* Duty status

### GPS-Based Reporting

Residents can drop a digital pin on the map to provide precise emergency locations.

### Live Communication

Residents and responders can communicate through a secure, timestamped communication log.

### Escalation Protocol

Responders can escalate incidents when additional support is required, automatically notifying nearby units.

### Analytics & Reporting

Administrators can monitor incident trends through charts, calendars, and downloadable PDF reports.

## Technology Stack

### Backend

* Python 3
* Flask
* Flask-Login
* Flask-WTF
* SQLAlchemy
* SQLite
* Flask-Mail
* FPDF

### Frontend

* HTML5
* CSS3
* JavaScript

### Libraries & APIs

* Leaflet.js
* OpenStreetMap Nominatim API
* FullCalendar.js
* Chart.js

## Database Design

The system uses a relational database structure consisting of:

* User
* Incident
* AreaCode

Relationships are managed using SQLAlchemy and Foreign Keys to ensure secure ownership and traceability of incidents.

## Project Architecture

1. Resident submits emergency report.
2. System identifies incident type and area code.
3. Appropriate responders receive automated email alerts.
4. Responder dispatches to incident.
5. Communication occurs through the live log.
6. Incident is resolved or escalated.
7. Incident is archived for reporting and analytics.

## Future Improvements

* Mobile application integration
* Real-time push notifications
* SMS emergency alerts
* AI-powered incident prioritization
* Live responder tracking
* Multi-language support

## Author

**Thobani Alex Shezi**

Advanced Diploma in ICT – Application Development

Durban University of Technology

## License

This project is developed for educational and portfolio purposes.
