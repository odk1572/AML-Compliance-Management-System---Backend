package com.app.aml.feature.strfiling.service;

import com.app.aml.feature.casemanagement.entity.CaseNote;
import com.app.aml.feature.ingestion.entity.Transaction;
import com.app.aml.feature.strfiling.entity.StrFiling;
import com.lowagie.text.DocumentException;
import lombok.RequiredArgsConstructor;
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

        // --- FORMATTERS ---
        DateTimeFormatter fullFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")
                .withZone(ZoneId.systemDefault());

        // This is the helper we will use inside the HTML loops (for notes/transactions)
        DateTimeFormatter dmyFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
                .withZone(ZoneId.systemDefault());

        // 1. Format the main filing date
        if (filing.getSysCreatedAt() != null) {
            context.setVariable("formattedCreatedAt", fullFormatter.format(filing.getSysCreatedAt()));
        } else {
            context.setVariable("formattedCreatedAt", "N/A");
        }

        // 2. Pass the date helper to Thymeleaf
        context.setVariable("dateHelper", dmyFormatter);

        String htmlContent = thymeleaf.process("str/str-report", context);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(htmlContent);
            renderer.layout();
            renderer.createPDF(outputStream);
            return outputStream.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new RuntimeException("Error rendering STR PDF", e);
        }
    }

    @Override
    public byte[] generateXml(StrFiling filing, List<Transaction> txns) {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();

            Element rootElement = doc.createElement("STR");
            doc.appendChild(rootElement);

            Element filingRef = doc.createElement("FilingReference");
            filingRef.appendChild(doc.createTextNode(filing.getFilingReference()));
            rootElement.appendChild(filingRef);

            Element regBody = doc.createElement("RegulatoryBody");
            regBody.appendChild(doc.createTextNode(filing.getRegulatoryBody()));
            rootElement.appendChild(regBody);

            Element typology = doc.createElement("TypologyCategory");
            typology.appendChild(doc.createTextNode(filing.getTypologyCategory().name()));
            rootElement.appendChild(typology);

            Element subjectName = doc.createElement("SubjectName");
            subjectName.appendChild(doc.createTextNode(filing.getSubjectName()));
            rootElement.appendChild(subjectName);

            Element subjectAcct = doc.createElement("SubjectAccountNo");
            subjectAcct.appendChild(doc.createTextNode(filing.getSubjectAccountNo()));
            rootElement.appendChild(subjectAcct);

            Element narrative = doc.createElement("SuspicionNarrative");
            narrative.appendChild(doc.createTextNode(filing.getSuspicionNarrative()));
            rootElement.appendChild(narrative);

            Element transactionsNode = doc.createElement("Transactions");
            rootElement.appendChild(transactionsNode);

            for (Transaction txn : txns) {
                Element txnNode = doc.createElement("Transaction");
                Element txnId = doc.createElement("TransactionId");
                txnId.appendChild(doc.createTextNode(txn.getId().toString()));
                txnNode.appendChild(txnId);
                Element amount = doc.createElement("Amount");
                amount.appendChild(doc.createTextNode(txn.getAmount() != null ? txn.getAmount().toString() : "0"));
                txnNode.appendChild(amount);
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
}