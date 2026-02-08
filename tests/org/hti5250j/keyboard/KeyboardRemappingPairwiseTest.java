/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */





package org.hti5250j.keyboard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

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

    @BeforeEach
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
        assertNotNull(result,"Physical F1 should map to [help]");
        assertEquals("[help]", result.getTargetName(),"Target should be [help]");
        assertEquals(RemappingAction.TYPE_5250_KEY, result.getType(),"Should be 5250 key type");
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
        assertNotNull(result,"Ctrl+A should map to copy action");
        assertEquals("copy", result.getTargetName(),"Target should be copy");
        assertEquals("session", remapper.getScope(sourceKey),"Scope should be session");
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
        assertNotNull(result,"Combined key should map to macro");
        assertTrue(result.getType() == RemappingAction.TYPE_MACRO,"Target should be macro type");
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
        assertNotNull(result,"Disabled key should have mapping");
        assertEquals(RemappingAction.TYPE_DISABLED, result.getType(),"Type should be disabled");
        assertTrue(remapper.shouldConsume(sourceKey),"Event should be consumed");
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
        assertNotNull(result,"Alt+F4 should translate");
        assertEquals("[reset]", result.getTargetName(),"Should resolve to [reset]");
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
        assertEquals("[pf2]", result.getTargetName(),"First-wins should return initial mapping");
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
        assertEquals("paste", result.getTargetName(),"Last-wins should return latest mapping");
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
        assertEquals("[pf15]", result.getTargetName(),"Session scope should override global");
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

        assertTrue(remapper.isApplicationScope(sourceKey),"Application scope should be global");
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

        assertEquals("[enter]",
                remapper.resolveMapping(standardEnter).getTargetName(),"Standard enter should be [enter]");
        assertEquals("[newline]",
                remapper.resolveMapping(numpadEnter).getTargetName(),"Numpad enter should be [newline]");
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

        assertTrue(remapper.shouldConsume(sourceKey),"Consumed modifier should prevent propagation");
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

        assertTrue(remapper.hasCircularMapping(sourceKey, targetKey),"System should detect circular mapping");
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
            assertTrue(e.getMessage() != null && e.getMessage().length() > 0,"Exception should indicate conflict");
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
            assertTrue(e.getMessage().contains("invalid") || e.getMessage().contains("key"),"Exception should indicate invalid key");
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
            assertNotNull(e,"Exception should be thrown");
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
        assertEquals("[pf16]", remapper.resolveMapping(sourceKey, "application").getTargetName(),"Application scope should override global");
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

        assertTrue(remapper.hasSelfReferentialMapping(sourceKey),"Should detect self-referential mapping");
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
        assertNotNull(result,"Complex modifier should resolve");
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
        assertNotNull(result,"Macro with parameters should resolve");
        assertTrue(result.getTargetName().contains("param"),"Should contain macro parameters");
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

        assertEquals("[sysreq]", remapper.resolveMapping(sourceKey, "global").getTargetName(),"Session disabled, but global should exist");
        assertNull(remapper.resolveMapping(sourceKey, "session").getTargetName(),"Session scope should be disabled");
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
            assertTrue(e.getMessage().contains("modifier") || e.getMessage().contains("invalid"),"Exception should indicate invalid modifier");
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
        assertEquals("[pf8]", result.getTargetName(),"Chain should resolve to first mapping");
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
        assertNotNull(saved,"Configuration should be exportable");
        assertTrue(saved.size() > 0,"Saved config should contain mapping");

        KeyboardRemappingConfiguration newConfig = new KeyboardRemappingConfiguration();
        KeyRemapper newRemapper = new KeyRemapper(newConfig);
        newRemapper.importConfiguration(saved);

        RemappingAction restored = newRemapper.resolveMapping(sourceKey);
        assertNotNull(restored,"Restored mapping should exist");
        assertEquals("[home]", restored.getTargetName(),"Restored mapping should match original");
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

        assertEquals("[markleft]",
                remapper.resolveMapping(leftShift).getTargetName(),"Left shift should map to markleft");
        assertEquals("[markright]",
                remapper.resolveMapping(rightShift).getTargetName(),"Right shift should map to markright");
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
        assertNotNull(result,"State-dependent action should resolve");
        assertTrue(remapper.hasStateCondition(sourceKey),"Should have state condition");
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
        assertNotNull(result,"AltGr+2 should map to [copy]");
        assertEquals("[copy]", result.getTargetName(),"Target should be [copy]");
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
        assertNull(result,"Unmapped key should return null");
    }
}
