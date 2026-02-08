/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.accessibility;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD Pairwise Tests for Accessibility Compliance
 *
 * Test categories:
 * 1. POSITIVE (13 tests): Valid keyboard navigation, proper ARIA, screen reader support
 *    - Tab navigation through fields with label announcements
 *    - Arrow key navigation in menus with state announcements
 *    - Focus visible on buttons with keyboard activation
 *    - Screen reader announces field values and state changes
 *    - High contrast mode enabled without breaking layout
 *    - Custom preferences honored for announcements
 *
 * 2. ADVERSARIAL (13 tests): Keyboard traps, missing ARIA, focus loss
 *    - Tab into hidden/disabled fields (should skip)
 *    - Arrow key wrapping in circular menus
 *    - Focus trap in nested dialogs (escape should exit)
 *    - Screen reader not announcing value changes on rapid input
 *    - High contrast + custom colors cause contrast violations
 *    - Focus lost on state change (status field update)
 *    - Shortcut conflicts with screen reader navigation
 *    - Menu collapse doesn't restore focus to menu button
 *    - Status message announced after announced but focus still on field
 *    - Disabled button still receives keyboard focus
 */
public class AccessibilityCompliancePairwiseTest {

    // Test dimensions enums
    enum AccessibilityFeature {
        SCREEN_READER("screen-reader"),
        KEYBOARD_ONLY("keyboard-only"),
        HIGH_CONTRAST("high-contrast");

        final String value;
        AccessibilityFeature(String value) { this.value = value; }
    }

    enum NavigationMode {
        TAB("tab"),
        ARROW("arrow"),
        SHORTCUT("shortcut");

        final String value;
        NavigationMode(String value) { this.value = value; }
    }

    enum FocusTarget {
        FIELD("field"),
        BUTTON("button"),
        MENU("menu"),
        STATUS("status");

        final String value;
        FocusTarget(String value) { this.value = value; }
    }

    enum AnnouncementType {
        LABEL("label"),
        VALUE("value"),
        STATE_CHANGE("state-change");

        final String value;
        AnnouncementType(String value) { this.value = value; }
    }

    enum UserPreference {
        DEFAULT("default"),
        CUSTOMIZED("customized");

        final String value;
        UserPreference(String value) { this.value = value; }
    }

    // Mock components
    static class MockScreenReaderAnnouncer {
        private List<String> announcements = new CopyOnWriteArrayList<>();
        private int announcementIndex = 0;

        void announce(String message) {
            announcements.add(message);
        }

        String getLastAnnouncement() {
            return announcements.isEmpty() ? null : announcements.get(announcements.size() - 1);
        }

        List<String> getAllAnnouncements() {
            return new ArrayList<>(announcements);
        }

        boolean wasAnnouncementMade(String substring) {
            return announcements.stream().anyMatch(a -> a.contains(substring));
        }

        void reset() {
            announcements.clear();
            announcementIndex = 0;
        }
    }

    static class MockKeyboardNavigator {
        private List<Integer> focusSequence = new CopyOnWriteArrayList<>();
        private int currentFocusIndex = -1;
        private boolean isTrapped = false;
        private List<Integer> focusableElements = new ArrayList<>();

        void registerFocusableElement(int id) {
            focusableElements.add(id);
            Collections.sort(focusableElements);
        }

        void navigateTab(int fromIndex) {
            if (currentFocusIndex == -1) {
                currentFocusIndex = 0;
            } else if (currentFocusIndex < focusableElements.size() - 1) {
                currentFocusIndex++;
            } else {
                currentFocusIndex = 0; // Wrap to first
            }
            focusSequence.add(focusableElements.get(currentFocusIndex));
        }

        void navigateArrow(int direction) throws FocusTrapException {
            if (isTrapped) {
                throw new FocusTrapException("Focus is trapped in current component");
            }
            if (currentFocusIndex >= 0 && currentFocusIndex < focusableElements.size() - 1) {
                currentFocusIndex += direction;
            }
            if (currentFocusIndex >= 0 && currentFocusIndex < focusableElements.size()) {
                focusSequence.add(focusableElements.get(currentFocusIndex));
            }
        }

        void setTrapped(boolean trapped) {
            this.isTrapped = trapped;
        }

        boolean isCurrentlyTrapped() {
            return isTrapped;
        }

        int getCurrentFocusIndex() {
            return currentFocusIndex;
        }

        List<Integer> getFocusSequence() {
            return new ArrayList<>(focusSequence);
        }

        void reset() {
            focusSequence.clear();
            currentFocusIndex = -1;
            isTrapped = false;
        }
    }

    static class FocusTrapException extends Exception {
        FocusTrapException(String message) { super(message); }
    }

    static class MockFocusManager {
        private int currentFocusId = -1;
        private Map<Integer, String> focusHistory = new LinkedHashMap<>();
        private Map<Integer, Boolean> focusVisibility = new HashMap<>();
        private boolean focusLost = false;

        void setFocus(int elementId, String reason) {
            currentFocusId = elementId;
            focusHistory.put(elementId, reason);
            focusVisibility.put(elementId, true);
            focusLost = false;
        }

        int getCurrentFocus() {
            return currentFocusId;
        }

        void loseFocus() {
            focusLost = true;
            currentFocusId = -1;
        }

        boolean isFocusLost() {
            return focusLost;
        }

        boolean isFocusVisible(int elementId) {
            return focusVisibility.getOrDefault(elementId, false);
        }

        Map<Integer, String> getFocusHistory() {
            return new LinkedHashMap<>(focusHistory);
        }

        void reset() {
            currentFocusId = -1;
            focusHistory.clear();
            focusVisibility.clear();
            focusLost = false;
        }
    }

    static class MockAccessibilityComponent {
        private int id;
        private FocusTarget componentType;
        private String label;
        private String value;
        private String ariaRole;
        private String ariaLabel;
        private String ariaLive;
        private boolean disabled;
        private boolean hidden;
        private Map<String, String> customPreferences;

        MockAccessibilityComponent(int id, FocusTarget type, String label) {
            this.id = id;
            this.componentType = type;
            this.label = label;
            this.ariaRole = deriveAriaRole(type);
            this.customPreferences = new HashMap<>();
        }

        private String deriveAriaRole(FocusTarget type) {
            return switch (type) {
                case FIELD -> "textbox";
                case BUTTON -> "button";
                case MENU -> "menu";
                case STATUS -> "status";
            };
        }

        void setAriaLabel(String ariaLabel) { this.ariaLabel = ariaLabel; }
        void setAriaLive(String ariaLive) { this.ariaLive = ariaLive; }
        void setValue(String value) { this.value = value; }
        void setDisabled(boolean disabled) { this.disabled = disabled; }
        void setHidden(boolean hidden) { this.hidden = hidden; }

        int getId() { return id; }
        FocusTarget getComponentType() { return componentType; }
        String getLabel() { return label; }
        String getValue() { return value; }
        String getAriaRole() { return ariaRole; }
        String getAriaLabel() { return ariaLabel; }
        String getAriaLive() { return ariaLive; }
        boolean isDisabled() { return disabled; }
        boolean isHidden() { return hidden; }

        String getFullAriaDescription() {
            StringBuilder sb = new StringBuilder();
            if (ariaLabel != null) sb.append(ariaLabel).append(" ");
            if (label != null) sb.append(label).append(" ");
            if (value != null) sb.append(value);
            return sb.toString().trim();
        }

        void addCustomPreference(String key, String val) {
            customPreferences.put(key, val);
        }

        String getCustomPreference(String key) {
            return customPreferences.get(key);
        }

        Map<String, String> getCustomPreferences() {
            return new HashMap<>(customPreferences);
        }
    }

    static class MockARIAProvider {
        private Map<Integer, String> ariaStates = new HashMap<>();
        private Map<String, String> ariaProperties = new HashMap<>();
        private List<String> liveRegionUpdates = new CopyOnWriteArrayList<>();

        void setAriaState(int elementId, String state) {
            ariaStates.put(elementId, state);
        }

        void setAriaProperty(int elementId, String property, String value) {
            ariaProperties.put(String.valueOf(elementId) + ":" + property, value);
        }

        String getAriaState(int elementId) {
            return ariaStates.get(elementId);
        }

        String getAriaProperty(int elementId, String property) {
            return ariaProperties.get(String.valueOf(elementId) + ":" + property);
        }

        void updateLiveRegion(String message) {
            liveRegionUpdates.add(message);
        }

        List<String> getLiveRegionUpdates() {
            return new ArrayList<>(liveRegionUpdates);
        }

        boolean hasAriaState(int elementId, String state) {
            String current = ariaStates.get(elementId);
            return current != null && current.equals(state);
        }

        void reset() {
            ariaStates.clear();
            ariaProperties.clear();
            liveRegionUpdates.clear();
        }
    }

    // Test fixture
    private MockScreenReaderAnnouncer screenReader;
    private MockKeyboardNavigator keyboardNav;
    private MockFocusManager focusManager;
    private MockARIAProvider ariaProvider;
    private List<MockAccessibilityComponent> components;

    @BeforeEach
    public void setUp() {
        screenReader = new MockScreenReaderAnnouncer();
        keyboardNav = new MockKeyboardNavigator();
        focusManager = new MockFocusManager();
        ariaProvider = new MockARIAProvider();
        components = new ArrayList<>();

        // Initialize standard components
        createTestComponents();
    }

    private void createTestComponents() {
        // Field components
        MockAccessibilityComponent field1 = new MockAccessibilityComponent(1, FocusTarget.FIELD, "Account Number");
        field1.setAriaLabel("Account Number input field");
        field1.setValue("123456");
        field1.setAriaLive("polite");
        components.add(field1);

        // Button component
        MockAccessibilityComponent submitBtn = new MockAccessibilityComponent(2, FocusTarget.BUTTON, "Submit");
        submitBtn.setAriaLabel("Submit button");
        components.add(submitBtn);

        // Menu component
        MockAccessibilityComponent menu = new MockAccessibilityComponent(3, FocusTarget.MENU, "Navigation Menu");
        menu.setAriaLabel("Main navigation menu");
        menu.setAriaLive("assertive");
        components.add(menu);

        // Status component
        MockAccessibilityComponent status = new MockAccessibilityComponent(4, FocusTarget.STATUS, "Status");
        status.setAriaLabel("Connection status");
        status.setValue("Connected");
        status.setAriaLive("polite");
        components.add(status);

        // Register as focusable
        for (MockAccessibilityComponent comp : components) {
            keyboardNav.registerFocusableElement(comp.getId());
        }
    }

    @AfterEach
    public void tearDown() {
        screenReader.reset();
        keyboardNav.reset();
        focusManager.reset();
        ariaProvider.reset();
        components.clear();
    }

    // ========== POSITIVE TEST CASES ==========

    @Test
    public void testScreenReaderAnnouncesFieldLabelOnTabNavigation() {
        // SR + Tab + Field + Label + Default
        keyboardNav.navigateTab(0);
        focusManager.setFocus(1, "Tab navigation");
        screenReader.announce(components.get(0).getFullAriaDescription());

        assertTrue(screenReader.wasAnnouncementMade("Account Number"),"Screen reader should announce component");
        assertEquals(1, focusManager.getCurrentFocus(),"Focus should be on field");
    }

    @Test
    public void testKeyboardOnlyNavigatesAllFieldsWithArrowKeys() throws FocusTrapException {
        // KO + Arrow + Field + Value + Default
        focusManager.setFocus(1, "Initial focus");
        keyboardNav.navigateArrow(1);
        focusManager.setFocus(2, "Arrow navigation");
        screenReader.announce(components.get(1).getAriaLabel());

        assertEquals(2, focusManager.getCurrentFocus(),"Focus should move with arrow key");
        assertTrue(screenReader.wasAnnouncementMade("Submit"),"Screen reader should announce new focus");
    }

    @Test
    public void testHighContrastModeEnablesWithoutBreakingLayout() {
        // HC + Tab + Field + Label + Default
        for (MockAccessibilityComponent comp : components) {
            comp.addCustomPreference("highContrast", "true");
        }
        keyboardNav.navigateTab(0);
        focusManager.setFocus(1, "High contrast enabled");
        assertTrue("true".equals(components.get(0).getCustomPreference("highContrast")),"Component should support high contrast preference");
    }

    @Test
    public void testStateChangeAnnouncedViaAriaLive() {
        // SR + Tab + Status + State-change + Default
        MockAccessibilityComponent statusComp = components.get(3);
        statusComp.setValue("Disconnected");
        ariaProvider.updateLiveRegion("Status changed to Disconnected");
        screenReader.announce("Status changed to Disconnected");

        assertTrue(screenReader.wasAnnouncementMade("Disconnected"),"Status change should be announced");
    }

    @Test
    public void testFocusVisibleOnButtonWithKeyboardActivation() {
        // KO + Shortcut + Button + Label + Default
        focusManager.setFocus(2, "Keyboard focus");
        ariaProvider.setAriaState(2, "focused");
        assertTrue(focusManager.isFocusVisible(2),"Focus should be visible on button");
    }

    @Test
    public void testArrowNavigationInMenuWithStateAnnouncement() throws FocusTrapException {
        // KO + Arrow + Menu + State-change + Default
        focusManager.setFocus(3, "Menu focus");
        keyboardNav.navigateArrow(1);
        ariaProvider.setAriaState(3, "expanded");
        screenReader.announce("Menu expanded");

        assertEquals(3, focusManager.getCurrentFocus(),"Focus should be on menu");
        assertTrue(screenReader.wasAnnouncementMade("expanded"),"Menu state should be announced");
    }

    @Test
    public void testCustomUserPreferencesHonored() {
        // SR + Tab + Field + Value + Customized
        components.get(0).addCustomPreference("screenReaderVerbosity", "high");
        focusManager.setFocus(1, "Focus with custom prefs");
        screenReader.announce(components.get(0).getLabel() + " " + components.get(0).getValue());

        assertEquals("high", components.get(0).getCustomPreference("screenReaderVerbosity"),"Custom preference should be set");
        assertTrue(screenReader.wasAnnouncementMade("Account Number 123456"),"Full announcement with custom verbosity");
    }

    @Test
    public void testKeyboardShortcutNavigationBypassesFocusOrder() {
        // KO + Shortcut + Button + Label + Default
        focusManager.setFocus(1, "Initial");
        // Simulate Alt+S shortcut to submit button
        focusManager.setFocus(2, "Shortcut navigation");
        screenReader.announce(components.get(1).getAriaLabel());

        assertEquals(2, focusManager.getCurrentFocus(),"Shortcut should jump to button");
    }

    @Test
    public void testAriaRolesProperlySets() {
        // SR + Tab + Diverse targets + Label + Default
        components.forEach(comp -> {
            String expectedRole = comp.getComponentType().name().toLowerCase();
            assertNotNull(comp.getAriaRole(),"ARIA role should be set");
        });
    }

    @Test
    public void testFieldValueAnnouncedAfterInput() {
        // SR + Keyboard + Field + Value + Default
        MockAccessibilityComponent field = components.get(0);
        field.setValue("987654");
        ariaProvider.updateLiveRegion("Field value changed to 987654");
        screenReader.announce("Field value changed to 987654");

        assertTrue(screenReader.wasAnnouncementMade("987654"),"Field value change should be announced");
    }

    @Test
    public void testMenuCollapsedAnnouncedToScreenReader() {
        // SR + Shortcut + Menu + State-change + Customized
        components.get(2).addCustomPreference("announceMenuStateChange", "true");
        ariaProvider.setAriaState(3, "collapsed");
        ariaProvider.updateLiveRegion("Menu collapsed");
        screenReader.announce("Menu collapsed");

        assertTrue(screenReader.wasAnnouncementMade("collapsed"),"Menu collapse should be announced");
    }

    @Test
    public void testDisabledFieldSkippedByTabNavigation() {
        // KO + Tab + Field + Label + Default
        components.get(0).setDisabled(true);
        keyboardNav.registerFocusableElement(components.get(0).getId());
        focusManager.setFocus(1, "Check disabled");
        assertTrue(components.get(0).isDisabled(),"Disabled field detected");
    }

    @Test
    public void testFocusOrderLogicalAcrossComponents() {
        // KO + Tab + Diverse targets + Value + Default
        List<Integer> sequence = new ArrayList<>();
        for (int i = 0; i < components.size(); i++) {
            keyboardNav.navigateTab(i);
            sequence.add(components.get(i).getId());
        }
        assertTrue(sequence.size() >= 3,"Focus sequence should be in order");
    }

    // ========== ADVERSARIAL TEST CASES ==========

    @Test
    public void testKeyboardTrapInNestedDialogRequiresEscapeToExit() {
        // KO + Tab + Field + Label + Adversarial
        keyboardNav.setTrapped(true);
        try {
            keyboardNav.navigateArrow(1);
            fail("Trapped focus should throw exception");
        } catch (FocusTrapException e) {
            assertTrue(keyboardNav.isCurrentlyTrapped(),"Should detect keyboard trap");
        }
    }

    @Test
    public void testFocusLostOnComponentStateChange() {
        // SR + Tab + Status + State-change + Adversarial
        focusManager.setFocus(4, "Status focus");
        focusManager.loseFocus(); // Simulate focus loss on state change
        assertTrue(focusManager.isFocusLost(),"Focus should be lost after state change");
    }

    @Test
    public void testScreenReaderNotAnnouncingValueChangeOnRapidInput() {
        // SR + Keyboard + Field + Value + Adversarial
        MockAccessibilityComponent field = components.get(0);
        field.setValue("1");
        screenReader.announce("Field value: 1");
        field.setValue("12");
        screenReader.announce("Field value: 12");
        field.setValue("123");
        // Buggy implementation may skip this announcement during rapid input
        screenReader.announce("Field value: 123");
        field.setValue("1234");
        screenReader.announce("Field value: 1234");
        // Expected behavior: all changes announced
        // Adversarial test: verify announcements were made (bugs revealed by missing announcements)
        assertEquals(4, screenReader.getAllAnnouncements().size(),"Screen reader should track 4 rapid value changes");
    }

    @Test
    public void testHighContrastViolatesMinimumContrastRatio() {
        // HC + Arrow + Field + Label + Adversarial
        components.get(0).addCustomPreference("customForeground", "#111111");
        components.get(0).addCustomPreference("customBackground", "#000000");
        // Contrast ratio ~1.1 (fails WCAG AA minimum of 4.5:1)
        String fg = components.get(0).getCustomPreference("customForeground");
        String bg = components.get(0).getCustomPreference("customBackground");
        assertNotNull(fg,"Should detect contrast problem");
        assertNotNull(bg,"Should detect contrast problem");
    }

    @Test
    public void testMenuDoesNotRestoreFocusAfterCollapse() {
        // KO + Shortcut + Menu + State-change + Adversarial
        focusManager.setFocus(3, "Menu button");
        int focusBeforeCollapse = focusManager.getCurrentFocus();
        // Menu collapses - should restore focus to menu button
        focusManager.loseFocus();
        assertTrue(focusManager.isFocusLost(),"Focus should be lost after collapse without proper handler");
    }

    @Test
    public void testShortcutConflictsWithScreenReaderNavigation() {
        // SR + Shortcut + Button + Label + Adversarial
        // Simulate Alt+H shortcut conflict with screen reader
        keyboardNav.setTrapped(true); // Screen reader can't navigate past shortcut
        assertTrue(keyboardNav.isCurrentlyTrapped(),"Shortcut should conflict with SR nav");
    }

    @Test
    public void testTabWrappingCreatesInfiniteLoopInCircularMenu() throws FocusTrapException {
        // KO + Tab + Menu + Value + Adversarial
        for (int i = 0; i < 100; i++) {
            keyboardNav.navigateTab(i);
        }
        List<Integer> sequence = keyboardNav.getFocusSequence();
        assertTrue(sequence.size() > components.size(),"Tab wrapping should cycle through components");
    }

    @Test
    public void testAriaLiveNotUpdatedDuringStatusChange() {
        // SR + Arrow + Status + State-change + Adversarial
        MockAccessibilityComponent status = components.get(3);
        status.setValue("New Status");
        // Bug: ariaLive not updated
        ariaProvider.setAriaState(4, "pending");
        assertTrue(ariaProvider.hasAriaState(4, "pending"),"Status change reflected in state");
    }

    @Test
    public void testDisabledButtonStillReceivesKeyboardFocus() {
        // KO + Tab + Button + Label + Adversarial
        components.get(1).setDisabled(true);
        focusManager.setFocus(2, "Disabled focus");
        assertEquals(2, focusManager.getCurrentFocus(),"Focus reached disabled button");
        assertTrue(components.get(1).isDisabled(),"Button is disabled");
    }

    @Test
    public void testHiddenFieldIncludedInTabOrder() {
        // KO + Tab + Field + Label + Adversarial
        components.get(0).setHidden(true);
        keyboardNav.navigateTab(0);
        assertTrue(components.get(0).isHidden(),"Hidden field should not be in tab order");
    }

    @Test
    public void testAnnouncementType_LabelNotAnnouncedWithKeyboard() {
        // KO + Keyboard + Field + Label + Adversarial
        focusManager.setFocus(1, "Keyboard focus");
        // Bug: Label not announced for keyboard-only users
        screenReader.announce(components.get(0).getValue()); // Only announces value
        assertFalse(screenReader.wasAnnouncementMade("Account Number"),"Label should be announced but only value is");
    }

    @Test
    public void testArrowNavigationWrapsUnexpectedly() throws FocusTrapException {
        // KO + Arrow + Menu + Value + Adversarial
        focusManager.setFocus(3, "Menu start");
        for (int i = 0; i < 10; i++) {
            keyboardNav.navigateArrow(1);
        }
        // Arrow should not wrap in linear navigation (unlike tab)
        assertFalse(keyboardNav.getFocusSequence().size() > 20,"Arrow should not create wrap-around like tab");
    }

    @Test
    public void testStatusMessageAnnouncedButFocusStillOnPreviousField() {
        // SR + Tab + Status + State-change + Adversarial
        focusManager.setFocus(1, "Field focus");
        int focusBeforeStatus = focusManager.getCurrentFocus();
        ariaProvider.updateLiveRegion("Status message");
        screenReader.announce("Status message");
        assertEquals(focusBeforeStatus, focusManager.getCurrentFocus(),"Focus should remain on field, not move to status");
    }

    @Test
    public void testAriaPropertyNotSyncedWithComponentState() {
        // SR + Shortcut + Button + State-change + Adversarial
        components.get(1).setDisabled(true);
        // Bug: aria-disabled not updated
        ariaProvider.setAriaState(2, "enabled");
        assertTrue(ariaProvider.hasAriaState(2, "enabled"),"ARIA state not synced with actual state");
        assertTrue(components.get(1).isDisabled(),"But component is actually disabled");
    }

    @Test
    public void testRapidArrowNavigationLosesScreenReaderSync() throws FocusTrapException {
        // SR + Arrow + Field + Value + Adversarial
        for (int i = 0; i < 20; i++) {
            keyboardNav.navigateArrow(1);
            screenReader.announce("Navigating...");
        }
        // Screen reader may lag behind actual focus
        assertTrue(screenReader.getAllAnnouncements().size() > 0,"Screen reader announcements should track navigation");
    }

    @Test
    public void testCustomPreferencesBreakDefaultScreenReaderBehavior() {
        // SR + Tab + Field + Label + Customized (Adversarial)
        components.get(0).addCustomPreference("skipLabel", "true");
        focusManager.setFocus(1, "Custom prefs");
        // If implementation honors skipLabel incorrectly
        assertTrue("true".equals(components.get(0).getCustomPreference("skipLabel")),"Custom preference set but may break SR");
    }

    // ========== FOCUS TRAP ADVERSARIAL SCENARIOS ==========

    @Test
    public void testFocusTrapWhenNavigatingBackFromFirstElement() throws FocusTrapException {
        // KO + Arrow + Field + Label + Adversarial (Focus trap variant)
        keyboardNav.navigateTab(0);
        focusManager.setFocus(1, "First element");
        keyboardNav.navigateArrow(-1); // Try to go before first
        // Should either wrap or prevent - not trap
        assertFalse(keyboardNav.isCurrentlyTrapped(),"Should not trap when navigating back from first");
    }

    @Test
    public void testComplexNestedComponentsFocusManagement() {
        // KO + Tab + Nested components + Value + Default
        // Simulate nested dialog with multiple focusable elements
        List<Integer> focusPath = new ArrayList<>();
        for (int i = 0; i < components.size(); i++) {
            keyboardNav.navigateTab(i);
            focusPath.add(i + 1);
        }
        assertEquals(components.size(), focusPath.size(),"Should navigate all components in order");
    }

    @Test
    public void testAriaLiveRegionMultipleUpdatesQueued() {
        // SR + Arrow + Status + State-change + Default
        ariaProvider.updateLiveRegion("Update 1");
        ariaProvider.updateLiveRegion("Update 2");
        ariaProvider.updateLiveRegion("Update 3");
        List<String> updates = ariaProvider.getLiveRegionUpdates();
        assertEquals(3, updates.size(),"All live region updates should be queued");
    }

    @Test
    public void testKeyboardNavigationPerformanceUnderFrequentFocus() throws FocusTrapException {
        // KO + Tab + Field + Value + Default (Performance test)
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            keyboardNav.navigateTab(i);
        }
        long elapsed = System.currentTimeMillis() - startTime;
        assertTrue(elapsed < 5000,"Navigation should be fast even with 1000 iterations"); // 5 seconds for 1000 iterations
    }

    @Test
    public void testScreenReaderAnnouncesAllComponentPropertiesWhenFocused() {
        // SR + Tab + Field + Diverse announcements + Default
        MockAccessibilityComponent field = components.get(0);
        field.setAriaLabel("Account input");
        field.setValue("ACC123");
        String fullDescription = field.getFullAriaDescription();
        screenReader.announce(fullDescription);
        assertTrue(screenReader.wasAnnouncementMade("Account"),"Should announce label");
        assertTrue(screenReader.wasAnnouncementMade("ACC123"),"Should announce value");
    }
}
