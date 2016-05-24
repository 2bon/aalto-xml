package com.studytrails.tutorials.aalto.staxparsing;

import com.fasterxml.aalto.AsyncInputFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;

import javax.xml.stream.events.XMLEvent;
import java.io.*;


public class TestAsyncParsing {



    private void execute (String xmlFileName) throws Exception {
        try {
            InputStream xmlInputStream;
            String xmlString;
            try {
                xmlInputStream = getClass ().getResourceAsStream (xmlFileName);
                xmlString = getStringFromInputStream (xmlInputStream);

            }catch (Exception e)
            {
                System.out.println("x23yz= " );
                xmlInputStream = new FileInputStream (xmlFileName);

                xmlString = getStringFromInputStream (xmlInputStream);
            }

            byte[] XML = xmlString.getBytes ();

            AsyncXMLInputFactory xmlInputFactory = new InputFactoryImpl ();

            AsyncXMLStreamReader asyncXMLStreamReader = (AsyncXMLStreamReader) xmlInputFactory.createXMLStreamReader (xmlInputStream);//createAsyncXMLStreamReader();
            AsyncInputFeeder asyncInputFeeder = asyncXMLStreamReader.getInputFeeder ();
            int bufferFeedLength = 1; // feed 1 byte at a time to the asynchronous parser
            int inputPtr = 0;
            int type = 0;
            do {
                //keep looping till event is complete
                while ((type = asyncXMLStreamReader.next ()) == AsyncXMLStreamReader.EVENT_INCOMPLETE) {
                    byte[] buffer = new byte[]{XML[inputPtr]};
                    inputPtr++;
                    //asyncInputFeeder;//.feedInput (buffer, 0, bufferFeedLength);
                    //check for end of input
                    if (inputPtr >= XML.length) {
                        asyncInputFeeder.endOfInput ();
                        }
                    }
                //handle parser event and extract parsed data
                switch (type) {
                    case XMLEvent.START_DOCUMENT:
                        System.out.println ("start document");
                        break;
                    case XMLEvent.START_ELEMENT:
                        System.out.println ("start element: " + asyncXMLStreamReader.getName ());
                        break;
                    case XMLEvent.CHARACTERS:
                        System.out.println ("characters: " + asyncXMLStreamReader.getText ());
                        break;
                    case XMLEvent.END_ELEMENT:
                        System.out.println ("end element: " + asyncXMLStreamReader.getName ());
                        break;
                    case XMLEvent.END_DOCUMENT:
                        System.out.println ("end document");
                        break;
                    default:
                        break;
                    }

                } while (type != XMLEvent.END_DOCUMENT);

            asyncXMLStreamReader.close ();

            } catch (RuntimeException t) {
            t.printStackTrace ();
            }

        }


            //helper method to convert InputStream to String


    private String getStringFromInputStream (InputStream is) {

        BufferedReader br = null;
        StringBuilder sb = new StringBuilder ();

        String line;
        try {

            br = new BufferedReader (new InputStreamReader (is));
            while ((line = br.readLine ()) != null) {
                sb.append (line);
                }

            } catch (IOException e) {
            e.printStackTrace ();
            } finally {
            if (br != null) {
                try {
                    br.close ();
                    } catch (IOException e) {
                    e.printStackTrace ();
                    }
                }
            }

        return sb.toString ();

        }




    public static void main (String[] args) throws Exception {
        final String wikiHtml=System.getProperty("user.dir")+"\\"+"c.xml";//"C:\\Users\\rli\\IdeaProjects\\aalto-xml\\src\\test\\resources\\c.xml";//

        System.out.println("wikiHtml = " +
                wikiHtml);
        TestAsyncParsing par= new TestAsyncParsing ();

        par.execute (wikiHtml);
        // par.execute ("C:\\Users\\rli\\IdeaProjects\\aalto-xml\\src\\test\\resources\\CWiki.html");
//              par.execute ("https://en.wikipedia.org/wiki/RickLi");
        try {  }
        catch (Exception e)
        {
        }

    }


}