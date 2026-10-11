package com.behsazan.corebanking.productbuilder.application;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** R28 normalized employment choices; no concatenated values in EMPLOYMENT_TYPE_CODE. */
public final class LoanEmploymentSelectionPolicy {
    private LoanEmploymentSelectionPolicy() { }
    public static List<String> normalize(List<?> requested, Set<String> allowed) {
        if (requested == null || requested.size() > 20)
            throw new ProductBuilderValidationException("حداکثر ۲۰ نوع اشتغال و فهرست صریح انتخاب‌ها مجاز است.");
        LinkedHashSet<String> selected = new LinkedHashSet<>();
        for (Object raw : requested) {
            if (!(raw instanceof String code) || !allowed.contains(code) || !selected.add(code))
                throw new ProductBuilderValidationException("نوع اشتغال نامعتبر یا تکراری ارسال شده است.");
        }
        return List.copyOf(selected);
    }
}
