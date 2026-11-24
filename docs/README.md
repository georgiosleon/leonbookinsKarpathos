# Developer documentation

This folder documents the current implementation. Treat the source code and
tests as authoritative when behavior changes.

- [Application deep dive](application-deep-dive.md): product purpose, stack,
  domain entities, API surface, frontend flows, deploy paths, and current
  boundaries (expands the overview in the top-level README).
- [Architecture and design decisions](architecture.md): system overview,
  request flow, class diagram, and guidance on Kafka and Protocol Buffers.
- [Frontend](frontend.md): React/Vite structure, UI behavior, and API
  integration.
- [Backend](backend.md): Spring Boot layers, API, persistence, security, licensing, and
  reports.
- [Tests](testing.md): existing test suites, what they cover, and how to run
  them.
- [Resource management](resources.md): admin workflow and how resources map
  to existing booking data.
- [i18n review checklist](i18n-review-checklist.md): keys and checks for
  native-speaker translation review.
- [System architecture diagram](diagrams/system-architecture.puml) and
  [booking/resource class diagram](diagrams/booking-resource-classes.puml):
  editable PlantUML sources.

The PlantUML files can be rendered with any PlantUML-compatible tool or
extension. Mermaid versions of the architecture and class diagrams are also
included in [architecture.md](architecture.md) for inline GitHub rendering.

The project is currently a single deployable Spring Boot application. The
React app is built into static assets served by Spring Boot. The normal
application path is synchronous HTTP/JSON between the browser and backend,
with SQLite persistence.
