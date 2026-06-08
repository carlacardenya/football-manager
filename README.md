# Football Manager — REST API Exercise (S1)


## 1. What is this exercise about?

You are going to build the backend of a small **Football Manager** game: a REST API that lets you create teams, register players, sign them, transfer them between teams, and simulate matches. Everything will live in memory (no database).

The project is split into three resources you will recognise from any sports management game:

- **Players** — people with a name, a position (`GK`, `DEF`, `MID`, `FWD`) and a skill level. They can be free agents, signed to a team, or injured.
- **Teams** — clubs with a name, a city, a budget and a roster of up to 11 players. They accumulate wins, draws, losses and goals over time.
- **Matches** — simulated games between two teams. The result is computed from the combined skill of the players on the pitch, with a bit of randomness, a small home advantage and a chance of injury.

Your job is to expose this game as a clean REST API. The domain classes, DTOs and exceptions are already provided. By the end, your application should pass the full test suite that comes in the project.

---

## 2. Learning objectives

After completing this exercise you should be comfortable with:

1. Designing **REST endpoints** following resource-oriented URLs and proper HTTP verbs.
2. Building a Spring Boot REST API using `@RestController`, `@RequestMapping`, `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`.
3. Reading and writing JSON via `@RequestBody`, `@PathVariable`, `@RequestParam`.
4. Returning correct HTTP status codes (`200`, `201`, `204`, `400`, `404`, `409`).
5. Mapping exceptions to HTTP responses with `@ExceptionHandler`.
6. Separating layers: **domain** (entities) ↔ **DTOs** (transport objects) ↔ **controllers** (HTTP layer).
7. **Constructor injection** between controllers (the Match controller depends on the Team and Player controllers).
8. Writing readable, idiomatic Java using `Stream`, `Comparator`, `Optional` and records.
9. Running and reading the output of an automated test suite (JUnit 5 + MockMvc).

---

## 3. Tech stack & prerequisites

You need installed locally:

- **JDK 17+** (`java -version` should show 17 or higher)
- **Maven 3.8+** (`mvn -v`)
- An IDE — IntelliJ IDEA Community is recommended (it understands Spring Boot out of the box)
- A REST client to try things by hand: **Postman**, **Insomnia**, **HTTPie** or `curl`

How to run the application:

```bash
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`.

How to run the tests:

```bash
mvn test
```

This runs all four test classes (`PlayerApiTest`, `TeamApiTest`, `TransferApiTest`, `MatchApiTest`). When you start, **all of them will be red**. Your goal is to turn them all green.

---

## 4. Project structure

```
football-manager-skeleton/
├── pom.xml
├── README.md                                  ← this file
└── src/
    ├── main/
    │   ├── java/com/tecnocampus/footballmanager/
    │   │   ├── FootballManagerApplication.java       ← given (Spring Boot bootstrap)
    │   │   ├── api/                                  ← YOU work here
    │   │   │   ├── PlayerRestController.java        ← skeleton (TODOs)
    │   │   │   ├── TeamRestController.java          ← skeleton (TODOs)
    │   │   │   └── MatchRestController.java         ← skeleton (TODOs)
    │   │   ├── domain/                               ← given (entities)
    │   │   │   ├── Player.java
    │   │   │   ├── Team.java
    │   │   │   ├── Match.java
    │   │   │   ├── Position.java                    (enum: GK, DEF, MID, FWD)
    │   │   │   ├── PlayerStatus.java                (enum: FREE_AGENT, SIGNED, INJURED)
    │   │   │   └── MatchResult.java                 (enum: HOME_WIN, AWAY_WIN, DRAW)
    │   │   ├── dto/                                  ← given (records)
    │   │   │   ├── CreateTeamDTO.java
    │   │   │   ├── CreatePlayerDTO.java
    │   │   │   ├── UpdatePlayerDTO.java
    │   │   │   ├── PlayMatchDTO.java
    │   │   │   ├── TeamDTO.java                     (with .from(Team))
    │   │   │   ├── PlayerDTO.java                   (with .from(Player))
    │   │   │   ├── MatchDTO.java                    (with .from(Match))
    │   │   │   ├── SignResultDTO.java
    │   │   │   ├── ReleaseResultDTO.java
    │   │   │   └── TransferResultDTO.java
    │   │   └── exception/                            ← given (RuntimeExceptions)
    │   │       ├── TeamNotFoundException.java
    │   │       ├── PlayerNotFoundException.java
    │   │       ├── MatchNotFoundException.java
    │   │       ├── InvalidDataException.java
    │   │       ├── DuplicateTeamException.java
    │   │       ├── InsufficientBudgetException.java
    │   │       └── TeamFullException.java
    │   └── resources/
    │       └── application.properties               ← given
    └── test/
        └── java/com/tecnocampus/footballmanager/
            ├── PlayerApiTest.java                   ← given (do NOT modify)
            ├── TeamApiTest.java                     ← given (do NOT modify)
            ├── TransferApiTest.java                 ← given (do NOT modify)
            └── MatchApiTest.java                    ← given (do NOT modify)
```

**Rule of thumb:** you only edit the three files inside `src/main/java/com/tecnocampus/footballmanager/api/`. Everything else is already done for you.

---

## 5. The domain model

Before writing any code, read the three domain classes carefully. They are the source of truth.

### `Player`
| Field         | Type           | Default              | Notes                                    |
|---------------|----------------|----------------------|------------------------------------------|
| `id`          | `String`       | UUID auto-generated  | Read-only                                |
| `name`        | `String`       | constructor arg      | Mutable via `setName`                    |
| `position`    | `Position`     | constructor arg      | **Immutable** after creation             |
| `skillLevel`  | `int`          | constructor arg      | 1–99                                     |
| `teamId`      | `String`       | `null`               | Set when signed                          |
| `status`      | `PlayerStatus` | `FREE_AGENT`         | `FREE_AGENT` → `SIGNED` → `INJURED`      |
| `goals`       | `int`          | 0                    | Incremented during match simulation      |
| `gamesPlayed` | `int`          | 0                    | Incremented during match simulation      |

Helpers: `addGoal()`, `addGamePlayed()`.

### `Team`
| Field          | Type      | Default     | Notes                                 |
|----------------|-----------|-------------|---------------------------------------|
| `id`           | `String`  | UUID        | Read-only                             |
| `name`         | `String`  | arg         |                                       |
| `city`         | `String`  | arg         |                                       |
| `budget`       | `int`     | **1000**    | Spent on signings/transfers           |
| `wins`         | `int`     | 0           | Updated by match simulation           |
| `draws`        | `int`     | 0           |                                       |
| `losses`       | `int`     | 0           |                                       |
| `goalsFor`     | `int`     | 0           |                                       |
| `goalsAgainst` | `int`     | 0           |                                       |
| `players`      | `List<Player>` | empty  | Maximum **11** players                |

Computed properties already on the entity:
- `getPoints()` → `wins * 3 + draws`
- `getGoalDifference()` → `goalsFor - goalsAgainst`
- `getTeamPower()` → sum of `skillLevel` of `SIGNED` players (injured don't count)
- `getAvailablePlayers()` → list of `SIGNED` players (injured don't count)

Helpers: `addPlayer`, `removePlayer`, `addWin`, `addDraw`, `addLoss`, `addGoalsFor`, `addGoalsAgainst`, plus setters for `budget`, `wins`, `draws`, `losses`, `goalsFor`, `goalsAgainst`.

### `Match`
| Field        | Type            |
|--------------|-----------------|
| `id`         | `String` (UUID) |
| `homeTeam`   | `Team`          |
| `awayTeam`   | `Team`          |
| `homeGoals`  | `int`           |
| `awayGoals`  | `int`           |
| `result`     | `MatchResult`   |
| `scorers`    | `List<String>`  |
| `playedAt`   | `LocalDateTime` |

Constructor signature:

```java
new Match(homeTeam, awayTeam, homeGoals, awayGoals, result, scorers);
```

`id` and `playedAt` are auto-generated.

---

## 6. What you need to implement

### 6.1 `PlayerRestController` — `/players`

| Method | Path                            | Body                | Returns / Status            |
|--------|---------------------------------|---------------------|-----------------------------|
| POST   | `/players`                      | `CreatePlayerDTO`   | `PlayerDTO` · **201**       |
| GET    | `/players?position=&status=`    | —                   | `List<PlayerDTO>` · **200** |
| GET    | `/players/top-scorers`          | —                   | `List<PlayerDTO>` · **200** |
| GET    | `/players/{playerId}`           | —                   | `PlayerDTO` · **200**/**404** |
| PUT    | `/players/{playerId}`           | `UpdatePlayerDTO`   | `PlayerDTO` · **200**/**404** |
| DELETE | `/players/{playerId}`           | —                   | **204** No Content          |
| POST   | `/players/{playerId}/recover`   | —                   | `PlayerDTO` · **200**/**400** |

**Business rules**

- **Create**: validate `name` (≥ 2 chars), `skillLevel` (1–99), `position` (must match the enum). On any violation throw `InvalidDataException`.
- **List**: both query params are optional. If `position=FWD` is given, only forwards. If `status=INJURED`, only injured players. If both are given, both filters apply.
- **Top scorers**: top 10 by `goals` (descending). Tie-break 1: fewer `gamesPlayed`. Tie-break 2: higher `skillLevel`.
- **Update**: same validation as create. **Position cannot change.**
- **Delete**: if the player is signed to a team, first set their status back to `FREE_AGENT` and clear `teamId`, then remove them from any team's roster, then drop them from the players list.
- **Recover**: only valid for `INJURED` players. Otherwise throw `InvalidDataException` (→ 400). On success, set status to `SIGNED`.

**Exception handlers** (inside this same controller):

| Exception                | HTTP Status |
|--------------------------|-------------|
| `PlayerNotFoundException`| 404         |
| `InvalidDataException`   | 400         |

Error response format (used by the tests):

```json
{ "error": "InvalidDataException", "message": "...", "status": 400 }
```

**Helper required by other controllers**

```java
Player findPlayerById(String id) { ... }
```

Package-private. Looks the player up in the in-memory list and throws `PlayerNotFoundException` if missing.

---

### 6.2 `TeamRestController` — `/teams`

| Method | Path                                                  | Body            | Returns / Status                |
|--------|-------------------------------------------------------|-----------------|---------------------------------|
| POST   | `/teams`                                              | `CreateTeamDTO` | `TeamDTO` · **201**             |
| GET    | `/teams`                                              | —               | `List<TeamDTO>` · **200**       |
| GET    | `/teams/league-table`                                 | —               | `List<TeamDTO>` · **200**       |
| GET    | `/teams/{teamId}`                                     | —               | `TeamDetailDTO` · **200**/**404** |
| DELETE | `/teams/{teamId}`                                     | —               | **204** No Content              |
| POST   | `/teams/{teamId}/sign/{playerId}`                     | —               | `SignResultDTO` · **200**       |
| POST   | `/teams/{teamId}/release/{playerId}`                  | —               | `ReleaseResultDTO` · **200**    |
| POST   | `/teams/{fromTeamId}/transfer/{playerId}/to/{toTeamId}`| —              | `TransferResultDTO` · **200**   |

**Business rules**

- **Create**: validate `name` (≥ 3 chars), `city` (not empty). Names are **unique, case-insensitive** — duplicates throw `DuplicateTeamException` (→ 409).
- **League table**: order by `points` desc → `goalDifference` desc → `goalsFor` desc.
- **Get one**: returns `TeamDetailDTO` (includes the full player list).
- **Delete**: every player on the team becomes `FREE_AGENT` (status + `teamId = null`).
- **Sign**:
  - The player must be `FREE_AGENT`.
  - The team must have **fewer than 11** players, otherwise `TeamFullException` (→ 400).
  - Cost = `skillLevel × 2`. If the team can't afford it, throw `InsufficientBudgetException` (→ 400).
  - Effects: `status = SIGNED`, `teamId = team.id`, `team.budget -= cost`, `team.addPlayer(player)`.
- **Release**:
  - The player must currently belong to this team.
  - Refund = `skillLevel × 1`.
  - Effects: `status = FREE_AGENT`, `teamId = null`, `team.budget += refund`, `team.removePlayer(player)`.
- **Transfer**:
  - `fromTeamId != toTeamId`.
  - The player must currently belong to `fromTeam`.
  - The player must **not** be `INJURED`.
  - The destination team must have fewer than 11 players.
  - Transfer fee = `skillLevel × 3`. The destination team must afford it.
  - Effects: move the player between rosters, update `teamId`, `fromTeam.budget += fee`, `toTeam.budget -= fee`.

**Exception handlers** (in this controller):

| Exception                     | HTTP Status |
|-------------------------------|-------------|
| `TeamNotFoundException`       | 404         |
| `PlayerNotFoundException`     | 404         |
| `DuplicateTeamException`      | 409         |
| `InvalidDataException`        | 400         |
| `InsufficientBudgetException` | 400         |
| `TeamFullException`           | 400         |

**Helper required by `MatchRestController`**

```java
Team findTeamById(String id) { ... }
List<Team> getTeams() { ... }
```

---

### 6.3 `MatchRestController` — `/matches`

| Method | Path                                  | Body            | Returns / Status            |
|--------|---------------------------------------|-----------------|-----------------------------|
| POST   | `/matches`                            | `PlayMatchDTO`  | `MatchDTO` · **201**        |
| GET    | `/matches`                            | —               | `List<MatchDTO>` · **200**  |
| GET    | `/matches/{matchId}`                  | —               | `MatchDTO` · **200**/**404**|
| GET    | `/teams/{teamId}/matches`             | —               | `List<MatchDTO>` · **200**  |

> Note: the last endpoint starts with `/teams/...` but it lives in the **Match controller** because it returns matches, not teams. This is a common REST design choice — group endpoints by the resource they return, not by URL prefix.

**Match simulation algorithm — the core of this controller**

Given a `PlayMatchDTO(homeTeamId, awayTeamId)`:

1. **Reject self-play** — if `homeTeamId.equals(awayTeamId)` → `InvalidDataException` (400).
2. **Resolve teams** — `teamController.findTeamById(homeTeamId)` and the same for away. If either is missing the exception bubbles up as 404.
3. **Roster check** — get `homeTeam.getAvailablePlayers()` and `awayTeam.getAvailablePlayers()`. If either is empty → `InvalidDataException` (400). Available means status `SIGNED` (injured players don't play).
4. **Compute team power** — `homePower = homeTeam.getTeamPower()`, same for away. (Sum of skill levels of available players.)
5. **Generate goals** — using `java.util.Random`:
   - `homeGoals = random.nextInt(homePower / 50 + 2);` ← +1 home advantage and +1 to compensate for `nextInt`'s exclusive upper bound.
   - `awayGoals = random.nextInt(awayPower / 50 + 1);`
6. **Determine result** — `HOME_WIN` if `homeGoals > awayGoals`, `AWAY_WIN` if `<`, otherwise `DRAW`.
7. **Update team stats** — `addGoalsFor`, `addGoalsAgainst` on both, then `addWin`/`addDraw`/`addLoss` according to the result.
8. **Update player stats** — every available player on both teams gets `addGamePlayed()`.
9. **Distribute scorers** — for each goal scored by a team, pick a random scorer from a *weighted pool* of that team's available players where each `FWD` is added **twice** and each `MID` **once** (forwards are twice as likely to score). If a team has no FWD/MID available, fall back to picking any available player. Each scorer call should also `addGoal()` and the player's `name` should be appended to the match's `scorers` list.
10. **Apply injury chance** — for each team, with **10% probability** (`random.nextInt(10) == 0`) pick one available player at random and set their status to `INJURED`.
11. **Persist the match** — `new Match(homeTeam, awayTeam, homeGoals, awayGoals, result, scorers)` and add it to the in-memory list.
12. **Return** `MatchDTO.from(match)` with status **201 Created**.

**Other endpoints**

- **GET `/matches`** — sort by `playedAt` descending, map to DTO.
- **GET `/matches/{id}`** — find or `MatchNotFoundException`.
- **GET `/teams/{teamId}/matches`** — first verify the team exists (`teamController.findTeamById(...)`), then filter matches where `homeTeam.id == teamId || awayTeam.id == teamId`, sort by `playedAt` descending.

**Exception handlers** (in this controller):

| Exception                  | HTTP Status |
|----------------------------|-------------|
| `MatchNotFoundException`   | 404         |
| `TeamNotFoundException`    | 404         |
| `InvalidDataException`     | 400         |

**Wiring**

`MatchRestController` depends on the other two controllers. Use **constructor injection** — Spring will provide the beans automatically:

```java
public MatchRestController(TeamRestController teamController,
                           PlayerRestController playerController) {
    this.teamController = teamController;
    this.playerController = playerController;
}
```

---

## 7. Validation rules (cheat sheet)

| Field          | Rule                                          | Exception              |
|----------------|-----------------------------------------------|------------------------|
| Player name    | not blank, at least **2** characters          | `InvalidDataException` |
| Player skill   | between **1** and **99** (inclusive)          | `InvalidDataException` |
| Player position| must match `Position` enum (`GK/DEF/MID/FWD`) | `InvalidDataException` |
| Team name      | not blank, at least **3** characters          | `InvalidDataException` |
| Team city      | not blank                                     | `InvalidDataException` |
| Team name uniq | case-insensitive                              | `DuplicateTeamException` |
| Roster size    | strictly less than **11** before signing      | `TeamFullException`    |
| Sign cost      | `skill × 2`, must fit in budget               | `InsufficientBudgetException` |
| Transfer fee   | `skill × 3`, must fit in destination budget   | `InsufficientBudgetException` |
| Self-play      | home and away must differ                     | `InvalidDataException` |
| Empty roster   | both teams must have ≥ 1 available player     | `InvalidDataException` |

---

## 8. Error response format

All exception handlers should return a JSON body with this shape:

```json
{
  "error":   "DuplicateTeamException",
  "message": "Team name already exists: Real Madrid",
  "status":  409
}
```

You can build it with `Map.of("error", ..., "message", ex.getMessage(), "status", 409)` — Spring will serialise it for you.

---

## 9. Suggested order of work

Don't try to write all three controllers at once. Follow this order:

1. **`findPlayerById` + `findTeamById`** — the two private helpers. They unblock everything else.
2. **`POST /players`** — the simplest endpoint. Once green, you understand the pattern.
3. **`POST /teams`** — the second simplest, plus uniqueness check.
4. **`POST /teams/{id}/sign/{playerId}`** — at this point you can already run the `MatchApiTest` setup (`@BeforeAll`) without errors.
5. **`GET /players/{id}` + `GET /teams/{id}`** — easy wins.
6. **`POST /players/{id}/recover`** — small but covered by a test.
7. **`POST /matches`** — the meaty one. Implement step by step from §6.3.
8. **`GET /matches`, `GET /matches/{id}`, `GET /teams/{id}/matches`** — straight reads.
9. **Sorting endpoints** — `GET /teams/league-table`, `GET /players/top-scorers`.
10. **Release & Transfer** — once Sign works, these are variations on the same idea.
11. **Update & Delete players, Delete teams** — finishing touches.
12. **Exception handlers** — add them as you go, not at the end. A missing handler turns an expected 400 into a real 500 and the test fails for the wrong reason.

After every change, run only the tests that should now pass:

```bash
mvn test -Dtest=PlayerApiTest
mvn test -Dtest=TeamApiTest
mvn test -Dtest=TransferApiTest
mvn test -Dtest=MatchApiTest
```

Have fun. The hard part of this exercise is not the football logic — it's getting comfortable with Spring's annotations, JSON contracts and the test-driven feedback loop. By the end you will have written a real REST API.
