/*
 * SPDX-FileCopyrightText: Copyright (c) 2001
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.framework.common;

import java.util.*;
import java.awt.event.*;
import javax.swing.Timer;

import org.hti5250j.Session5250;
import org.hti5250j.tools.logging.*;
import org.hti5250j.interfaces.SessionsInterface;


/**
 * Contains a collection of Session objects. This list is a static snapshot
 * of the list of Session objects available at the time of the snapshot.
 */
public class Sessions implements SessionsInterface, ActionListener {

    private List<Session5250> sessions = null;
    private int count = 0;
    private Timer heartBeater;

    private HTI5250jLogger log = HTI5250jLogFactory.getLogger(this.getClass());

    public Sessions() {

        sessions = new ArrayList<Session5250>();
    }

    public void actionPerformed(ActionEvent e) {

        Session5250 ses;
        for (int x = 0; x < sessions.size(); x++) {
            try {
                ses = sessions.get(x);
                if (ses.isConnected() && ses.isSendKeepAlive()) {
                    ses.getVT().sendHeartBeat();
                    if (log.isDebugEnabled()) {
                        log.debug(" sent heartbeat to " + ses.getSessionName());
                    }
                }
            } catch (Exception ex) {
                log.warn(ex.getMessage());
            }
        }

    }

    protected void addSession(Session5250 newSession) {
        sessions.add(newSession);
        log.debug("adding Session: " + newSession.getSessionName());
        if (newSession.isSendKeepAlive() && heartBeater == null) {
            heartBeater = new Timer(15000, this);
//         heartBeater = new Timer(3000,this);
            heartBeater.start();

        }
        ++count;
    }

    protected void removeSession(Session5250 session) {
        if (session != null) {
            log.debug("Removing session: " + session.getSessionName());
            if (session.isConnected())
                session.disconnect();
            sessions.remove(session);
            --count;
        }
    }

    protected void removeSession(String sessionName) {
        log.debug("Remove session by name: " + sessionName);
        removeSession(item(sessionName));

    }

    protected void removeSession(int index) {
        log.debug("Remove session by index: " + index);
//      removeSession((SessionGUI)(((Session5250)item(index)).getGUI()));
        removeSession(item(index));
    }

    public int getCount() {

        return count;
    }

    public Session5250 item(int index) {

        return sessions.get(index);

    }

    public Session5250 item(String sessionName) {

        Session5250 s = null;
        int x = 0;

        while (x < sessions.size()) {

            s = sessions.get(x);

            if (s.getSessionName().equals(sessionName))
                return s;

            x++;
        }

        return null;

    }

    public Session5250 item(Session5250 sessionObject) {

        Session5250 s = null;
        int x = 0;

        while (x < sessions.size()) {

            s = sessions.get(x);

            if (s.equals(sessionObject))
                return s;

            x++;
        }

        return null;

    }

    public ArrayList<Session5250> getSessionsList() {
        ArrayList<Session5250> newS = new ArrayList<Session5250>(sessions.size());
        for (int x = 0; x < sessions.size(); x++)
            newS.add(sessions.get(x));
        return newS;
    }

    public void refresh() {


    }


}
