package gr.stevenicol.samples.jasper;

import io.quarkiverse.jasperreports.repository.ReadOnlyStreamingService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRCsvDataSource;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.pdf.JRPdfExporter;
import org.apache.camel.builder.RouteBuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;


@ApplicationScoped
public class RestRouter extends RouteBuilder {

    @Inject
    ReadOnlyStreamingService repo;

    private static final String sqlQuery = """
            SELECT
             TOP 50
             [id] = a.[id],
             [Code] = a.[Code],
             [LastName] = a.[LastName],
             [FirstName] = a.[FirstName],
             [FatherName] = a.[FatherName],
             [AMKA] = a.[AMKA],
             [name] = a.[name]
           FROM Person a 
           WHERE ((a.IsPatient=1)) 
             AND (NULLIF(cancelled,0) IS NULL) 
             AND a.AMKA IS NOT NULL 
             AND a.FatherName IS NOT NULL 
           ORDER BY a.id DESC
           """;

    private static final String BLANK_REPORT_NAME = "Blank_A4.jasper";

    @Override
    public void configure() throws Exception {

        from("direct:customer-report")
                .id("customer-report")
                .setBody(constant(sqlQuery))
                .log("${body}")
                .to("jdbc:camel")
                .log("${body}")
                .marshal().csv()
                .log("${body}")
                .process(exchange -> {

                    long start = System.currentTimeMillis();

                    String data = exchange.getIn().getBody(String.class);

                    Map<String, Object> parameters = new HashMap<>();
                    parameters.put("SomeName", "Alpha Bank");

                    String[] columnNames = new String[] { "id", "Code", "LastName", "FirstName", "FatherName",  "AMKA", "DateBirth", "Phone", "EMail", "MobilePhone", "name"};
                    InputStream is = new ByteArrayInputStream(data.getBytes());

                    JRCsvDataSource dataSource = new JRCsvDataSource(is, StandardCharsets.UTF_8.name());
                    dataSource.setRecordDelimiter("\n");
                    dataSource.setColumnNames(columnNames);

                    JasperPrint jasperPrint = JasperFillManager.getInstance(repo.getContext()).fillFromRepo(BLANK_REPORT_NAME, parameters, dataSource);

                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    JRPdfExporter exporter = new JRPdfExporter();

                    exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                    exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));

                    exporter.exportReport();

                    var result = outputStream.toByteArray();
                    exchange.getIn().setBody(result, byte[].class);

                    exchange.getIn().setHeader("Content-Disposition", "attachment;filename=sampleJasper.pdf");
                    exchange.getIn().setHeader("Content-Type", "application/pdf");

                })
                .end();

    }
}
