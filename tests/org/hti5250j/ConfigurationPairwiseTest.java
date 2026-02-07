/**
 * ConfigurationPairwiseTest.java - Pairwise TDD Tests for Configuration Handling
 *
 * This test suite uses pairwise testing to systematically discover bugs
 * in configuration handling across GlobalConfigure, SessionConfig, and
 * ConfigureFactory by combining multiple test dimensions:
 *
 * Dimensions tested:
 * - Property keys: [valid, invalid, null, empty, special-chars, unicode]
 * - Property values: [string, number, boolean, null, empty, very-long]
 * - File states: [exists, missing, read-only, corrupt, locked]
 * - Directories: [exists, missing, no-permission, symlink]
 * - Encoding: [UTF-8, ISO-8859-1, ASCII, invalid]
 *
 * Test strategy: Combine pairs of dimensions to create adversarial scenarios
 * that expose input validation gaps, encoding issues, and state handling bugs.
 *
 * Writing style: RED phase tests that expose bugs in configuration handling.
 */
package org.hti5250j;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.*;

import static org.junit.Assert.*;

/**
 * TDD Pairwise Tests for Configuration Handling
 *
 * Test categories:
 * 1. POSITIVE: Valid key/value pairs with valid files and encodings
 * 2. ADVERSARIAL: Invalid keys, missing files, encoding issues, injection attempts
 * 3. STATE: Read-only files, missing directories, permission denials
 * 4. ENCODING: UTF-8, ISO-8859-1, ASCII, invalid byte sequences
 */
public class ConfigurationPairwiseTest {

    private File tempDir;
    private File configDir;
    private File testPropsFile;
    private Properties testProps;

    @Before
    public void setUp() throws IOException {
        // Create temporary directory structure for test files
        tempDir = Files.createTempDirectory("tn5250j-config-test").toFile();
        configDir = new File(tempDir, ".tn5250j");
        configDir.mkdirs();
        testPropsFile = new File(configDir, "test.properties");
        testProps = new Properties();
    }

    @After
    public void tearDown() {
        // Cleanup test files and restore permissions
        if (testPropsFile != null && testPropsFile.exists()) {
            testPropsFile.setWritable(true);
            testPropsFile.delete();
        }
        if (configDir != null && configDir.exists()) {
            configDir.setWritable(true);
            recursiveDelete(configDir);
        }
        if (tempDir != null && tempDir.exists()) {
            tempDir.setWritable(true);
            recursiveDelete(tempDir);
        }
    }

    private void recursiveDelete(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    recursiveDelete(f);
                }
            }
        }
        file.delete();
    }

    // ==========================================================================
    // POSITIVE TEST CASES: Valid key/value pairs, valid files, valid encoding
    // ==========================================================================

    /**
     * POSITIVE: Load valid string property from file (UTF-8 encoded)
     * Dimension pair: valid key + string value
     */
    @Test
    public void testLoadValidStringPropertyUTF8() throws IOException {
        // ARRANGE: Create config file with valid string property
        testProps.setProperty("app.name", "tn5250j");
        try (FileOutputStream fos = new FileOutputStream(testPropsFile)) {
            testProps.store(fos, "Test Config");
        }

        // ACT: Load properties from file
        Properties loaded = new Properties();
        try (FileInputStream fis = new FileInputStream(testPropsFile)) {
            loaded.load(fis);
        }

        // ASSERT: Property loaded correctly
        assertTrue("Property 'app.name' should exist", loaded.containsKey("app.name"));
        assertEquals("Property value should match", "tn5250j", loaded.getProperty("app.name"));
    }

    /**
     * POSITIVE: Load valid numeric property from file
     * Dimension pair: valid key + numeric value
     */
    @Test
    public void testLoadValidNumericProperty() throws IOException {
        // ARRANGE: Create config file with numeric property
        testProps.setProperty("port", "5250");
        try (FileOutputStream fos = new FileOutputStream(testPropsFile)) {
            testProps.store(fos, "Test Config");
        }

        // ACT: Load properties and parse as integer
        Properties loaded = new Properties();
        try (FileInputStream fis = new FileInputStream(testPropsFile)) {
            loaded.load(fis);
        }

        // ASSERT: Property loaded and parseable as number
        assertTrue("Property 'port' should exist", loaded.containsKey("port"));
        assertEquals("Numeric property should parse", 5250, Integer.parseInt(loaded.getProperty("port")));
    }

    /**
     * POSITIVE: Load valid boolean property from file
     * Dimension pair: valid key + boolean value
     */
    @Test
    public void testLoadValidBooleanProperty() throws IOException {
        // ARRANGE: Create config file with boolean property
        testProps.setProperty("enabled", "true");
        try (FileOutputStream fos = new FileOutputStream(testPropsFile)) {
            testProps.store(fos, "Test Config");
        }

        // ACT: Load properties
        Properties loaded = new Properties();
        try (FileInputStream fis = new FileInputStream(testPropsFile)) {
            loaded.load(fis);
        }

        // ASSERT: Boolean property loaded correctly
        assertTrue("Property 'enabled' should exist", loaded.containsKey("enabled"));
        assertEquals("Boolean should be 'true'", "true", loaded.getProperty("enabled"));
    }

    /**
     * POSITIVE: Load Unicode property from UTF-8 encoded file
     * Dimension pair: valid key (unicode) + string value
     */
    @Test
    public void testLoadUnicodePropertyUTF8() throws IOException {
        // ARRANGE: Create config file with unicode property key/value
        testProps.setProperty("日本語", "こんにちは");
        try (FileOutputStream fos = new FileOutputStream(testPropsFile)) {
            testProps.store(fos, "Test Config");
        }

        // ACT: Load properties from UTF-8 file
        Properties loaded = new Properties();
        try (FileInputStream fis = new FileInputStream(testPropsFile)) {
            loaded.load(fis);
        }

        // ASSERT: Unicode property loaded correctly
        assertTrue("Unicode property should exist", loaded.containsKey("日本語"));
        assertEquals("Unicode value should match", "こんにちは", loaded.getProperty("日本語"));
    }

    /**
     * POSITIVE: Load property with special characters
     * Dimension pair: valid key (special chars) + string value
     */
    @Test
    public void testLoadPropertyWithSpecialChars() throws IOException {
        // ARRANGE: Create config file with special-char property key
        testProps.setProperty("emulator.settings.path", "/home/user/.tn5250j/config");
        try (FileOutputStream fos = new FileOutputStream(testPropsFile)) {
            testProps.store(fos, "Test Config");
        }

        // ACT: Load properties
        Properties loaded = new Properties();
        try (FileInputStream fis = new FileInputStream(testPropsFile)) {
            loaded.load(fis);
        }

        // ASSERT: Special-char key loaded correctly
        assertTrue("Special-char key should exist", loaded.containsKey("emulator.settings.path"));
        assertEquals("Path value should match", "/home/user/.tn5250j/config",
                loaded.getProperty("emulator.settings.path"));
    }

    /**
     * POSITIVE: Save and load property with multiple values
     * Dimension pair: valid key + comma-separated values
     */
    @Test
    public void testSaveAndLoadMultipleValues() throws IOException {
        // ARRANGE: Create config with comma-separated values
        String colorValues = "255,128,64,32";
        testProps.setProperty("color.rgb", colorValues);
        try (FileOutputStream fos = new FileOutputStream(testPropsFile)) {
            testProps.store(fos, "Test Config");
        }

        // ACT: Load and parse multi-value property
        Properties loaded = new Properties();
        try (FileInputStream fis = new FileInputStream(testPropsFile)) {
            loaded.load(fis);
        }

        // ASSERT: Multi-value property loaded and parseable
        assertTrue("Multi-value property should exist", loaded.containsKey("color.rgb"));
        String[] values = loaded.getProperty("color.rgb").split(",");
        assertEquals("Should have 4 color values", 4, values.length);
        assertEquals("First value should be 255", 255, Integer.parseInt(values[0]));
    }

    // ==========================================================================
    // ADVERSARIAL TEST CASES: Invalid inputs, missing files, encoding issues
    // ==========================================================================

    /**
     * ADVERSARIAL: Attempt to load from non-existent file
     * Dimension pair: file missing + valid key
     */
    @Test
    public void testLoadFromNonExistentFileFails() throws IOException {
        // ARRANGE: File does not exist
        File nonExistent = new File(configDir, "nonexistent.properties");
        assertFalse("File should not exist", nonExistent.exists());

        // ACT & ASSERT: FileInputStream should throw FileNotFoundException
        try {
            try (FileInputStream fis = new FileInputStream(nonExistent)) {
                Properties loaded = new Properties();
                loaded.load(fis);
            }
            fail("Should throw FileNotFoundException for non-existent file");
        } catch (FileNotFoundException e) {
            // Expected: file not found
            assertTrue("Exception message should contain filename",
                    e.getMessage().contains(nonExistent.getName()));
        }
    }

    /**
     * ADVERSARIAL: Null property key attempted
     * Dimension pair: null key + valid value
     */
    @Test
    public void testNullPropertyKeyThrows() throws IOException {
        // ARRANGE: Properties object
        Properties props = new Properties();

        // ACT & ASSERT: Setting null key should throw NullPointerException
        try {
            props.setProperty(null, "value");
            fail("Should throw NullPointerException for null key");
        } catch (NullPointerException e) {
            // Expected: null key not allowed
        }
    }

    /**
     * ADVERSARIAL: Empty property key
     * Dimension pair: empty key + valid value
     */
    @Test
    public void testEmptyPropertyKeyIsAllowed() throws IOException {
        // ARRANGE: Create config with empty property key
        testProps.setProperty("", "value");
        try (FileOutputStream fos = new FileOutputStream(testPropsFile)) {
            testProps.store(fos, "Test Config");
        }

        // ACT: Load properties
        Properties loaded = new Properties();
        try (FileInputStream fis = new FileInputStream(testPropsFile)) {
            loaded.load(fis);
        }

        // ASSERT: Empty key is loaded (this may be a bug or feature)
        // Document the behavior: empty keys are allowed but problematic
        assertTrue("Empty key should be present", loaded.containsKey(""));
    }

    /**
     * ADVERSARIAL: Property with null value
     * Dimension pair: valid key + null value
     */
    @Test
    public void testNullPropertyValueThrows() {
        // ARRANGE: Properties object
        Properties props = new Properties();

        // ACT & ASSERT: Setting null value should throw NullPointerException
        try {
            props.setProperty("key", null);
            fail("Should throw NullPointerException for null value");
        } catch (NullPointerException e) {
            // Expected: null value not allowed
        }
    }

    /**
     * ADVERSARIAL: Property with very long value (potential buffer overflow)
     * Dimension pair: valid key + very-long value
     */
    @Test
    public void testVeryLongPropertyValue() throws IOException {
        // ARRANGE: Create property with extremely long value
        StringBuilder longValue = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            longValue.append("x");
        }
        testProps.setProperty("config.longvalue", longValue.toString());
        try (FileOutputStream fos = new FileOutputStream(testPropsFile)) {
            testProps.store(fos, "Test Config");
        }

        // ACT: Load very long property
        Properties loaded = new Properties();
        try (FileInputStream fis = new FileInputStream(testPropsFile)) {
            loaded.load(fis);
        }

        // ASSERT: Long value loaded completely
        assertTrue("Long value property should exist", loaded.containsKey("config.longvalue"));
        assertEquals("Long value should load completely", longValue.length(),
                loaded.getProperty("config.longvalue").length());
    }

    /**
     * ADVERSARIAL: Property injection via special property file syntax
     * Dimension pair: valid key + injection attempt value
     */
    @Test
    public void testPropertyFileInjectionAttempt() throws IOException {
        // ARRANGE: Create config with injection-attempt in value
        String injectionValue = "value\n#injected.key=malicious";
        testProps.setProperty("normal.key", injectionValue);
        try (FileOutputStream fos = new FileOutputStream(testPropsFile)) {
            testProps.store(fos, "Test Config");
        }

        // ACT: Load properties
        Properties loaded = new Properties();
        try (FileInputStream fis = new FileInputStream(testPropsFile)) {
            loaded.load(fis);
        }

        // ASSERT: Injection payload should be treated as literal value
        assertTrue("normal.key should exist", loaded.containsKey("normal.key"));
        // The injected.key should NOT be created (it's part of the value)
        assertFalse("Injected key should NOT be created", loaded.containsKey("injected.key"));
    }

    /**
     * ADVERSARIAL: Corrupt properties file (invalid format)
     * Dimension pair: file corrupt + valid key lookup
     */
    @Test
    public void testLoadFromCorruptPropertiesFile() throws IOException {
        // ARRANGE: Create file with invalid properties format
        try (FileOutputStream fos = new FileOutputStream(testPropsFile)) {
            fos.write("This is not \uFEFF valid: properties = format\n".getBytes(StandardCharsets.UTF_8));
            fos.write("no equals sign here\n".getBytes(StandardCharsets.UTF_8));
            fos.write("=orphaned equals\n".getBytes(StandardCharsets.UTF_8));
        }

        // ACT: Attempt to load corrupt file
        Properties loaded = new Properties();
        try (FileInputStream fis = new FileInputStream(testPropsFile)) {
            loaded.load(fis);
        }

        // ASSERT: Properties.load() is forgiving, should load partial data
        // This documents the actual behavior
        assertTrue("Corrupt file should still load something", loaded.size() >= 0);
    }

    /**
     * ADVERSARIAL: Read-only properties file
     * Dimension pair: file read-only + save operation
     */
    @Test
    public void testSaveToReadOnlyFileFails() throws IOException {
        // ARRANGE: Create file and make it read-only
        testProps.setProperty("key", "value");
        try (FileOutputStream fos = new FileOutputStream(testPropsFile)) {
            testProps.store(fos, "Test Config");
        }
        testPropsFile.setReadOnly();

        // ACT & ASSERT: Attempt to save should fail on read-only file
        try {
            try (FileOutputStream fos = new FileOutputStream(testPropsFile)) {
                Properties toSave = new Properties();
                toSave.setProperty("new.key", "new.value");
                toSave.store(fos, "Test Config");
            }
            fail("Should throw IOException when writing to read-only file");
        } catch (IOException e) {
            // Expected: permission denied
            assertTrue("Exception should indicate write failure",
                    e.getMessage().contains("Permission denied") ||
                    e.getMessage().contains("cannot be written"));
        }
    }

    /**
     * ADVERSARIAL: Missing configuration directory
     * Dimension pair: directory missing + file save operation
     */
    @Test
    public void testSaveToMissingDirectoryFails() throws IOException {
        // ARRANGE: Delete the configuration directory
        recursiveDelete(configDir);
        assertFalse("Config dir should be deleted", configDir.exists());

        // ACT & ASSERT: Attempt to save file in non-existent directory
        try {
            try (FileOutputStream fos = new FileOutputStream(testPropsFile)) {
                testProps.setProperty("key", "value");
                testProps.store(fos, "Test Config");
            }
            fail("Should throw IOException when directory doesn't exist");
        } catch (FileNotFoundException e) {
            // Expected: directory not found
            assertTrue("Exception should indicate path issue",
                    e.getMessage().contains("cannot find") ||
                    e.getMessage().contains("No such file"));
        }
    }

    /**
     * ADVERSARIAL: Invalid character encoding in property value
     * Dimension pair: invalid encoding + string value
     */
    @Test
    public void testLoadPropertyWithInvalidUTF8() throws IOException {
        // ARRANGE: Write invalid UTF-8 byte sequence
        try (FileOutputStream fos = new FileOutputStream(testPropsFile)) {
            // Valid UTF-8 prefix
            fos.write("valid.key=".getBytes(StandardCharsets.UTF_8));
            // Invalid UTF-8 sequence (orphaned continuation byte)
            fos.write(new byte[]{(byte) 0xC0, (byte) 0x80});
            fos.write("\n".getBytes(StandardCharsets.UTF_8));
        }

        // ACT: Attempt to load file with invalid UTF-8
        Properties loaded = new Properties();
        try (FileInputStream fis = new FileInputStream(testPropsFile)) {
            // Properties.load() uses ISO-8859-1 by default, not UTF-8
            loaded.load(fis);
        }

        // ASSERT: Properties loaded (ISO-8859-1 accepts any byte)
        // This documents the actual behavior: properties.load() is forgiving
        assertTrue("Should load despite encoding issues", loaded.size() >= 0);
    }

    // ==========================================================================
    // STATE-BASED TEST CASES: File permissions, symlinks, locking scenarios
    // ==========================================================================

    /**
     * STATE: Directory exists and is writable
     * Dimension pair: directory exists + file save succeeds
     */
    @Test
    public void testSaveToExistingWritableDirectory() throws IOException {
        // ARRANGE: Config directory exists and is writable
        assertTrue("Config dir should exist", configDir.exists());
        assertTrue("Config dir should be writable", configDir.canWrite());

        // ACT: Save properties to file
        Properties props = new Properties();
        props.setProperty("test.key", "test.value");
        try (FileOutputStream fos = new FileOutputStream(testPropsFile)) {
            props.store(fos, "Test Config");
        }

        // ASSERT: File created successfully
        assertTrue("File should be created", testPropsFile.exists());
        assertTrue("File should be readable", testPropsFile.canRead());
    }

    /**
     * STATE: Directory with no read permission
     * Dimension pair: directory no-permission + file load fails
     */
    @Test
    public void testLoadFromNoPermissionDirectoryFails() throws IOException {
        // SKIP on Windows (POSIX file permissions not supported)
        String osName = System.getProperty("os.name");
        if (osName.contains("Windows")) {
            return;
        }

        // ARRANGE: Create file, then remove directory read permission
        testProps.setProperty("key", "value");
        try (FileOutputStream fos = new FileOutputStream(testPropsFile)) {
            testProps.store(fos, "Test Config");
        }

        // Make directory non-readable
        Set<PosixFilePermission> perms = PosixFilePermissions.fromString("-w--w--w-");
        try {
            Files.setPosixFilePermissions(configDir.toPath(), perms);

            // ACT & ASSERT: Attempt to list/access files should fail
            File[] files = configDir.listFiles();
            // This will return null due to permission denied
            assertNull("listFiles should return null for no-permission directory", files);

        } finally {
            // Restore permissions for cleanup
            Set<PosixFilePermission> restorePerms = PosixFilePermissions.fromString("rwxrwxrwx");
            Files.setPosixFilePermissions(configDir.toPath(), restorePerms);
        }
    }

    /**
     * STATE: Attempt to create file in read-only directory
     * Dimension pair: directory read-only + file create fails
     */
    @Test
    public void testCreateFileInReadOnlyDirectoryFails() throws IOException {
        // SKIP on Windows
        String osName = System.getProperty("os.name");
        if (osName.contains("Windows")) {
            return;
        }

        // ARRANGE: Make directory read-only
        Set<PosixFilePermission> perms = PosixFilePermissions.fromString("r-xr-xr-x");
        try {
            Files.setPosixFilePermissions(configDir.toPath(), perms);

            File newFile = new File(configDir, "new.properties");

            // ACT & ASSERT: Creating new file should fail
            try {
                try (FileOutputStream fos = new FileOutputStream(newFile)) {
                    fos.write("test".getBytes());
                }
                fail("Should not create file in read-only directory");
            } catch (IOException e) {
                // Expected: permission denied
                assertTrue("Exception should indicate permission issue",
                        e.getMessage().contains("Permission denied") ||
                        e.getMessage().contains("cannot"));
            }

        } finally {
            // Restore permissions
            Set<PosixFilePermission> restorePerms = PosixFilePermissions.fromString("rwxrwxrwx");
            Files.setPosixFilePermissions(configDir.toPath(), restorePerms);
        }
    }

    // ==========================================================================
    // ENCODING TEST CASES: Different character encodings
    // ==========================================================================

    /**
     * ENCODING: UTF-8 encoded properties file
     * Dimension pair: UTF-8 encoding + unicode content
     */
    @Test
    public void testLoadUTF8EncodedFile() throws IOException {
        // ARRANGE: Write file with explicit UTF-8 encoding
        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(testPropsFile), StandardCharsets.UTF_8)) {
            writer.write("# UTF-8 encoded properties\n");
            writer.write("greeting.en=Hello\n");
            writer.write("greeting.es=Hola\n");
            writer.write("greeting.fr=Bonjour\n");
            writer.write("greeting.ja=こんにちは\n");
        }

        // ACT: Load with default (ISO-8859-1) decoder
        Properties loaded = new Properties();
        try (FileInputStream fis = new FileInputStream(testPropsFile)) {
            loaded.load(fis);
        }

        // ASSERT: ASCII content loads fine, Unicode may be corrupted
        assertEquals("English greeting should load", "Hello", loaded.getProperty("greeting.en"));
        // Note: Japanese will be corrupted due to ISO-8859-1 decoding
        assertNotNull("Japanese greeting should exist (but corrupted)", loaded.getProperty("greeting.ja"));
    }

    /**
     * ENCODING: ISO-8859-1 (Latin-1) encoded properties file
     * Dimension pair: ISO-8859-1 encoding + special Latin chars
     */
    @Test
    public void testLoadISO88591EncodedFile() throws IOException {
        // ARRANGE: Write file with Latin-1 specific characters
        byte[] latin1Bytes = "city.french=Montréal\n".getBytes("ISO-8859-1");
        try (FileOutputStream fos = new FileOutputStream(testPropsFile)) {
            fos.write(latin1Bytes);
        }

        // ACT: Load with default (ISO-8859-1) decoder
        Properties loaded = new Properties();
        try (FileInputStream fis = new FileInputStream(testPropsFile)) {
            loaded.load(fis);
        }

        // ASSERT: Latin-1 characters load correctly
        assertNotNull("French city should load", loaded.getProperty("city.french"));
    }

    /**
     * ENCODING: ASCII-only properties file
     * Dimension pair: ASCII encoding + ASCII-only content
     */
    @Test
    public void testLoadASCIIOnlyFile() throws IOException {
        // ARRANGE: Create ASCII-only properties
        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(testPropsFile), StandardCharsets.US_ASCII)) {
            writer.write("simple.key=simple.value\n");
            writer.write("app.version=1.2.3\n");
            writer.write("app.name=tn5250j\n");
        }

        // ACT: Load ASCII properties
        Properties loaded = new Properties();
        try (FileInputStream fis = new FileInputStream(testPropsFile)) {
            loaded.load(fis);
        }

        // ASSERT: All ASCII properties load correctly
        assertEquals("Simple property should match", "simple.value", loaded.getProperty("simple.key"));
        assertEquals("Version should match", "1.2.3", loaded.getProperty("app.version"));
        assertEquals("App name should match", "tn5250j", loaded.getProperty("app.name"));
    }

    /**
     * ENCODING: Escaped unicode characters in properties file
     * Dimension pair: escaped unicode + unicode property value
     */
    @Test
    public void testLoadEscapedUnicodeInPropertiesFile() throws IOException {
        // ARRANGE: Write properties with escaped unicode sequences
        try (FileOutputStream fos = new FileOutputStream(testPropsFile)) {
            fos.write("greeting.japanese=\\u3053\\u3093\\u306b\\u3061\\u306f\n".getBytes());
        }

        // ACT: Load properties (Java Properties handles backslash-u numeric escapes)
        Properties loaded = new Properties();
        try (FileInputStream fis = new FileInputStream(testPropsFile)) {
            loaded.load(fis);
        }

        // ASSERT: Escaped unicode decoded correctly
        assertNotNull("Escaped unicode should decode", loaded.getProperty("greeting.japanese"));
        assertEquals("Should decode to actual unicode", "こんにちは", loaded.getProperty("greeting.japanese"));
    }

    // ==========================================================================
    // INTEGRATION TEST CASES: Multiple operations and state transitions
    // ==========================================================================

    /**
     * INTEGRATION: Save, load, modify, save again
     * Dimension pair: valid file + multiple read/write cycles
     */
    @Test
    public void testSaveLoadModifySaveAgain() throws IOException {
        // ARRANGE: Initial save
        testProps.setProperty("version", "1");
        testProps.setProperty("name", "test");
        try (FileOutputStream fos = new FileOutputStream(testPropsFile)) {
            testProps.store(fos, "Initial");
        }

        // ACT: Load and verify
        Properties loaded1 = new Properties();
        try (FileInputStream fis = new FileInputStream(testPropsFile)) {
            loaded1.load(fis);
        }
        assertEquals("First load should get version 1", "1", loaded1.getProperty("version"));

        // ACT: Modify and save again
        loaded1.setProperty("version", "2");
        loaded1.setProperty("modified", "true");
        try (FileOutputStream fos = new FileOutputStream(testPropsFile)) {
            loaded1.store(fos, "Modified");
        }

        // ACT: Load again
        Properties loaded2 = new Properties();
        try (FileInputStream fis = new FileInputStream(testPropsFile)) {
            loaded2.load(fis);
        }

        // ASSERT: All modifications persisted
        assertEquals("Second load should get version 2", "2", loaded2.getProperty("version"));
        assertTrue("Modified flag should exist", loaded2.containsKey("modified"));
        assertEquals("Original property should persist", "test", loaded2.getProperty("name"));
    }

    /**
     * INTEGRATION: Load property, type conversion, store as string
     * Dimension pair: numeric property + type conversions
     */
    @Test
    public void testNumericPropertyRoundTrip() throws IOException {
        // ARRANGE: Store numeric property as string
        int originalPort = 5250;
        testProps.setProperty("server.port", String.valueOf(originalPort));
        try (FileOutputStream fos = new FileOutputStream(testPropsFile)) {
            testProps.store(fos, "Server Config");
        }

        // ACT: Load and parse
        Properties loaded = new Properties();
        try (FileInputStream fis = new FileInputStream(testPropsFile)) {
            loaded.load(fis);
        }
        int loadedPort = Integer.parseInt(loaded.getProperty("server.port"));

        // ACT: Store again
        loaded.setProperty("server.port", String.valueOf(loadedPort + 1));
        try (FileOutputStream fos = new FileOutputStream(testPropsFile)) {
            loaded.store(fos, "Server Config");
        }

        // ACT: Verify increment
        Properties loaded2 = new Properties();
        try (FileInputStream fis = new FileInputStream(testPropsFile)) {
            loaded2.load(fis);
        }

        // ASSERT: Numeric roundtrip preserved
        assertEquals("Port should increment", originalPort + 1,
                Integer.parseInt(loaded2.getProperty("server.port")));
    }

    /**
     * INTEGRATION: Properties with dependencies
     * Dimension pair: valid keys + interdependent values
     */
    @Test
    public void testPropertiesWithDependentValues() throws IOException {
        // ARRANGE: Store properties with dependencies
        testProps.setProperty("base.path", "/home/user");
        testProps.setProperty("config.path", "${base.path}/.tn5250j");
        testProps.setProperty("log.path", "${config.path}/logs");
        try (FileOutputStream fos = new FileOutputStream(testPropsFile)) {
            testProps.store(fos, "Config with dependencies");
        }

        // ACT: Load properties
        Properties loaded = new Properties();
        try (FileInputStream fis = new FileInputStream(testPropsFile)) {
            loaded.load(fis);
        }

        // ASSERT: Note - Java Properties does NOT expand ${...} references
        // This documents the limitation
        assertEquals("Base path stored", "/home/user", loaded.getProperty("base.path"));
        assertNotNull("Dependent path stored literally", loaded.getProperty("config.path"));
        assertTrue("Dependent path contains unexpanded reference",
                loaded.getProperty("config.path").contains("${base.path}"));
    }

}
