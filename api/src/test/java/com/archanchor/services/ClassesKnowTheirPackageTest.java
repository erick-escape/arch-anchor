package com.archanchor.services;

import com.archanchor.domains.clazz.Clazz;
import com.archanchor.domains.module.Module;
import com.archanchor.mapstruct.ModuleMapper;
import com.archanchor.mapstruct.ModuleMapperImpl;
import com.archanchor.services.rating.ActiveRatingWeights;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Issue #43: a class records the package it is declared in, so that a reference to it can
 * be recognised as internal wherever the class ends up after a move or merge. It survives
 * the DTO in which modules are saved between requests.
 */
class ClassesKnowTheirPackageTest {

	@TempDir
	Path workspace;

	private final ModuleService moduleService = new ModuleService(new UniformDependenciesParserFactory("java.sql"),
			new ActiveRatingWeights());

	@Test
	void aClassRecordsThePackageItDeclares() throws IOException {
		Path project = projectWithSource("shop/web/CatalogController.java", """
				/*
				 * package not.this.one;
				 */
				package shop.web;

				public class CatalogController {
				}
				""");

		List<Module> modules = moduleService.getModules(project.toString());

		assertThat(modules).singleElement()
			.satisfies(
					web -> assertThat(web.getClazzes()).extracting(Clazz::getPackageName).containsExactly("shop.web"));
	}

	@Test
	void aClassInTheDefaultPackageHasNoPackage() throws IOException {
		Path project = projectWithSource("shop/web/Main.java", "public class Main {\n}\n");

		List<Module> modules = moduleService.getModules(project.toString());

		assertThat(modules.get(0).getClazzes().get(0).getPackageName()).isNull();
	}

	@Test
	void thePackageIsKeptInTheSavedModules() throws IOException {
		Path project = projectWithSource("shop/web/CatalogController.java",
				"package shop.web;\nclass CatalogController {}\n");
		ModuleMapper moduleMapper = new ModuleMapperImpl();

		Module saved = moduleMapper.toEntity(moduleMapper.toDto(moduleService.getModules(project.toString()).get(0)));

		assertThat(saved.getClazzes().get(0).getPackageName()).isEqualTo("shop.web");
	}

	private Path projectWithSource(String source, String content) throws IOException {
		Path file = workspace.resolve("shop-project/src/main/java").resolve(source);
		Files.createDirectories(file.getParent());
		Files.writeString(file, content);
		return workspace.resolve("shop-project");
	}

}
