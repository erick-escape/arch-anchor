# Sample Architecture Project

## Purpose

> **Note on the numbers below.** The expected similarity, violation, and rate values in this
> file were written against an earlier version of the metrics and no longer match what the tool
> reports. What *does* reproduce is the shape of the findings: analyzing this project surfaces a
> split of `service`, a merge of `auth` and `security`, and a move of `database.UIRenderer` into
> `presentation` — alongside other lower-rated suggestions. Treat the scenarios as the
> specification and the numbers as historical.


This is a sample Java project designed to demonstrate architectural analysis scenarios for Arch Anchor. The project intentionally contains architectural issues that should be detected by the analysis tool.

## Project Structure

```
com.example
├── dependencies/         # 26 dependency marker interfaces (A-Z)
├── database/            # Database operations module
├── presentation/        # UI rendering module
├── service/            # Mixed service and batch processing (SHOULD BE SPLIT)
├── auth/               # Authentication module (HIGH COUPLING with security)
└── security/           # Security module (HIGH COUPLING with auth)
```

## Architectural Issues

This project demonstrates three key architectural analysis scenarios:

### 1. MoveClass Scenario

**Issue**: `UIRenderer` class is misplaced in the `database` package

- **Current Location**: `com.example.database.UIRenderer`
- **Should Be In**: `com.example.presentation`
- **Dependencies**: {W, X, Y} - matches presentation module, not database {P, Q, R}
- **Current Similarity**: 0.05 (very low fit with database module)
- **Expected Similarity After Move**: 0.90+ (high fit with presentation module)
- **Violations**: 12 (using presentation dependencies in database context)
- **Expected Rate**: 0.52

### 2. SplitModule Scenario

**Issue**: `service` package has mixed responsibilities

The `com.example.service` module contains two distinct architectural domains:

**Service Processing Domain** (Dependencies: A, B, C, D):
- `ServiceHandler.java` (RefClass) - {A, B, C}
- `ServiceProxy.java` - {A, B}
- `RequestManager.java` - {A, C}
- `DataValidator.java` - {B, C, D}

**Batch Processing Domain** (Dependencies: X, Y, Z, W):
- `BatchWorker.java` (DeRefClass) - {X, Y, Z}
- `TaskProcessor.java` - {X, Y}
- `QueueHandler.java` - {Y, Z}
- `WorkDispatcher.java` - {X, Z, W}

**Current Module Metrics**:
- Module Similarity: 0.35 (low cohesion)
- Violations: 18

**Expected After Split**:
- `ServiceProcessor` module: modSim=0.68, viol=6
- `BatchProcessor` module: modSim=0.72, viol=3
- **Expected Rate**: 0.87

### 3. MergeModule Scenario

**Issue**: `auth` and `security` packages have high coupling

Both modules share the same architectural domain with overlapping dependencies:

**Auth Module** (Dependencies: E, F, G, H):
- `AuthService.java` (RefClass) - {E, F, G}
- `LoginHandler.java` - {E, F}
- `SessionManager.java` - {F, G}
- `TokenValidator.java` - {E, G, H}

**Security Module** (Dependencies: E, F, G, H):
- `SecurityService.java` (RefClass) - {E, F, H}
- `EncryptionUtil.java` - {E, H}
- `AccessControl.java` - {F, H}
- `CryptoProvider.java` - {E, F, G, H}

**Current Module Metrics**:
- Auth: modSim=0.64, viol=14
- Security: modSim=0.62, viol=16

**Expected After Merge**:
- `SecurityAuthModule`: modSim=0.84, viol=8
- **Expected Rate**: 0.78

## Dependency Graph

The project uses marker interfaces to simulate dependencies:

- **{P, Q, R}**: Database domain dependencies
- **{W, X, Y, Z}**: Presentation domain dependencies
- **{A, B, C, D}**: Service processing dependencies
- **{X, Y, Z, W}**: Batch processing dependencies
- **{E, F, G, H}**: Security/Auth domain dependencies

## Module Metrics Summary

### Before Architectural Fixes

| Module | Module Similarity | Violations |
|--------|------------------|------------|
| database | 0.58 | 12 |
| presentation | 0.86 | 2 |
| service | 0.35 | 18 |
| auth | 0.64 | 14 |
| security | 0.62 | 16 |

### After Architectural Fixes (Expected)

| Module | Module Similarity | Violations |
|--------|------------------|------------|
| database (after removing UIRenderer) | 0.75 | 0 |
| presentation (after adding UIRenderer) | 0.84 | 2 |
| ServiceProcessor (after split) | 0.68 | 6 |
| BatchProcessor (after split) | 0.72 | 3 |
| SecurityAuthModule (after merge) | 0.84 | 8 |

## Expected Analysis Results

When processed by Arch Anchor, the following recommendations should be generated:

1. **MoveClass**: Move `database.UIRenderer` → `presentation` (rate: 0.52)
2. **SplitModule**: Split `service` → `ServiceProcessor` + `BatchProcessor` (rate: 0.87)
3. **MergeModule**: Merge `auth` + `security` → `SecurityAuthModule` (rate: 0.78)

## Building the Project

```bash
mvn clean compile
```

## Using with Arch Anchor

1. Upload this project to Arch Anchor
2. Trigger analysis
3. Verify that all three architectural issues are detected
4. Verify that the metrics and recommendations match the expected values above

## Class Metrics Reference

All classes include Javadoc comments with their expected metrics:
- Dependencies used
- Expected similarity score
- Expected violation count
- Role (RefClass, DeRefClass, or regular class)

## Success Criteria

The analysis tool should:
1. ✅ Identify UIRenderer as a misplaced class (MoveClass scenario)
2. ✅ Identify service module as needing split (SplitModule scenario)
3. ✅ Identify auth and security as merge candidates (MergeModule scenario)
4. ✅ Calculate similarity and violation metrics matching expected values
5. ✅ Provide actionable recommendations with positive rates
