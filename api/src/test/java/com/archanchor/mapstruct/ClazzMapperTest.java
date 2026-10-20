package com.archanchor.mapstruct;

import com.archanchor.domains.clazz.Clazz;
import com.archanchor.domains.dependency.Dependency;
import com.archanchor.domains.dependency.Type;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ClazzMapper}. Every class of a module goes entity -> DTO -> entity on
 * its way to and from {@code modules.bin}, so a field the mapper drops is lost for good
 * (#8, #10).
 */
class ClazzMapperTest {

	private final ClazzMapper clazzMapper = new ClazzMapperImpl();

	@Test
	void roundTripKeepsEveryField() {
		// Every field non-null and distinct from the others, so a dropped or swapped
		// mapping shows; enforceMode was once added to both sides by hand (#10).
		Clazz original = misplacedSalesReport();

		Clazz roundTripped = clazzMapper.toEntity(clazzMapper.toDto(original));

		assertThat(roundTripped).usingRecursiveComparison().isEqualTo(original);
	}

	private static Clazz misplacedSalesReport() {
		Dependency javaSql = new Dependency("java.sql",
				List.of(new Type("java.sql.Connection"), new Type("java.sql.ResultSet")));
		Clazz salesReport = new Clazz("c1", "SalesReport", List.of(javaSql), 0.25, 0.75, "web", "persistence", "MUST");
		salesReport.setPackageName("shop.web");
		return salesReport;
	}

}
