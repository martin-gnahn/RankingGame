# RankingGame - Projektplan

Stand: 2026-06-12

## 1. Zielbild

RankingGame ist ein professionelles E-Commerce-Referenzprojekt mit einem Spring-Boot-Backend, PostgreSQL-Datenbank, sauberer Schichtenarchitektur, automatisierten Tests und reproduzierbarer lokaler Entwicklungsumgebung.

Das Projekt soll als bewerbungstaugliches Portfolio-Projekt dienen. Der Schwerpunkt liegt nicht auf maximalem Funktionsumfang, sondern auf nachvollziehbarer Architektur, wartbarem Code, sauberer API-Gestaltung, Tests und einem gut dokumentierten Entwicklungsprozess.

## 2. Projekterfolg

Das Projekt gilt als erfolgreich, wenn:

- ein Nutzer Produkte anzeigen, suchen und paginiert abrufen kann
- ein Nutzer Produkte in einen Warenkorb legen und daraus entfernen kann
- ein Nutzer aus dem Warenkorb eine Bestellung erzeugen kann
- der Produktbestand beim Checkout konsistent reduziert wird
- Admin-Funktionen fuer Produktverwaltung geschuetzt sind
- die Anwendung lokal mit Docker Compose gestartet werden kann
- Flyway die Datenbankstruktur reproduzierbar verwaltet
- zentrale Businesslogik durch Tests abgesichert ist
- CI bei jedem Push Build und Tests ausfuehrt

## 3. MVP-Scope

Der MVP umfasst:

- Produktverwaltung mit CRUD API
- Produktsuche und Pagination
- Warenkorb
- Checkout und Bestellungen
- einfache JWT-basierte Authentifizierung
- Rollen `ADMIN` und `CUSTOMER`
- PostgreSQL, Flyway und Docker Compose
- automatisierte Tests fuer Repository, Service und Controller
- GitHub Actions fuer Build und Tests

Nicht Teil des MVP:

- echtes Payment
- Versandlogik
- E-Mail-Versand
- komplexes Rechte- und Mandantensystem
- produktionsreifes Monitoring
- Frontend, ausser es wird spaeter explizit priorisiert

## 4. Technologie-Stack

### Backend

- Java 21
- Spring Boot
- Spring Web MVC
- Spring Data JPA
- Spring Validation
- Spring Security
- PostgreSQL
- Flyway
- Lombok

### Testing

- JUnit 5
- Mockito
- MockMvc
- Spring Boot Test

### Infrastruktur

- Docker
- Docker Compose
- Maven Wrapper
- GitHub Actions

### Optionales Frontend

- Angular
- TypeScript
- Angular Material

## 5. Architektur

### Paketstruktur

Die Zielstruktur ist feature-basiert. Jedes Feature kapselt Controller, Service, Repository, Entity, DTOs und Mapper.

```text
src/main/java/com/example/rankinggame
|-- product
|   |-- controller
|   |-- service
|   |-- repository
|   |-- entity
|   |-- dto
|   `-- mapper
|-- cart
|   |-- controller
|   |-- service
|   |-- repository
|   |-- entity
|   |-- dto
|   `-- mapper
|-- order
|   |-- controller
|   |-- service
|   |-- repository
|   |-- entity
|   |-- dto
|   `-- mapper
|-- user
|   |-- controller
|   |-- service
|   |-- repository
|   |-- entity
|   |-- dto
|   `-- mapper
`-- common
    |-- exception
    |-- security
    `-- validation
```

### Architekturregeln

- Controller enthalten keine Businesslogik.
- Services enthalten Businesslogik und Transaktionsgrenzen.
- Repositories enthalten nur Datenzugriffe.
- REST APIs kommunizieren ueber DTOs, nicht direkt ueber Entities.
- Mapper trennen DTOs und Entities.
- Fehler werden zentral ueber einen Global Exception Handler abgebildet.
- Datenbankschema wird ausschliesslich ueber Flyway-Migrationen veraendert.
- Tests gehoeren zum jeweiligen Arbeitspaket und werden nicht auf spaeter verschoben.

## 6. Statusmodell

Jedes Arbeitspaket hat genau einen Status:

- `OFFEN`: noch nicht begonnen
- `IN ARBEIT`: begonnen, aber nicht abgenommen
- `BLOCKIERT`: kann ohne Entscheidung oder Vorarbeit nicht weitergefuehrt werden
- `ERLEDIGT`: implementiert, getestet und dokumentiert

## 7. Definition of Done

Ein Arbeitspaket gilt als erledigt, wenn:

- die Anwendung kompiliert
- relevante Tests vorhanden und gruen sind
- REST-Endpunkte manuell oder automatisiert pruefbar sind
- Fehlerfaelle behandelt werden
- Flyway-Migrationen vorhanden sind, falls Datenmodell betroffen ist
- Architekturregeln eingehalten werden
- `PROJECT_PLAN.md` aktualisiert wurde
- die Aenderungen commit-faehig sind

## 8. Aktueller Stand

Vorhanden im Repository:

- Maven/Spring-Boot-Projekt
- PostgreSQL-Service in `docker-compose.yml`
- Datenbank-Konfiguration in `application.properties`
- Flyway-Migrationsordner
- Produkt-Entity
- Migration fuer `products`

Noch zu pruefen:

- ob die Anwendung gegen PostgreSQL sauber startet
- ob Flyway-Migrationen automatisch laufen
- ob das Produktmodell fachlich komplett ist
- ob Tests bereits eingerichtet und lauffaehig sind

## 9. Meilensteine

### M1 - Laufende Backend-Basis

Ziel: Die Anwendung startet lokal reproduzierbar mit PostgreSQL.

Enthaelt:

- Projektsetup
- Docker Compose
- Datenbankkonfiguration
- Flyway
- erstes Datenmodell

Ergebnis:

- `./mvnw test` laeuft
- `docker compose up -d` startet PostgreSQL
- Spring Boot verbindet sich mit der Datenbank
- Flyway erstellt das Schema

### M2 - Produktkatalog

Ziel: Produkte koennen erstellt, gelesen, geaendert, geloescht, gesucht und paginiert abgerufen werden.

Enthaelt:

- Product Repository
- DTOs und Mapper
- Product Service
- Product Controller
- Fehlerbehandlung
- Tests
- Suche
- Pagination

Ergebnis:

- vollstaendige Product REST API
- saubere Fehlerantworten
- Testabdeckung fuer Kernlogik

### M3 - Warenkorb

Ziel: Nutzer koennen Produkte in einem Warenkorb sammeln.

Enthaelt:

- Cart und CartItem Modell
- Warenkorb-Service
- Warenkorb-API
- Validierung von Produktbestand und Mengen
- Tests

Ergebnis:

- Warenkorb anzeigen
- Artikel hinzufuegen
- Artikel entfernen
- Mengen aktualisieren

### M4 - Bestellung und Checkout

Ziel: Aus einem Warenkorb kann eine Bestellung erzeugt werden.

Enthaelt:

- Order und OrderItem Modell
- Checkout-Service
- Bestandsreduktion
- Order API
- Transaktionssicherheit
- Tests

Ergebnis:

- Checkout erzeugt Bestellung
- Bestand wird konsistent reduziert
- Warenkorb wird nach erfolgreichem Checkout geleert

### M5 - Security

Ziel: Kunden und Admins koennen sich authentifizieren; Admin-Funktionen sind geschuetzt.

Enthaelt:

- User Modell
- Registrierung
- Login
- JWT
- Rollen `ADMIN` und `CUSTOMER`
- Security-Konfiguration
- Tests

Ergebnis:

- oeffentliche Endpunkte sind erreichbar
- geschuetzte Endpunkte verlangen Authentifizierung
- Admin-Endpunkte verlangen Admin-Rolle

### M6 - Betrieb und Qualitaet

Ziel: Das Projekt ist CI-faehig und gut vorzeigbar.

Enthaelt:

- Dockerfile
- GitHub Actions
- README-Erweiterung
- Beispielrequests
- finale Test- und Architekturpruefung

Ergebnis:

- CI baut und testet das Projekt
- Projekt kann von Dritten lokal gestartet werden
- Bewerbungsreview ist ohne Zusatzwissen moeglich

## 10. Arbeitspakete

### AP-01 Projektsetup

Status: `IN ARBEIT`

Prioritaet: Hoch

Ziel: Technische Basis fuer lokale Entwicklung bereitstellen.

Tasks:

- [x] Spring-Boot-Projekt anlegen
- [x] Java 21 konfigurieren
- [x] Maven Wrapper verwenden
- [x] PostgreSQL via Docker Compose konfigurieren
- [x] Datenbankverbindung konfigurieren
- [x] Flyway einrichten
- [ ] Anwendung mit PostgreSQL starten und verifizieren
- [ ] Basistest ausfuehren

Abnahmekriterien:

- `docker compose up -d` startet PostgreSQL
- Spring Boot startet ohne Datenbankfehler
- Flyway wird beim Start ausgefuehrt
- `./mvnw test` ist gruen

### AP-02 Produktmodell

Status: `IN ARBEIT`

Prioritaet: Hoch

Ziel: Produktdaten dauerhaft speichern.

Geplantes Entity:

```java
Long id;
String name;
String description;
BigDecimal price;
Integer stock;
LocalDateTime createdAt;
```

Tasks:

- [x] Product Entity anlegen
- [x] Flyway-Migration fuer `products` anlegen
- [ ] Product Repository anlegen
- [ ] Constraints pruefen: Preis positiv, Bestand nicht negativ
- [ ] Repository-Test fuer Persistenz anlegen

Abnahmekriterien:

- Tabelle `products` wird per Flyway erstellt
- Product kann gespeichert und gelesen werden
- fachliche Constraints sind dokumentiert oder validiert

### AP-03 Product CRUD API

Status: `OFFEN`

Prioritaet: Hoch

Ziel: Produkte ueber REST verwalten.

Endpoints:

```http
POST   /products
GET    /products
GET    /products/{id}
PUT    /products/{id}
DELETE /products/{id}
```

Tasks:

- [ ] Request DTO fuer Create/Update anlegen
- [ ] Response DTO anlegen
- [ ] Mapper anlegen
- [ ] Product Service implementieren
- [ ] Product Controller implementieren
- [ ] Validierung fuer Pflichtfelder, Preis und Bestand

Abnahmekriterien:

- alle CRUD-Endpunkte funktionieren
- ungueltige Requests liefern `400`
- nicht gefundene Produkte liefern `404`
- Controller enthaelt keine Businesslogik

### AP-04 Fehlerbehandlung

Status: `OFFEN`

Prioritaet: Hoch

Ziel: Einheitliche Fehlerantworten fuer REST APIs bereitstellen.

Tasks:

- [ ] `ProductNotFoundException` anlegen
- [ ] `GlobalExceptionHandler` anlegen
- [ ] Fehler-DTO definieren
- [ ] Validation-Fehler lesbar ausgeben

Abnahmekriterien:

- `404` bei nicht gefundenen Produkten
- `400` bei Validierungsfehlern
- Fehlerantworten haben ein einheitliches Format

### AP-05 Produkttests

Status: `OFFEN`

Prioritaet: Hoch

Ziel: Produktkatalog gegen Regressionen absichern.

Tasks:

- [ ] Repository Tests
- [ ] Service Tests
- [ ] Controller Tests mit MockMvc
- [ ] Fehlerfaelle testen

Abnahmekriterien:

- Kernpfade und Fehlerpfade sind getestet
- Tests laufen mit `./mvnw test`
- Zielwert: mindestens 80 Prozent Coverage fuer Product-Modul

### AP-06 Produktsuche

Status: `OFFEN`

Prioritaet: Mittel

Ziel: Produkte nach Namen oder Beschreibung finden.

Endpoint:

```http
GET /products?search=keyboard
```

Tasks:

- [ ] Suchparameter in Product API integrieren
- [ ] Repository Query definieren
- [ ] Service-Logik testen

Abnahmekriterien:

- Suche ist case-insensitive
- leere Suche liefert alle Produkte
- Tests decken Treffer und Nicht-Treffer ab

### AP-07 Pagination und Sortierung

Status: `OFFEN`

Prioritaet: Mittel

Ziel: Produktlisten kontrolliert paginiert abrufen.

Endpoint:

```http
GET /products?page=0&size=20&sort=name,asc
```

Tasks:

- [ ] Pageable in API verwenden
- [ ] sinnvolle Default-Werte setzen
- [ ] maximale Page Size begrenzen
- [ ] Tests fuer Pagination ergaenzen

Abnahmekriterien:

- API liefert Page-Metadaten
- Default Pagination funktioniert
- zu grosse Page Sizes werden begrenzt

### AP-08 Warenkorbmodell

Status: `OFFEN`

Prioritaet: Hoch

Ziel: Warenkorb und Positionen speichern.

Entities:

- Cart
- CartItem

Tasks:

- [ ] Datenmodell definieren
- [ ] Flyway-Migration erstellen
- [ ] Repositories anlegen
- [ ] Beziehungen zu Product definieren

Abnahmekriterien:

- ein Cart kann mehrere CartItems enthalten
- CartItems referenzieren Produkte
- Mengen sind positiv

### AP-09 Warenkorb API

Status: `OFFEN`

Prioritaet: Hoch

Ziel: Warenkorb ueber REST bedienen.

Endpoints:

```http
GET    /cart
POST   /cart/items
PUT    /cart/items/{id}
DELETE /cart/items/{id}
```

Tasks:

- [ ] DTOs anlegen
- [ ] Cart Service implementieren
- [ ] Cart Controller implementieren
- [ ] Produktbestand beim Hinzufuegen pruefen
- [ ] Tests ergaenzen

Abnahmekriterien:

- Artikel koennen hinzugefuegt, geaendert und entfernt werden
- Warenkorb kann angezeigt werden
- ungueltige Mengen werden abgelehnt

### AP-10 Bestellmodell

Status: `OFFEN`

Prioritaet: Hoch

Ziel: Bestellungen dauerhaft speichern.

Entities:

- Order
- OrderItem

Tasks:

- [ ] Datenmodell definieren
- [ ] Statusmodell fuer Bestellungen definieren
- [ ] Flyway-Migration erstellen
- [ ] Repositories anlegen

Abnahmekriterien:

- Bestellung enthaelt mehrere Positionen
- Preise werden zum Bestellzeitpunkt gespeichert
- Bestellstatus ist nachvollziehbar

### AP-11 Checkout

Status: `OFFEN`

Prioritaet: Hoch

Ziel: Warenkorb in eine Bestellung umwandeln.

Endpoint:

```http
POST /orders
```

Tasks:

- [ ] Bestellung aus Warenkorb erzeugen
- [ ] Bestand reduzieren
- [ ] Warenkorb nach Erfolg leeren
- [ ] Transaktionalitaet sicherstellen
- [ ] Fehler bei unzureichendem Bestand behandeln
- [ ] Tests fuer Erfolgs- und Fehlerpfade

Abnahmekriterien:

- Checkout ist atomar
- Bestand wird korrekt reduziert
- bei Fehler bleibt der Datenstand konsistent

### AP-12 Order API

Status: `OFFEN`

Prioritaet: Mittel

Ziel: Bestellungen abrufen.

Endpoints:

```http
GET /orders
GET /orders/{id}
```

Tasks:

- [ ] DTOs anlegen
- [ ] Order Service erweitern
- [ ] Order Controller anlegen
- [ ] Tests ergaenzen

Abnahmekriterien:

- Bestellliste ist abrufbar
- einzelne Bestellung ist abrufbar
- nicht gefundene Bestellung liefert `404`

### AP-13 User Modell

Status: `OFFEN`

Prioritaet: Mittel

Ziel: Nutzer als Grundlage fuer Authentifizierung speichern.

Tasks:

- [ ] User Entity anlegen
- [ ] Role Enum anlegen
- [ ] Flyway-Migration erstellen
- [ ] User Repository anlegen

Abnahmekriterien:

- Nutzer haben eindeutige E-Mail-Adressen
- Passwoerter werden nicht im Klartext gespeichert
- Rollen sind modelliert

### AP-14 JWT Auth

Status: `OFFEN`

Prioritaet: Mittel

Ziel: Registrierung und Login bereitstellen.

Endpoints:

```http
POST /auth/register
POST /auth/login
```

Tasks:

- [ ] Auth DTOs anlegen
- [ ] PasswordEncoder konfigurieren
- [ ] Register-Flow implementieren
- [ ] Login-Flow implementieren
- [ ] JWT erzeugen und pruefen
- [ ] Security Filter Chain konfigurieren
- [ ] Tests ergaenzen

Abnahmekriterien:

- Registrierung legt Nutzer an
- Login liefert gueltiges Token
- ungueltige Logins werden abgelehnt

### AP-15 Rollen und Zugriffsschutz

Status: `OFFEN`

Prioritaet: Mittel

Ziel: Admin- und Kundenfunktionen trennen.

Rollen:

- `ADMIN`
- `CUSTOMER`

Tasks:

- [ ] Admin-Endpunkte schuetzen
- [ ] Kunden-Endpunkte schuetzen
- [ ] Zugriffstests ergaenzen

Abnahmekriterien:

- Produktverwaltung ist Admins vorbehalten
- Warenkorb und Bestellungen sind Kunden vorbehalten
- nicht autorisierte Zugriffe liefern `401` oder `403`

### AP-16 Dockerisierung der Anwendung

Status: `OFFEN`

Prioritaet: Mittel

Ziel: Anwendung und Datenbank gemeinsam containerisiert starten.

Tasks:

- [ ] Dockerfile erstellen
- [ ] Compose um App-Service erweitern
- [ ] Umgebungsvariablen fuer DB-Konfiguration verwenden
- [ ] Startanleitung dokumentieren

Abnahmekriterien:

- Anwendung startet per Docker Compose
- App-Service erreicht PostgreSQL-Service
- Konfiguration enthaelt keine lokalen Sonderannahmen

### AP-17 GitHub Actions

Status: `OFFEN`

Prioritaet: Mittel

Ziel: Automatischer Build und Testlauf bei Push und Pull Request.

Tasks:

- [ ] Workflow fuer Maven Build erstellen
- [ ] Tests ausfuehren
- [ ] Java 21 konfigurieren
- [ ] Build-Artefakt optional erzeugen

Abnahmekriterien:

- CI laeuft bei Push
- CI bricht bei roten Tests ab
- Workflow ist im Repository dokumentiert

### AP-18 README und Developer Experience

Status: `OFFEN`

Prioritaet: Mittel

Ziel: Projekt fuer Reviewer und Bewerbungen leicht nachvollziehbar machen.

Tasks:

- [ ] README mit Projektbeschreibung erweitern
- [ ] lokale Startanleitung dokumentieren
- [ ] API-Beispiele dokumentieren
- [ ] Architekturentscheidungen kurz erklaeren

Abnahmekriterien:

- fremde Entwickler koennen das Projekt lokal starten
- MVP-Funktionen sind aus der README ersichtlich
- wichtige Befehle sind dokumentiert

### AP-19 Architektur-Review

Status: `OFFEN`

Prioritaet: Niedrig

Ziel: Struktur und Grenzen nach MVP-Fertigstellung pruefen.

Tasks:

- [ ] Paketstruktur pruefen
- [ ] Duplikate und inkonsistente Patterns identifizieren
- [ ] Transaktionsgrenzen pruefen
- [ ] Testluecken dokumentieren

Abnahmekriterien:

- Architekturregeln sind eingehalten
- bekannte technische Schulden sind dokumentiert

### AP-20 Erweiterungen nach MVP

Status: `OFFEN`

Prioritaet: Niedrig

Moegliche Erweiterungen:

- Domain Events fuer Checkout
- Redis Cache fuer Produktlisten
- Performance-Optimierung
- OpenAPI/Swagger
- Angular Frontend
- Testcontainers

Abnahmekriterien:

- Erweiterungen werden erst nach stabilem MVP priorisiert
- jede Erweiterung erhaelt ein eigenes Arbeitspaket

## 11. Arbeitsreihenfolge

Empfohlene naechste Schritte:

1. AP-01 abschliessen: Start, Datenbank, Flyway und Tests verifizieren.
2. AP-02 abschliessen: Repository und erste Persistenztests ergaenzen.
3. AP-03 und AP-04 gemeinsam umsetzen, weil CRUD API und Fehlerbehandlung eng zusammenhaengen.
4. AP-05 direkt danach testen und stabilisieren.
5. AP-06 und AP-07 als Abrundung des Produktkatalogs.
6. Danach Warenkorb, Bestellung, Security und Betrieb in dieser Reihenfolge.

## 12. Risiken und Entscheidungen

### Risiko: Spring Boot Version

Das Projekt nutzt aktuell eine moderne Spring-Boot-Version. Falls Dependencies oder Test-Starter unerwartet inkompatibel sind, wird die Version bewusst stabilisiert und im Plan dokumentiert.

### Risiko: Security zu frueh

Security kann die Entwicklung der fachlichen APIs verlangsamen. Deshalb wird Auth erst nach Produkt, Warenkorb und Checkout eingefuehrt.

### Risiko: zu grosser Scope

Das Projekt bleibt MVP-orientiert. Erweiterungen wie Redis, Domain Events oder Frontend werden erst nach stabiler Backend-Version begonnen.

### Entscheidung: Feature-basierte Struktur

Die feature-basierte Struktur ist fuer dieses Projekt passender als eine rein technische Schichtenstruktur, weil sie die fachlichen Module Produkt, Warenkorb, Bestellung und Nutzer klar trennt.

## 13. Codex-Arbeitsanweisung

Wenn Codex an diesem Projekt arbeitet:

1. Aktuellen Stand im Repository analysieren.
2. Erstes Arbeitspaket mit Status `OFFEN` oder `IN ARBEIT` finden.
3. Nur dieses Arbeitspaket bearbeiten, ausser der Nutzer bittet ausdruecklich um mehr.
4. Tests passend zum Arbeitspaket erstellen oder aktualisieren.
5. Anwendung oder relevante Tests ausfuehren.
6. Status und Tasks in diesem Projektplan aktualisieren.
7. Am Ende dokumentieren:
   - geaenderte Dateien
   - ausgefuehrte Tests
   - erledigte Punkte
   - naechstes Arbeitspaket

Keine unnoetigen Refactorings ausserhalb des aktuellen Arbeitspakets.
