/*
 * SPDX-FileCopyrightText: Copyright (c) 2002
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: Kenneth J. Pouncey
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */





package org.hti5250j.spoolfile;

import javax.swing.JTabbedPane;

public class SpoolFilterPane extends JTabbedPane {

    private static final long serialVersionUID = 1L;
    private UserTabPanel user;
    private OutputQueueTabPanel queue;
    //   private JobTabPanel job;
    private SpoolNameTabPanel spoolName;
    private UserDataTabPanel userData;

    public SpoolFilterPane() {
        try {
            jbInit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void jbInit() throws Exception {
        user = new UserTabPanel();
        queue = new OutputQueueTabPanel();
//      job = new JobTabPanel();
        spoolName = new SpoolNameTabPanel();
        userData = new UserDataTabPanel();

        this.addTab("User", user);
        this.addTab("Output Queue", queue);
//      this.addTab("Job",job);
        this.addTab("Spool Name", spoolName);
        this.addTab("User Data", userData);

    }

    public String getUser() {
        return user.getUser();
    }

    public void setUser(String filter) {
        user.setUser(filter);
        setSelectedComponent(user);
    }

    public String getQueue() {
        return queue.getQueue();
    }

    public String getLibrary() {

        return queue.getLibrary();

    }

    public String getJobName() {
        return " ";
    }

    public String getJobUser() {
        return " ";

    }

    public String getJobNumber() {
        return " ";

    }

    public String getUserData() {
        return userData.getUserData();

    }

    public void setUserData(String filter) {

        userData.setUserData(filter);
        setSelectedComponent(userData);
    }

    public String getSpoolName() {
        return spoolName.getSpoolName();

    }

    public void setSpoolName(String filter) {

        spoolName.setSpoolName(filter);
        setSelectedComponent(spoolName);
    }

    /**
     * Reset the values in the current panel to default values
     */
    public void resetCurrent() {
        ((QueueFilterInterface) this.getSelectedComponent()).reset();
    }

    /**
     * Reset the values in all filter panels to default values
     */
    public void resetAll() {
        for (int x = 0; x < this.getTabCount(); x++) {
            ((QueueFilterInterface) this.getComponent(x)).reset();
        }
    }
}
