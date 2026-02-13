/*
 * SPDX-FileCopyrightText: Copyright (c) 2001
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */





package org.hti5250j.tools.filters;


import java.io.*;
import java.util.ArrayList;

public class FixedWidthOutputFilter implements OutputFilterInterface {

    PrintStream fout = null;
    StringBuffer sb = new StringBuffer();

    // create instance of file for output
    public void createFileInstance(String fileName) throws
            FileNotFoundException {
        fout = new PrintStream(new FileOutputStream(fileName));
    }

    /**
     * Write the html header of the output file
     */
    public void parseFields(byte[] cByte, ArrayList ffd, StringBuffer rb) {

        FileFieldDef f;

        // write out the html record information for each field that is selected
        for (int x = 0; x < ffd.size(); x++) {
            f = (FileFieldDef) ffd.get(x);
            if (f.isWriteField()) {


                switch (f.getFieldType()) {

                    case 'P':
                    case 'S':
                        rb.append(getFixedLength(cByte, f));
                        break;
                    default:
                        rb.append(getFixedLength(cByte, f));
                        break;

                }
            }
        }

        fout.println(rb);

    }

    /**
     * Return the field in fixed width field.
     *
     * @param cByte
     * @param f
     * @return
     */
    private String getFixedLength(byte[] cByte, FileFieldDef f) {

        sb.setLength(0);

        switch (f.getFieldType()) {

            case 'P':
            case 'S':
                sb.append(f.parseData(cByte));
                formatNumeric(sb);
                while (sb.length() < f.getFieldLength()) {
                    sb.insert(0, ' ');
                }
                break;
            default:
                sb.append(f.parseData(cByte));
                while (sb.length() < f.getFieldLength()) {
                    sb.append(' ');
                }
                break;

        }

        return sb.toString();


    }

    private void formatNumeric(StringBuffer sb) {

        if (sb.length() == 0) {
            sb.append('0');
            return;
        }

        int len = sb.length();
        int counter = 0;
        boolean done = false;
        boolean neg = false;

        while (!done && counter < len) {

            switch (sb.charAt(counter)) {

                case '0':
                case '+':
                case ' ':
                    sb.setCharAt(counter, ' ');
                    break;
                case '-':
                    sb.setCharAt(counter, ' ');
                    neg = true;
                    break;
                default:
                    done = true;
                    break;
            }

            if (!done) {
                counter++;
            }
        }

        if (counter > 0) {
            counter--;
        }

        if (neg) {
            sb.setCharAt(counter, '-');
        }

        if (sb.length() == 0) {
            sb.append('0');
        }

    }

    /**
     * Write the html header of the output file
     */
    public void writeHeader(String fileName, String host,
                            ArrayList ffd, char decChar) {

//      FileFieldDef f;
//      StringBuffer sb = new StringBuffer();
//      //  loop through each of the fields and write out the field name for
//      //    each selected field
//      for (int x = 0; x < ffd.size(); x++) {
//         f = (FileFieldDef)ffd.get(x);
//         if (f.isWriteField()) {
//            sb.append(f.getFieldName());
//         }
//      }
//
//      fout.println (sb.toString().toCharArray());
    }


    /**
     * write the footer of the html output
     */
    public void writeFooter(ArrayList ffd) {

        fout.flush();
        fout.close();

    }

    public boolean isCustomizable() {
        return false;
    }

    public void setCustomProperties() {

    }

}
