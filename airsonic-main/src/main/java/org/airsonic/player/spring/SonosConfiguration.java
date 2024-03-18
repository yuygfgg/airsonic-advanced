package org.airsonic.player.spring;

import org.airsonic.player.service.SonosService;
import org.airsonic.player.service.sonos.SonosFaultInterceptor;
import org.airsonic.player.service.sonos.SonosLinkSecurityInterceptor;
import org.apache.cxf.Bus;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.jaxws.EndpointImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.ws.Endpoint;

import java.util.Collections;


@Configuration
@ImportResource({"classpath:META-INF/cxf/cxf.xml", "classpath:META-INF/cxf/cxf-servlet.xml"})
public class SonosConfiguration {

    @Autowired
    private JAXBContext jaxbContext;

    @Bean
    public Endpoint sonosEndpoint(Bus bus, SonosService sonosService,
            SonosFaultInterceptor sonosFaultInterceptor,
            SonosLinkSecurityInterceptor sonosSecurity) {
        EndpointImpl endpoint = new EndpointImpl(bus, sonosService);
        endpoint.setOutFaultInterceptors(Collections.singletonList(sonosFaultInterceptor));
        endpoint.setDataBinding(new JAXBDataBinding(jaxbContext));
        endpoint.setInInterceptors(Collections.singletonList(sonosSecurity));
        endpoint.publish("/Sonos");
        return endpoint;
    }
}
