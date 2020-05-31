package com.example.kn_project_app;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

class DeviceConverter {
    static ArrayList<Device> convertFromXmlToDevice(String list) {
        ArrayList<Device> arrayList = new ArrayList<>();
        try {
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(list));
            Document document = db.parse(is);
            document.getDocumentElement().normalize();
            System.out.println("*************************************");
            NodeList nList = document.getElementsByTagName("wireless-network");
            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);
                try {
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element eElement = (Element) nNode;
                        if (eElement.getAttribute("type").equals("infrastructure")) {
                            System.out.println("SSID : " + eElement.getElementsByTagName("essid").item(0).getTextContent());
                            System.out.println("BSSID : " + eElement.getElementsByTagName("BSSID").item(0).getTextContent());
                            System.out.println("channel : " + eElement.getElementsByTagName("channel").item(0).getTextContent());
                            System.out.println("freqmhz : " + eElement.getElementsByTagName("freqmhz").item(0).getTextContent());
                            arrayList.add(new Device(R.drawable.access_point, eElement.getElementsByTagName("essid").item(0).getTextContent().equals("") ? "<SSID unknown>" : eElement.getElementsByTagName("essid").item(0).getTextContent(), "Access Point", eElement.getElementsByTagName("channel").item(0).getTextContent(), eElement.getElementsByTagName("BSSID").item(0).getTextContent(), eElement.getElementsByTagName("freqmhz").item(0).getTextContent() + " MHz", eElement.getElementsByTagName("BSSID").item(0).getTextContent()));
                            NodeList internalList = eElement.getElementsByTagName("wireless-client");
                            for (int temp2 = 0; temp2 < internalList.getLength(); temp2++) {
                                Node internalNode = internalList.item(temp2);
                                if (internalNode.getNodeType() == Node.ELEMENT_NODE) {
                                    Element internalElement = (Element) internalNode;
                                    System.out.println("MAC : " + internalElement.getElementsByTagName("client-mac").item(0).getTextContent());
                                    System.out.println("channel : " + internalElement.getElementsByTagName("channel").item(0).getTextContent());
                                    arrayList.add(new Device(R.drawable.phone, internalElement.getElementsByTagName("client-manuf").item(0).getTextContent().equals("Unknown") ? "<Manuf unkown>" : internalElement.getElementsByTagName("client-manuf").item(0).getTextContent(), "Client", internalElement.getElementsByTagName("channel").item(0).getTextContent(), internalElement.getElementsByTagName("client-mac").item(0).getTextContent(), eElement.getElementsByTagName("freqmhz").item(0).getTextContent() + " MHz", eElement.getElementsByTagName("BSSID").item(0).getTextContent()));
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            System.out.println("*************************************");
        } catch (ParserConfigurationException | IOException | SAXException e) {
            e.printStackTrace();
        }
        return arrayList;
    }
}
