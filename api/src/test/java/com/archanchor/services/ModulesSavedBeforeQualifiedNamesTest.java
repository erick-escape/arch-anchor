package com.archanchor.services;

import com.archanchor.domains.clazz.Clazz;
import com.archanchor.domains.module.Module;
import com.archanchor.dto.module.ModuleDTO;
import com.archanchor.mapstruct.ModuleMapper;
import com.archanchor.mapstruct.ModuleMapperImpl;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A {@code modules.bin} written before classes stored their qualified name (#83) must
 * still be read, and its classes still be recognised in each other's references: the file
 * outlives an upgrade of the tool. {@code /saved/modules-before-83.bin} was written by
 * the code as it was before the change: one module, {@code shop/web}, holding
 * {@code shop.web.CatalogController} and a class of the default package.
 */
class ModulesSavedBeforeQualifiedNamesTest {

	private final ModuleMapper moduleMapper = new ModuleMapperImpl();

	@Test
	void aClassSavedBeforeIsKnownByItsPackageAndNameJoinedByADot() throws IOException, ClassNotFoundException {
		Module web = moduleMapper.toEntity(savedBefore().get(0));

		assertThat(web.getClazzes()).extracting(Clazz::getName).containsExactly("CatalogController", "Unpackaged");
		assertThat(web.getClazzes()).extracting(Clazz::getQualifiedName)
			.containsExactly("shop.web.CatalogController", null);
		assertThat(web.getRefClazzes()).extracting(Clazz::getQualifiedName)
			.containsExactly("shop.web.CatalogController");
	}

	@Test
	void savingItAgainStoresTheName() throws IOException, ClassNotFoundException {
		Module web = moduleMapper.toEntity(savedBefore().get(0));

		ModuleDTO savedAgain = moduleMapper.toDto(web);

		assertThat(savedAgain.clazzes()[0].qualifiedName()).isEqualTo("shop.web.CatalogController");
		assertThat(savedAgain.clazzes()[1].qualifiedName()).isNull();
	}

	@SuppressWarnings("unchecked")
	private static List<ModuleDTO> savedBefore() throws IOException, ClassNotFoundException {
		try (InputStream saved = ModulesSavedBeforeQualifiedNamesTest.class
			.getResourceAsStream("/saved/modules-before-83.bin");
				ObjectInputStream objectInput = new ObjectInputStream(saved)) {
			return (List<ModuleDTO>) objectInput.readObject();
		}
	}

}
