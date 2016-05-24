package net.dataNinja.ee.dom;

import net.dataNinja.ee.WFCException;
import net.dataNinja.ee.in.ReaderConfig;
import org.codehaus.stax2.ri.dom.DOMWrappingReader;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.dom.DOMSource;
import java.util.Collections;

/**
 * Concrete DOM-backed implementation, based on the Stax2 reference
 * implementation's default implementation.
 */
public class DOMReaderImpl
    extends DOMWrappingReader
{
    protected final ReaderConfig _config;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    protected DOMReaderImpl(DOMSource src, ReaderConfig cfg)
        throws XMLStreamException
    {
        super(src, cfg.willSupportNamespaces(), cfg.willCoalesceText());
        _config = cfg;
        if (cfg.hasInternNamesBeenEnabled()) {
            setInternNames(cfg.willInternNames());
        }
        if (cfg.hasInternNsURIsBeenEnabled()) {
            setInternNsURIs(cfg.willInternNsURIs());
        }
    }

    public static DOMReaderImpl createFrom(DOMSource src, ReaderConfig cfg)
        throws XMLStreamException
    {
        return new DOMReaderImpl (src, cfg);
    }

    /*
    /**********************************************************************
    /* Defined/Overridden config methods
    /**********************************************************************
     */

    @Override
    public boolean isPropertySupported(String name) {
        return _config.isPropertySupported(name);
    }

    @Override
    public Object getProperty(String name)
    {
        if (name.equals("javax.xml.stream.entities")) {
            // !!! TBI
            return Collections.EMPTY_LIST;
        }
        if (name.equals("javax.xml.stream.notations")) {
            // !!! TBI
            return Collections.EMPTY_LIST;
        }
        // true -> mandatory, unrecognized will throw exception
        return _config.getProperty(name, true);
    }

    @Override
    public boolean setProperty(String name, Object value)
    {
        /* Note: can not call local method, since it'll return false for
         * recognized but non-mutable properties
         */
        return _config.setProperty(name, value);
    }

    /*
    /**********************************************************************
    /* Defined/Overridden error reporting
    /**********************************************************************
     */

    @Override
    protected void throwStreamException(String msg, Location loc)
        throws XMLStreamException
    {
        throw new WFCException (msg, loc);
    }
}
