/* Woodstox Lite ("wool") XML processor
 *
 * Copyright (c) 2006- Tatu Saloranta, tatu.saloranta@iki.fi
 *
 * Licensed under the License specified in the file LICENSE which is
 * included with the source code.
 * You may not use this file except in compliance with the License.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.dataNinja.ee.stax;

import net.dataNinja.ee.AsyncByteArrayFeeder;
import net.dataNinja.ee.AsyncByteBufferFeeder;
import net.dataNinja.ee.AsyncXMLInputFactory;
import net.dataNinja.ee.AsyncXMLStreamReader;
import net.dataNinja.ee.async.AsyncByteArrayScanner;
import net.dataNinja.ee.async.AsyncByteBufferScanner;
import net.dataNinja.ee.async.AsyncStreamReaderImpl;
import net.dataNinja.ee.dom.DOMReaderImpl;
import net.dataNinja.ee.evt.EventAllocatorImpl;
import net.dataNinja.ee.evt.EventReaderImpl;
import net.dataNinja.ee.impl.IoStreamException;
import net.dataNinja.ee.in.ByteSourceBootstrapper;
import net.dataNinja.ee.in.CharSourceBootstrapper;
import net.dataNinja.ee.in.ReaderConfig;
import net.dataNinja.ee.util.URLUtil;
import org.codehaus.stax2.XMLEventReader2;
import org.codehaus.stax2.XMLStreamReader2;
import org.codehaus.stax2.io.Stax2ByteArraySource;
import org.codehaus.stax2.io.Stax2CharArraySource;
import org.codehaus.stax2.io.Stax2Source;
import org.codehaus.stax2.ri.Stax2FilteredStreamReader;
import org.codehaus.stax2.ri.Stax2ReaderAdapter;
import org.codehaus.stax2.ri.evt.Stax2EventReaderAdapter;
import org.codehaus.stax2.ri.evt.Stax2FilteredEventReader;
import org.xml.sax.InputSource;

import javax.xml.stream.*;
import javax.xml.stream.util.XMLEventAllocator;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.net.URL;
import java.nio.ByteBuffer;

/**
 * Aalto implementation of basic Stax factory (both
 * {@link XMLInputFactory} and {@link org.codehaus.stax2.XMLInputFactory2})
 * as well as API for producing non-blocking (async) parsers
 * (that is, {@link AsyncXMLInputFactory}).
 *
 * @author Tatu Saloranta
 */
public final class InputFactoryImpl
    extends AsyncXMLInputFactory
{
    /**
     * This is the currently active configuration that will be used
     * for readers created by this factory.
     */
    final ReaderConfig _config;

    // // // StAX - mandated objects:

    protected XMLEventAllocator _allocator = null;

    /*
    /**********************************************************************
    /* Life-cycle:
    /**********************************************************************
     */

    public InputFactoryImpl() {
        _config = new ReaderConfig ();
    }

    /*
    /**********************************************************************
    /* Stax, XMLInputFactory: filtered reader factory methods
    /**********************************************************************
     */

    // // // Filtered reader factory methods

    @Override
    public XMLEventReader createFilteredReader(XMLEventReader reader, EventFilter filter)
    {
        return new Stax2FilteredEventReader(Stax2EventReaderAdapter.wrapIfNecessary(reader), filter);
    }

    @Override
    public XMLStreamReader createFilteredReader(XMLStreamReader reader, StreamFilter filter)
        throws XMLStreamException
    {
         Stax2FilteredStreamReader fr = new Stax2FilteredStreamReader(reader, filter);
        /* As per Stax 1.0 TCK, apparently the filtered
         * reader is expected to be automatically forwarded to the first
         * acceptable event. This is different from the way RI works, but
         * since specs don't say anything about filtered readers, let's
         * consider TCK to be "more formal" for now, and implement that
         * behavior.
         */
        if (!filter.accept(fr)) { // START_DOCUMENT ok?
            // Ok, nope, this should do the trick:
            fr.next();
        }
        return fr;
    }

    /*
    /**********************************************************************
    /* Stax, XMLInputFactory: XMLEventReader factory methods
    /**********************************************************************
     */

    @Override
    public XMLEventReader createXMLEventReader(InputStream in) throws XMLStreamException {
        return createXMLEventReader(in, null);
    }

    @Override
    public XMLEventReader createXMLEventReader(InputStream in, String enc) throws XMLStreamException {
        return constructER(constructSR(in, enc, true));
    }

    @Override
    public XMLEventReader createXMLEventReader(Reader r) throws XMLStreamException {
        return createXMLEventReader(null, r);
    }

    @Override
    public XMLEventReader createXMLEventReader(javax.xml.transform.Source source) throws XMLStreamException {
        return constructER(constructSR(source, true));
    }

    @Override
    public XMLEventReader createXMLEventReader(String systemId, InputStream in)
        throws XMLStreamException
    {
        return constructER(constructSR(systemId, in, true));
    }

    @Override
    public XMLEventReader createXMLEventReader(String systemId, Reader r)
        throws XMLStreamException
    {
        return constructER(constructSR(systemId, r, true));
    }

    @Override
    public XMLEventReader createXMLEventReader(XMLStreamReader sr)
        throws XMLStreamException
    {
        return constructER(Stax2ReaderAdapter.wrapIfNecessary(sr));
    }

    /*
    /**********************************************************************
    /* Stax, XMLInputFactory: XMLStreamReader factory methods
    /**********************************************************************
     */

    @Override
    public XMLStreamReader createXMLStreamReader(InputStream in)
        throws XMLStreamException
    {
        return constructSR(in, null, false);
    }
    
    @Override
    public XMLStreamReader createXMLStreamReader(InputStream in, String enc)
        throws XMLStreamException
    {
        return constructSR(in, enc, false);
    }

    @Override
    public XMLStreamReader createXMLStreamReader(Reader r)
        throws XMLStreamException
    {
        return constructSR(null, r, false);
    }

    @Override
    public XMLStreamReader createXMLStreamReader(String systemId, Reader r)
        throws XMLStreamException
    {
        return constructSR(systemId, r, false);
    }

    @Override
    public XMLStreamReader createXMLStreamReader(javax.xml.transform.Source src)
        throws XMLStreamException
    {
        return constructSR(src, false);
    }

    @Override
    public XMLStreamReader createXMLStreamReader(String systemId, InputStream in)
        throws XMLStreamException
    {
        return constructSR(systemId, in, false);
    }

    /*
    /**********************************************************************
    /* Stax, XMLInputFactory; generic accessors/mutators
    /**********************************************************************
     */

    @Override
    public Object getProperty(String name)
    {
        // false -> is mandatory, unrecognized will throw IllegalArgumentException
        return _config.getProperty(name, true);
    }

    @Override
    public void setProperty(String propName, Object value)
    {
        _config.setProperty(propName, value);
    } 

    @Override
    public XMLEventAllocator getEventAllocator() {
        return _allocator;
    }
    
    @Override
    public XMLReporter getXMLReporter() {
        return _config.getXMLReporter();
    }

    @Override
    public XMLResolver getXMLResolver() {
        return _config.getXMLResolver();
    }

    @Override
    public boolean isPropertySupported(String name) {
        return _config.isPropertySupported(name);
    }

    @Override
    public void setEventAllocator(XMLEventAllocator allocator) {
        _allocator = allocator;
    }

    @Override
    public void setXMLReporter(XMLReporter r) {
        _config.setXMLReporter(r);
    }

    @Override
    public void setXMLResolver(XMLResolver r) {
        _config.setXMLResolver(r);
    }

    /*
    /**********************************************************************
    /* Stax2 implementation; additional factory methods
    /**********************************************************************
     */

    @Override
    public XMLEventReader2 createXMLEventReader(URL src)
        throws XMLStreamException
    {
        return constructER(constructSR(src, true));
    }

    @Override
    public XMLEventReader2 createXMLEventReader(File f)
        throws XMLStreamException
    {
        return constructER(constructSR(f, true));
    }

    @Override
    public XMLStreamReader2 createXMLStreamReader(URL src)
        throws XMLStreamException
    {
        return constructSR(src, false);
    }

    /**
     * Convenience factory method that allows for parsing a document
     * stored in the specified file.
     */
    @Override
    public XMLStreamReader2 createXMLStreamReader(File f)
        throws XMLStreamException
    {
        return constructSR(f, false);
    }

    // // // StAX2 "Profile" mutators

    @Override
    public void configureForXmlConformance()
    {
        _config.configureForXmlConformance();
    }

    @Override
    public void configureForConvenience()
    {
        _config.configureForConvenience();
    }

    @Override
    public void configureForSpeed()
    {
        _config.configureForSpeed();
    }

    @Override
    public void configureForLowMemUsage()
    {
        _config.configureForLowMemUsage();
    }

    @Override
    public void configureForRoundTripping()
    {
        _config.configureForRoundTripping();
    }

    /*
    /**********************************************************************
    /* Non-blocking reader factories (AsyncXMLInputFactory)
    /**********************************************************************
     */

    @Override
    public AsyncXMLStreamReader<AsyncByteArrayFeeder> createAsyncForByteArray()
    {
        // TODO: pass system and/or public ids?
        ReaderConfig cfg = getNonSharedConfig(null, null, null, false, false);
        cfg.setActualEncoding("UTF-8");
        return new AsyncStreamReaderImpl<AsyncByteArrayFeeder> (new AsyncByteArrayScanner (cfg));
    }

    @Override
    public AsyncXMLStreamReader<AsyncByteArrayFeeder> createAsyncFor(byte[] input) throws XMLStreamException
    {
        return createAsyncFor(input, 0, input.length);
    }

    @Override
    public AsyncXMLStreamReader<AsyncByteArrayFeeder> createAsyncFor(byte[] input, int offset, int length)
        throws XMLStreamException
    {
         ReaderConfig cfg = getNonSharedConfig(null, null, null, false, false);
         cfg.setActualEncoding("UTF-8");
         AsyncByteArrayScanner scanner = new AsyncByteArrayScanner (cfg);
         scanner.feedInput(input, offset, length);
         return new AsyncStreamReaderImpl<AsyncByteArrayFeeder> (scanner);
    }

    @Override
    public AsyncXMLStreamReader<AsyncByteBufferFeeder> createAsyncForByteBuffer() {
        ReaderConfig cfg = getNonSharedConfig(null, null, null, false, false);
        cfg.setActualEncoding("UTF-8");
        return new AsyncStreamReaderImpl<AsyncByteBufferFeeder> (new AsyncByteBufferScanner (cfg));
    }

    @Override
    public AsyncXMLStreamReader<AsyncByteBufferFeeder> createAsyncFor(ByteBuffer input) throws XMLStreamException
    {
        ReaderConfig cfg = getNonSharedConfig(null, null, null, false, false);
        cfg.setActualEncoding("UTF-8");
        AsyncByteBufferScanner scanner = new AsyncByteBufferScanner (cfg);
        scanner.feedInput(input);
        return new AsyncStreamReaderImpl<AsyncByteBufferFeeder> (scanner);
    }

    /*
    /**********************************************************************
    /* Internal/package methods
    /**********************************************************************
     */

    /**
     * Method called when a non-shared copy of the current configuration
     * is needed. This is usually done when a new reader is constructed.
     */
    public ReaderConfig getNonSharedConfig(String systemId, String publicId,
                                           String extEncoding,
                                           boolean forEventReader,
                                           boolean forceAutoClose)
    {
        ReaderConfig cfg = _config.createNonShared(publicId, systemId, extEncoding);
        if (forEventReader) {
            /* No point in lazy parsing for event readers: no more efficient
             * (and possible less) since all data is needed, always; and
             * exceptions also get lazily thrown after the fact.
             */
            cfg.doParseLazily(false);
        }
        if (forceAutoClose) {
            cfg.doAutoCloseInput(true);
        }
        return cfg;
    }

    protected XMLStreamReader2 constructSR(InputStream in, String enc,
            boolean forEventReader)
        throws XMLStreamException
    {
        ReaderConfig cfg = getNonSharedConfig(null, null, enc, forEventReader, false);
        return StreamReaderImpl.construct(ByteSourceBootstrapper.construct(cfg, in));
    }

    protected XMLStreamReader2 constructSR(String systemId, Reader r,
                                           boolean forEventReader)
        throws XMLStreamException
    {
        ReaderConfig cfg = getNonSharedConfig(null, systemId, null, forEventReader, false);
        return StreamReaderImpl.construct(CharSourceBootstrapper.construct(cfg, r));
    }

    protected XMLStreamReader2 constructSR(String systemId, InputStream in,
                                        boolean forEventReader)
        throws XMLStreamException
    {
        ReaderConfig cfg = getNonSharedConfig(null, systemId, null, forEventReader, false);
        return StreamReaderImpl.construct(ByteSourceBootstrapper.construct(cfg, in));
    }

    protected XMLStreamReader2 constructSR(javax.xml.transform.Source src,
                                          boolean forEventReader)
        throws XMLStreamException
    {
        if (src instanceof Stax2Source) {
            return constructSR2((Stax2Source) src, forEventReader);
        }

        Reader r = null;
        InputStream in = null;
        String pubId = null;
        String sysId = null;
        String encoding = null;
        boolean autoCloseInput;

        if (src instanceof StreamSource) {
            StreamSource ss = (StreamSource) src;
            sysId = ss.getSystemId();
            pubId = ss.getPublicId();
            in = ss.getInputStream();
            if (in == null) {
                r = ss.getReader();
            }
            /* Caller still has access to stream/reader (except if we only
             * get system-id); no need to force auto-close here
             */
            autoCloseInput = false;
        } else if (src instanceof SAXSource) {
            SAXSource ss = (SAXSource) src;
            // Not a complete implementation, but maybe it's enough?
            sysId = ss.getSystemId();
            InputSource isrc = ss.getInputSource();
            if (isrc != null) {
                sysId = isrc.getSystemId();
                pubId = isrc.getPublicId();
                encoding = isrc.getEncoding();
                in = isrc.getByteStream();
                if (in == null) {
                    r = isrc.getCharacterStream();
                }
            }
            /* Caller still has access to stream/reader (except if we only
             * get system-id); no need to force auto-close here
             */
            autoCloseInput = false;
        } else if (src instanceof DOMSource) {
            autoCloseInput = false; // shouldn't matter
            ReaderConfig cfg = getNonSharedConfig(pubId, sysId, encoding, forEventReader, autoCloseInput);
            return DOMReaderImpl.createFrom((DOMSource) src, cfg);
        } else {
            throw new IllegalArgumentException("Can not instantiate StAX reader for XML source type "+src.getClass()+" (unrecognized type)");
        }
        if (in != null) {
            ReaderConfig cfg = getNonSharedConfig(pubId, sysId, encoding, forEventReader, autoCloseInput);
            return StreamReaderImpl.construct(ByteSourceBootstrapper.construct(cfg, in));
        }
        if (r != null) {
            ReaderConfig cfg = getNonSharedConfig(pubId, sysId, encoding, forEventReader, autoCloseInput);
            return StreamReaderImpl.construct(CharSourceBootstrapper.construct(cfg, r));
        }
        if (sysId != null && sysId.length() > 0) {
            /* If we must construct URL from system id, caller will not have
             * access to resulting stream, need to force auto-closing.
             */
            autoCloseInput = true;
            ReaderConfig cfg = getNonSharedConfig(pubId, sysId, encoding, forEventReader, autoCloseInput);
            try {
                URL url = URLUtil.urlFromSystemId(sysId);
                in = URLUtil.inputStreamFromURL(url);
                return StreamReaderImpl.construct(ByteSourceBootstrapper.construct(cfg, in));
            } catch (IOException ioe) {
                throw new IoStreamException (ioe);
            }
        }
        throw new XMLStreamException("Can not create Stax reader for the Source passed -- neither reader, input stream nor system id was accessible; can not use other types of sources (like embedded SAX streams)");
    }

    protected XMLStreamReader2 constructSR2(Stax2Source ss, boolean forEventReader)
        throws XMLStreamException
    {
        /* Caller has no access to these input sources, so we must force
         * auto-close ('true' after 'forEventReader')
         */
        ReaderConfig cfg = getNonSharedConfig(ss.getPublicId(), ss.getSystemId(), ss.getEncoding(), forEventReader, true);

        // Byte arrays can be accessed VERY efficiently...
        if (ss instanceof Stax2ByteArraySource) {
            Stax2ByteArraySource bs = (Stax2ByteArraySource) ss;
            return StreamReaderImpl.construct(ByteSourceBootstrapper.construct
                                              (cfg, bs.getBuffer(), bs.getBufferStart(), bs.getBufferLength()));
        }
        if (ss instanceof Stax2CharArraySource) {
            Stax2CharArraySource cs = (Stax2CharArraySource) ss;
            return StreamReaderImpl.construct(CharSourceBootstrapper.construct
                                              (cfg, cs.getBuffer(), cs.getBufferStart(), cs.getBufferLength()));
        }
        
        /* Ok, and this is the default, if we don't know a better
         * type-specific method:
         */
        try {
            InputStream in = ss.constructInputStream();
            if (in != null) {
                return StreamReaderImpl.construct(ByteSourceBootstrapper.construct(cfg, in));
            }
            Reader r = ss.constructReader();
            if (r != null) {
                return StreamReaderImpl.construct(CharSourceBootstrapper.construct(cfg, r));
            }
        } catch (IOException ioe) {
            throw new IoStreamException (ioe);
        }

        throw new IllegalArgumentException("Can not create stream reader for given Stax2Source: neither InputStream nor Reader available");
    }

    protected XMLStreamReader2 constructSR(URL src, boolean forEventReader)
        throws XMLStreamException
    {
        InputStream in;
        try {
            in = URLUtil.inputStreamFromURL(src);
        } catch (IOException ioe) {
            throw new IoStreamException (ioe);
        }
        // Construct from URL? Must auto-close:
        ReaderConfig cfg = getNonSharedConfig(URLUtil.urlToSystemId(src), null, null, forEventReader, true);
        return StreamReaderImpl.construct(ByteSourceBootstrapper.construct(cfg, in));
    }

    protected XMLStreamReader2 constructSR(File f, boolean forEventReader)
        throws XMLStreamException
    {
        try {
            InputStream in = new FileInputStream(f);
            String systemId = URLUtil.fileToSystemId(f);
        // Construct from File? Must auto-close:
            ReaderConfig cfg = getNonSharedConfig(systemId, null, null, forEventReader, true);
            return StreamReaderImpl.construct(ByteSourceBootstrapper.construct(cfg, in));
        } catch (IOException ioe) {
            throw new IoStreamException (ioe);
        }
    }

    public XMLEventReader2 constructER(XMLStreamReader2 sr) {
        return new EventReaderImpl (createEventAllocator(), sr);
    }

    protected XMLEventAllocator createEventAllocator() 
    {
        // Explicitly set allocate?
        if (_allocator != null) {
            return _allocator.newInstance();
        }
        // Complete or fast one?
        return _config.willPreserveLocation() ?
            EventAllocatorImpl.getDefaultInstance()
            : EventAllocatorImpl.getFastInstance();
    }
}

