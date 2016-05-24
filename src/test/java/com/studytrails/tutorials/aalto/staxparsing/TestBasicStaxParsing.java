package com.studytrails.tutorials.aalto.staxparsing;

import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.stax2.XMLStreamReader2;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.XMLEvent;
import java.io.FileInputStream;
import java.io.InputStream;
        
public class TestBasicStaxParsing {
    
            private void execute(String xmlFileName) throws Exception {
        
        InputStream xmlInputStream = getClass().getResourceAsStream(xmlFileName);
                xmlInputStream = new FileInputStream (xmlFileName);
//Load Aalto's StAX parser factory
        XMLInputFactory2 xmlInputFactory = (XMLInputFactory2) XMLInputFactory.newFactory("com.fasterxml.aalto.stax.InputFactoryImpl", this.getClass().getClassLoader());
        XMLStreamReader2 xmlStreamReader = (XMLStreamReader2) xmlInputFactory.createXMLStreamReader(xmlInputStream);
        while(xmlStreamReader.hasNext()){
            int eventType = xmlStreamReader.next();
            switch (eventType) {
                case XMLEvent.START_ELEMENT:
                    System.out.print("<"+xmlStreamReader.getName().toString()+">");
                    break;
                case XMLEvent.CHARACTERS:
                    System.out.print(xmlStreamReader.getText());
                    break;
                case XMLEvent.END_ELEMENT:
                    System.out.println("</"+xmlStreamReader.getName().toString()+">");
                    break;
                default:
                    //do nothing
                    break;
                }
            }
        
        }
    
            public static void main(String[] args) throws Exception {
                final String wikiHtml=System.getProperty("user.dir")+"\\"+"c.xml";//"C:\\Users\\rli\\IdeaProjects\\aalto-xml\\src\\test\\resources\\c.xml";//

                System.out.println("wikiHtml = " +
                        wikiHtml);
                TestBasicStaxParsing par= new TestBasicStaxParsing ();

                par.execute (wikiHtml);
                // par.execute ("C:\\Users\\rli\\IdeaProjects\\aalto-xml\\src\\test\\resources\\CWiki.html");
//              par.execute ("https://en.wikipedia.org/wiki/RickLi");
                try {  }
                catch (Exception e)
                {
                }

            }
    
}