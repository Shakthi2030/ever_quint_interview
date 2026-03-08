
# Meeting Room Booking Service

Simple Spring Boot API for managing meeting rooms and bookings.

## Run

```bash
mvn spring-boot:run
```

## APIs

Create Room
POST /rooms

List Rooms
GET /rooms

Create Booking
POST /bookings

List Bookings
GET /bookings

Cancel Booking
POST /bookings/{id}/cancel
