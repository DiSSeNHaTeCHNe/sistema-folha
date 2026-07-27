package br.com.techne.sistemafolha.arch;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameContaining;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

@AnalyzeClasses(
    packages = "br.com.techne.sistemafolha",
    importOptions = ImportOption.DoNotIncludeTests.class
)
class ModularArchitectureTest {

    @ArchTest
    static final ArchRule folha_and_dashboard_must_not_access_beneficios_infrastructure =
        noClasses()
            .that(simpleNameContaining("Folha").or(simpleNameContaining("Dashboard")))
            .and().resideOutsideOfPackage("..beneficios..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..beneficios.infrastructure..")
            .because("Folha and Dashboard must consume Benefícios only via ports, not infrastructure");

    @ArchTest
    static final ArchRule folha_must_not_access_beneficios_infrastructure =
        noClasses()
            .that().resideInAnyPackage("..folha..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..beneficios.infrastructure..")
            .because("Folha module must consume Benefícios only via ports, not infrastructure");

    @ArchTest
    static final ArchRule dashboard_must_not_access_beneficios_infrastructure =
        noClasses()
            .that().resideInAnyPackage("..dashboard..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..beneficios.infrastructure..")
            .because("Dashboard must consume Benefícios only via ports, not infrastructure");

    @ArchTest
    static final ArchRule importacao_must_not_access_beneficios_infrastructure =
        noClasses()
            .that().resideInAnyPackage("..importacao..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..beneficios.infrastructure..")
            .because("Importação must not depend on Benefícios infrastructure");

    @ArchTest
    static final ArchRule auth_must_not_access_beneficios_infrastructure =
        noClasses()
            .that().resideInAnyPackage("..auth..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..beneficios.infrastructure..")
            .because("Auth must not depend on Benefícios infrastructure");

    @ArchTest
    static final ArchRule cadastros_must_not_access_beneficios_infrastructure =
        noClasses()
            .that().resideInAnyPackage("..cadastros..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..beneficios.infrastructure..")
            .because("Cadastros must not depend on Benefícios infrastructure");

    @ArchTest
    static final ArchRule organograma_must_not_access_beneficios_infrastructure =
        noClasses()
            .that().resideInAnyPackage("..organograma..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..beneficios.infrastructure..")
            .because("Organograma must not depend on Benefícios infrastructure");

    @ArchTest
    static final ArchRule domain_layer_must_not_import_foreign_infrastructure =
        noClasses()
            .that().resideInAnyPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "..beneficios.infrastructure..",
                "..folha.infrastructure..",
                "..cadastros.infrastructure..",
                "..organograma.infrastructure..",
                "..auth.infrastructure..",
                "..importacao.infrastructure..",
                "..dashboard.infrastructure.."
            )
            .because("Domain layer must not depend on infrastructure (own or foreign)");

    @ArchTest
    static final ArchRule controllers_must_reside_in_api_layer =
        classes()
            .that().areAnnotatedWith(RestController.class)
            .should().resideInAnyPackage("..api..")
            .because("HTTP controllers belong in the api layer of their domain");

    @ArchTest
    static final ArchRule controllers_must_not_inject_repositories =
        noFields()
            .that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
            .should().haveRawType(resideInAnyPackage("..repository..")
                .or(resideInAnyPackage("..infrastructure..")))
            .because("Controllers must delegate to application services, not repositories");

    @ArchTest
    static final ArchRule acl_consumers_must_not_access_organograma_internals =
        noClasses()
            .that().resideInAnyPackage(
                "..folha..",
                "..beneficios..",
                "..dashboard..",
                "..importacao..",
                "..auth..",
                "..security.."
            )
            .should().dependOnClassesThat()
            .resideInAnyPackage("..organograma.infrastructure..", "..organograma.application..")
            .because("Cross-domain ACL must use OrganogramaAcessoPort only, not organograma internals");

    @ArchTest
    static final ArchRule beneficios_application_must_not_access_foreign_infrastructure =
        noClasses()
            .that().resideInAnyPackage("..beneficios..application..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "..folha.infrastructure..",
                "..cadastros.infrastructure..",
                "..organograma.infrastructure..",
                "..auth.infrastructure.."
            )
            .because("beneficios.application must not depend on foreign infrastructure (same-domain OK)");

    @ArchTest
    static final ArchRule folha_application_must_not_access_foreign_infrastructure =
        noClasses()
            .that().resideInAnyPackage("..folha..application..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "..beneficios.infrastructure..",
                "..cadastros.infrastructure..",
                "..organograma.infrastructure..",
                "..auth.infrastructure.."
            )
            .because("folha.application must not depend on foreign infrastructure (same-domain OK)");

    @ArchTest
    static final ArchRule cadastros_application_must_not_access_foreign_infrastructure =
        noClasses()
            .that().resideInAnyPackage("..cadastros..application..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "..beneficios.infrastructure..",
                "..folha.infrastructure..",
                "..organograma.infrastructure..",
                "..auth.infrastructure.."
            )
            .because("cadastros.application must not depend on foreign infrastructure (same-domain OK)");

    @ArchTest
    static final ArchRule organograma_application_must_not_access_foreign_infrastructure =
        noClasses()
            .that().resideInAnyPackage("..organograma..application..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "..beneficios.infrastructure..",
                "..folha.infrastructure..",
                "..cadastros.infrastructure..",
                "..auth.infrastructure.."
            )
            .because("organograma.application (incl. acesso) must not depend on foreign infrastructure "
                + "(same-domain OK)");

    @ArchTest
    static final ArchRule auth_application_must_not_access_foreign_infrastructure =
        noClasses()
            .that().resideInAnyPackage("..auth..application..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "..beneficios.infrastructure..",
                "..folha.infrastructure..",
                "..cadastros.infrastructure..",
                "..organograma.infrastructure.."
            )
            .because("auth.application must not depend on foreign infrastructure (same-domain OK)");

    @ArchTest
    static final ArchRule dashboard_application_must_not_access_foreign_infrastructure =
        noClasses()
            .that().resideInAnyPackage("..dashboard..application..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "..beneficios.infrastructure..",
                "..folha.infrastructure..",
                "..cadastros.infrastructure..",
                "..organograma.infrastructure..",
                "..auth.infrastructure.."
            )
            .because("dashboard.application must not depend on foreign infrastructure (same-domain OK)");

    @ArchTest
    static final ArchRule importacao_application_must_not_access_foreign_infrastructure =
        noClasses()
            .that().resideInAnyPackage("..importacao..application..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "..beneficios.infrastructure..",
                "..folha.infrastructure..",
                "..cadastros.infrastructure..",
                "..organograma.infrastructure..",
                "..auth.infrastructure.."
            )
            .because("importacao.application must not depend on foreign infrastructure (same-domain OK)");
}
