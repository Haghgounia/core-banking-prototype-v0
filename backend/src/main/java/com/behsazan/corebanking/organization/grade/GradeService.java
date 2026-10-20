package com.behsazan.corebanking.organization.grade;

import com.behsazan.corebanking.organization.grade.GradeModels.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class GradeService {
    private static final Pattern CODE = Pattern.compile("[A-Z][A-Z0-9_]{0,39}");
    private final GradeRepository repository;
    public GradeService(GradeRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true) public Map<String,Object> branchType() { return repository.branchType(); }
    @Transactional(readOnly = true) public List<Organization> organizations() { return repository.organizations(); }
    @Transactional(readOnly = true) public Unit branchById(long id) { return repository.branchById(id); }
    @Transactional(readOnly = true) public List<Unit> branches(String text, int limit) { return repository.branchSearch(text, limit); }
    @Transactional(readOnly = true) public List<Level> levels(Long organizationId, boolean active) { return repository.levels(organizationId, active); }
    @Transactional(readOnly = true) public List<Period> periods(long unitId) { return repository.periods(unitId); }
    @Transactional(readOnly = true) public Coverage coverage() { return repository.coverage(); }

    @Transactional
    public Level createLevel(LevelInput input, String actor) {
        validateLevel(input);
        if (!repository.organizationExists(input.organizationId())) throw GradeException.invalid("سازمان انتخاب‌شده وجود ندارد.");
        if (!"BRANCH".equals(repository.unitTypeCode(input.organizationUnitTypeId())))
            throw GradeException.invalid("در نسخه فعلی درجات فقط برای نوع شعبه تعریف می‌شوند.");
        long id = repository.nextLevelId();
        repository.insertLevel(id, input, actor);
        return repository.level(id);
    }
    @Transactional
    public Level updateLevel(long id, LevelInput input, String actor) {
        validateLevel(input);
        Level saved = repository.level(id);
        if (saved.organizationId() != input.organizationId() || saved.organizationUnitTypeId() != input.organizationUnitTypeId()
                || !saved.gradeSchemeCode().equals(input.gradeSchemeCode()) || !saved.gradeCode().equals(input.gradeCode()))
            throw GradeException.invalid("کد، سازمان، نوع واحد و نظام درجه پس از ایجاد تغییرپذیر نیستند.");
        if (repository.updateLevel(id, input, actor) != 1)
            throw GradeException.conflict("درجه توسط کاربر دیگری تغییر کرده است؛ صفحه را تازه‌سازی کنید.");
        return repository.level(id);
    }

    /** Direct assignment: one transaction, no approval or user-role checks. */
    @Transactional
    public Period assign(AssignmentInput input, String actor) {
        if (input == null || input.organizationUnitId() <= 0 || input.gradeLevelId() <= 0)
            throw GradeException.invalid("شعبه و درجه معتبر باید انتخاب شوند.");
        try { GradePolicy.dates(input.effectiveFrom(), input.effectiveTo()); }
        catch (IllegalArgumentException ex) { throw GradeException.invalid(ex.getMessage()); }
        if (input.descriptionFa() != null && input.descriptionFa().length() > 500)
            throw GradeException.invalid("شرح بیش از ۵۰۰ کاراکتر است.");

        repository.lockUnit(input.organizationUnitId());
        Unit unit = repository.requireUnit(input.organizationUnitId());
        Level level = repository.level(input.gradeLevelId());
        validateCompatibility(unit, level);
        List<Period> effective = repository.effective(unit.id());
        Period proposed = new Period(0, unit.id(), level.id(), level.gradeCode(), level.nameFa(),
                input.effectiveFrom(), input.effectiveTo(), "CHANGE", "ACTIVE", input.descriptionFa(), actor, 0);
        PeriodPlan plan;
        try { plan = GradePolicy.plan(proposed, effective); }
        catch (IllegalArgumentException ex) { throw GradeException.conflict(ex.getMessage()); }
        if (plan.closePredecessorId() != null &&
                repository.closePrevious(plan.closePredecessorId(), plan.closePredecessorVersion(), input.effectiveFrom(), actor) != 1)
            throw GradeException.conflict("دوره قبلی هم‌زمان تغییر کرده است؛ دوباره تلاش کنید.");
        long id = repository.nextPeriodId();
        repository.insertActivePeriod(id, input, effective.isEmpty() ? "INITIAL" : "CHANGE", actor);
        return repository.period(id);
    }

    private void validateCompatibility(Unit unit, Level level) {
        try { GradePolicy.eligibility(unit.organizationId(), unit.organizationUnitTypeId(),
                level.organizationId(), level.organizationUnitTypeId(), repository.unitTypeCode(unit.organizationUnitTypeId())); }
        catch (IllegalArgumentException ex) { throw GradeException.invalid(ex.getMessage()); }
        if (!level.active()) throw GradeException.invalid("درجه غیرفعال قابل تخصیص نیست.");
    }
    private void validateLevel(LevelInput input) {
        if (input == null || input.organizationId() <= 0 || input.organizationUnitTypeId() <= 0)
            throw GradeException.invalid("سازمان و نوع واحد اجباری‌اند.");
        if (input.gradeSchemeCode() == null || !CODE.matcher(input.gradeSchemeCode()).matches() ||
                input.gradeCode() == null || !CODE.matcher(input.gradeCode()).matches())
            throw GradeException.invalid("کد نظام و کد درجه باید از حروف بزرگ لاتین، عدد و زیرخط تشکیل شوند.");
        if (input.nameFa() == null || input.nameFa().isBlank() || input.nameFa().strip().length() > 150)
            throw GradeException.invalid("عنوان فارسی درجه معتبر نیست.");
        if (input.nameEn() != null && input.nameEn().length() > 150) throw GradeException.invalid("نام انگلیسی طولانی است.");
        if (input.displayOrder() <= 0 || input.displayOrder() > 99999) throw GradeException.invalid("ترتیب نمایش باید بین ۱ تا ۹۹۹۹۹ باشد.");
    }
}
