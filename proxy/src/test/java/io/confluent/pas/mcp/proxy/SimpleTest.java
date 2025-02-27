package io.confluent.pas.mcp.proxy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.confluent.pas.mcp.common.services.Schemas;
import org.junit.jupiter.api.Test;
import scala.Console;

public class SimpleTest {

    @Test
    public void test() {
//        Schemas.ResourceRegistration resourceRegistration = new Schemas.ResourceRegistration(
//                Schemas.Registration.RegistrationType.RESOURCE,
//                "Resource",
//                "Resource Description",
//                "Resource Request Schema",
//                "Resource Response Schema",
//                "Coore",
//                "Mimet",
//                "URL"
//        );
//
//        try {
//            final String json = new ObjectMapper().writeValueAsString(resourceRegistration);
//
//            Console.print(json);
//
//            final Schemas.Registration rcs = new ObjectMapper().readValue(json, Schemas.Registration.class);
//
//            Console.print(rcs.getClass().toString());
//        } catch (JsonProcessingException e) {
//            throw new RuntimeException(e);
//        }
    }

}
