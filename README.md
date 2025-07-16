SafeHer is a microservices-based platform built using Spring Boot, Spring Cloud, Spring Security, and JWT, designed to enable crowd-sourced safety ratings and real-time reviews of places. With a modular, scalable architecture covering user management, place search, and safety reviews, it enhances situational awareness and promotes safer decision-making—especially for women.

Microservices Overview
 - Auth Service: Handles registration & login with JWT + BCrypt.
 - User Service: Manages user profiles; fetches user-specific ratings & places.
 - Place Service: Manages places; only creator or admin can update/delete.
 - Rating Service: Allows users to rate places; supports filtering by date range.
 - API Gateway: Validates JWT, routes requests securely to downstream services.

Key Features
 - JWT-based Authentication
 - Fault Tolerance with Resilience4j: Circuit Breaker, Retry, Rate Limiter
 - Average Place Ratings (out of 5) and filtering by Date Range
 - Role-Based Access (USER, ADMIN)
 - Centralized Configuration and Service Discovery (Spring Cloud Config & Eureka)
 - API Gateway via Spring Cloud Gateway

Tech Stack
 - Backend: Spring Boot, Spring Cloud, Spring Security
 - Databases: MySQL, MongoDB
