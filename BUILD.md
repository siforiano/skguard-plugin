# SoulGuard - Build System Guide

## Quick Start

### Windows
```batch
# Run the interactive build menu
build.bat
```

### Linux/Mac
```bash
# Make the script executable (first time only)
chmod +x build.sh

# Run the interactive build menu
./build.sh
```

## Build Options

### 1. Build Lite (FREE version)
Generates `SoulGuard-Lite-1.0-SNAPSHOT.jar` with all current features.

**Command:**
```bash
mvn clean package -P lite
```

**Includes:**
- All 47 current modules
- 7 anti-cheat checks
- 2FA + Brute Force Protection
- Redis + MySQL support
- Unlimited players

**Excludes:**
- Premium-only modules (Web Panel, ML, etc.)

### 2. Build Premium (PAID version)
Generates `SoulGuard-Premium-1.0-SNAPSHOT.jar` with all features including premium modules.

**Command:**
```bash
mvn clean package -P premium
```

**Includes:**
- Everything from Lite
- Web Panel Module
- Machine Learning Detection
- Cloud Blacklist
- Forensics & Replay
- Advanced Integrations
- Developer API

### 3. Build Both Versions
Generates both JARs in one command.

**Command:**
```bash
# Windows
build.bat
# Select option 3

# Linux/Mac
./build.sh
# Select option 3
```

## Output Files

After building, find your JARs in the `target/` directory:

```
target/
├── SoulGuard-Lite-1.0-SNAPSHOT.jar      (FREE version)
└── SoulGuard-Premium-1.0-SNAPSHOT.jar   (PREMIUM version)
```

## How It Works

### Maven Profiles

The `pom.xml` contains two profiles:

1. **lite** - Excludes `src/main/java/com/soulguard/modules/premium/**`
2. **premium** - Includes everything (default)

### Runtime Detection

The plugin automatically detects which version is running:

```java
Edition edition = SoulGuard.getInstance().getEdition();
if (edition.isPremium()) {
    // Premium-only code
}
```

Detection works by trying to load a premium-only class. If it fails, the plugin knows it's running Lite.

## Development Workflow

### Adding a New Premium Feature

1. Create your module in `src/main/java/com/soulguard/modules/premium/`
2. Build both versions to test
3. Lite will automatically exclude it
4. Premium will include it

### Testing Both Versions

```bash
# Build Lite
mvn clean package -P lite

# Test on server
# Copy target/SoulGuard-Lite-1.0-SNAPSHOT.jar to server

# Build Premium
mvn clean package -P premium

# Test on server
# Copy target/SoulGuard-Premium-1.0-SNAPSHOT.jar to server
```

## Troubleshooting

### Build Fails

```bash
# Clean and rebuild
mvn clean
mvn package -P lite
```

### Wrong Version Detected

Check that premium modules are in the correct folder:
```
src/main/java/com/soulguard/modules/premium/
```

### Missing Dependencies

```bash
# Update dependencies
mvn dependency:resolve
```

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Build SoulGuard

on: [push]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - uses: actions/setup-java@v2
        with:
          java-version: '17'
      
      - name: Build Lite
        run: mvn clean package -P lite
      
      - name: Build Premium
        run: mvn clean package -P premium
      
      - name: Upload Artifacts
        uses: actions/upload-artifact@v2
        with:
          name: SoulGuard-Builds
          path: target/*.jar
```

## Version Management

To change the version number, edit `pom.xml`:

```xml
<version>1.0-SNAPSHOT</version>
```

This will update both JAR names automatically.

## Support

For build issues, check:
1. Java 17+ is installed
2. Maven 3.6+ is installed
3. All dependencies are accessible
4. No syntax errors in code

---

**Happy Building!** 🚀
