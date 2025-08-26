package co.com.crediya.solicitudes.api.config;

import co.com.crediya.solicitudes.model.loantype.LoanTypeEnum;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class LoanTypeDeserializer extends JsonDeserializer<LoanTypeEnum> {

    @Override
    public LoanTypeEnum deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getValueAsString();
        return LoanTypeEnum.fromCode(value);
    }
}
