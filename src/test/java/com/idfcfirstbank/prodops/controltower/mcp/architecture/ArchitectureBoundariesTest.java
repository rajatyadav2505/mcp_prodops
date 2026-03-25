package com.idfcfirstbank.prodops.controltower.mcp.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
    packages = "com.idfcfirstbank.prodops.controltower.mcp",
    importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureBoundariesTest {

  @ArchTest
  static final ArchRule domain_services_do_not_depend_on_adapters =
      noClasses()
          .that()
          .resideInAPackage("..domain.service..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..adapter..");

  @ArchTest
  static final ArchRule domain_packages_do_not_depend_on_security =
      noClasses()
          .that()
          .resideInAPackage("..domain..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..security..");

  @ArchTest
  static final ArchRule adapters_do_not_depend_on_domain_services =
      noClasses()
          .that()
          .resideInAPackage("..adapter..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..domain.service..");

  @ArchTest
  static final ArchRule mcp_layer_stays_out_of_adapters =
      noClasses()
          .that()
          .resideInAPackage("..mcp..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..adapter..");
}
