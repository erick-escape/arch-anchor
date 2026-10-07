/**
 * Finds a rendered FontAwesome icon by name. The icons are clickable controls in this UI, but
 * they render as `aria-hidden` SVGs, so role-based queries cannot reach them.
 *
 * @example
 * await userEvent.click(fontAwesomeIcon(document.body, 'ellipsis-vertical'));
 */
export function fontAwesomeIcon(root: ParentNode, iconName: string, index = 0): SVGElement {
  const icons = root.querySelectorAll<SVGElement>(`svg[data-icon="${iconName}"]`);
  const icon = icons[index];
  if (!icon) {
    throw new Error(
      `Expected FontAwesome icon "${iconName}" at index ${index}, found ${icons.length} icon(s)`
    );
  }
  return icon;
}
