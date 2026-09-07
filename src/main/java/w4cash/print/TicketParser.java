package w4cash.print;

import java.io.*;
import java.applet.*;

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;

import com.openbravo.data.loader.LocalRes;
import com.openbravo.pos.printer.DeviceDisplayBase;
import com.openbravo.pos.printer.DevicePrinter;
import com.openbravo.pos.printer.DeviceTicket;
import com.openbravo.pos.printer.TicketPrinterException;
import com.openbravo.pos.printer.printer.DevicePrinterPrinter;

/**
 * SAX handler that renders an Openbravo ticket template onto a
 * {@link DevicePrinter}.
 *
 * <p>
 * <b>Not thread-safe, and deliberately so:</b> parse state lives in the instance
 * fields below and the output device is held for the duration of a receipt.
 * Create one instance per print (see
 * {@code TicketPrintService#printPayment}) rather than sharing one. A shared
 * instance lets two concurrent prints interleave their
 * {@code beginReceipt}/{@code printText}/{@code endReceipt} calls onto the same
 * device and emit one merged, corrupt receipt.
 */
public class TicketParser extends DefaultHandler {

    // private DeviceTicket m_printer;
    // private DataLogicSystem m_system;

    private StringBuffer text;

    private String bctype;
    private String bcposition;
    private String xposition;
    private int m_iTextAlign;
    private int m_iTextLength;
    private int m_iTextStyle;

    private StringBuffer m_sVisorLine;
    private int m_iVisorAnimation;
    private String m_sVisorLine1;
    private String m_sVisorLine2;

    private double m_dValue1;
    private double m_dValue2;
    private int attribute3;

    private int m_iOutputType;
    private int m_currentPosition = 0;
    private Boolean m_linebreak = false;
    private static final int OUTPUT_NONE = 0;
    private static final int OUTPUT_DISPLAY = 1;
    private static final int OUTPUT_TICKET = 2;
    private static final int OUTPUT_FISCAL = 3;
    private DevicePrinter m_oOutputPrinter;
    protected int m_lastSize;

    /** Creates a new instance of TicketParser */
    public TicketParser() {
        m_oOutputPrinter = new DevicePrinterPrinter(null, null, "Microsoft Print to PDF",
                10,
                10,
                190,
                546,
                "A4");
        // m_printer = printer;
        // m_system = system;
    }

    public void printTicket(String sIn) throws TicketPrinterException {
        printTicket(new StringReader(sIn));
    }

    public void printTicket(Reader in) throws TicketPrinterException {

        try {
            // Both the factory and the parser are created per call: neither is
            // thread-safe. This used to be a lazily-initialised static SAXParser,
            // so every thread parsed through one shared instance.
            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXParser sp = spf.newSAXParser();
            sp.parse(new InputSource(in), this);

        } catch (ParserConfigurationException ePC) {
            throw new TicketPrinterException(LocalRes.getIntString("exception.parserconfig"), ePC);
        } catch (SAXException eSAX) {
            throw new TicketPrinterException(LocalRes.getIntString("exception.xmlfile"), eSAX);
        } catch (IOException eIO) {
            throw new TicketPrinterException(LocalRes.getIntString("exception.iofile"), eIO);
        }
    }

    @Override
    public void startDocument() throws SAXException {
        // inicalizo las variables pertinentes
        text = null;
        bctype = null;
        bcposition = null;
        m_sVisorLine = null;
        m_iVisorAnimation = DeviceDisplayBase.ANIMATION_NULL;
        m_sVisorLine1 = null;
        m_sVisorLine2 = null;
        m_iOutputType = OUTPUT_NONE;
        // line-layout state: reset too, so nothing carries over from a previous
        // parse on this instance (these were left untouched here before)
        m_currentPosition = 0;
        m_linebreak = false;
        m_lastSize = DevicePrinter.SIZE_0;
        // m_oOutputPrinter = null;
    }

    @Override
    public void endDocument() throws SAXException {
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

        switch (m_iOutputType) {
            case OUTPUT_NONE:
                if ("opendrawer".equals(qName)) {
                    String byteCode = attributes.getValue("byteCode");
                    // m_printer.getDevicePrinter(readString(attributes.getValue("printer"),
                    // "1")).openDrawer(byteCode);
                } else if ("play".equals(qName)) {
                    text = new StringBuffer();
                } else if ("ticket".equals(qName)) {
                    m_iOutputType = OUTPUT_TICKET;
                    // m_oOutputPrinter =
                    // m_printer.getDevicePrinter(readString(attributes.getValue("printer"), "1"));
                    m_oOutputPrinter.beginReceipt();
                } else if ("printer1".equals(qName)) {
                    m_iOutputType = OUTPUT_TICKET;
                    // m_oOutputPrinter = m_printer.getDevicePrinter("1");
                    m_oOutputPrinter.beginReceipt();
                } else if ("printer2".equals(qName)) {
                    m_iOutputType = OUTPUT_TICKET;
                    // m_oOutputPrinter = m_printer.getDevicePrinter("2");
                    m_oOutputPrinter.beginReceipt();
                } else if ("printer3".equals(qName)) {
                    m_iOutputType = OUTPUT_TICKET;
                    // m_oOutputPrinter = m_printer.getDevicePrinter("3");
                    m_oOutputPrinter.beginReceipt();
                } else if ("display".equals(qName)) {
                    m_iOutputType = OUTPUT_DISPLAY;
                    String animation = attributes.getValue("animation");
                    if ("scroll".equals(animation)) {
                        m_iVisorAnimation = DeviceDisplayBase.ANIMATION_SCROLL;
                    } else if ("flyer".equals(animation)) {
                        m_iVisorAnimation = DeviceDisplayBase.ANIMATION_FLYER;
                    } else if ("blink".equals(animation)) {
                        m_iVisorAnimation = DeviceDisplayBase.ANIMATION_BLINK;
                    } else if ("curtain".equals(animation)) {
                        m_iVisorAnimation = DeviceDisplayBase.ANIMATION_CURTAIN;
                    } else { // "none"
                        m_iVisorAnimation = DeviceDisplayBase.ANIMATION_NULL;
                    }
                    m_sVisorLine1 = null;
                    m_sVisorLine2 = null;
                    // m_oOutputPrinter = null;
                } else if ("fiscalreceipt".equals(qName)) {
                    m_iOutputType = OUTPUT_FISCAL;
                    // m_printer.getFiscalPrinter().beginReceipt();
                } else if ("fiscalzreport".equals(qName)) {
                    // m_printer.getFiscalPrinter().printZReport();
                } else if ("fiscalxreport".equals(qName)) {
                    // m_printer.getFiscalPrinter().printXReport();
                }
                break;
            case OUTPUT_TICKET:
                if ("image".equals(qName)) {
                    text = new StringBuffer();
                } else if ("qr".equals(qName)) {
                    text = new StringBuffer();
                } else if ("qrnolf".equals(qName)) {
                    text = new StringBuffer();
                    xposition = attributes.getValue("x");
                } else if ("barcode".equals(qName)) {
                    text = new StringBuffer();
                    bctype = attributes.getValue("type");
                    bcposition = attributes.getValue("position");
                } else if ("line".equals(qName)) {
                    m_currentPosition = 0;
                    m_lastSize = parseInt(attributes.getValue("size"), DevicePrinter.SIZE_0);
                    m_oOutputPrinter.beginLine(m_lastSize);
                } else if ("text".equals(qName)) {
                    text = new StringBuffer();
                    m_iTextStyle = ("true".equals(attributes.getValue("bold")) ? DevicePrinter.STYLE_BOLD
                            : DevicePrinter.STYLE_PLAIN)
                            | ("true".equals(attributes.getValue("underline")) ? DevicePrinter.STYLE_UNDERLINE
                                    : DevicePrinter.STYLE_PLAIN);
                    String sAlign = attributes.getValue("align");
                    if ("right".equals(sAlign)) {
                        m_iTextAlign = DevicePrinter.ALIGN_RIGHT;
                    } else if ("center".equals(sAlign)) {
                        m_iTextAlign = DevicePrinter.ALIGN_CENTER;
                    } else {
                        m_iTextAlign = DevicePrinter.ALIGN_LEFT;
                    }
                    m_iTextLength = parseInt(attributes.getValue("length"), 0);
                }
                break;
            case OUTPUT_DISPLAY:
                if ("line".equals(qName)) { // line 1 or 2 of the display
                    m_sVisorLine = new StringBuffer();
                } else if ("line1".equals(qName)) { // linea 1 del visor
                    m_sVisorLine = new StringBuffer();
                } else if ("line2".equals(qName)) { // linea 2 del visor
                    m_sVisorLine = new StringBuffer();
                } else if ("text".equals(qName)) {
                    text = new StringBuffer();
                    String sAlign = attributes.getValue("align");
                    if ("right".equals(sAlign)) {
                        m_iTextAlign = DevicePrinter.ALIGN_RIGHT;
                    } else if ("center".equals(sAlign)) {
                        m_iTextAlign = DevicePrinter.ALIGN_CENTER;
                    } else {
                        m_iTextAlign = DevicePrinter.ALIGN_LEFT;
                    }
                    m_iTextLength = parseInt(attributes.getValue("length"));
                }
                break;
            case OUTPUT_FISCAL:
                if ("line".equals(qName)) {
                    text = new StringBuffer();
                    m_dValue1 = parseDouble(attributes.getValue("price"));
                    m_dValue2 = parseDouble(attributes.getValue("units"), 1.0);
                    attribute3 = parseInt(attributes.getValue("tax"));

                } else if ("message".equals(qName)) {
                    text = new StringBuffer();
                } else if ("total".equals(qName)) {
                    text = new StringBuffer();
                    m_dValue1 = parseDouble(attributes.getValue("paid"));
                }
                break;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {

        switch (m_iOutputType) {
            case OUTPUT_NONE:
                if ("play".equals(qName)) {
                    try {
                        AudioClip oAudio = Applet
                                .newAudioClip(getClass().getClassLoader().getResource(text.toString()));
                        oAudio.play();
                    } catch (Exception fnfe) {
                        // throw new ResourceNotFoundException( fnfe.getMessage() );
                    }
                    text = null;
                }
                break;
            case OUTPUT_TICKET:
                if ("image".equals(qName)) {
                    /*
                     * try {
                     * BufferedImage image = m_system.getResourceAsImage(text.toString());
                     * if (image != null) {
                     * m_oOutputPrinter.printImage(image);
                     * }
                     * } catch (Exception fnfe) {
                     * // throw new ResourceNotFoundException( fnfe.getMessage() );
                     * }
                     * text = null;
                     */
                } else if ("qr".equals(qName)) {
                    try {
                        // TODO: Use ZXing library instead of QRGen for generating QR codes
                        // File imageFile = QRCode.from(text.toString()).to(ImageType.JPG).file();

                        // BufferedImage image = ImageIO.read(imageFile);
                        // m_oOutputPrinter.printImage(image);
                    } catch (Exception e) {

                    }
                    text = null;
                } else if ("qrnolf".equals(qName)) {
                    try {
                        // TODO: Use ZXing library instead of QRGen for generating QR codes
                        // File imageFile = QRCode.from(text.toString()).to(ImageType.JPG).file();

                        // BufferedImage image = ImageIO.read(imageFile);
                        // m_oOutputPrinter.printImageNoLF(image, x);
                    } catch (Exception e) {

                    }
                    text = null;
                    // } catch (Exception ex) {
                    // m_oOutputPrinter.printImageNoLF(image, 0);
                    // }
                    // } catch (Exception e) {

                    // }
                    text = null;
                } else if ("barcode".equals(qName)) {
                    m_oOutputPrinter.printBarCode(bctype, bcposition, text.toString());
                    text = null;
                } else if ("text".equals(qName)) {
                    if (m_iTextLength > 0) {

                        String txtCut = text.toString();
                        int cutIndex = 0;
                        if (txtCut.length() > m_iTextLength) {
                            cutIndex = txtCut.lastIndexOf(' ', m_iTextLength);
                            if (cutIndex > 0) {
                                cutIndex = cutIndex + 1;
                                txtCut = txtCut.substring(0, cutIndex);
                            } else {
                                cutIndex = m_iTextLength;
                                txtCut = txtCut.substring(0, cutIndex);
                            }
                        }

                        String txtPrefix = "";
                        if (m_linebreak && m_currentPosition > 0) {
                            txtPrefix = new String(new char[m_currentPosition]).replace('\0', ' ');
                        }

                        switch (m_iTextAlign) {
                            case DevicePrinter.ALIGN_RIGHT:
                                m_oOutputPrinter.printText(m_iTextStyle,
                                        txtPrefix + DeviceTicket.alignRight(txtCut, m_iTextLength));
                                break;
                            case DevicePrinter.ALIGN_CENTER:
                                m_oOutputPrinter.printText(m_iTextStyle,
                                        txtPrefix + DeviceTicket.alignCenter(txtCut, m_iTextLength));
                                break;
                            default: // DevicePrinter.ALIGN_LEFT
                                m_oOutputPrinter.printText(m_iTextStyle,
                                        txtPrefix + DeviceTicket.alignLeft(txtCut, m_iTextLength));
                                break;
                        }

                        if (cutIndex > 0) {
                            // add new line
                            m_oOutputPrinter.endLine();
                            m_oOutputPrinter.beginLine(m_lastSize);
                            text = new StringBuffer(text.substring(cutIndex));

                            m_linebreak = true;
                            endElement(uri, localName, qName);
                        } else {
                            m_currentPosition = m_currentPosition + m_iTextLength;
                            m_linebreak = false;
                        }

                    } else {
                        m_oOutputPrinter.printText(m_iTextStyle, text.toString());
                    }
                    text = null;
                } else if ("line".equals(qName)) {
                    m_oOutputPrinter.endLine();
                } else if ("ticket".equals(qName)) {
                    m_oOutputPrinter.endReceipt();
                    m_iOutputType = OUTPUT_NONE;
                    // m_oOutputPrinter = null;
                } else if ("printer1".equals(qName)) {
                    m_oOutputPrinter.endReceipt();
                    m_iOutputType = OUTPUT_NONE;
                    // m_oOutputPrinter = null;
                } else if ("printer2".equals(qName)) {
                    m_oOutputPrinter.endReceipt();
                    m_iOutputType = OUTPUT_NONE;
                    // m_oOutputPrinter = null;
                } else if ("printer3".equals(qName)) {
                    m_oOutputPrinter.endReceipt();
                    m_iOutputType = OUTPUT_NONE;
                    // m_oOutputPrinter = null;
                }
                break;
            case OUTPUT_DISPLAY:
                if ("line".equals(qName)) { // line 1 or 2 of the display
                    if (m_sVisorLine1 == null) {
                        m_sVisorLine1 = m_sVisorLine.toString();
                    } else {
                        m_sVisorLine2 = m_sVisorLine.toString();
                    }
                    m_sVisorLine = null;
                } else if ("line1".equals(qName)) { // linea 1 del visor
                    m_sVisorLine1 = m_sVisorLine.toString();
                    m_sVisorLine = null;
                } else if ("line2".equals(qName)) { // linea 2 del visor
                    m_sVisorLine2 = m_sVisorLine.toString();
                    m_sVisorLine = null;
                } else if ("text".equals(qName)) {
                    if (m_iTextLength > 0) {
                        switch (m_iTextAlign) {
                            case DevicePrinter.ALIGN_RIGHT:
                                m_sVisorLine.append(DeviceTicket.alignRight(text.toString(), m_iTextLength));
                                break;
                            case DevicePrinter.ALIGN_CENTER:
                                m_sVisorLine.append(DeviceTicket.alignCenter(text.toString(), m_iTextLength));
                                break;
                            default: // DevicePrinter.ALIGN_LEFT
                                m_sVisorLine.append(DeviceTicket.alignLeft(text.toString(), m_iTextLength));
                                break;
                        }
                    } else {
                        m_sVisorLine.append(text);
                    }
                    text = null;
                } else if ("display".equals(qName)) {
                    // m_printer.getDeviceDisplay().writeVisor(m_iVisorAnimation, m_sVisorLine1,
                    // m_sVisorLine2);
                    m_iVisorAnimation = DeviceDisplayBase.ANIMATION_NULL;
                    m_sVisorLine1 = null;
                    m_sVisorLine2 = null;
                    m_iOutputType = OUTPUT_NONE;
                    // m_oOutputPrinter = null;
                }
                break;
            case OUTPUT_FISCAL:
                if ("fiscalreceipt".equals(qName)) {
                    // m_printer.getFiscalPrinter().endReceipt();
                    m_iOutputType = OUTPUT_NONE;
                } else if ("line".equals(qName)) {
                    // m_printer.getFiscalPrinter().printLine(text.toString(), m_dValue1, m_dValue2,
                    // attribute3);
                    text = null;
                } else if ("message".equals(qName)) {
                    // m_printer.getFiscalPrinter().printMessage(text.toString());
                    text = null;
                } else if ("total".equals(qName)) {
                    // m_printer.getFiscalPrinter().printTotal(text.toString(), m_dValue1);
                    text = null;
                }
                break;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (text != null) {
            text.append(ch, start, length);
        }
    }

    private int parseInt(String sValue, int iDefault) {
        try {
            return Integer.parseInt(sValue);
        } catch (NumberFormatException eNF) {
            return iDefault;
        }
    }

    private int parseInt(String sValue) {
        return parseInt(sValue, 0);
    }

    private double parseDouble(String sValue, double ddefault) {
        try {
            return Double.parseDouble(sValue);
        } catch (NumberFormatException eNF) {
            return ddefault;
        }
    }

    private double parseDouble(String sValue) {
        return parseDouble(sValue, 0.0);
    }

    private String readString(String sValue, String sDefault) {
        if (sValue == null || sValue.equals("")) {
            return sDefault;
        } else {
            return sValue;
        }
    }
}
