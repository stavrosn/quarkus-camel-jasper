package gr.stevenicol.samples.jasper;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;


@ApplicationScoped
public class RestApi extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        rest("/jasper")
                .get("/customer/{customerId}")
                .produces("application/pdf")
                    .to("direct:customer-report");

    }
}
