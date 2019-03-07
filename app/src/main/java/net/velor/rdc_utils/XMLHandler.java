package net.velor.rdc_utils;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

class XMLHandler {
    private static final String ATTRIBUTE_TYPE = "type";
    private Document mShiftDom;
    private String mShifts;

    XMLHandler(Cursor shifts) {
        if (shifts.moveToFirst()) {
            // смены есть
            mShifts = shifts.getString(shifts.getColumnIndex(DbWork.COL_NAME_SHIFTS));
        } else {
            // создам строку, заполню все дни выходными
            int counter = 0;
            StringBuilder xml = new StringBuilder();
            xml.append("<?xml version=\"1.0\" encoding=\"utf-8\"?><month year='");
            xml.append(MainActivity.sYear);
            xml.append("' month='");
            xml.append(MainActivity.sMonth);
            xml.append("'>");
            Calendar mycal = new GregorianCalendar(MainActivity.sYear, MainActivity.sMonth - 1, 1);
            // узнаю, сколько дней нужно прорисовать
            int daysInMonth = mycal.getActualMaximum(Calendar.DAY_OF_MONTH);
            while (counter < daysInMonth) {
                ++counter;
                xml.append("<day id='");
                xml.append(counter);
                xml.append("' type='-1'/>");
            }
            xml.append("</month>");
            mShifts = xml.toString();
        }
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = null;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(mShifts));
            mShiftDom = dBuilder.parse(is);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
    }

    static int checkShift(Cursor shedule, int day) {
        if(shedule.moveToFirst()){
            String shifts = shedule.getString(shedule.getColumnIndex(DbWork.COL_NAME_SHIFTS));
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            try {
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                InputSource is = new InputSource(new StringReader(shifts));
                Document shiftDom = dBuilder.parse(is);
                // нахожу нужный день
                Element targetDay = shiftDom.getElementById(String.valueOf(day));
                return Integer.valueOf(targetDay.getAttribute(ATTRIBUTE_TYPE));
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            }
        }
        return -1;
    }

    String setDay(String day, int type) {
        Element dayElement = mShiftDom.getElementById(day);
        // нужно распарсить XML, найти день и изменить его тип
        dayElement.setAttribute(ATTRIBUTE_TYPE, String.valueOf(type));
        mShifts = toString(mShiftDom);
        return mShifts;
    }

    public static String toString(Document doc) {
        try {
            StringWriter sw = new StringWriter();
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

            transformer.transform(new DOMSource(doc), new StreamResult(sw));
            return sw.toString();
        } catch (Exception ex) {
            throw new RuntimeException("Error converting to String", ex);
        }
    }

    public String getDayType(String day) {
        Element dayElement = mShiftDom.getElementById(day);
        return dayElement.getAttribute(ATTRIBUTE_TYPE);
    }
}
