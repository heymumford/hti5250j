# CCSID Refactoring - Technical Specification

**Based on**: CCSID_DUPLICATION_ANALYSIS.md
**Recommended Solution**: Option 1 - JSON Configuration + Factory Pattern
**Estimated Effort**: 6 hours (implementation + testing)
**Lines Saved**: 970 lines (62% reduction)

---

## 1. Current Architecture

### Class Hierarchy
```
ICodepageConverter (interface)
    ^
    |
    +-- CodepageConverterAdapter (abstract, 122 lines)
    |   - init(): ICodepageConverter
    |   - uni2ebcdic(char): byte
    |   - ebcdic2uni(int): char
    |   - private formatUniToEbcdicError()
    |   - private formatEbcdicToUniError()
    |   - protected abstract getCodePage(): char[]
    |
    +-- CCSID37 (81 lines): getName() -> "37"
    +-- CCSID273 (80 lines): getName() -> "273"
    +-- CCSID277 (78 lines): getName() -> "277"
    +-- ... (17 more adapters)
    +-- CCSID1148 (78 lines): getName() -> "1148"
    |
    +-- CCSID930 (122 lines, direct implementer - LEAVE UNCHANGED)

BuiltInCodePageFactory
    - getConverter(ccsidName: String): ICodepageConverter
    - (Currently instantiates all 21 classes)
```

### Current Files
```
src/org/hti5250j/encoding/builtin/
├── ICodepageConverter.java          (43 lines) - KEEP
├── CodepageConverterAdapter.java    (122 lines) - KEEP
├── CodepageConverterFactory.java    (50 lines) - NEW
├── ConfigurableCodepageConverter.java (40 lines) - NEW
├── BuiltInCodePageFactory.java      (? lines) - MODIFY (one method)
├── CCSID37.java                     (81 lines) - DELETE
├── CCSID273.java                    (80 lines) - DELETE
├── CCSID277.java                    (78 lines) - DELETE
├── ... (17 more CCSID files)        (78 lines each) - DELETE
├── CCSID1148.java                   (78 lines) - DELETE
├── CCSID930.java                    (122 lines) - KEEP
├── CodepageConverterAdapter.java    (122 lines) - KEEP
└── ccsid-mappings.json              (500 lines) - NEW
```

---

## 2. New Architecture - JSON-Based Configuration

### 2.1 JSON Configuration File Structure

**File**: `src/resources/ccsid-mappings.json`

```json
{
  "ccsid_mappings": [
    {
      "ccsid_id": "37",
      "name": "37",
      "description": "CECP: USA, Canada (ESA*), Netherlands, Portugal, Brazil, Australia, New Zealand",
      "codepage": [
        0x0000, 0x0001, 0x0002, 0x0003, 0x009C, 0x0009, 0x0086, 0x007F,
        0x0097, 0x008D, 0x008E, 0x000B, 0x000C, 0x000D, 0x000E, 0x000F,
        ... (256 entries total, representing Unicode codepoints)
      ]
    },
    {
      "ccsid_id": "273",
      "name": "273",
      "description": "CECP: Austria, Germany",
      "codepage": [
        0x0000, 0x0001, 0x0002, 0x0003, 0x009C, 0x0009, 0x0086, 0x007F,
        ... (256 entries, values differ from CCSID37)
      ]
    },
    ...
    {
      "ccsid_id": "1148",
      "name": "1148",
      "description": "Latin-1 with Euro symbol",
      "codepage": [
        0x0000, 0x0001, ... (256 entries)
      ]
    }
  ]
}
```

### 2.2 Factory Class Implementation

**File**: `src/org/hti5250j/encoding/builtin/CodepageConverterFactory.java`

```java
package org.hti5250j.encoding.builtin;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Factory for loading CCSID converters from JSON configuration.
 *
 * Loads character mappings from ccsid-mappings.json and creates
 * ConfigurableCodepageConverter instances on demand.
 *
 * @author Refactoring Task
 * @see ConfigurableCodepageConverter
 */
public class CodepageConverterFactory {

    private static final String CONFIG_PATH = "ccsid-mappings.json";
    private static final Map<String, CCSIDMapping> MAPPINGS = new HashMap<>();
    private static volatile boolean initialized = false;

    static {
        try {
            loadMappings();
            initialized = true;
        } catch (IOException e) {
            throw new RuntimeException(
                "Failed to load CCSID mappings from " + CONFIG_PATH, e
            );
        }
    }

    /**
     * Get a converter for the specified CCSID.
     *
     * @param ccsidId CCSID identifier (e.g., "37", "500", "930")
     * @return Configured converter, or null if CCSID not found
     */
    public static ICodepageConverter getConverter(String ccsidId) {
        CCSIDMapping mapping = MAPPINGS.get(ccsidId);
        if (mapping == null) {
            return null;
        }
        return new ConfigurableCodepageConverter(mapping);
    }

    /**
     * Load all CCSID mappings from JSON resource file.
     *
     * @throws IOException if resource cannot be read
     */
    private static void loadMappings() throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                    CodepageConverterFactory.class
                        .getResourceAsStream("/" + CONFIG_PATH)
                )
            )) {

            StringBuilder json = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                json.append(line);
            }

            Gson gson = new Gson();
            JsonObject root = gson.fromJson(json.toString(), JsonObject.class);
            JsonArray mappingsArray = root.getAsJsonArray("ccsid_mappings");

            for (JsonElement element : mappingsArray) {
                JsonObject obj = element.getAsJsonObject();
                String ccsidId = obj.get("ccsid_id").getAsString();
                String name = obj.get("name").getAsString();
                String description = obj.get("description").getAsString();

                // Parse codepage array
                JsonArray codepageArray = obj.getAsJsonArray("codepage");
                char[] codepage = new char[256];
                for (int i = 0; i < 256; i++) {
                    codepage[i] = (char) codepageArray.get(i).getAsInt();
                }

                CCSIDMapping mapping = new CCSIDMapping(
                    ccsidId, name, description, codepage
                );
                MAPPINGS.put(ccsidId, mapping);
            }
        }
    }

    /**
     * Internal data structure for CCSID mappings.
     */
    static class CCSIDMapping {
        final String ccsidId;
        final String name;
        final String description;
        final char[] codepage;

        CCSIDMapping(String ccsidId, String name, String description,
                     char[] codepage) {
            this.ccsidId = ccsidId;
            this.name = name;
            this.description = description;
            this.codepage = codepage;
        }
    }
}
```

### 2.3 Configurable Converter Implementation

**File**: `src/org/hti5250j/encoding/builtin/ConfigurableCodepageConverter.java`

```java
package org.hti5250j.encoding.builtin;

/**
 * Configurable implementation of ICodepageConverter for single-byte
 * EBCDIC-to-Unicode character mappings.
 *
 * Instances are created by CodepageConverterFactory with configuration
 * loaded from ccsid-mappings.json.
 *
 * @author Refactoring Task
 * @see CodepageConverterFactory
 */
public class ConfigurableCodepageConverter extends CodepageConverterAdapter {

    private final String name;
    private final String description;
    private final char[] codepage;

    /**
     * Create a converter for a specific CCSID.
     *
     * @param mapping CCSID mapping with character arrays
     */
    ConfigurableCodepageConverter(CodepageConverterFactory.CCSIDMapping mapping) {
        this.name = mapping.name;
        this.description = mapping.description;
        this.codepage = mapping.codepage;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getEncoding() {
        return name;
    }

    @Override
    protected char[] getCodePage() {
        return codepage;
    }
}
```

---

## 3. Migration Steps

### Step 1: Prepare Resources Directory (15 min)

```bash
mkdir -p src/resources
# Create ccsid-mappings.json with all 20 single-byte CCSID mappings
# (Extract character arrays from existing CCSID*.java files)
```

### Step 2: Add Dependencies (5 min)

If not already present, add to `build.gradle`:
```gradle
dependencies {
    implementation 'com.google.code.gson:gson:2.10.1'
}
```

### Step 3: Create Factory Classes (30 min)

- Create `CodepageConverterFactory.java`
- Create `ConfigurableCodepageConverter.java`
- Add unit tests for factory

### Step 4: Update BuiltInCodePageFactory (30 min)

**Before**:
```java
public ICodepageConverter getConverter(String sCodePage) {
    if (sCodePage.equals("37")) return new CCSID37().init();
    if (sCodePage.equals("273")) return new CCSID273().init();
    if (sCodePage.equals("500")) return new CCSID500().init();
    // ... 17 more if statements
    if (sCodePage.equals("1148")) return new CCSID1148().init();
    if (sCodePage.equals("930")) return new CCSID930().init();
    return null;
}
```

**After**:
```java
public ICodepageConverter getConverter(String sCodePage) {
    // Try factory first (20 single-byte adapters)
    ICodepageConverter converter = CodepageConverterFactory.getConverter(sCodePage);
    if (converter != null) {
        return converter.init();
    }

    // Handle DBCS cases separately
    if (sCodePage.equals("930")) {
        return new CCSID930().init();
    }

    return null;
}
```

### Step 5: Testing (1 hour)

```bash
# Unit tests for factory
- testGetConverterReturnsValidInstance()
- testAllCCSIDsLoadable()
- testCharacterMappingsAccurate()

# Integration tests
- testConverterBehaviorUnchanged() (character mapping validation)
- testAllCodepagesImplement8BitMapping()

# Performance tests
- testFactoryInitializationTime()
- testConverterInstantiationTime()
```

### Step 6: Cleanup (15 min)

```bash
# Delete 20 CCSID*.java files
rm src/org/hti5250j/encoding/builtin/CCSID{37,273,277,278,280,284,285,297,424,500,870,871,875,1025,1026,1112,1140,1141,1147,1148}.java

# Update project files
# - Remove from build.gradle if listed
# - Update IDE project configuration
```

### Step 7: Documentation (30 min)

- Update CHANGELOG
- Update JavaDoc comments
- Add migration notes to README

---

## 4. Risk Analysis

### Risk 1: JSON Parsing Overhead
**Severity**: LOW
**Mitigation**: JSON parsed once at startup, cached in static Map
**Impact**: <1ms additional startup time

### Risk 2: Classpath Issues
**Severity**: LOW
**Mitigation**: ccsid-mappings.json must be in src/resources for proper packaging
**Impact**: Resource not found at runtime if misconfigured

### Risk 3: Character Array Accuracy
**Severity**: MEDIUM
**Mitigation**:
- Extract arrays programmatically from CCSID*.java files
- Validate extracted arrays match originals
- Unit test all 256 mappings per CCSID
**Impact**: Incorrect mappings could break terminal display

### Risk 4: CCSID930 Compatibility
**Severity**: LOW
**Mitigation**: Leave CCSID930 unchanged (different architecture)
**Impact**: None, completely separate from refactoring

---

## 5. Backward Compatibility

### Public API Changes
- **Method signatures**: NO changes
- **Class names**: All existing CCSID*.java classes deleted (BREAKING if directly imported)
- **Factory interface**: CodepageConverterFactory replaces direct class instantiation

### Migration Path
```java
// Old code:
ICodepageConverter converter = new CCSID500().init();

// New code:
ICodepageConverter converter = CodepageConverterFactory.getConverter("500");
if (converter != null) {
    converter = converter.init();
}

// Through BuiltInCodePageFactory (preferred):
ICodepageConverter converter = factory.getConverter("500");
```

### Impact Assessment
- ✅ Internal use (via BuiltInCodePageFactory): NO change
- ⚠️ Direct imports (rare): Will break, but fixable in minutes
- ✅ API contracts: Preserved (same interface, same behavior)

---

## 6. Performance Implications

### Startup Time
- Current: Minimal (classes loaded on demand or at startup)
- After: +1-2ms for JSON parsing + HashMap population
- **Negligible impact**: JSON parsed once, cached for application lifetime

### Runtime Behavior
- Current: O(1) lookup in factory if statements
- After: O(1) HashMap lookup
- **No change**: Same performance characteristics

### Memory Usage
- Current: 20 CCSID classes in classpath (even if not all used)
- After: Single JSON file, HashMap cached at runtime
- **Improvement**: Slightly smaller runtime footprint

---

## 7. Testing Strategy

### Unit Tests
```java
class CodepageConverterFactoryTest {

    @Test
    void testFactoryLoadsAllCCSIDs() {
        // Verify all 20 single-byte CCSIDs load from JSON
    }

    @Test
    void testCharacterMappingAccuracy() {
        // Verify extracted codepage arrays match CCSID standards
        // Sample test: CCSID37 @ index 0x40 should be ' ' (space)
    }

    @Test
    void testConverterInitialization() {
        // Verify init() properly builds reverse lookup tables
    }
}

class ConfigurableCodepageConverterTest {

    @Test
    void testUnicodeToEbcdicConversion() {
        // Test uni2ebcdic() for sample characters
    }

    @Test
    void testEbcdicToUnicodeConversion() {
        // Test ebcdic2uni() for sample bytes
    }

    @Test
    void testAllCCSIDsConvertIdentically() {
        // Verify each JSON-loaded converter behaves same as original class
    }
}
```

### Integration Tests
```java
class CCSIDRefactoringIntegrationTest {

    @Test
    void testBuiltInFactoryReturnsConverters() {
        // Verify BuiltInCodePageFactory still returns converters
    }

    @Test
    void testTerminalRenderingUnchanged() {
        // Test actual terminal character rendering with new converters
    }
}
```

### Acceptance Tests
```bash
# Run existing terminal display tests
# Verify no visual changes in emulator output
# Test multiple CCSIDs in succession
```

---

## 8. Rollback Plan

If critical issues discovered:

```bash
# 1. Keep backup of original files
git checkout HEAD -- src/org/hti5250j/encoding/builtin/CCSID*.java

# 2. Revert factory changes
git checkout HEAD -- src/org/hti5250j/encoding/builtin/BuiltInCodePageFactory.java

# 3. Remove new files
rm src/org/hti5250j/encoding/builtin/CodepageConverterFactory.java
rm src/org/hti5250j/encoding/builtin/ConfigurableCodepageConverter.java
rm src/resources/ccsid-mappings.json

# 4. Rebuild and test
gradle clean build
```

**Estimated rollback time**: <10 minutes

---

## 9. Success Criteria

- [ ] All 20 single-byte CCSIDs load from JSON
- [ ] Character mapping accuracy validated (256 chars per CCSID)
- [ ] Backward compatibility maintained (through factory)
- [ ] CCSID930 remains unchanged and functional
- [ ] Startup time overhead <5ms
- [ ] All existing terminal tests pass without modification
- [ ] 970+ lines of code eliminated
- [ ] New CCSID addition requires only JSON modification

---

## 10. Future Enhancements

### Post-Refactoring Opportunities

1. **Configuration Management**
   - Allow users to provide custom CCSID mappings
   - Support loading from multiple JSON sources

2. **Validation Tooling**
   - JSON schema validation for ccsid-mappings.json
   - Pre-commit hooks to validate mappings

3. **Documentation Generation**
   - Auto-generate CCSID support matrix from JSON
   - Create human-readable CCSID reference from JSON

4. **Performance Optimization**
   - Pre-compile character mapping arrays to binary format
   - Support lazy-loading of CCSID mappings

---

## Appendix: JSON Schema Validation

Optional JSON schema for validation:

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "ccsid_mappings": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "ccsid_id": {
            "type": "string",
            "pattern": "^[0-9]{2,4}$"
          },
          "name": {
            "type": "string"
          },
          "description": {
            "type": "string"
          },
          "codepage": {
            "type": "array",
            "minItems": 256,
            "maxItems": 256,
            "items": {
              "type": "integer",
              "minimum": 0,
              "maximum": 65535
            }
          }
        },
        "required": ["ccsid_id", "name", "description", "codepage"]
      }
    }
  },
  "required": ["ccsid_mappings"]
}
```

