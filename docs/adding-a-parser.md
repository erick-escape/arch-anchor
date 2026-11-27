# Adding a language

Arch Anchor reads Java and PHP. Everything after reading a project (modules, reference
classes, similarity, violations and the recommendations) is the same for every language
and never opens a source file. What changes from one language to the next is answered in
one place, a `LanguageParser`, and adding a language means writing one. This guide explains
what a parser owes the tool, the questions it has to answer for its language, and how to
add it, with the Java and PHP parsers as worked examples.

Contents:

1. [What a parser owes the tool](#1-what-a-parser-owes-the-tool)
2. [Five questions every language answers](#2-five-questions-every-language-answers)
3. [Step by step](#3-step-by-step)
4. [Rules](#4-rules)
5. [Packaging](#5-packaging)
6. [Checklist](#6-checklist)

## 1. What a parser owes the tool

A parser is a Spring bean that implements
[`LanguageParser`](../api/src/main/java/com/archanchor/services/parsers/LanguageParser.java):

```java
public interface LanguageParser {
	String language();                                                 // "java", "php"
	String expectedSources();                                          // where it looks, for the user
	List<Path> sourceFilesOf(Path projectDirectory) throws IOException; // production sources; nothing parsed
	List<ParsedClazz> clazzesOf(Path projectDirectory) throws IOException; // every class they declare
}

public record ParsedClazz(Path sourceFile, String name, String packageName, String qualifiedName,
		List<Dependency> dependencies) {}
```

Spring registers every `LanguageParser` bean in
[`LanguageParsers`](../api/src/main/java/com/archanchor/services/parsers/LanguageParsers.java),
and nothing else needs to know the language exists. Here is what the tool does with each
answer.

**`language()`** names the language in lower case. Two parsers of one language stop the
application at startup.

**`sourceFilesOf(project)`** decides which language a project is read in. Every parser is
asked, and the one that lists the most files reads the project (a tie goes to the first
language by name). A project is read in one language only, so that a stray file in another
does not add modules to it. The same question decides whether a directory of an imported
repository is a project root, and which subdirectories to suggest when it is not. It is
asked of many directories, so it must be cheap: list files, parse nothing. For a project in
another language it must return an empty list.

**`expectedSources()`** is what the user reads when no parser finds anything:

```
Found no sources to analyse in 'notes'. Expected Java sources under src/main/, or src/ when
there is no src/main/; or PHP sources in the directories composer.json autoloads, or anywhere
outside vendor/ and node_modules/ when it names none.
```

**`clazzesOf(project)`** gives the classes, and
[`ModuleService.getModules`](../api/src/main/java/com/archanchor/services/ModuleService.java)
builds the modules from them:

- **A module is a directory**: the directory of a class's source file. Modules are named after
  their directory and its parent (`bookstore/model`), with more of the path when two would
  otherwise share a name. The tool sorts classes by source file (`SourceFileOrder`). The
  classes of one file keep the order the parser gave them.
- **Similarity and violations compare package names.** A class's dependencies are a list of
  `Dependency(packageName, types)`. The similarity of two classes is computed from the sets of
  package names they depend on, and a module's violations are the packages it depends on that
  its reference classes do not. Package names are the vocabulary of the whole analysis: they
  appear in the exported constraints (`bookstore/web CAN-DEPEND bookstore.service`).
- **The qualified name links a dependency to a module.** Each `Type` in a dependency carries
  the `fullyQualifiedName` of the class it refers to. When it equals the `qualifiedName` of a
  class of the project, the dependency is on that class's module. That is how the graph draws
  an edge between two modules, and how a class moved into a module stops counting that
  module's own classes as dependencies (`ModuleScope`, #43). The parser must therefore write a
  class's name the same way in both places.
- **No classes is refused.** A project whose sources declare no class gets a 422 that names
  the language and counts the files read: "Expected a class in the 2 php source files of
  'site', found none".

## 2. Five questions every language answers

Each question below has an answer per language, and those answers are the whole difference
between two parsers. Write the new language's answers in the issue before writing code.

| Question | Java | PHP |
|---|---|---|
| Which files are production sources? | `.java` files under `src/main/`, or under `src/` when there is no `src/main/` (#47) | `.php` files under the directories `composer.json` autoloads (`psr-4`, `psr-0`, `classmap`; never `autoload-dev`). Without one, every `.php` file. Never inside Composer's `vendor/`, `node_modules/` or a dot-directory |
| What is a class? | One per file, named after the file. `package-info.java` and `module-info.java` are not classes (#31) | Each class, interface, trait or enum a file declares. A file may declare several, or none: a template, a file of routes or a migration returning an anonymous class is not a class |
| What are its package and qualified name? | The package the file declares; `bookstore.web.SalesReport`. A class of the default package has neither, since nothing can import it | Its namespace; `App\Models\Order`. A class of the global namespace has no package, and its qualified name is its bare name |
| What is a dependency? | Every type the file names, resolved through its imports and its classpath | Every class the file imports, used or not, and every name written where PHP expects a class: `extends`, `implements`, types, attributes, `new`, `X::`, `instanceof`, `catch`. Resolved by PHP's own rules for imports and aliases. Function names, `self`, `static`, `parent` and scalar types are not classes |
| How are platform and third-party classes filed? | Under their packages: `java.sql`, `java.time`, `org.springframework.web.bind.annotation` | Third-party classes under their namespaces. Built-in classes under `php` (core, SPL, standard) or `ext-<name>` for the other extensions PHP ships with (`ext-pdo`, `ext-date`). A global class that is neither built-in nor declared in the project goes under `(global)` |

What each answer has to achieve:

1. **Production sources only.** Tests and generated code are not modules: a test class
   depends on what it tests and on its test library, and would distort the similarity of the
   module it lands in. The answer must not need third-party code to be present (see
   [Rules](#4-rules)). It must also come out the same for a folder uploaded from the browser,
   which carries only sources and manifests, and for a repository imported from GitHub,
   which carries everything.
2. **A class is a unit others can name.** A file that declares nothing others can depend on is
   not a class. Counted as one, it would be a class without dependencies, which is similar to
   nothing, lowers its module's similarity, and can anchor a split (#31).
3. **One name, written one way.** The package is the string a dependency on the class is filed
   under, and the qualified name is the string other classes' dependencies refer to it by. Use
   the language's own spelling (`App\Models`, not `app.models`): it is what an architect of
   that language recognises, and what a paper about their system will quote.
4. **A dependency is a name the source writes**, resolved as the language resolves it, never
   guessed from a variable's value or a string. Group the types by package. Drop references
   to the class's own package: every class of a module would otherwise share a dependency on
   its own package, and similarity would rise for nothing. Keep the order deterministic,
   since exported constraints are numbered in it.
5. **Platform classes under what provides them.** Split the platform where a developer of the
   language would see separate parts, as Java has `java.sql` and `java.time`. One package
   for every built-in class would make using the database and using dates the same
   dependency, so a module that does one could not be told from a module that does the
   other. When the platform's classes have no package of their own, as in PHP, generate the
   table from an upstream source with a script that is checked in. Never write it by hand
   (`PhpPlatformClassesGenerator.java`, from JetBrains phpstorm-stubs).

## 3. Step by step

### Open an issue

Use the feature request template. Name the language, the parsing library you have in mind
and its license, and give the five answers above. Each step below is reviewable on its own
if it gets an issue and a pull request of its own, as the PHP parser did (#81, #83, #85,
#87, #89).

### Write the bookstore in the language

[`examples/bookstore`](../examples/bookstore/README.md) is a four-layer application of
thirteen classes with one class in the wrong layer. Every language the tool reads has it:
[`examples/bookstore-php`](../examples/bookstore-php/README.md) is the same bookstore class for
class. Write `examples/bookstore-<language>/` from the class table in the Java README.

The bookstore's numbers depend only on the set of packages each class depends on. If every
class of the new example depends on the counterparts of its Java class's packages, the tool
finds the same reference classes, the same violation and the same three recommendations
with the same rates. That equality is the strongest evidence a parser can give that it
reads its language correctly. The sets are:

| Classes | Depend on |
|---|---|
| `Book`, `Customer`, `Order` | the date package (`java.time`) |
| `BookRepository`, `CustomerRepository`, `OrderRepository` | the database package (`java.sql`), the model |
| `CatalogService`, `CustomerService`, `OrderService` | the persistence layer, the model |
| `CatalogController`, `CustomerController`, `OrderController` | the service layer, the model |
| `SalesReport`, in the web layer | the database package, the model |

Idiomatic code in the new language will sometimes name a package its Java counterpart does
not. The PHP bookstore's README lists
[what was avoided and why](../examples/bookstore-php/README.md#why-it-looks-the-way-it-does):
no base-library class (exceptions, collections) named anywhere, dates read from text in the
model rather than in the repositories, only the database's own exception caught. Expect to
make choices of the same kind, and record them in the example's README with the tool's output
once the parser works.

### Hold the example to the bookstore's numbers

Write the test before the parser. The assertions shared by every bookstore are in
[`BookstoreRunningExample`](../api/src/test/java/com/archanchor/services/BookstoreRunningExample.java),
and a language's test only says what its example and modules are called:

```java
class BookstoreKotlinRunningExampleTest extends BookstoreRunningExample {

	BookstoreKotlinRunningExampleTest() {
		super("bookstore-kotlin",
				new Layers("bookstore/model", "bookstore/persistence", "bookstore/service", "bookstore/web"));
	}

}
```

Then compare the new example with the Java one class by class, as
[`BookstorePhpRunningExampleTest`](../api/src/test/java/com/archanchor/services/BookstorePhpRunningExampleTest.java)
does: every class depends on the packages its Java counterpart does, through a table from
Java's package names to the language's, and every class is as similar to its module as its
counterpart. When the numbers disagree, this test names the class responsible.

Add the parser to
[`EveryLanguage.parsers()`](../api/src/test/java/com/archanchor/services/parsers/EveryLanguage.java),
which wires the parsers by hand for the tests that analyse real projects. CI runs the API's
tests whenever `examples/` changes.

### Write the parser

Put it in `api/src/main/java/com/archanchor/services/parsers/<language>/`, as a
`@Component` that implements `LanguageParser`.

- **Keep the parsing library behind an interface of your own**, implemented by the one
  class that touches the library. `PhpSourceReader` returns facts in the project's terms
  (namespaces, imports, declarations and the names written in them), and
  `TreeSitterPhpSourceReader` is the only class that imports tree-sitter. The parser is then
  tested with a named fake reader, and the library can be replaced without touching the
  parser. The Java parser reads dependencies through `JavaDependencyReader` for the same
  reason.
- **Test the reader on inline sources.** Its tests are where the assumptions about the
  library are confirmed: node kinds, what a broken file yields, how text is decoded.
- **Test name resolution apart**, if the language has imports and aliases (`PhpNameResolver`).
- **Test the source listing on a temporary directory** (`@TempDir`), including what must be
  left out.
- `ScriptedLanguageParser`, in the tests, stands for a parser of any language when a test is
  about what the tool does with a parser's answer rather than about reading sources.

Two places outside the parser assume how names are written, and need changing only if the
new language writes names differently:

- A type nested in another is found through its enclosing type, by cutting the name at its
  last `.`: `ModuleScope.enclosing` in the API and `enclosingTypeOf` in
  `web/src/screens/Analyze/graph/moduleEdges.ts`. Change both together.
- `web/src/utils/simpleTypeName.ts` shortens a type to what follows the last `.` or `\`.

Build every `Type` with both of its fields, `new Type(fullyQualifiedName, className)`. The
one-argument constructor splits the name at a `.`, which is right only for Java.

### Let the web send the language's files

A folder chosen in the browser is filtered before it is uploaded, so that only what a parser
reads leaves the user's machine.

- [`web/src/services/analysableFiles.ts`](../web/src/services/analysableFiles.ts): add the
  language's source extensions to `SOURCE_EXTENSIONS`, and its manifest to `MANIFESTS` if the
  parser reads one. If the ecosystem installs packages inside the project, as `node_modules/`
  and Composer's `vendor/` are, leave that directory out by the same rule the parser uses.
- `web/src/services/projectUploadApi.ts` names the expected extensions when a folder holds
  none, and `web/src/screens/Home/Hero.tsx` names the languages (its test is `Home.test.tsx`).
- `web/src/test/fixtures/bookstoreFixture.ts` gives the bookstore's modules in each language,
  so that the analysis page is tested on the names the language writes.

### Check it in the image

[`.github/scripts/image-smoke-test.sh`](../.github/scripts/image-smoke-test.sh) runs the
Docker image and analyses every bookstore over HTTP. Add `analyse bookstore-<language>` at its
end. The Release workflow runs it on amd64 and on emulated arm64 for every pull request that
changes `Dockerfile` or `api/pom.xml`, which adding a library does.

### Update the documentation

- `README.md`: the languages in the opening and in "What it does"; where a project's root is,
  in "Running it on your own project"; what the parser does not see, in "Limitations".
- `api/CLAUDE.md` and `CLAUDE.md`: the parser's classes, one line each.
- `CITATION.cff` (abstract and keywords), the `Dockerfile`'s description label and the
  description in `api/pom.xml`.
- This guide: a column in the table of [section 2](#2-five-questions-every-language-answers).

## 4. Rules

- **No special cases for a framework or a library.** The parser reads every project of its
  language by the same rules. The PHP parser reads Laravel, Symfony and plain PHP alike. A
  shortcut that makes one ecosystem's projects come out better is a bug, even when the
  result looks right.
- **Never run the analysed code or its build.** No interpreter, no `composer install`,
  `npm install` or `mvn`: the project may be anyone's, and reading it must not execute it.
  The language's runtime is not in the image either (see [Packaging](#5-packaging)).
- **Read the project as it was given.** An uploaded folder carries no third-party code,
  so a parser must not depend on `vendor/`, `node_modules/` or the like being present.
  The Java parser downloads the jars a `pom.xml` or `build.gradle` declares, to resolve
  types; it is the one place an analysis goes online, and a new parser should not add
  another.
- **Stay inside the project directory.** A path read from a manifest is normalised and
  ignored when it leaves the project (`ComposerAutoload`), and the parser does not follow
  links out of it.
- **Same sources, same answer.** Classes, their order and their dependencies must depend on
  the sources alone, never on the order a filesystem lists files in or on a hash map's: a tie
  between equally rated classes goes to the first one (#15). List sources with
  `SourceFileOrder`.
- **A broken file never fails the project.** It yields what can still be read of it; the
  Java parser keeps the class without dependencies, the PHP parser keeps whatever its
  readable part declares. Log a warning that names the file. A file too large to be written
  by hand can be left out (the PHP parser skips files over 2 MB).

## 5. Packaging

The tool is released as one jar and one Docker image, and runs on Java 17 with nothing else
installed (#77). A parser keeps it that way: the analysed language's runtime is never needed,
on the user's machine or in the image.

- **A library written for the JVM** needs nothing more than its dependency in `api/pom.xml`,
  under a license compatible with MIT.
- **A native library**, as tree-sitter is, must ship builds for Linux with glibc on x86_64 and
  aarch64, the two architectures of the image (`eclipse-temurin:17-jre-jammy`), and should
  ship them for macOS and Windows, where people run the tool from source. Load it when the
  first source of the language is read, never when the application starts, so that a
  platform without it still analyses every other language. When it cannot be loaded, say
  which platform it was (`os.name`, `os.arch`) and which ones are supported
  (`TreeSitterPhpSourceReader`). A bug in native code can crash the JVM instead of throwing,
  so give the library one file at a time and release what it allocates after each.
- **Data generated from an upstream source**, such as the PHP built-in classes, is checked in
  together with its generator, the upstream version it was generated from, and the upstream
  license's attribution.

## 6. Checklist

Copy it into the pull request.

- [ ] An issue with the five answers for the language
- [ ] `examples/bookstore-<language>/`, with a README that records the tool's output and says
      why the code looks the way it does
- [ ] `Bookstore<Language>RunningExampleTest` extends `BookstoreRunningExample`, and the
      example matches the Java bookstore class by class
- [ ] The parser in `services/parsers/<language>/`, its library behind an interface of its own,
      `language()` in lower case, `expectedSources()` written for the user
- [ ] Tests of the reader on inline sources, of name resolution, and of the source listing
- [ ] The parser in `EveryLanguage.parsers()`
- [ ] The language's files in `analysableFiles.ts`; the languages named in the upload error and
      on the home page; the bookstore fixture in the language
- [ ] `analyse bookstore-<language>` in `image-smoke-test.sh`, and the Release workflow green on
      both architectures
- [ ] README, both `CLAUDE.md` files, `CITATION.cff`, the `Dockerfile` label, `api/pom.xml` and
      the table in this guide
- [ ] `cd api && ./mvnw test` and `cd web && npm run lint && npm test && npm run build`
- [ ] A real project in the language analysed, with what you checked of the result in the
      pull request
