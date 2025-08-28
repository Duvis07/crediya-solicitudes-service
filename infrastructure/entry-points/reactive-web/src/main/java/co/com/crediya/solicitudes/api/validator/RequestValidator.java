package co.com.crediya.solicitudes.api.validator;

import co.com.crediya.solicitudes.api.exceptions.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Validator;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class RequestValidator {
    
    private final Validator validator;
    
    public <T> Mono<T> validate(T request, String objectName) {
        return Mono.fromCallable(() -> {
            BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(request, objectName);
            validator.validate(request, bindingResult);
            
            if (bindingResult.hasErrors()) {
                log.warn("Validation errors found for {}: {}", objectName, bindingResult.getAllErrors());
                throw new ValidationException("Validation failed", bindingResult);
            }
            
            return request;
        }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
    }
}
