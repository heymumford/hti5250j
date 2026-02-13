/*
 * SPDX-FileCopyrightText: Copyright (c) 2001
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: Kenneth J. Pouncey
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */





package org.hti5250j.sql;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import com.ibm.as400.vaccess.*;
import com.ibm.as400.access.*;

import java.sql.*;

import org.hti5250j.tools.LangTool;
import org.hti5250j.tools.GUIGraphicsUtils;
import org.hti5250j.tools.system.OperatingSystem;

public class SqlWizard extends JFrame {

    private static final long serialVersionUID = 1L;
    private SQLConnection connection;
    private AS400 system;
    private SQLQueryBuilderPane queryBuilder;
    private SQLResultSetTablePane tablePane;
    private String name;
    private String password;
    private String host;
    private String queryText;
    private JTextArea queryTextArea;

    public SqlWizard(String host, String name, String password) {

        this.host = host;
        this.name = name;
        this.password = password;

        enableEvents(AWTEvent.WINDOW_EVENT_MASK);

        try {
            jbInit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void jbInit() throws Exception {

        try {

            setIconImages(GUIGraphicsUtils.getApplicationIcons());

            // set title
            setTitle(LangTool.getString("xtfr.wizardTitle"));

            Driver driver2 = (Driver) Class.forName("com.ibm.as400.access.AS400JDBCDriver").newInstance();
            DriverManager.registerDriver(driver2);

            connection = new SQLConnection("jdbc:as400://" + host, name, password);

            queryBuilder = new SQLQueryBuilderPane(connection);
            queryBuilder.setTableSchemas(new String[]{"*USRLIBL"});
            queryBuilder.load();

            JButton done = new JButton(LangTool.getString("xtfr.tableDone"));
            done.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    fillQueryTextArea();

                }
            });
            JPanel panel = new JPanel();
            panel.add(done);
            getContentPane().add(queryBuilder, BorderLayout.CENTER);
            getContentPane().add(panel, BorderLayout.SOUTH);

            Dimension max = new Dimension(OperatingSystem.getScreenBounds().width,
                    OperatingSystem.getScreenBounds().height);

            pack();

            if (getSize().width > max.width) {
                setSize(max.width, getSize().height);
            }

            if (getSize().height > max.height) {
                setSize(getSize().width, max.height);
            }

            //Center the window
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            Dimension frameSize = getSize();
            if (frameSize.height > screenSize.height) {
                frameSize.height = screenSize.height;
            }
            if (frameSize.width > screenSize.width) {
                frameSize.width = screenSize.width;
            }

            setLocation((screenSize.width - frameSize.width) / 2,
                    (screenSize.height - frameSize.height) / 2);

            setVisible(true);
        } catch (ClassNotFoundException cnfe) {

            JOptionPane.showMessageDialog(null, "Error loading AS400 JDBC Driver",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);

        }
    }

    private void fillQueryTextArea() {
        queryTextArea.append(queryBuilder.getQuery());

        this.setVisible(false);
        this.dispose();
    }

    public void setQueryTextArea(JTextArea qta) {
        queryTextArea = qta;
    }
}
