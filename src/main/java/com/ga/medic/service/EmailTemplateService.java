package com.ga.medic.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailTemplateService {

    private final ITemplateEngine templateEngine;

    public String render(String templateName, Map<String, ?> variables) {
        Context context = new Context(Locale.getDefault());
        variables.forEach(context::setVariable);
        return templateEngine.process(templateName, context);
    }
}
