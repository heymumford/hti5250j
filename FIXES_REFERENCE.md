# TN5250J Threading Fixes - Code Examples

## Fix #1: Mark Shared Fields as Volatile

**File:** `/Users/vorthruna/Projects/tn5250j/src/org/tn5250j/framework/tn5250/tnvt.java`

**Location:** Lines 89-125 (field declarations)

**Before:**
```java
private Socket sock;
private BufferedInputStream bin;
private BufferedOutputStream bout;
private boolean waitingForInput;
private boolean cursorOn = false;
private boolean connected = false;
private boolean keepTrucking = true;
private boolean pendingUnlock = false;
```

**After:**
```java
private volatile Socket sock;
private volatile BufferedInputStream bin;
private volatile BufferedOutputStream bout;
private volatile boolean waitingForInput;
private volatile boolean cursorOn = false;
private volatile boolean connected = false;
private volatile boolean keepTrucking = true;
private volatile boolean pendingUnlock = false;
```

**Rationale:** Volatile ensures all threads see up-to-date values. Prevents instruction reordering that could cause visibility issues.

---

## Fix #2: Wrap UI Calls in SwingUtilities.invokeLater()

**File:** `/Users/vorthruna/Projects/tn5250j/src/org/tn5250j/framework/tn5250/tnvt.java`

**Location:** Lines 958-1111 (run method)

**Before (Lines 980, 1000, etc.):**
```java
public void run() {
    while (keepTrucking) {
        // ...
        screen52.setCursorActive(false);    // DIRECT CALL - EDT VIOLATION
        // ...
        screen52.updateDirty();              // DIRECT CALL - EDT VIOLATION
        // ...
    }
}
```

**After:**
```java
public void run() {
    while (keepTrucking) {
        // ...
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                screen52.setCursorActive(false);
            }
        });
        
        // ...
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                screen52.updateDirty();
            }
        });
        // ...
    }
}
```

**Or with Lambda (Java 8+):**
```java
SwingUtilities.invokeLater(() -> screen52.setCursorActive(false));
SwingUtilities.invokeLater(() -> screen52.updateDirty());
```

**Locations to Fix:**
- Line 980: `screen52.setCursorActive(false)`
- Line 1000: `screen52.updateDirty()`
- Line 1052: `screen52.setCursorActive(true)`
- Line 1058: `screen52.setCursorActive(true)`
- Line 1066: `screen52.drawFields()`
- Line 1086: `screen52.updateDirty()`
- Line 1102: `screen52.setCursorActive(true)`
- Line 1295: `screen52.clearAll()`
- Line 1313: `screen52.goto_XY(y)`
- Line 1397: `screen52.drawFields()`
- Line 1409: `screen52.goto_XY(pos - 1)`
- Line 1479: `screen52.clearAll()`
- Line 1505: `screen52.clearAll()`

---

## Fix #3: Synchronize Listener Fire Methods

**File:** `/Users/vorthruna/Projects/tn5250j/src/org/tn5250j/tools/FTP5250Prot.java`

**Location:** Lines 174-228

**Before:**
```java
public synchronized void addFTPStatusListener(FTPStatusListener listener) {
    if (listeners == null) {
        listeners = new java.util.Vector<FTPStatusListener>(3);
    }
    listeners.addElement(listener);
}

private void fireStatusEvent() {  // NOT SYNCHRONIZED
    if (listeners != null) {
        int size = listeners.size();
        for (int i = 0; i < size; i++) {
            FTPStatusListener target = listeners.elementAt(i);
            target.statusReceived(status);
        }
    }
}

private void fireCommandEvent() {  // NOT SYNCHRONIZED
    if (listeners != null) {
        int size = listeners.size();
        for (int i = 0; i < size; i++) {
            FTPStatusListener target = listeners.elementAt(i);
            target.commandStatusReceived(status);
        }
    }
}

private void fireInfoEvent() {  // NOT SYNCHRONIZED
    if (listeners != null) {
        int size = listeners.size();
        for (int i = 0; i < size; i++) {
            FTPStatusListener target = listeners.elementAt(i);
            target.fileInfoStatusReceived(status);
        }
    }
}

public synchronized void removeFTPStatusListener(FTPStatusListener listener) {
    if (listeners != null) {
        listeners.removeElement(listener);
    }
}
```

**After:**
```java
public synchronized void addFTPStatusListener(FTPStatusListener listener) {
    if (listeners == null) {
        listeners = new java.util.Vector<FTPStatusListener>(3);
    }
    listeners.addElement(listener);
}

private synchronized void fireStatusEvent() {  // NOW SYNCHRONIZED
    if (listeners != null) {
        int size = listeners.size();
        for (int i = 0; i < size; i++) {
            FTPStatusListener target = listeners.elementAt(i);
            target.statusReceived(status);
        }
    }
}

private synchronized void fireCommandEvent() {  // NOW SYNCHRONIZED
    if (listeners != null) {
        int size = listeners.size();
        for (int i = 0; i < size; i++) {
            FTPStatusListener target = listeners.elementAt(i);
            target.commandStatusReceived(status);
        }
    }
}

private synchronized void fireInfoEvent() {  // NOW SYNCHRONIZED
    if (listeners != null) {
        int size = listeners.size();
        for (int i = 0; i < size; i++) {
            FTPStatusListener target = listeners.elementAt(i);
            target.fileInfoStatusReceived(status);
        }
    }
}

public synchronized void removeFTPStatusListener(FTPStatusListener listener) {
    if (listeners != null) {
        listeners.removeElement(listener);
    }
}
```

**Key Change:** Add `synchronized` keyword to fire methods to match add/remove pattern.

---

## Fix #4: Add Try-Finally for Resource Cleanup

**File:** `/Users/vorthruna/Projects/tn5250j/src/org/tn5250j/tools/FTP5250Prot.java`

**Location:** Lines 513-629

**Before:**
```java
private boolean loadFFD(boolean useInternal) {
    Socket socket = null;
    BufferedReader dis = null;
    String remoteFile = "QTEMP/FFD";
    String recLength = "";
    Vector allowsNullFields = null;

    try {
        socket = createPassiveSocket("RETR " + remoteFile);
        if (socket == null) {
            return false;
        }

        dis = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        String data;
        if (ffd != null) {
            ffd.clear();
            ffd = null;
        }

        ffd = new ArrayList();
        while ((data = dis.readLine()) != null) {
            // Process data
            FileFieldDef ffDesc = new FileFieldDef(vt, decChar);
            // ... more processing ...
        }
        // ... continue processing ...
    } catch (Exception e) {
        return false;  // BUG: socket and dis never closed
    }
    // NO FINALLY BLOCK
}
```

**After (with finally):**
```java
private boolean loadFFD(boolean useInternal) {
    Socket socket = null;
    BufferedReader dis = null;
    String remoteFile = "QTEMP/FFD";
    String recLength = "";
    Vector allowsNullFields = null;

    try {
        socket = createPassiveSocket("RETR " + remoteFile);
        if (socket == null) {
            return false;
        }

        dis = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        String data;
        if (ffd != null) {
            ffd.clear();
            ffd = null;
        }

        ffd = new ArrayList();
        while ((data = dis.readLine()) != null) {
            // Process data
            FileFieldDef ffDesc = new FileFieldDef(vt, decChar);
            // ... more processing ...
        }
        // ... continue processing ...
    } catch (Exception e) {
        return false;
    } finally {
        // GUARANTEE CLEANUP
        if (dis != null) {
            try {
                dis.close();
            } catch (Exception e) {
                log.warn("Error closing BufferedReader: " + e.getMessage());
            }
        }
        if (socket != null) {
            try {
                socket.close();
            } catch (Exception e) {
                log.warn("Error closing socket: " + e.getMessage());
            }
        }
    }
}
```

**Or with Try-With-Resources (Java 7+):**
```java
private boolean loadFFD(boolean useInternal) {
    String remoteFile = "QTEMP/FFD";
    String recLength = "";
    Vector allowsNullFields = null;

    try (Socket socket = createPassiveSocket("RETR " + remoteFile);
         BufferedReader dis = new BufferedReader(
             new InputStreamReader(socket.getInputStream()))) {

        if (socket == null) {
            return false;
        }

        String data;
        if (ffd != null) {
            ffd.clear();
            ffd = null;
        }

        ffd = new ArrayList();
        while ((data = dis.readLine()) != null) {
            // Process data
            FileFieldDef ffDesc = new FileFieldDef(vt, decChar);
            // ... more processing ...
        }
        // ... continue processing ...
        
    } catch (Exception e) {
        log.warn("Error in loadFFD: " + e.getMessage());
        return false;
    }
    // TRY-WITH-RESOURCES AUTOMATICALLY CLOSES socket and dis
}
```

**Recommendation:** Use try-with-resources if codebase is Java 7+.

---

## Fix #5: Use Timeout for BlockingQueue

**File:** `/Users/vorthruna/Projects/tn5250j/src/org/tn5250j/framework/tn5250/tnvt.java`

**Location:** Lines 958-1111 (run method)

**Before:**
```java
public void run() {
    while (keepTrucking) {
        try {
            bk.initialize((byte[]) dsq.take());  // BLOCKS FOREVER
        } catch (InterruptedException ie) {
            log.warn("   vt thread interrupted and stopping ");
            keepTrucking = false;
            continue;
        }
        // ... process ...
    }
}
```

**After:**
```java
public void run() {
    while (keepTrucking) {
        try {
            byte[] data = dsq.poll(5, java.util.concurrent.TimeUnit.SECONDS);
            if (data != null) {
                bk.initialize(data);
            }
        } catch (InterruptedException ie) {
            log.warn("   vt thread interrupted and stopping ");
            keepTrucking = false;
            continue;
        }
        // ... process ...
    }
}
```

**Change:** Replace `take()` with `poll(timeout, unit)` to allow periodic wakeup and graceful shutdown.

---

## Fix #6: Add Socket Timeout to Prevent Indefinite Blocking

**File:** `/Users/vorthruna/Projects/tn5250j/src/org/tn5250j/framework/tn5250/tnvt.java`

**Location:** Lines 280-289 (connect method)

**Before:**
```java
sock = sc.createSocket(s, port);

if (sock == null) {
    log.warn("I did not get a socket");
    disconnect();
    return false;
}

connected = true;
sock.setKeepAlive(true);
sock.setTcpNoDelay(true);
sock.setSoLinger(false, 0);
```

**After:**
```java
sock = sc.createSocket(s, port);

if (sock == null) {
    log.warn("I did not get a socket");
    disconnect();
    return false;
}

connected = true;
sock.setKeepAlive(true);
sock.setTcpNoDelay(true);
sock.setSoLinger(false, 0);
sock.setSoTimeout(30000);  // 30 second read timeout
```

**Rationale:** Prevents thread from blocking indefinitely if server stalls or connection drops.

---

## Fix #7: Fix invokeAndWait Deadlock Risk

**File:** `/Users/vorthruna/Projects/tn5250j/src/org/tn5250j/framework/tn5250/tnvt.java`

**Location:** Lines 254-259

**Before:**
```java
try {
    SwingUtilities.invokeAndWait(new Runnable() {
        public void run() {
            screen52.getOIA().setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT,
                    ScreenOIA.OIA_LEVEL_INPUT_INHIBITED, "X - Connecting");
        }
    });
} catch (Exception exc) {
    log.warn("setStatus(ON) " + exc.getMessage());
}
```

**After (Safe):**
```java
if (SwingUtilities.isEventDispatchThread()) {
    // Already on EDT - no need to invoke
    screen52.getOIA().setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT,
            ScreenOIA.OIA_LEVEL_INPUT_INHIBITED, "X - Connecting");
} else {
    // On worker thread - use invokeLater instead of invokeAndWait
    SwingUtilities.invokeLater(new Runnable() {
        public void run() {
            screen52.getOIA().setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT,
                    ScreenOIA.OIA_LEVEL_INPUT_INHIBITED, "X - Connecting");
        }
    });
}
```

**Rationale:** Prevents deadlock if connect() is accidentally called from EDT thread.

---

## Fix #8: Synchronize Screen5250 Listener Access

**File:** `/Users/vorthruna/Projects/tn5250j/src/org/tn5250j/framework/tn5250/Screen5250.java`

**Location:** listener access throughout class

**Before:**
```java
private Vector<ScreenListener> screenListeners = null;

public void addScreenListener(ScreenListener listener) {
    if (screenListeners == null) {
        screenListeners = new Vector<ScreenListener>();
    }
    screenListeners.add(listener);
}

private void fireScreenChanged(int update) {
    if (screenListeners != null) {
        int size = screenListeners.size();
        for (int i = 0; i < size; i++) {
            screenListeners.get(i).screenChanged(this, update);
        }
    }
}
```

**After:**
```java
private Vector<ScreenListener> screenListeners = null;

public synchronized void addScreenListener(ScreenListener listener) {
    if (screenListeners == null) {
        screenListeners = new Vector<ScreenListener>();
    }
    screenListeners.add(listener);
}

private synchronized void fireScreenChanged(int update) {
    if (screenListeners != null) {
        int size = screenListeners.size();
        for (int i = 0; i < size; i++) {
            screenListeners.get(i).screenChanged(this, update);
        }
    }
}
```

**Change:** Add `synchronized` to fireScreenChanged() and fireCursorChanged() methods.

---

## Summary of Changes

| Issue | File | Line(s) | Fix |
|-------|------|---------|-----|
| Unsync shared state | tnvt.java | 89-125 | Add `volatile` keyword |
| EDT violations | tnvt.java | 980, 1000, etc. | Wrap in SwingUtilities.invokeLater() |
| Listener races | FTP5250Prot.java | 174-206 | Add `synchronized` to fire methods |
| Resource leaks | FTP5250Prot.java | 513-629 | Add try-finally or try-with-resources |
| Blocking queue | tnvt.java | 968 | Use poll(timeout) instead of take() |
| No socket timeout | tnvt.java | 281 | Add sock.setSoTimeout(30000) |
| invokeAndWait deadlock | tnvt.java | 254 | Check isEventDispatchThread() first |
| Listener sync | Screen5250.java | fire methods | Add `synchronized` keyword |

