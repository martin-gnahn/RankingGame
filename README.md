# RankingGame

RankingGame ist eine Fullstack-Implementierung eines multiplayerfähigen Partyspiels, inspiriert vom Prinzip von *Top
Ten*: Eine Gruppe bekommt eine Frage mit einer Skala, alle Spieler geben verdeckt eine Antwort ab, und der aktuelle
Captain sortiert die Antworten anschließend in die vermutete Reihenfolge.

Das Projekt ist als Lern- und Portfolio-Projekt gebaut, aber mit bewusst seriöser technischer Substanz: persistente
Räume, sessionbasierte Spieler-Authentifizierung, Live-Updates per WebSocket, Flyway-Migrationen, eine getrennte
Domain-/Use-Case-Schicht und Tests auf mehreren Ebenen.

## Aktueller Stand

Implementiert ist ein spielbarer Kern-Flow:

- Raum erstellen und einem Raum per Code beitreten
- Host-Erkennung und Start des Spiels durch den Host
- Lobby mit Spielerstatus und Live-Aktualisierung
- sessionbasierte Zugriffskontrolle über `X-Player-Session-Token`
- Start eines Ranking-Game mit mindestens zwei Spielern
- zufällige Kartenwerte von 1 bis 10 pro Runde und Spieler
- Antwortabgabe pro Spieler und Runde
- automatischer Wechsel von `ANSWER_SUBMISSION` nach `SORTING`, sobald alle Spieler geantwortet haben
- Sortieren der Antworten durch den Captain
- Live-Events für Spielerbeitritt, Rejoin, Disconnect, Spielstart, Antwortabgabe, Sortierstart, Ranking und Chat
- Chat in Lobby und Spielansicht
- deutsche UI-Texte via `@ngx-translate`
- OpenAPI/Swagger-Dokumentation für die REST API

Noch nicht fertig ist ein kompletter Spielabschluss über mehrere Runden mit finaler Punkteabrechnung. Die Datenbank
enthält bereits Grundlagen für Scores, und der Domain-Code kennt Startpunkte, aber die aktuelle UI und der aktuelle
Use-Case-Flow konzentrieren sich auf Raum, erste Runde, Antwortphase, Sortierphase und Live-Synchronisation.

## Tech Stack

Backend:

- Java 21
- Spring Boot 4.1
- Spring Web MVC für REST
- Spring WebSocket mit STOMP
- Spring Data JPA
- PostgreSQL
- Flyway
- springdoc-openapi
- JUnit, MockMvc, Mockito, Testcontainers

Frontend:

- Angular 20
- Angular Standalone Components
- Angular Router
- Angular Material/CDK
- RxJS
- `@stomp/stompjs`
- `@ngx-translate/core`
- Karma/Jasmine für Component- und Service-Tests
- Playwright für E2E-Tests

Deployment-nahe Konfiguration:

- Vercel-Konfiguration für das Angular-Frontend
- Produktionsumgebung im Frontend zeigt auf `https://api.martin-gnahn.dev`
- Spring-Profil `prod` liest PostgreSQL-Zugangsdaten aus `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD`

## Architektur

Die Codebasis ist entlang einer einfachen Schichtenarchitektur organisiert:

```text
Angular Frontend
    -> REST/WebSocket Adapter
    -> Spring Application Services
    -> Domain Engine
    -> Persistence Adapter / JPA / PostgreSQL
```

Die wichtigsten Pakete im Backend:

- `controllers`: REST-Adapter, Request/Response-Mapping, Authentifizierung pro Request
- `websocket`: STOMP-Endpunkte, Presence-Handling und Realtime-Event-Publishing
- `usecases`: applikationsnahe Services für Spiel- und Raumabläufe
- `engine`: fachliche Spielobjekte und Regeln ohne Spring/JPA-Abhängigkeit
- `repositories`: Repository-Interfaces und JPA-basierte Persistenzzugriffe
- `entities`: datenbanknahe JPA-Entities
- `mapper`: Mapping zwischen Entities, Domain-Modellen und DTOs
- `dto`: API-Verträge für REST-Kommunikation

Ein wichtiger Designpunkt ist die Trennung der fachlichen Spielregeln von den Transport- und Persistenzdetails.
Beispielsweise prüfen `Game` und `Round` Regeln wie Mindestspielerzahl, Antwortphase, Sortierphase und
Captain-Berechtigung, während Controller nur HTTP- oder WebSocket-Eingaben in Use-Case-Aufrufe übersetzen.

Die Realtime-Schicht verwendet Domain-/Use-Case-Events, die nach erfolgreichem Commit über
`@TransactionalEventListener(phase = AFTER_COMMIT)` an `/topic/rooms/{roomCode}` publiziert werden. Dadurch sehen
Clients nur Events zu bereits persistierten Zustandsänderungen.

## Lokales Setup

Voraussetzungen:

- Java 21
- Node.js und npm
- Docker oder eine lokale PostgreSQL-Instanz

PostgreSQL starten:

```powershell
docker compose up -d
```

Backend starten:

```powershell
.\mvnw.cmd spring-boot:run
```

Das Backend läuft standardmäßig auf:

```text
http://localhost:8080
```

Frontend-Abhängigkeiten installieren und starten:

```powershell
npm ci --prefix frontend
npm run start --prefix frontend
```

Das Frontend läuft standardmäßig auf:

```text
http://localhost:4200
```

Die lokale Frontend-Konfiguration nutzt:

```text
REST:      http://localhost:8080/api
WebSocket: ws://localhost:8080/ws
```

## API und Live-Kommunikation

Zentrale REST-Endpunkte:

- `POST /api/rooms`: Raum erstellen
- `POST /api/rooms/{roomCode}/players`: Raum beitreten
- `GET /api/rooms/{roomCode}`: Lobby-/Raumstatus laden
- `POST /api/rooms/{roomCode}/ranking-game/start`: Spiel starten
- `GET /api/rooms/{roomCode}/ranking-game/current-round`: aktive Runde für den aktuellen Spieler laden
- `GET /api/rooms/{roomCode}/ranking-game/current-round/players`: aktive Spieler laden
- `POST /api/rooms/{roomCode}/ranking-game/rounds/{roundId}/answers`: Antwort abgeben
- `GET /api/rooms/{roomCode}/ranking-game/rounds/{roundId}/answers`: abgegebene Antworten lesen
- `POST /api/rooms/{roomCode}/ranking-game/rounds/{roundId}/answer/position/new`: Antwort als nächste Ranking-Position
  einsortieren
- `GET /api/rooms/{roomCode}/ranking-game/rounds/{roundId}/answer/position/all`: aktuelles Ranking lesen
- `GET /api/rooms/{roomCode}/chat/messages`: Chat-Historie lesen
- `GET /health`: Health Check

Swagger UI ist bei laufendem Backend erreichbar unter:

```text
http://localhost:8080/swagger-ui.html
```

WebSocket/STOMP:

- Endpoint: `/ws`
- Client sendet an: `/app/rooms/{roomCode}/join-live`
- Client sendet Chat an: `/app/rooms/{roomCode}/chat`
- Clients abonnieren: `/topic/rooms/{roomCode}`

REST und STOMP verwenden denselben Header:

```text
X-Player-Session-Token
```

Der Token wird beim Erstellen oder Beitreten eines Raums ausgegeben, im Frontend lokal gespeichert und bei geschützten
Requests automatisch gesetzt.

## Datenbank

Die Datenbank wird über Flyway versioniert. Relevante Tabellen sind unter anderem:

- `rooms`
- `players`
- `questions`
- `game_sessions`
- `game_session_players`
- `rounds`
- `round_card_assignments`
- `answers`
- `ranking_entries`
- `chat_messages`
- `scores`

Die Migrationen seed-en aktuell 50 deutsche Standardfragen. Hibernate läuft lokal mit `ddl-auto=validate`, wodurch die
Anwendung gegen das Flyway-Schema validiert statt es automatisch zu verändern.

## Tests

Backend-Tests ausführen:

```powershell
.\mvnw.cmd test
```

Frontend-Unit-Tests:

```powershell
npm test --prefix frontend -- --watch=false
```

E2E-Tests mit Playwright:

```powershell
npm run e2e --prefix frontend
```

Die Playwright-Tests starten das Angular-Frontend selbst. Das Backend muss vorher erreichbar sein, standardmäßig unter:

```text
http://localhost:8080/health
```

Der Backend-Testumfang deckt unter anderem Domain-Regeln, Use Cases, Controller-Verhalten, JPA-Repositories,
WebSocket-Konfiguration und Integration-Flows ab. Besonders relevant sind die Tests für konkurrierende Antwortabgaben:
Der Wechsel in die Sortierphase wird mit Locking abgesichert, damit parallele finale Submissions die Runde nicht
mehrfach fortschreiben.

## Projektstruktur

```text
.
|-- src/main/java/com/example/rankinggame
|   |-- auth
|   |-- config
|   |-- controllers
|   |-- dto
|   |-- engine
|   |-- entities
|   |-- events
|   |-- mapper
|   |-- repositories
|   |-- usecases
|   `-- websocket
|-- src/main/resources
|   |-- application.properties
|   |-- application-prod.properties
|   `-- db/migration
|-- src/test/java/com/example/rankinggame
|-- frontend
|   |-- src/app
|   |-- public/i18n
|   `-- e2e
|-- test-requests
|-- docker-compose.yml
|-- pom.xml
`-- vercel.json
```

## Technische Entscheidungen

**Domain-naher Kern statt Controller-Logik**  
Spielregeln wie "nur der Captain darf sortieren", "Antworten sind nur in der Antwortphase erlaubt" oder "ein Spiel
braucht mindestens zwei Spieler" liegen in Domain- oder Use-Case-nahem Code, nicht in den REST-Controllern.

**Persistente Spielzustände statt reinem In-Memory-Game**  
Räume, Spieler, Runden, Antworten, Rankings, Chat-Nachrichten und Sessions liegen in PostgreSQL. Dadurch ist der Code
näher an einem real deploybaren Multiplayer-System als an einem reinen Prototyp.

**Realtime nach Commit**  
Live-Events werden erst nach erfolgreichem Transaktions-Commit publiziert. Das reduziert Race Conditions zwischen
UI-Reloads und noch nicht sichtbaren Datenbankzuständen.

**Session-Token statt Login-System**  
Für ein Partyspiel ist ein leichtgewichtiger Raum-/Spieler-Token passend: kein Account-System, aber trotzdem Schutz
davor, dass beliebige Clients fremde Räume lesen oder Aktionen ausführen.

**Frontend als echte Spieloberfläche**  
Die Angular-App enthält nicht nur Formulare, sondern eigenständige Lobby-, Game-, Ranking-, Chat-, Error- und
Guard-Flows inklusive lokaler Sessionverwaltung und automatischer Token-Injection.

## Kritisch prüfbare offene Punkte

Diese Punkte sind bewusst sichtbar, weil sie für eine technische Weiterentwicklung wichtig wären:

- Mehrere Runden und finaler Spielabschluss sind noch nicht vollständig durchgezogen.
- Die Punkte-/Scoring-Logik ist angelegt, aber noch nicht als fertiger Ende-zu-Ende-Flow integriert.
- `RoundProgressService` ist zentral und nebenläufigkeitssensibel; die vorhandenen Tests decken wichtige Fälle ab, aber
  diese Klasse verdient bei Erweiterungen besondere Aufmerksamkeit.
- `RoundCardAssignmentService` funktioniert, ist aber laut TODOs noch zu komplex und sollte vereinfacht werden.
- Es gibt noch einige historisch gewachsene Migrationen und Tabellenteile aus frühen Entwicklungsphasen.
- Die Realtime-Schicht nutzt den einfachen Spring Message Broker. Für größere Skalierung wäre ein externer Broker oder
  eine skalierungsfähige Presence-Strategie zu bewerten.
- Die aktuelle Authentifizierung ist für raumbezogene Spielsitzungen passend, aber kein vollwertiges Benutzer- oder
  Rollenmodell.

## Entwicklungsnotizen

Hilfreiche manuelle Requests liegen in `test-requests`. Die lokale PostgreSQL-Kurznotiz steht in
`README_PostgreSQL_Container.md`. Weitere Ticket- und Workflow-Hinweise liegen in `docs`.

Der wichtigste Maßstab für weitere Arbeit am Projekt ist: Spielregeln im Domain-/Use-Case-Code halten, Controller
schlank lassen, JPA-Entities nicht als API-Verträge verwenden und neue Tests dort ergänzen, wo fachliches Verhalten oder
Nebenläufigkeit betroffen ist.
