/*
 * SPDX-FileCopyrightText: Copyright (c) 2001 - 2004
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: Kenneth J. Pouncey
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.hti5250j.event.EmulatorActionEvent;
import org.hti5250j.event.EmulatorActionListener;
import org.hti5250j.event.SessionChangeEvent;
import org.hti5250j.event.SessionConfigEvent;
import org.hti5250j.event.SessionConfigListener;
import org.hti5250j.event.SessionJumpEvent;
import org.hti5250j.event.SessionJumpListener;
import org.hti5250j.event.SessionListener;
import org.hti5250j.framework.tn5250.Rect;
import org.hti5250j.framework.tn5250.Screen5250;
import org.hti5250j.framework.tn5250.tnvt;
import org.hti5250j.gui.ConfirmTabCloseDialog;
import org.hti5250j.keyboard.KeyboardHandler;
import org.hti5250j.keyboard.KeyMnemonicSerializer;
import org.hti5250j.mailtools.SendEMailDialog;
import org.hti5250j.sessionsettings.SessionSettings;
import org.hti5250j.tools.LangTool;
import org.hti5250j.tools.Macronizer;
import org.hti5250j.tools.logging.HTI5250jLogFactory;
import org.hti5250j.tools.logging.HTI5250jLogger;

import static org.hti5250j.SessionConfig.*;
import static org.hti5250j.keyboard.KeyMnemonic.ENTER;

/**
 * A host GUI session
 * (Hint: old name was SessionGUI)
 */
public class SessionPanel extends JPanel implements RubberBandCanvasIF, SessionConfigListener, SessionListener {

    private static final long serialVersionUID = 1L;

    private boolean firstScreen;
    private char[] signonSave;
    private boolean headlessMode = false;

    private Screen5250 screen;
    protected Session5250 session;
    private GuiGraphicBuffer guiGraBuf;
    protected TNRubberBand rubberband;
    private KeypadPanel keypadPanel;
    private String newMacName;
    private Vector<SessionJumpListener> sessionJumpListeners = null;
    private Vector<EmulatorActionListener> actionListeners = null;
    private boolean macroRunning;
    private boolean stopMacro;
    private boolean doubleClick;
    protected SessionConfig sesConfig;
    protected KeyboardHandler keyHandler;
    private final SessionScroller scroller = new SessionScroller();

    private final HTI5250jLogger log = HTI5250jLogFactory.getLogger(this.getClass());

    public SessionPanel(Session5250 session) {
        this.keypadPanel = new KeypadPanel(session.getConfiguration().getConfig());
        this.session = session;

        sesConfig = session.getConfiguration();

        try {
            jbInit();
        } catch (Exception exception) {
            log.warn("Error in constructor: " + exception.getMessage());
        }

        session.getConfiguration().addSessionConfigListener(this);
        session.addSessionListener(this);
    }

    //Component initialization
    private void jbInit() throws Exception {
        this.setLayout(new BorderLayout());
        session.setGUI(this);
        screen = session.getScreen();

        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent componentEvent) {
                resizeMe();
            }
        });

        // Only initialize GUI components if not in headless mode
        if (!headlessMode) {
            ensureGuiGraphicBufferInitialized();
        }

        setRubberBand(new TNRubberBand(this));
        keyHandler = KeyboardHandler.getKeyboardHandlerInstance(session);

        if (!sesConfig.isPropertyExists("width") ||
                !sesConfig.isPropertyExists("height")) {
            // set the initialize size
            if (guiGraBuf != null) {
                this.setSize(guiGraBuf.getPreferredSize());
            } else {
                // Headless mode: use reasonable defaults
                this.setSize(640, 480);
            }
        } else {

            int width = 640, height = 480;
            try {
                if (sesConfig.isPropertyExists("width")) {
                    width = Integer.parseInt(sesConfig.getProperties().getProperty("width"));
                }
                if (sesConfig.isPropertyExists("height")) {
                    height = Integer.parseInt(sesConfig.getProperties().getProperty("height"));
                }
            } catch (NumberFormatException e) {
                // Use defaults
            }
            this.setSize(width, height);
        }

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent mouseEvent) {
                /** @todo check for popup trigger on linux
                 *
                 */
                //	            if (mouseEvent.isPopupTrigger()) {
                // using SwingUtilities because popuptrigger does not work on linux
                if (SwingUtilities.isRightMouseButton(mouseEvent)) {
                    actionPopup(mouseEvent);
                }

            }

            @Override
            public void mouseClicked(MouseEvent mouseEvent) {

                if (SwingUtilities.isRightMouseButton(mouseEvent)) {
                    return;
                }

                if (mouseEvent.getClickCount() == 2 & doubleClick) {
                    screen.sendKeys(ENTER);
                } else {
                    int pos = guiGraBuf.getPosFromView(mouseEvent.getX(), mouseEvent.getY());
                    if (log.isDebugEnabled()) {
                        log.debug((screen.getRow(pos)) + "," + (screen.getCol(pos)));
                        log.debug(mouseEvent.getX() + "," + mouseEvent.getY() + "," + guiGraBuf.columnWidth + ","
                                + guiGraBuf.rowHeight);
                    }

                    boolean moved = screen.moveCursor(pos);
                    // this is a note to not execute this code here when we
                    // implement the remain after edit function option.
                    if (moved) {
                        if (rubberband.isAreaSelected()) {
                            rubberband.reset();
                        }
                        screen.repaintScreen();
                    }
                    getFocusForMe();
                }
            }

        });

        if (YES.equals(sesConfig.getProperties().getProperty("mouseWheel", ""))) {
            scroller.addMouseWheelListener(this);
        }

        log.debug("Initializing macros");
        Macronizer.init();

        keypadPanel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                screen.sendKeys(((JButton) actionEvent.getSource()).getActionCommand());
                getFocusForMe();
            }
        });
        keypadPanel.setVisible(sesConfig.getConfig().isKeypadEnabled());
        this.add(keypadPanel, BorderLayout.SOUTH);

        this.requestFocus();

        doubleClick = YES.equals(sesConfig.getProperties().getProperty("doubleClick", ""));
    }

    public void setRunningHeadless(boolean headless) {
        this.headlessMode = headless;

        if (headless) {
            // Remove listeners if GUI component exists
            if (guiGraBuf != null) {
                screen.getOIA().removeOIAListener(guiGraBuf);
                screen.removeScreenListener(guiGraBuf);
                guiGraBuf = null; // Free ~2MB
            }
            // Prevent future initialization
        } else {
            // Re-enable GUI if needed
            if (guiGraBuf == null) {
                ensureGuiGraphicBufferInitialized();
            }
            screen.getOIA().addOIAListener(guiGraBuf);
            screen.addScreenListener(guiGraBuf);
        }
    }

    @Override
    public void processKeyEvent(KeyEvent evt) {

        keyHandler.processKeyEvent(evt);

        if (!evt.isConsumed())
            super.processKeyEvent(evt);
    }

    public void sendScreenEMail() {
        new SendEMailDialog((JFrame) SwingUtilities.getRoot(this), this);
    }

    /**
     * This routine allows areas to be bounded by using the keyboard
     *
     * @param ke
     * @param last
     */
    public void doKeyBoundArea(KeyEvent ke, String last) {

        Point selectionPoint = new Point();

        // If there is not area selected then we send to the previous position
        // of the cursor because the cursor position has already been updated
        // to the current position.
        //
        // The getPointFromRowCol is 0,0 based so we will take the current row
        // and column and make these calculations ourselves to be passed
        if (!rubberband.isAreaSelected()) {

            // mark left we will mark the column to the right of where the cursor
            // is now.
            if (last.equals("[markleft]"))
                guiGraBuf.getPointFromRowCol(screen.getCurrentRow() - 1,
                        screen.getCurrentCol() + 1,
                        selectionPoint);
            // mark right will mark the current position to the left of the
            // current cursor position
            if (last.equals("[markright]"))
                guiGraBuf.getPointFromRowCol(screen.getCurrentRow() - 1,
                        screen.getCurrentCol() - 2,
                        selectionPoint);


            if (last.equals("[markup]"))
                guiGraBuf.getPointFromRowCol(screen.getCurrentRow() + 1,
                        screen.getCurrentCol() - 1,
                        selectionPoint);
            // mark down will mark the current position minus the current
            // row.
            if (last.equals("[markdown]"))
                guiGraBuf.getPointFromRowCol(screen.getCurrentRow() - 2,
                        screen.getCurrentCol() - 1,
                        selectionPoint);
            MouseEvent mousePressedEvent = new MouseEvent(this,
                    MouseEvent.MOUSE_PRESSED,
                    System.currentTimeMillis(),
                    InputEvent.BUTTON1_MASK,
                    selectionPoint.x, selectionPoint.y,
                    1, false);
            dispatchEvent(mousePressedEvent);

        }

        guiGraBuf.getPointFromRowCol(screen.getCurrentRow() - 1,
                screen.getCurrentCol() - 1,
                selectionPoint);
        //	      rubberband.getCanvas().translateEnd(selectionPoint);
        MouseEvent mouseDraggedEvent = new MouseEvent(this,
                MouseEvent.MOUSE_DRAGGED,
                System.currentTimeMillis(),
                InputEvent.BUTTON1_MASK,
                selectionPoint.x, selectionPoint.y,
                1, false);
        dispatchEvent(mouseDraggedEvent);

    }


    /**
     * @param reallyclose TRUE if session/tab should be closed;
     *                    FALSE, if only ask for confirmation
     * @return True if closed; False if still open
     */
    public boolean confirmCloseSession(boolean reallyclose) {
        // regular, only ask on connected sessions
        boolean close = !isConnected() || confirmTabClose();
        if (close) {
            // special case, no SignonScreen than confirm signing off
            close = isOnSignOnScreen() || confirmSignOffClose();
        }
        if (close && reallyclose) {
            fireEmulatorAction(EmulatorActionEvent.CLOSE_SESSION);
        }
        return close;
    }

    /**
     * Asks the user to confirm tab close,
     * only if configured (option 'confirm tab close')
     *
     * @return true if tab should be closed, false if not
     */
    private boolean confirmTabClose() {
        boolean result = true;
        if (session.getConfiguration().isPropertyExists("confirmTabClose")) {
            this.requestFocus();
            final ConfirmTabCloseDialog tabclsdlg = new ConfirmTabCloseDialog(this);
            if (YES.equals(session.getConfiguration().getProperties().getProperty("confirmTabClose", ""))) {
                if (!tabclsdlg.show()) {
                    result = false;
                }
            }
        }
        return result;
    }

    /**
     * Check is the parameter to confirm that the Sign On screen is the current
     * screen.  If it is then we check against the saved Signon Screen in memory
     * and take the appropriate action.
     *
     * @return whether or not the signon on screen is the current screen
     */
    private boolean confirmSignOffClose() {

        if (sesConfig.isPropertyExists("confirmSignoff") &&
                YES.equals(sesConfig.getProperties().getProperty("confirmSignoff", ""))) {
            this.requestFocus();
            int result = JOptionPane.showConfirmDialog(
                    this.getParent(),            // the parent that the dialog blocks
                    LangTool.getString("messages.signOff"),  // the dialog message array
                    LangTool.getString("cs.title"),    // the title of the dialog window
                    JOptionPane.CANCEL_OPTION        // option type
            );

            if (result == 0) {
                return true;
            }

            return false;
        }
        return true;
    }

    public void getFocusForMe() {
        this.grabFocus();
    }

    @Override
    public boolean isFocusTraversable() {
        return true;
    }

    // Override to inform focus manager that component is managing focus changes.
    //    This is to capture the tab and shift+tab keys.
    @Override
    public boolean isManagingFocus() {
        return true;
    }

    @Override
    public void onConfigChanged(SessionConfigEvent configEvent) {
        final String configName = configEvent.getPropertyName();

        if (CONFIG_KEYPAD_ENABLED.equals(configName)) {
            keypadPanel.setVisible(YES.equals(configEvent.getNewValue()));
            this.validate();
        }

        if (CONFIG_KEYPAD_MNEMONICS.equals(configName)) {
            keypadPanel.reInitializeButtons(new KeyMnemonicSerializer().deserialize((String) configEvent.getNewValue()));
        }

        if (CONFIG_KEYPAD_FONT_SIZE.equals(configName)) {
            keypadPanel.updateButtonFontSize(Float.parseFloat((String) configEvent.getNewValue()));
        }

        if ("doubleClick".equals(configName)) {
            doubleClick = YES.equals(configEvent.getNewValue());
        }

        if ("mouseWheel".equals(configName)) {
            if (YES.equals(configEvent.getNewValue())) {
                scroller.addMouseWheelListener(this);
            } else {
                scroller.removeMouseWheelListener(this);
            }
        }

        resizeMe();
        repaint();
    }

    public tnvt getVT() {

        return session.getVT();

    }

    public void toggleDebug() {
        session.getVT().toggleDebug();
    }

    public void startNewSession() {
        fireEmulatorAction(EmulatorActionEvent.START_NEW_SESSION);
    }

    public void startDuplicateSession() {
        fireEmulatorAction(EmulatorActionEvent.START_DUPLICATE);
    }

    /**
     * Toggles connection (connect or disconnect)
     */
    public void toggleConnection() {

        if (isConnected()) {
            // special case, no SignonScreen than confirm signing off
            boolean disconnect = confirmTabClose() && (isOnSignOnScreen() || confirmSignOffClose());
            if (disconnect) {
                session.getVT().disconnect();
            }
        } else {
            // lets set this puppy up to connect within its own thread
            Runnable connectIt = new Runnable() {
                @Override
                public void run() {
                    session.getVT().connect();
                }

            };

            // now lets set it to connect within its own daemon thread
            //    this seems to work better and is more responsive than using
            //    swingutilities's invokelater
            Thread ct = new Thread(connectIt);
            ct.setDaemon(true);
            ct.start();

        }

    }

    public void nextSession() {
        fireSessionJump(HTI5250jConstants.JUMP_NEXT);
    }

    public void prevSession() {
        fireSessionJump(HTI5250jConstants.JUMP_PREVIOUS);
    }

    /**
     * Notify all registered listeners of the onSessionJump event.
     *
     * @param dir The direction to jump.
     */
    private void fireSessionJump(int dir) {
        if (sessionJumpListeners != null) {
            int size = sessionJumpListeners.size();
            final SessionJumpEvent jumpEvent = new SessionJumpEvent(this);
            jumpEvent.setJumpDirection(dir);
            for (int i = 0; i < size; i++) {
                SessionJumpListener target = sessionJumpListeners.elementAt(i);
                target.onSessionJump(jumpEvent);
            }
        }
    }

    /**
     * Notify all registered listeners of the onEmulatorAction event.
     *
     * @param action The action to be performed.
     */
    protected void fireEmulatorAction(int action) {

        if (actionListeners != null) {
            int size = actionListeners.size();
            for (int i = 0; i < size; i++) {
                EmulatorActionListener target = actionListeners.elementAt(i);
                EmulatorActionEvent sae = new EmulatorActionEvent(this);
                sae.setAction(action);
                target.onEmulatorAction(sae);
            }
        }
    }

    public boolean isMacroRunning() {

        return macroRunning;
    }

    public boolean isStopMacroRequested() {

        return stopMacro;
    }

    public boolean isSessionRecording() {

        return keyHandler.isRecording();
    }

    public void setMacroRunning(boolean isMacroRunning) {
        macroRunning = isMacroRunning;
        if (macroRunning)
            screen.getOIA().setScriptActive(true);
        else
            screen.getOIA().setScriptActive(false);

        stopMacro = !macroRunning;
    }

    public void setStopMacroRequested() {
        setMacroRunning(false);
    }

    public void closeDown() {

        sesConfig.saveSessionProps(getParent());
        if (session.getVT() != null) session.getVT().disconnect();
        // Added by Luc to fix a memory leak. The keyHandler was still receiving
        //   events even though nothing was really attached.
        keyHandler.sessionClosed(this);
        keyHandler = null;

    }

    /**
     * Show the session attributes screen for modification of the attribute/
     * settings of the session.
     */
    public void actionAttributes() {
        new SessionSettings((Frame) SwingUtilities.getRoot(this), sesConfig).showIt();
        getFocusForMe();
    }

    private void actionPopup(MouseEvent mouseEvent) {
        new SessionPopup(this, mouseEvent);
    }

    public void actionSpool() {

        try {
            org.hti5250j.spoolfile.SpoolExporter spooler =
                    new org.hti5250j.spoolfile.SpoolExporter(session.getVT(), this);
            spooler.setVisible(true);
        } catch (NoClassDefFoundError ncdfe) {
            JOptionPane.showMessageDialog(this,
                    LangTool.getString("messages.noAS400Toolbox"),
                    "Error",
                    JOptionPane.ERROR_MESSAGE, null);
        }

    }

    public void executeMacro(ActionEvent actionEvent) {
        executeMacro(actionEvent.getActionCommand());
    }

    public void executeMacro(String macro) {
        Macronizer.invoke(macro, this);
    }

    protected void stopRecordingMe() {
        if (keyHandler.getRecordBuffer().length() > 0) {
            Macronizer.setMacro(newMacName, keyHandler.getRecordBuffer());
            log.debug(keyHandler.getRecordBuffer());
        }

        keyHandler.stopRecording();
    }

    protected void startRecordingMe() {

        String macName = JOptionPane.showInputDialog(null,
                LangTool.getString("macro.message"),
                LangTool.getString("macro.title"),
                JOptionPane.PLAIN_MESSAGE);
        if (macName != null) {
            macName = macName.trim();
            if (macName.length() > 0) {
                log.info(macName);
                newMacName = macName;
                keyHandler.startRecording();
            }
        }
    }


    /* default */ void resizeMe() {
        Rectangle drawingBounds = getDrawingBounds();
        if (guiGraBuf != null) {
            guiGraBuf.resizeScreenArea(drawingBounds.width, drawingBounds.height);
        }
        screen.repaintScreen();
        Graphics graphics = getGraphics();
        if (graphics != null) {
            graphics.setClip(0, 0, this.getWidth(), this.getHeight());
        }
        repaint(0, 0, getWidth(), getHeight());
    }

    public Rectangle getDrawingBounds() {

        Rectangle bounds = this.getBounds();
        if (keypadPanel != null && keypadPanel.isVisible())
            //	         r.height -= (int)(keyPad.getHeight() * 1.25);
            bounds.height -= (keypadPanel.getHeight());

        bounds.setSize(bounds.width, bounds.height);

        return bounds;

    }

    @Override
    protected void paintComponent(Graphics graphics) {
        log.debug("paint from screen");

        ensureGuiGraphicBufferInitialized();

        // Skip rendering if in headless mode with no GUI component
        if (guiGraBuf == null) {
            return;
        }

        Graphics2D graphics2D = (Graphics2D) graphics;
        if (rubberband.isAreaSelected() && !rubberband.isDragging()) {
            rubberband.erase();
            //   //         rubberband.draw();
        }

        //Rectangle r = g.getClipBounds();

        graphics2D.setColor(guiGraBuf.colorBg);
        graphics2D.fillRect(0, 0, getWidth(), getHeight());

        guiGraBuf.drawImageBuffer(graphics2D);

        if (rubberband.isAreaSelected() && !rubberband.isDragging()) {
            //	         rubberband.erase();
            rubberband.draw();
        }

        //	      keyPad.repaint();

    }

    @Override
    public void update(Graphics graphics) {
        log.info("update paint from gui");
        paint(graphics);

    }

    public boolean isHotSpots() {
        return guiGraBuf.hotSpots;
    }

    public void toggleHotSpots() {
        guiGraBuf.hotSpots = !guiGraBuf.hotSpots;
    }

    /**
     * @todo: Change to be mnemonic key.
     * <p>
     * This toggles the ruler line.
     */
    public void crossHair() {
        screen.setCursorActive(false);
        guiGraBuf.crossHair++;
        if (guiGraBuf.crossHair > 3)
            guiGraBuf.crossHair = 0;
        screen.setCursorActive(true);
    }

    private void ensureGuiGraphicBufferInitialized() {
        if (guiGraBuf == null && !headlessMode) {
            guiGraBuf = new GuiGraphicBuffer(screen, this, sesConfig);
            guiGraBuf.getImageBuffer(0, 0);
        }
    }

    /**
     * Copy & Paste start code
     */
    public final void actionCopy() {
        final Rect area = getBoundingArea();
        rubberband.reset();
        screen.repaintScreen();
        final String textcontent = screen.copyText(area);
        Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
        StringSelection contents = new StringSelection(textcontent);
        cb.setContents(contents, null);
    }

    /**
     * Sum them
     *
     * @param which formatting option to use
     * @return vector string of numeric values
     */
    protected final Vector<Double> sumThem(boolean which) {
        log.debug("Summing");
        return screen.sumThem(which, getBoundingArea());
    }

    /**
     * This routine is responsible for setting up a PrinterJob on this component
     * and initiating the print session.
     */
    public final void printMe() {

        Thread printerThread = new PrinterThread(screen, guiGraBuf.font, screen.getColumns(),
                screen.getRows(), Color.black, true, this);

        printerThread.start();

    }

    /**
     * Add a SessionJumpListener to the listener list.
     *
     * @param listener The SessionListener to be added
     */
    public synchronized void addSessionJumpListener(SessionJumpListener listener) {

        if (sessionJumpListeners == null) {
            sessionJumpListeners = new java.util.Vector<SessionJumpListener>(3);
        }
        sessionJumpListeners.addElement(listener);

    }

    /**
     * Remove a SessionJumpListener from the listener list.
     *
     * @param listener The SessionJumpListener to be removed
     */
    public synchronized void removeSessionJumpListener(SessionJumpListener listener) {
        if (sessionJumpListeners == null) {
            return;
        }
        sessionJumpListeners.removeElement(listener);

    }

    /**
     * Add a EmulatorActionListener to the listener list.
     *
     * @param listener The EmulatorActionListener to be added
     */
    public synchronized void addEmulatorActionListener(EmulatorActionListener listener) {

        if (actionListeners == null) {
            actionListeners = new java.util.Vector<EmulatorActionListener>(3);
        }
        actionListeners.addElement(listener);

    }

    /**
     * Remove a EmulatorActionListener from the listener list.
     *
     * @param listener The EmulatorActionListener to be removed
     */
    public synchronized void removeEmulatorActionListener(EmulatorActionListener listener) {
        if (actionListeners == null) {
            return;
        }
        actionListeners.removeElement(listener);

    }

    /**
     *
     * RubberBanding start code
     *
     */

    /**
     * Returns a pointer to the graphics area that we can draw on
     */
    @Override
    public Graphics getDrawingGraphics() {
        if (guiGraBuf == null) {
            return null;
        }
        return guiGraBuf.getDrawingArea();
    }

    protected final void setRubberBand(TNRubberBand newValue) {
        rubberband = newValue;
    }

    public Rect getBoundingArea() {
        if (guiGraBuf == null) {
            return new Rect(0, 0, screen.getColumns(), screen.getRows());
        }
        Rectangle awtRect = new Rectangle();
        guiGraBuf.getBoundingArea(awtRect);
        return new Rect(awtRect.x, awtRect.y, awtRect.width, awtRect.height);
    }

    @Override
    public Point translateStart(Point start) {
        if (guiGraBuf == null) {
            return start;
        }
        return guiGraBuf.translateStart(start);
    }

    @Override
    public Point translateEnd(Point end) {
        if (guiGraBuf == null) {
            return end;
        }
        return guiGraBuf.translateEnd(end);
    }

    public int getPosFromView(int x, int y) {
        if (guiGraBuf == null) {
            return 0;
        }
        return guiGraBuf.getPosFromView(x, y);
    }

    public void getBoundingArea(Rectangle bounds) {
        if (guiGraBuf != null) {
            guiGraBuf.getBoundingArea(bounds);
        }
    }

    @Override
    public void areaBounded(RubberBand band, int x1, int y1, int x2, int y2) {


        //	      repaint(x1,y1,x2-1,y2-1);
        repaint();
        if (log.isDebugEnabled()) {
            log.debug(" bound " + band.getEndPoint());
        }
    }

    @Override
    public boolean canDrawRubberBand(RubberBand rubberBand) {

        // before we get the row col we first have to translate the x,y point
        //   back to screen coordinates because we are translating the starting
        //   point to the 5250 screen coordinates
        //	      return !screen.isKeyboardLocked() && (screen.isWithinScreenArea(b.getStartPoint().x,b.getStartPoint().y));
        if (guiGraBuf == null) {
            return false;
        }
        return guiGraBuf.isWithinScreenArea(rubberBand.getStartPoint().x, rubberBand.getStartPoint().y);

    }

    /**
     * RubberBanding end code
     */

    public class TNRubberBand extends RubberBand {

        public TNRubberBand(RubberBandCanvasIF canvas) {
            super(canvas);
        }

        @Override
        protected void drawBoundingShape(Graphics graphics, int startX, int startY, int width, int height) {
            graphics.drawRect(startX, startY, width, height);
        }

        protected Rectangle getBoundingArea() {

            Rectangle bounds = new Rectangle();
            getBoundingArea(bounds);
            return bounds;
        }

        protected void getBoundingArea(Rectangle bounds) {

            if ((getEndPoint().x > getStartPoint().x) && (getEndPoint().y > getStartPoint().y)) {
                bounds.setBounds(getStartPoint().x, getStartPoint().y, getEndPoint().x - getStartPoint().x, getEndPoint().y - getStartPoint().y);
            } else if ((getEndPoint().x < getStartPoint().x) && (getEndPoint().y < getStartPoint().y)) {
                bounds.setBounds(getEndPoint().x, getEndPoint().y, getStartPoint().x - getEndPoint().x, getStartPoint().y - getEndPoint().y);
            } else if ((getEndPoint().x > getStartPoint().x) && (getEndPoint().y < getStartPoint().y)) {
                bounds.setBounds(getStartPoint().x, getEndPoint().y, getEndPoint().x - getStartPoint().x, getStartPoint().y - getEndPoint().y);
            } else if ((getEndPoint().x < getStartPoint().x) && (getEndPoint().y > getStartPoint().y)) {
                bounds.setBounds(getEndPoint().x, getStartPoint().y, getStartPoint().x - getEndPoint().x, getEndPoint().y - getStartPoint().y);
            }

            //	         return r;
        }

        @Override
        protected Point getEndPoint() {

            if (this.endPoint == null) {
                Point point = new Point(0, 0);
                guiGraBuf.getPointFromRowCol(0, 0, point);
                setEndPoint(point);
            }
            return this.endPoint;
        }

        @Override
        protected Point getStartPoint() {

            if (this.startPoint == null) {
                Point point = new Point(0, 0);
                guiGraBuf.getPointFromRowCol(0, 0, point);
                setStartPoint(point);
            }
            return this.startPoint;

        }
    }


    public Session5250 getSession() {
        return this.session;
    }

    public void setSession(Session5250 session) {
        this.session = session;
    }


    public boolean isConnected() {

        return session.getVT() != null && session.getVT().isConnected();

    }

    public boolean isOnSignOnScreen() {

        // check to see if we should check.
        if (firstScreen) {

            char[] screenChars = screen.getScreenAsChars();

            Rectangle region = this.sesConfig.getRectangleProperty("signOnRegion");

            int fromRow = region.x;
            int fromCol = region.y;
            int toRow = region.width;
            int toCol = region.height;

            // make sure we are within range.
            if (fromRow == 0)
                fromRow = 1;
            if (fromCol == 0)
                fromCol = 1;
            if (toRow == 0)
                toRow = 24;
            if (toCol == 0)
                toCol = 80;

            int pos = 0;

            for (int row = fromRow; row <= toRow; row++)
                for (int col = fromCol; col <= toCol; col++) {
                    pos = screen.getPos(row - 1, col - 1);
                    //               System.out.println(signonSave[pos]);
                    if (signonSave[pos] != screenChars[pos])
                        return false;
                }
        }

        return true;
    }

    /**
     * @return
     * @see org.hti5250j.Session5250#getSessionName()
     */
    public String getSessionName() {
        return session.getSessionName();
    }

    public String getAllocDeviceName() {
        if (session.getVT() != null) {
            return session.getVT().getAllocatedDeviceName();
        }
        return null;
    }

    public String getHostName() {
        if (session.getVT() != null) {
            return session.getVT().getHostName();
        }
        return session.getConnectionProperties().getProperty(HTI5250jConstants.SESSION_HOST);
    }

    public Screen5250 getScreen() {

        return screen;

    }


    public void connect() {

        session.connect();
    }

    public void disconnect() {

        session.disconnect();
    }

    @Override
    public void onSessionChanged(SessionChangeEvent changeEvent) {

        switch (changeEvent.getState()) {
            case HTI5250jConstants.STATE_CONNECTED:
                // first we check for the signon save or now
                if (!firstScreen) {
                    firstScreen = true;
                    signonSave = screen.getScreenAsChars();
                    //               System.out.println("Signon saved");
                }

                // check for on connect macro
                String mac = sesConfig.getProperties().getProperty("connectMacro", "");
                if (mac.length() > 0)
                    executeMacro(mac);
                break;
            default:
                firstScreen = false;
                signonSave = null;
        }
    }

    /**
     * Add a SessionListener to the listener list.
     *
     * @param listener The SessionListener to be added
     */
    public synchronized void addSessionListener(SessionListener listener) {

        session.addSessionListener(listener);

    }

    /**
     * Remove a SessionListener from the listener list.
     *
     * @param listener The SessionListener to be removed
     */
    public synchronized void removeSessionListener(SessionListener listener) {
        session.removeSessionListener(listener);

    }

}
