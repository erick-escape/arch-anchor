package com.archanchor.services.parsers.php;

import com.archanchor.services.parsers.php.PhpSourceFacts.ClassDeclaration;
import com.archanchor.services.parsers.php.PhpSourceFacts.ClassImport;
import com.archanchor.services.parsers.php.PhpSourceFacts.NamespaceBlock;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * How what was found in a PHP file, each at its position, becomes the file's namespaces
 * with their imports and declarations (#85). Positions are in whatever order they were
 * found: the builder sorts them.
 */
class PhpSourceFactsBuilderTest {

	private final PhpSourceFactsBuilder builder = new PhpSourceFactsBuilder();

	@Test
	void aNamespaceStatementHoldsWhatFollowsItUpToTheNextOne() {
		builder.namespaceStatement(10, "App\\Models");
		builder.classImport(30, new ClassImport("App\\Support\\Money", "Money"));
		builder.declaration(50, 90, "Order");
		builder.namespaceStatement(100, "App\\Http");
		builder.declaration(120, 160, "Kernel");

		assertThat(builder.build().namespaceBlocks()).containsExactly(
				new NamespaceBlock("App\\Models", List.of(new ClassImport("App\\Support\\Money", "Money")),
						List.of(new ClassDeclaration("Order", List.of()))),
				new NamespaceBlock("App\\Http", List.of(), List.of(new ClassDeclaration("Kernel", List.of()))));
	}

	@Test
	void aNamespaceBlockHoldsOnlyWhatItsBracesEnclose() {
		builder.namespaceBlock(10, 100, "App\\Billing");
		builder.declaration(20, 90, "Invoice");
		builder.namespaceBlock(110, 200, "");
		builder.declaration(120, 190, "LegacyMailer");

		assertThat(builder.build().namespaceBlocks()).extracting(NamespaceBlock::namespace)
			.containsExactly("App\\Billing", "");
		assertThat(builder.build().namespaceBlocks().get(1).declarations()).extracting(ClassDeclaration::name)
			.containsExactly("LegacyMailer");
	}

	@Test
	void codeOutsideAnyNamespaceIsInTheGlobalOne() {
		builder.classImport(5, new ClassImport("Vendor\\Mailer", "Mailer"));
		builder.declaration(20, 60, "LegacyMailer");
		builder.writtenClassName(40, "Mailer");

		assertThat(builder.build().namespaceBlocks())
			.containsExactly(new NamespaceBlock("", List.of(new ClassImport("Vendor\\Mailer", "Mailer")),
					List.of(new ClassDeclaration("LegacyMailer", List.of("Mailer")))));
	}

	@Test
	void aWrittenNameBelongsToTheDeclarationItIsWrittenInInTheOrderOfTheSource() {
		builder.declaration(10, 100, "Order");
		builder.declaration(110, 200, "Invoice");
		builder.writtenClassName(150, "Money");
		builder.writtenClassName(60, "Customer");
		builder.writtenClassName(30, "Book");

		assertThat(builder.build().namespaceBlocks().get(0).declarations()).containsExactly(
				new ClassDeclaration("Order", List.of("Book", "Customer")),
				new ClassDeclaration("Invoice", List.of("Money")));
	}

	// A function next to a class is not the class: what it names is nobody's dependency.
	@Test
	void aNameWrittenOutsideEveryDeclarationBelongsToNone() {
		builder.declaration(10, 100, "Order");
		builder.writtenClassName(120, "Logger");

		assertThat(builder.build().namespaceBlocks().get(0).declarations())
			.containsExactly(new ClassDeclaration("Order", List.of()));
	}

	// PHP allows a class to be declared inside a method; what that class names is its
	// own.
	@Test
	void aNameWrittenInADeclarationInsideAnotherBelongsToTheInnerOne() {
		builder.declaration(10, 200, "Outer");
		builder.declaration(50, 100, "Inner");
		builder.writtenClassName(30, "Before");
		builder.writtenClassName(70, "Within");
		builder.writtenClassName(150, "After");

		assertThat(builder.build().namespaceBlocks().get(0).declarations()).containsExactly(
				new ClassDeclaration("Outer", List.of("Before", "After")),
				new ClassDeclaration("Inner", List.of("Within")));
	}

	@Test
	void aFileThatDeclaresNothingHasNoNamespaceBlocks() {
		builder.namespaceStatement(10, "App\\Views");
		builder.classImport(30, new ClassImport("App\\Models\\User", "User"));

		assertThat(builder.build().namespaceBlocks()).isEmpty();
	}

}
