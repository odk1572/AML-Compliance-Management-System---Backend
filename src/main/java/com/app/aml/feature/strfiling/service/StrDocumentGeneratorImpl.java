package com.app.aml.feature.strfiling.service;

import com.app.aml.feature.casemanagement.entity.CaseNote;
import com.app.aml.feature.ingestion.entity.Transaction;
import com.app.aml.feature.strfiling.entity.StrFiling;
import com.lowagie.text.DocumentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StrDocumentGeneratorImpl implements StrDocumentGenerator {

    private final TemplateEngine thymeleaf;

    @Override
    public byte[] generatePdf(StrFiling filing, List<Transaction> txns, List<CaseNote> notes, List<String> evidence) {
        Context context = new Context();
        context.setVariable("filing", filing);
        context.setVariable("transactions", txns);
        context.setVariable("notes", notes);
        context.setVariable("evidence", evidence);

        DateTimeFormatter dmyHm = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm").withZone(ZoneId.systemDefault());
        DateTimeFormatter dmy = DateTimeFormatter.ofPattern("dd-MM-yyyy").withZone(ZoneId.systemDefault());

        context.setVariable("formattedCreatedAt", filing.getSysCreatedAt() != null ? dmyHm.format(filing.getSysCreatedAt()) : "N/A");
        context.setVariable("dateHelper", dmy);
        String htmlContent = thymeleaf.process("str/str-report", context);

        htmlContent = sanitizeXmlContent(htmlContent);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(htmlContent);
            renderer.layout();
            renderer.createPDF(outputStream);
            return outputStream.toByteArray();
        } catch (DocumentException | IOException e) {
            log.error("Failed to render STR PDF. This is often due to invalid characters like '&' in the data.", e);
            throw new RuntimeException("Error rendering STR PDF", e);
        }
    }

    @Override
    public byte[] generateXml(StrFiling filing, List<Transaction> txns) {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();

            Element rootElement = doc.createElement("STR_Report");
            doc.appendChild(rootElement);

            addTextNode(doc, rootElement, "FilingReference", filing.getFilingReference());
            addTextNode(doc, rootElement, "RegulatoryBody", filing.getRegulatoryBody());
            addTextNode(doc, rootElement, "TypologyCategory", filing.getTypologyCategory().name());
            addTextNode(doc, rootElement, "SubjectName", filing.getSubjectName());
            addTextNode(doc, rootElement, "SubjectAccountNo", filing.getSubjectAccountNo());
            addTextNode(doc, rootElement, "SuspicionNarrative", filing.getSuspicionNarrative());

            Element transactionsNode = doc.createElement("LinkedTransactions");
            rootElement.appendChild(transactionsNode);

            for (Transaction txn : txns) {
                Element txnNode = doc.createElement("Transaction");

                addTextNode(doc, txnNode, "TransactionId", txn.getId().toString());
                addTextNode(doc, txnNode, "Reference", txn.getTransactionRef());
                addTextNode(doc, txnNode, "Amount", txn.getAmount() != null ? txn.getAmount().toString() : "0");
                addTextNode(doc, txnNode, "Currency", txn.getCurrencyCode());
                addTextNode(doc, txnNode, "Timestamp", txn.getTransactionTimestamp().toString());
                addTextNode(doc, txnNode, "OriginatorAccount", txn.getOriginatorAccountNo());
                addTextNode(doc, txnNode, "BeneficiaryAccount", txn.getBeneficiaryAccountNo());

                // FIX: You were missing this line in your loop!
                transactionsNode.appendChild(txnNode);
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

            DOMSource source = new DOMSource(doc);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            StreamResult result = new StreamResult(outputStream);
            transformer.transform(source, result);
            return outputStream.toByteArray();

        } catch (ParserConfigurationException | TransformerException e) {
            throw new RuntimeException("Error generating STR XML", e);
        }
    }


    private String sanitizeXmlContent(String html) {
        if (html == null) return "";
        return html
                .replace("&nbsp;", "&#160;") // Flying Saucer hates &nbsp;
                .replaceAll("&(?![a-zA-Z0-9#]+;)", "&amp;"); // Escapes any '&' NOT followed by an existing entity
    }

    private void addTextNode(Document doc, Element parent, String tagName, String content) {
        Element node = doc.createElement(tagName);
        // doc.createTextNode automatically handles escaping for XML nodes
        node.appendChild(doc.createTextNode(content != null ? content : ""));
        parent.appendChild(node);
    }
}