import type { FolderFile } from './projectUploadApi';

// What the parsers read (#87): the sources, and the manifests that say where the sources are
// (composer.json) or what they depend on (pom.xml, build.gradle). Adding a language adds its
// extension and manifests here.
const SOURCE_EXTENSIONS = ['.java', '.php'];
const MANIFESTS = new Set(['pom.xml', 'build.gradle', 'build.gradle.kts', 'composer.json']);

const NODE_PACKAGES = 'node_modules';
const COMPOSER_PACKAGES = 'vendor';
// Composer writes it into the directory it installs packages in; a vendor/ of the project's own
// has none.
const COMPOSER_AUTOLOADER = 'autoload.php';

/**
 * Whether the tool reads a file of this name: a source or a manifest. Everything else, `.env`
 * included, stays on the user's machine.
 *
 * @example isAnalysableFile('Order.php'); // true
 */
export function isAnalysableFile(fileName: string): boolean {
  const lowerCase = fileName.toLowerCase();
  return MANIFESTS.has(fileName) || SOURCE_EXTENSIONS.some((ext) => lowerCase.endsWith(ext));
}

/**
 * Whether a directory of this name holds no code of the project's own, whatever is in it:
 * tooling (`.git`, `.idea`) and the packages npm installed.
 *
 * @example isOutsideTheProject('node_modules'); // true
 */
export function isOutsideTheProject(directoryName: string): boolean {
  return directoryName.startsWith('.') || directoryName === NODE_PACKAGES;
}

/**
 * Whether a directory is where Composer installed the project's packages, told by its name and
 * the names of the files directly in it.
 *
 * @example isComposerVendor('vendor', ['autoload.php']); // true
 */
export function isComposerVendor(directoryName: string, fileNames: string[]): boolean {
  return directoryName === COMPOSER_PACKAGES && fileNames.includes(COMPOSER_AUTOLOADER);
}

/**
 * The files of an uploaded folder that the tool reads, outside third-party directories. A folder
 * picker lists the whole tree, so those directories are told apart by path here.
 *
 * @example analysableFilesOf(folderFilesOf(files)); // without node_modules/, vendor/ or .env
 */
export function analysableFilesOf(files: FolderFile[]): FolderFile[] {
  const composerVendors = composerVendorsAmong(files.map((file) => file.path));
  return files.filter(
    ({ path }) =>
      isAnalysableFile(path.split('/').pop() ?? path) && !isInThirdParty(path, composerVendors)
  );
}

// "shop/vendor/autoload.php" makes "shop/vendor" Composer's.
function composerVendorsAmong(paths: string[]): Set<string> {
  const autoloader = `/${COMPOSER_PACKAGES}/${COMPOSER_AUTOLOADER}`;
  return new Set(
    paths
      .filter((path) => path.endsWith(autoloader))
      .map((path) => path.slice(0, -(COMPOSER_AUTOLOADER.length + 1)))
  );
}

// The first segment is the folder the user chose, taken whatever its name; the last is the file.
function isInThirdParty(path: string, composerVendors: Set<string>): boolean {
  const segments = path.split('/');
  for (let end = 2; end < segments.length; end += 1) {
    const directory = segments.slice(0, end).join('/');
    if (isOutsideTheProject(segments[end - 1]) || composerVendors.has(directory)) return true;
  }
  return false;
}
