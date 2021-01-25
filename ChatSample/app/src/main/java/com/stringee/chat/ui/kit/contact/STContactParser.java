package com.stringee.chat.ui.kit.contact;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;


/**
 * Parser code for parsing the vCard file and creating a CSV file.
 */
public class STContactParser {

    public static final String BEGIN_VCARD = "BEGIN:VCARD";
    public static final String END_VCARD = "END:VCARD";

    public STContactData stContactData;

    /**
     * This method will validate basic initial data exported from contact.
     *
     * @param data
     * @return
     */
    public static boolean validateData(String data) {
        return (data != null && data.replaceAll("[\n\r]", "").trim().startsWith(BEGIN_VCARD) && data.replaceAll("[\n\r]", "").trim().endsWith(END_VCARD));
    }

    /**
     * @param contact
     * @return
     * @throws Exception
     */
    public STContactData parseCVFContactData(String contact) throws Exception {
        String[] lines;
        if (contact.contains("\r\n")) {
            lines = contact.split("\r\n");
        } else {
            lines = contact.split("\n");
        }
        StringBuffer contactBuffer = new StringBuffer();
        StringBuffer imageByteCode = null;
        for (String sLine : lines) {
            if (sLine.equalsIgnoreCase(BEGIN_VCARD)) {

                //START
                stContactData = new STContactData();

            } else if (sLine.equalsIgnoreCase(END_VCARD)) {

                //END
                stContactData.setPhone(contactBuffer.toString());
                if (imageByteCode != null) {
                    stContactData.setAvatar(stringToBitMap(imageByteCode.toString()));
                }
                return stContactData;

            } else if (sLine.startsWith("FN:")) {

                String[] tokens = sLine.split(":");
                if (tokens.length == 2) {
                    stContactData.setName(tokens[1]);
                }

            } else if (sLine.startsWith("TEL;")) {
                String[] tokens = sLine.split(":");
                if (tokens.length == 2) {
                    contactBuffer.append(tokens[1] + "\n");
                }
            } else if (sLine.startsWith("PHOTO")) {

                String[] tokens = sLine.split(":");
                if (tokens.length >= 2) {
                    imageByteCode = new StringBuffer().append(tokens[1]);
                }
            } else if (sLine.startsWith("EMAIL")) {
                String[] tokens = sLine.split(":");
                if (tokens.length >= 2) {
                    stContactData.setEmail(tokens[1]);
                }
            } else {
                if (imageByteCode != null) {
                    imageByteCode.append(sLine);
                }
            }
        }
        return null;
    }

    /**
     * @param encodedString
     * @return bitmap (from given string)
     */
    public Bitmap stringToBitMap(String encodedString) {
        try {
            byte[] encodeByte = Base64.decode(encodedString, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
            return bitmap;
        } catch (Exception e) {
            return null;
        }
    }

}

