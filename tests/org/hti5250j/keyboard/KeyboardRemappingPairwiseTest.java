package org.hti5250j.keyboard;

import org.junit.Before;
import org.junit.Test;

import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Pairwise TDD tests for keyboard remapping configuration in HTI5250j.
 *
 * Pairwise dimensions:
 * 1. Key source: [physical, virtual, combined]
 * 2. Target action: [5250-key, local-action, macro, disabled]
 * 3. Modifier handling: [pass-through, consume, translate]
 * 4. Conflict resolution: [first-wins, last-wins, error]
 * 5. Scope: [global, session, application]
 *
 * Test strategy: Generate orthogonal coverage across 5 dimensions to test
 * 25+ scenarios (5^2 coverage) including happy paths and adversarial cases.
 */
public class KeyboardRemappingPairwiseTest {

    private KeyboardRemappingConfiguration config;
    private KeyRemapper remapper;
    private KeyStroker sourceKey;
    private RemappingAction targetAction;

    @Before
    public void setUp() {
        config = new KeyboardRemappingConfiguration();
        remapper = new KeyRemapper(config);
    }

    // ==================== POSITIVE TESTS ====================

    /**
     * Test: Physical key (F1) remapped to 5250 key ([help])
     * Dimensions: Source=physical, Target=5250-key, Modifier=pass-through, Conflict=first-wins, Scope=global
     */
    @Test
    public void testPhysicalKeyToFifty250KeyMapping() {
        sourceKey = new KeyStroker(KeyEvent.VK_F1, false, false, false, false,
                KeyStroker.KEY_LOCATION_STANDARD);
        targetAction = new RemappingAction("[help]", RemappingAction.TYPE_5250_KEY);

        remapper.addMapping(sourceKey, targetAction, "pass-through", "global");

        RemappingAction result = remapper.resolveMapping(sourceKey);
        assertNotNull("Physical F1 should map to [help]", result);
        assertEquals("Target should be [help]", "[help]", result.getTargetName());
        assertEquals("Should be 5250 key type", RemappingAction.TYPE_5250_KEY, result.getType());
    }

    /**
     * Test: Virtual key (Ctrl+A) remapped to local action (copy)
     * Dimensions: Source=virtual, Target=local-action, Modifier=pass-through, Conflict=first-wins, Scope=session
     */
    @Test
    public void testVirtualKeyToLocalActionMapping() {
        sourceKey = new KeyStroker(KeyEvent.VK_A, false, true, false, false,
                KeyStroker.KEY_LOCATION_STANDARD);
        targetAction = new RemappingAction("copy", RemappingAction.TYPE_LOCAL_ACTION);

        remapper.addMapping(sourceKey, targetAction, "pass-through", "session");

        RemappingAction result = remapper.resolveMapping(sourceKey);
        assertNotNull("Ctrl+A should map to copy action", result);
        assertEquals("Target should be copy", "copy", result.getTargetName());
        assertEquals("Scope should be session", "session", remapper.getScope(sourceKey));
    }

    /**
     * Test: Combined key (Shift+Ctrl+Home) remapped to macro
     * Dimensions: Source=combined, Target=macro, Modifier=pass-through, Conflict=first-wins, Scope=global
     */
    @Test
    public void testCombinedKeyToMacroMapping() {
        sourceKey = new KeyStroker(KeyEvent.VK_HOME, true, true, false, false,
                KeyStroker.KEY_LOCATION_STANDARD);
        targetAction = new RemappingAction("macro:startup", RemappingAction.TYPE_MACRO);

        remapper.addMapping(sourceKey, targetAction, "pass-through", "global");

        RemappingAction result = remapper.resolveMapping(sourceKey);
        assertNotNull("Combined key should map to macro", result);
        assertTrue("Target should be macro type", result.getType() == RemappingAction.TYPE_MACRO);
    }

    /**
     * Test: Physical key disabled (mapped to null action)
     * Dimensions: Source=physical, Target=disabled, Modifier=consume, Conflict=first-wins, Scope=application
     */
    @Test
    public void testPhysicalKeyDisabled() {
        sourceKey = new KeyStroker(KeyEvent.VK_PAUSE, false, false, false, false,
                KeyStroker.KEY_LOCATION_STANDARD);
        targetAction = new RemappingAction(null, RemappingAction.TYPE_DISABLED);

        remapper.addMapping(sourceKey, targetAction, "consume", "application");

        RemappingAction result = remapper.resolveMapping(sourceKey);
        assertNotNull("Disabled key should have mapping", result);
        assertEquals("Type should be disabled", RemappingAction.TYPE_DISABLED, result.getType());
        assertTrue("Event should be consumed", remapper.shouldConsume(sourceKey));
    }

    /**
     * Test: Virtual key (Alt+F4) translate modifier to different action
     * Dimensions: Source=virtual, Target=5250-key, Modifier=translate, Conflict=first-wins, Scope=session
     */
    @Test
    public void testVirtualKeyModifierTranslation() {
        sourceKey = new KeyStroker(KeyEvent.VK_F4, false, false, true, false,
                KeyStroker.KEY_LOCATION_STANDARD);
        targetAction = new RemappingAction("[reset]", RemappingAction.TYPE_5250_KEY);

        remapper.addMapping(sourceKey, targetAction, "translate", "session");
        remapper.setModifierTranslation(sourceKey, "Alt", "Ctrl");

        RemappingAction result = remapper.resolveMapping(sourceKey);
        assertNotNull("Alt+F4 should translate", result);
        assertEquals("Should resolve to [reset]", "[reset]", result.getTargetName());
    }

    /**
     * Test: Multiple mappings with first-wins conflict resolution
     * Dimensions: Source=physical, Target=5250-key, Modifier=pass-through, Conflict=first-wins, Scope=global
     */
    @Test
    public void testFirstWinsConflictResolution() {
        sourceKey = new KeyStroker(KeyEvent.VK_F2, false, false, false, false,
                KeyStroker.KEY_LOCATION_STANDARD);
        RemappingAction first = new RemappingAction("[pf2]", RemappingAction.TYPE_5250_KEY);
        RemappingAction second = new RemappingAction("[pf12]", RemappingAction.TYPE_5250_KEY);

        remapper.addMapping(sourceKey, first, "pass-through", "global", "first-wins");
        remapper.addMapping(sourceKey, second, "pass-through", "global", "first-wins");

        RemappingAction result = remapper.resolveMapping(sourceKey);
        assertEquals("First-wins should return initial mapping", "[pf2]", result.getTargetName());
    }

    /**
     * Test: Multiple mappings with last-wins conflict resolution
     * Dimensions: Source=virtual, Target=local-action, Modifier=consume, Conflict=last-wins, Scope=session
     */
    @Test
    public void testLastWinsConflictResolution() {
        sourceKey = new KeyStroker(KeyEvent.VK_C, false, true, false, false,
                KeyStroker.KEY_LOCATION_STANDARD);
        RemappingAction first = new RemappingAction("copy", RemappingAction.TYPE_LOCAL_ACTION);
        RemappingAction second = new RemappingAction("paste", RemappingAction.TYPE_LOCAL_ACTION);

        remapper.addMapping(sourceKey, first, "consume", "session", "last-wins");
        remapper.addMapping(sourceKey, second, "consume", "session", "last-wins");

        RemappingAction result = remapper.resolveMapping(sourceKey);
        assertEquals("Last-wins should return latest mapping", "paste", result.getTargetName());
    }

    /**
     * Test: Scope isolation - global mapping with session override
     * Dimensions: Source=physical, Target=5250-key, Modifier=pass-through, Conflict=first-wins, Scope=[global/session]
     */
    @Test
    public void testScopeIsolationGlobalWithSessionOverride() {
        sourceKey = new KeyStroker(KeyEvent.VK_F5, false, false, false, false,
                KeyStroker.KEY_LOCATION_STANDARD);
        RemappingAction globalAction = new RemappingAction("[pf5]", RemappingAction.TYPE_5250_KEY);
        RemappingAction sessionAction = new RemappingAction("[pf15]", RemappingAction.TYPE_5250_KEY);

        remapper.addMapping(sourceKey, globalAction, "pass-through", "global");
        remapper.addMapping(sourceKey, sessionAction, "pass-through", "session");

        RemappingAction result = remapper.resolveMapping(sourceKey, "session");
        assertEquals("Session scope should override global", "[pf15]", result.getTargetName());
    }

    /**
     * Test: Application scope affects all sessions
     * Dimensions: Source=combined, Target=macro, Modifier=pass-through, Conflict=first-wins, Scope=application
     */
    @Test
    public void testApplicationScopeGlobal() {
        sourceKey = new KeyStroker(KeyEvent.VK_HOME, true, false, false, false,
                KeyStroker.KEY_LOCATION_STANDARD);
        targetAction = new RemappingAction("macro:goto-begin", RemappingAction.TYPE_MACRO);

        remapper.addMapping(sourceKey, targetAction, "pass-through", "application");

        assertTrue("Application scope should be global",
                remapper.isApplicationScope(sourceKey));
    }

    /**
     * Test: Numpad key location differentiation in mapping
     * Dimensions: Source=physical, Target=5250-key, Modifier=pass-through, Conflict=first-wins, Scope=global
     */
    @Test
    public void testNumpadLocationDifferentiation() {
        KeyStroker standardEnter = new KeyStroker(KeyEvent.VK_ENTER, false, false, false, false,
                KeyStroker.KEY_LOCATION_STANDARD);
        KeyStroker numpadEnter = new KeyStroker(KeyEvent.VK_ENTER, false, false, false, false,
                KeyStroker.KEY_LOCATION_NUMPAD);

        RemappingAction standardAction = new RemappingAction("[enter]", RemappingAction.TYPE_5250_KEY);
        RemappingAction numpadAction = new RemappingAction("[newline]", RemappingAction.TYPE_5250_KEY);

        remapper.addMapping(standardEnter, standardAction, "pass-through", "global");
        remapper.addMapping(numpadEnter, numpadAction, "pass-through", "global");

        assertEquals("Standard enter should be [enter]", "[enter]",
                remapper.resolveMapping(standardEnter).getTargetName());
        assertEquals("Numpad enter should be [newline]", "[newline]",
                remapper.resolveMapping(numpadEnter).getTargetName());
    }

    /**
     * Test: Modifier consume prevents event propagation
     * Dimensions: Source=virtual, Target=5250-key, Modifier=consume, Conflict=first-wins, Scope=session
     */
    @Test
    public void testModifierConsumeEventPropagation() {
        sourceKey = new KeyStroker(KeyEvent.VK_F1, false, true, false, false,
                KeyStroker.KEY_LOCATION_STANDARD);
        targetAction = new RemappingAction("[help]", RemappingAction.TYPE_5250_KEY);

        remapper.addMapping(sourceKey, targetAction, "consume", "session");

        assertTrue("Consumed modifier should prevent propagation",
                remapper.shouldConsume(sourceKey));
    }

    // ==================== ADVERSARIAL / ERROR TESTS ====================

    /**
     * Test: Circular mapping detection (key A maps to key B, key B maps back to key A)
     * Dimensions: Source=physical, Target=5250-key, Modifier=pass-through, Conflict=error, Scope=global
     */
    @Test
    public void testCircularMappingDetection() {
        sourceKey = new KeyStroker(KeyEvent.VK_F1, false, false, false, false,
                KeyStroker.KEY_LOCATION_STANDARD);
        KeyStroker targetKey = new KeyStroker(KeyEvent.VK_F2, false, false, false, false,
                KeyStroker.KEY_LOCATION_STANDARD);

        RemappingAction action1 = new RemappingAction("[pf1]", RemappingAction.TYPE_5250_KEY);
        RemappingAction action2 = new RemappingAction("[pf2]", RemappingAction.TYPE_5250_KEY);

        remapper.addMapping(sourceKey, action1, "pass-through", "global", "error");
        remapper.addMapping(targetKey, action2, "pass-through", "global", "error");

        // Simulate circular reference
        remapper.markKeyAsMapped("[pf1]");
        remapper.markKeyAsMapped("[pf2]");

        assertTrue("System should detect circular mapping",
                remapper.hasCircularMapping(sourceKey, targetKey));
    }

    /**
     * Test: Conflicting mapping detection with error resolution
     * Dimensions: Source=physical, Target=5250-key, Modifier=pass-through, Conflict=error, Scope=global
     */
    @Test
    public void testConflictingMappingErrorDetection() {
        sourceKey = new KeyStroker(KeyEvent.VK_F3, false, false, false, false,
                KeyStroker.KEY_LOCATION_STANDARD);
        RemappingAction action1 = new RemappingAction("[pf3]", RemappingAction.TYPE_5250_KEY);
        RemappingAction action2 = new RemappingAction("[pf13]", RemappingAction.TYPE_5250_KEY);

        remapper.addMapping(sourceKey, action1, "pass-through", "global", "error");

        try {
            remapper.addMapping(sourceKey, action2, "pass-through", "global", "error");
            fail("Should throw exception on conflicting mapping with error resolution");
        } catch (KeyRemappingConflictException e) {
            assertTrue("Exception should indicate conflict",
                    e.getMessage() != null && e.getMessage().length() > 0);
        }
    }

    /**
     * Test: Invalid key code (-1) in mapping
     * Dimensions: Source=physical, Target=5250-key, Modifier=pass-through, Conflict=first-wins, Scope=global
     */
    @Test
    public void testInvalidKeyCodeMapping() {
        sourceKey = new KeyStroker(-1, false, false, false, false,
                KeyStroker.KEY_LOCATION_STANDARD);
        targetAction = new RemappingAction("[enter]", RemappingAction.TYPE_5250_KEY);

        try {
            remapper.addMapping(sourceKey, targetAction, "pass-through", "global");
            fail("Should reject invalid key code");
        } catch (IllegalArgumentException e) {
            assertTrue("Exception should indicate invalid key",
                    e.getMessage().contains("invalid") || e.getMessage().contains("key"));
        }
    }

    /**
     * Test: Null target action in mapping
     * Dimensions: Source=physical, Target=disabled, Modifier=pass-through, Conflict=first-wins, Scope=global
     */
    @Test
    public void testNullTargetActionMapping() {
        sourceKey = new KeyStroker(KeyEvent.VK_UNDEFINED, false, false, false, false,
                KeyStroker.KEY_LOCATION_STANDARD);

        try {
            remapper.addMapping(sourceKey, null, "pass-through", "global");
            fail("Should reject null target action");
        } catch (NullPointerException e) {
            assertNotNull("Exception should be thrown", e);
        }
    }

    /**
     * Test: Conflicting scopes - same key mapped in global and application
     * Dimensions: Source=physical, Target=5250-key, Modifier=pass-through, Conflict=error, Scope=[global/application]
     */
    @Test
    public void testConflictingScopeMappings() {
        sourceKey = new KeyStroker(KeyEvent.VK_F6, false, false, false, false,
                KeyStroker.KEY_LOCATION_STANDARD);
        RemappingAction globalAction = new RemappingAction("[pf6]", RemappingAction.TYPE_5250_KEY);
        RemappingAction appAction = new RemappingAction("[pf16]", RemappingAction.TYPE_5250_KEY);

        remapper.addMapping(sourceKey, globalAction, "pass-through", "global");
        remapper.addMapping(sourceKey, appAction, "pass-through", "application");

        // Both should exist but application should take precedence
        assertEquals("Application scope should override global",
                "[pf16]", remapper.resolveMapping(sourceKey, "application").getTargetName());
    }

    /**
     * Test: Self-referential mapping (F1 maps to [pf1], F1 also called [pf1])
     * Dimensions: Source=physical, Target=5250-key, Modifier=pass-through, Conflict=error, Scope=global
     */
    @Test
    public void testSelfReferentialMappingDetection() {
        sourceKey = new KeyStroker(KeyEvent.VK_F1, false, false, false, false,
                KeyStroker.KEY_LOCATION_STANDARD);
        RemappingAction action = new RemappingAction("[pf1]", RemappingAction.TYPE_5250_KEY);

        remapper.addMapping(sourceKey, action, "pass-through", "global", "error");

        assertTrue("Should detect self-referential mapping",
                remapper.hasSelfReferentialMapping(sourceKey));
    }

    /**
     * Test: Multiple modifiers in source key with translate handler
     * Dimensions: Source=combined, Target=5250-key, Modifier=translate, Conflict=first-wins, Scope=session
     */
    @Test
    public void testMultipleModifierTranslation() {
        sourceKey = new KeyStroker(KeyEvent.VK_HOME, true, true, true, false,
                KeyStroker.KEY_LOCATION_STANDARD);
        targetAction = new RemappingAction("[home]", RemappingAction.TYPE_5250_KEY);

        remapper.addMapping(sourceKey, targetAction, "translate", "session");
        remapper.setModifierTranslation(sourceKey, "Shift+Ctrl+Alt", "Alt");

        RemappingAction result = remapper.resolveMapping(sourceKey);
        assertNotNull("Complex modifier should resolve", result);
    }

    /**
     * Test: Macro with parameter passing in remapping
     * Dimensions: Source=virtual, Target=macro, Modifier=pass-through, Conflict=first-wins, Scope=session
     */
    @Test
    public void testMacroWithParameterRemapping() {
        sourceKey = new KeyStroker(KeyEvent.VK_F7, false, false, false, false,
                KeyStroker.KEY_LOCATION_STANDARD);
        targetAction = new RemappingAction("macro:login{param1=user,param2=pass}",
                RemappingAction.TYPE_MACRO);

        remapper.addMapping(sourceKey, targetAction, "pass-through", "session");

        RemappingAction result = remapper.resolveMapping(sourceKey);
        assertNotNull("Macro with parameters should resolve", result);
        assertTrue("Should contain macro parameters",
                result.getTargetName().contains("param"));
    }

    /**
     * Test: Disabled key in session doesn't affect global mapping
     * Dimensions: Source=physical, Target=disabled, Modifier=consume, Conflict=first-wins, Scope=[session/global]
     */
    @Test
    public void testDisabledSessionKeyPreservesGlobal() {
        sourceKey = new KeyStroker(KeyEvent.VK_ESCAPE, false, false, false, false,
                KeyStroker.KEY_LOCATION_STANDARD);
        RemappingAction globalAction = new RemappingAction("[sysreq]", RemappingAction.TYPE_5250_KEY);
        RemappingAction sessionAction = new RemappingAction(null, RemappingAction.TYPE_DISABLED);

        remapper.addMapping(sourceKey, globalAction, "pass-through", "global");
        remapper.addMapping(sourceKey, sessionAction, "consume", "session");

        assertEquals("Session disabled, but global should exist",
                "[sysreq]", remapper.resolveMapping(sourceKey, "global").getTargetName());
        assertNull("Session scope should be disabled",
                remapper.resolveMapping(sourceKey, "session").getTargetName());
    }

    /**
     * Test: Invalid modifier string in translation mapping
     * Dimensions: Source=virtual, Target=5250-key, Modifier=translate, Conflict=first-wins, Scope=global
     */
    @Test
    public void testInvalidModifierStringTranslation() {
        sourceKey = new KeyStroker(KeyEvent.VK_A, false, true, false, false,
                KeyStroker.KEY_LOCATION_STANDARD);
        targetAction = new RemappingAction("[enter]", RemappingAction.TYPE_5250_KEY);

        remapper.addMapping(sourceKey, targetAction, "translate", "global");

        try {
            remapper.setModifierTranslation(sourceKey, "InvalidMod", "Ctrl");
            fail("Should reject invalid modifier string");
        } catch (IllegalArgumentException e) {
            assertTrue("Exception should indicate invalid modifier",
                    e.getMessage().contains("modifier") || e.getMessage().contains("invalid"));
        }
    }

    /**
     * Test: Chain mapping resolution (A -> B, resolve A gets B's target)
     * Dimensions: Source=physical, Target=5250-key, Modifier=pass-through, Conflict=first-wins, Scope=global
     */
    @Test
    public void testChainMappingResolution() {
        sourceKey = new KeyStroker(KeyEvent.VK_F8, false, false, false, false,
                KeyStroker.KEY_LOCATION_STANDARD);
        KeyStroker intermediateKey = new KeyStroker(KeyEvent.VK_F18, true, false, false, false,
                KeyStroker.KEY_LOCATION_STANDARD);

        RemappingAction action1 = new RemappingAction("[pf8]", RemappingAction.TYPE_5250_KEY);
        RemappingAction action2 = new RemappingAction("[pf18]", RemappingAction.TYPE_5250_KEY);

        remapper.addMapping(sourceKey, action1, "pass-through", "global");
        remapper.addMapping(intermediateKey, action2, "pass-through", "global");

        RemappingAction result = remapper.resolveMapping(sourceKey);
        assertEquals("Chain should resolve to first mapping", "[pf8]", result.getTargetName());
    }

    /**
     * Test: Save and restore mapping configuration
     * Dimensions: Source=combined, Target=5250-key, Modifier=pass-through, Conflict=first-wins, Scope=global
     */
    @Test
    public void testSaveAndRestoreMappingConfiguration() {
        sourceKey = new KeyStroker(KeyEvent.VK_HOME, true, false, false, false,
                KeyStroker.KEY_LOCATION_STANDARD);
        RemappingAction action = new RemappingAction("[home]", RemappingAction.TYPE_5250_KEY);

        remapper.addMapping(sourceKey, action, "pass-through", "global");

        Map<String, Object> saved = remapper.exportConfiguration();
        assertNotNull("Configuration should be exportable", saved);
        assertTrue("Saved config should contain mapping", saved.size() > 0);

        KeyboardRemappingConfiguration newConfig = new KeyboardRemappingConfiguration();
        KeyRemapper newRemapper = new KeyRemapper(newConfig);
        newRemapper.importConfiguration(saved);

        RemappingAction restored = newRemapper.resolveMapping(sourceKey);
        assertNotNull("Restored mapping should exist", restored);
        assertEquals("Restored mapping should match original", "[home]", restored.getTargetName());
    }

    /**
     * Test: Right vs left modifier key differentiation
     * Dimensions: Source=physical, Target=5250-key, Modifier=pass-through, Conflict=first-wins, Scope=global
     */
    @Test
    public void testLeftRightModifierDifferentiation() {
        KeyStroker leftShift = new KeyStroker(KeyEvent.VK_SHIFT, false, false, false, false,
                KeyStroker.KEY_LOCATION_LEFT);
        KeyStroker rightShift = new KeyStroker(KeyEvent.VK_SHIFT, false, false, false, false,
                KeyStroker.KEY_LOCATION_RIGHT);

        RemappingAction leftAction = new RemappingAction("[markleft]", RemappingAction.TYPE_5250_KEY);
        RemappingAction rightAction = new RemappingAction("[markright]", RemappingAction.TYPE_5250_KEY);

        remapper.addMapping(leftShift, leftAction, "pass-through", "global");
        remapper.addMapping(rightShift, rightAction, "pass-through", "global");

        assertEquals("Left shift should map to markleft", "[markleft]",
                remapper.resolveMapping(leftShift).getTargetName());
        assertEquals("Right shift should map to markright", "[markright]",
                remapper.resolveMapping(rightShift).getTargetName());
    }

    /**
     * Test: Local action with emulator state dependency
     * Dimensions: Source=physical, Target=local-action, Modifier=pass-through, Conflict=first-wins, Scope=session
     */
    @Test
    public void testLocalActionWithStateDependency() {
        sourceKey = new KeyStroker(KeyEvent.VK_DELETE, false, false, false, false,
                KeyStroker.KEY_LOCATION_STANDARD);
        targetAction = new RemappingAction("delete-if-insert-mode", RemappingAction.TYPE_LOCAL_ACTION);

        remapper.addMapping(sourceKey, targetAction, "pass-through", "session");
        remapper.setActionStateCondition(sourceKey, "insert-mode", true);

        RemappingAction result = remapper.resolveMapping(sourceKey);
        assertNotNull("State-dependent action should resolve", result);
        assertTrue("Should have state condition",
                remapper.hasStateCondition(sourceKey));
    }

    /**
     * Test: AltGr modifier mapping on international keyboard
     * Dimensions: Source=virtual, Target=5250-key, Modifier=pass-through, Conflict=first-wins, Scope=global
     */
    @Test
    public void testAltGrModifierMapping() {
        sourceKey = new KeyStroker(KeyEvent.VK_2, false, false, false, true,
                KeyStroker.KEY_LOCATION_STANDARD);
        targetAction = new RemappingAction("[copy]", RemappingAction.TYPE_5250_KEY);

        remapper.addMapping(sourceKey, targetAction, "pass-through", "global");

        RemappingAction result = remapper.resolveMapping(sourceKey);
        assertNotNull("AltGr+2 should map to [copy]", result);
        assertEquals("Target should be [copy]", "[copy]", result.getTargetName());
    }

    /**
     * Test: Empty configuration state handling
     * Dimensions: Source=null, Target=null, Modifier=null, Conflict=null, Scope=null
     */
    @Test
    public void testEmptyConfigurationState() {
        KeyStroker unmappedKey = new KeyStroker(KeyEvent.VK_ALPHANUMERIC, false, false, false, false,
                KeyStroker.KEY_LOCATION_STANDARD);

        RemappingAction result = remapper.resolveMapping(unmappedKey);
        assertNull("Unmapped key should return null", result);
    }
}
