package com.archanchor.services.parsers.php;

import com.archanchor.services.parsers.php.PhpSourceFacts.ClassDeclaration;
import com.archanchor.services.parsers.php.PhpSourceFacts.ClassImport;
import com.archanchor.services.parsers.php.PhpSourceFacts.NamespaceBlock;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * What the tree-sitter grammar reports of a PHP file (#85). This is where everything
 * assumed of the grammar is checked: how it names namespaces, imports and declarations,
 * and every place in which PHP expects a class. It runs the real native library.
 */
class TreeSitterPhpSourceReaderTest {

	private final TreeSitterPhpSourceReader reader = new TreeSitterPhpSourceReader();

	@Test
	void aFileDeclaresItsNamespaceItsImportsAndItsClass() {
		PhpSourceFacts facts = reader.factsOf("""
				<?php
				declare(strict_types=1);

				namespace App\\Models;

				use App\\Support\\Money;

				final class Order extends Record {}
				""");

		assertThat(facts.namespaceBlocks())
			.containsExactly(new NamespaceBlock("App\\Models", List.of(new ClassImport("App\\Support\\Money", "Money")),
					List.of(new ClassDeclaration("Order", List.of("Record")))));
	}

	@Test
	void interfacesTraitsAndEnumsAreDeclarationsToo() {
		assertThat(declarationsOf("""
				<?php
				namespace App;
				interface Describes extends \\Stringable, Other\\Thing {}
				trait Audits { use Stamps; }
				enum Status: string implements HasLabel { case Open = 'open'; }
				abstract class Shape {}
				readonly class Point {}
				""")).containsExactly(new ClassDeclaration("Describes", List.of("\\Stringable", "Other\\Thing")),
				new ClassDeclaration("Audits", List.of("Stamps")), new ClassDeclaration("Status", List.of("HasLabel")),
				new ClassDeclaration("Shape", List.of()), new ClassDeclaration("Point", List.of()));
	}

	@Test
	void importsAreReadPlainAliasedListedAndGrouped() {
		assertThat(importsOf("""
				<?php
				namespace App;
				use App\\Models\\User;
				use App\\Models\\Invoice as Bill;
				use App\\Support\\Money, App\\Support\\Clock as Time;
				use App\\Http\\{Kernel, Middleware\\Auth as Guard};
				use \\Vendor\\Mailer;
				class A {}
				""")).containsExactly(new ClassImport("App\\Models\\User", "User"),
				new ClassImport("App\\Models\\Invoice", "Bill"), new ClassImport("App\\Support\\Money", "Money"),
				new ClassImport("App\\Support\\Clock", "Time"), new ClassImport("App\\Http\\Kernel", "Kernel"),
				new ClassImport("App\\Http\\Middleware\\Auth", "Guard"), new ClassImport("Vendor\\Mailer", "Mailer"));
	}

	@Test
	void importsOfFunctionsAndConstantsAreNotImportsOfClasses() {
		assertThat(importsOf("""
				<?php
				namespace App;
				use function App\\Support\\format_money;
				use const App\\Support\\TAX_RATE;
				use function App\\Support\\{first, second};
				use App\\Support\\{function helper, const LIMIT, Report};
				class A {}
				""")).containsExactly(new ClassImport("App\\Support\\Report", "Report"));
	}

	@Test
	void aClassNamesItsParentItsInterfacesItsTraitsAndItsAttributes() {
		assertThat(namesWrittenIn(
				"""
						<?php
						#[ORM\\Entity]
						#[\\Attribute(\\Attribute::TARGET_CLASS)]
						class A extends Base implements \\Countable, Contracts\\Describes
						{
							use Concerns\\Audits, Logs { Concerns\\Audits::stamp insteadof Logs; Logs::stamp as protected mark; }

							#[Route('/a')]
							public function show(#[Sensitive] string $token): void {}
						}
						"""))
			.containsExactly("ORM\\Entity", "\\Attribute", "\\Attribute", "Base", "\\Countable", "Contracts\\Describes",
					"Concerns\\Audits", "Logs", "Concerns\\Audits", "Logs", "Route", "Sensitive");
	}

	@Test
	void aClassNamesTheTypesOfItsPropertiesConstantsParametersAndReturns() {
		assertThat(namesWrittenIn("""
				<?php
				class A
				{
					public const Clock DEFAULT_CLOCK = null;
					private ?User $current = null;
					protected Order|Bill|null $subject;
					private \\Traversable&\\Countable $items;
					private (Shape&Solid)|null $body;
					public Label $label { set(Text $value) { $this->label = $value; } }

					public function __construct(private readonly Cash $price, ?\\DateTimeImmutable $at = null) {}
					public function show(User $user, int $id, namespace\\Local $local, string ...$rest): ?Report {}
				}
				""")).containsExactly("Clock", "User", "Order", "Bill", "\\Traversable", "\\Countable", "Shape",
				"Solid", "Label", "Text", "Cash", "\\DateTimeImmutable", "User", "namespace\\Local", "Report");
	}

	@Test
	void aClassNamesWhatItsCodeCreatesCallsAndTests() {
		assertThat(namesWrittenIn("""
				<?php
				class A
				{
					public function run($x)
					{
						$order = new Order(1);
						$found = User::find(1);
						$callable = Money::of(...);
						$status = Order::STATUS_OPEN;
						$name = Invoice::class;
						$shared = \\App\\Registry::$instance;
						if ($x instanceof Admin) {}
						try {} catch (\\RuntimeException | DomainError $e) {} catch (Oops) {}
						$typed = function (Book $b): Shelf { return new Shelf(); };
						$arrow = fn(Reader $r): ?Role => null;
						return match (true) { default => new Report(new Summary()) };
					}
				}
				""")).containsExactly("Order", "User", "Money", "Order", "Invoice", "\\App\\Registry", "Admin",
				"\\RuntimeException", "DomainError", "Oops", "Book", "Shelf", "Shelf", "Reader", "Role", "Report",
				"Summary");
	}

	// An anonymous class is not a class of the project; what it names is named by the
	// class it is written in.
	@Test
	void whatAnAnonymousClassNamesBelongsToTheClassItIsWrittenIn() {
		assertThat(namesWrittenIn("""
				<?php
				class A
				{
					public function make()
					{
						return new class(1) extends Bill implements \\JsonSerializable {
							public function jsonSerialize(): Payload { return new Payload(); }
						};
					}
				}
				""")).containsExactly("Bill", "\\JsonSerializable", "Payload", "Payload");
	}

	// None of these is a class a name could be traced to: they are constants, functions,
	// the class itself or its parent (self, static, parent), or decided when the code
	// runs.
	@Test
	void constantsFunctionsVariablesAndTheClassItselfAreNotNamedClasses() {
		assertThat(namesWrittenIn("""
				<?php
				class A extends B
				{
					public function run($className, self $me, int|string|null $scalar): static
					{
						$a = strlen('x') + PHP_INT_MAX + \\M_PI;
						$b = new $className();
						$c = $className::create();
						$d = $this::LIMIT;
						$e = static::make();
						$f = new (static::class)();
						$g = array_map(strtoupper(...), []);
						$h = (string) $scalar;
						$i = $me?->name;
						return new static();
					}
					public function back(): ?self { return new self(); }
					public function up(): mixed { return parent::up() ?? new parent(); }
					public function never(): never { throw new \\LogicException(self::class); }
					public function shape(callable $f, iterable $i, array $a, object $o, bool $b, float $x): void {}
					public function literal(): true|false|null { return null; }
				}
				""")).containsExactly("B", "\\LogicException");
	}

	@Test
	void aFileMayHoldSeveralNamespacesWrittenAsStatements() {
		PhpSourceFacts facts = reader.factsOf("""
				<?php
				namespace App\\Models;
				use App\\Support\\Money;
				class Order { public function total(): Money {} }
				namespace App\\Http;
				class Kernel extends \\App\\Models\\Order {}
				""");

		assertThat(facts.namespaceBlocks()).containsExactly(
				new NamespaceBlock("App\\Models", List.of(new ClassImport("App\\Support\\Money", "Money")),
						List.of(new ClassDeclaration("Order", List.of("Money")))),
				new NamespaceBlock("App\\Http", List.of(),
						List.of(new ClassDeclaration("Kernel", List.of("\\App\\Models\\Order")))));
	}

	@Test
	void aFileMayHoldSeveralNamespacesWrittenAsBlocksTheGlobalOneIncluded() {
		PhpSourceFacts facts = reader.factsOf("""
				<?php
				namespace App\\Billing {
					use App\\Support\\Money;
					class Invoice { public function total(): Money {} }
				}
				namespace {
					class LegacyMailer extends Swift_Mailer {}
				}
				""");

		assertThat(facts.namespaceBlocks()).containsExactly(
				new NamespaceBlock("App\\Billing", List.of(new ClassImport("App\\Support\\Money", "Money")),
						List.of(new ClassDeclaration("Invoice", List.of("Money")))),
				new NamespaceBlock("", List.of(),
						List.of(new ClassDeclaration("LegacyMailer", List.of("Swift_Mailer")))));
	}

	@Test
	void aClassDeclaredWithoutANamespaceIsInTheGlobalOne() {
		assertThat(reader.factsOf("<?php class LegacyMailer extends Swift_Mailer {}").namespaceBlocks())
			.containsExactly(new NamespaceBlock("", List.of(),
					List.of(new ClassDeclaration("LegacyMailer", List.of("Swift_Mailer")))));
	}

	// Common in code older than autoloading: declare the class unless something did
	// already.
	@Test
	void aClassDeclaredUnderAConditionIsStillDeclared() {
		assertThat(declarationsOf("""
				<?php
				if (!class_exists('Polyfill')) {
					class Polyfill extends Base {}
				}
				""")).containsExactly(new ClassDeclaration("Polyfill", List.of("Base")));
	}

	@Test
	void whatAFunctionNextToAClassNamesIsNotNamedByTheClass() {
		assertThat(declarationsOf("""
				<?php
				function helper(User $user): Order { return new Order(); }
				class A { public function run(): Report {} }
				$started = new Clock();
				""")).containsExactly(new ClassDeclaration("A", List.of("Report")));
	}

	@Test
	void aFileThatDeclaresNoClassHasNoNamespaceBlocks() {
		assertThat(reader.factsOf("""
				<?php
				use App\\Http\\Controllers\\HomeController;
				Route::get('/', [HomeController::class, 'index']);
				return new class extends Migration { public function up(): void {} };
				""").namespaceBlocks()).isEmpty();
		assertThat(reader.factsOf("<h1>No PHP here</h1>\n").namespaceBlocks()).isEmpty();
		assertThat(reader.factsOf("").namespaceBlocks()).isEmpty();
	}

	@Test
	void phpWrittenInsideHtmlIsRead() {
		assertThat(declarationsOf("""
				<html><body>
				<?php class Page extends Layout {} ?>
				<ul>
				<?php foreach ($items as $item): ?>
				  <li><?= htmlspecialchars($item->name) ?></li>
				<?php endforeach; ?>
				</ul>
				</body></html>
				""")).containsExactly(new ClassDeclaration("Page", List.of("Layout")));
	}

	// The library counts in bytes and Java in characters: é is two bytes, ☕ three, 😀
	// four.
	@Test
	void namesAreCutRightWhateverIsWrittenBeforeThem() {
		PhpSourceFacts facts = reader.factsOf("""
				<?php
				// café ☕ 😀
				namespace App\\Modèles;
				/** 😀😀 */
				class Café extends \\Base\\Thé { public function serve(): Tasse { return new Tasse('☕'); } }
				""");

		assertThat(facts.namespaceBlocks()).containsExactly(new NamespaceBlock("App\\Modèles", List.of(),
				List.of(new ClassDeclaration("Café", List.of("\\Base\\Thé", "Tasse", "Tasse")))));
	}

	// One statement the grammar cannot read must not cost the class the rest of its body.
	@Test
	void aSyntaxErrorCostsOnlyWhatCannotBeRead() {
		assertThat(declarationsOf("""
				<?php
				class A extends Base
				{
					public function broken() { return new Order(; }
					public function fine(User $user): Report { return new Report(); }
				}
				""")).singleElement().satisfies(declaration -> {
			assertThat(declaration.name()).isEqualTo("A");
			assertThat(declaration.writtenClassNames()).contains("Base", "User", "Report");
		});
	}

	@Test
	void aGrammarThatCannotBeLoadedIsReportedWithThePlatformItFailedOn() {
		TreeSitterPhpSourceReader unavailable = new TreeSitterPhpSourceReader(() -> {
			throw new UnsatisfiedLinkError("no tree-sitter-php in java.library.path");
		});

		assertThatThrownBy(() -> unavailable.factsOf("<?php class A {}")).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining(System.getProperty("os.name"))
			.hasMessageContaining(System.getProperty("os.arch"))
			.hasMessageContaining("Linux")
			.hasMessageContaining("x86_64")
			.hasMessageContaining(System.getProperty("java.io.tmpdir"))
			.hasCauseInstanceOf(UnsatisfiedLinkError.class);
	}

	private List<ClassDeclaration> declarationsOf(String phpSource) {
		return reader.factsOf(phpSource).namespaceBlocks().stream().flatMap(b -> b.declarations().stream()).toList();
	}

	private List<ClassImport> importsOf(String phpSource) {
		return reader.factsOf(phpSource).namespaceBlocks().get(0).classImports();
	}

	private List<String> namesWrittenIn(String phpSource) {
		return declarationsOf(phpSource).get(0).writtenClassNames();
	}

}
