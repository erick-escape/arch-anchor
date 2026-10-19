import { faMagnifyingGlass } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { Modal } from 'antd';
import { useEffect, useId, useMemo, useRef, useState, type KeyboardEvent } from 'react';
import styles from './CommandPalette.module.css';
import { searchCommands, type CommandGroup, type PaletteCommand } from './commandSearch';

interface CommandPaletteProps {
  open: boolean;
  commands: PaletteCommand[];
  onClose: () => void;
}

/**
 * The palette's dialog: a search field over the matching commands, grouped. Arrow keys move,
 * Enter runs, Esc closes; a click runs too. It starts empty every time it opens.
 *
 * @example <CommandPalette open={open} commands={commands} onClose={close} />
 */
export function CommandPalette({ open, commands, onClose }: CommandPaletteProps) {
  const run = (command: PaletteCommand) => {
    onClose();
    command.run();
  };
  return (
    <Modal
      open={open}
      title="Command palette"
      onCancel={onClose}
      footer={null}
      closable={false}
      width={560}
      destroyOnClose
      classNames={{ header: styles.hiddenTitle, content: styles.content }}
    >
      <PaletteSearch commands={commands} onRun={run} />
    </Modal>
  );
}

function usePaletteCursor(count: number) {
  const [active, setActive] = useState(0);
  const current = count === 0 ? -1 : Math.min(active, count - 1);
  const move = (step: number) => setActive((current + step + count) % count);
  return { current, setActive, move };
}

function PaletteSearch({
  commands,
  onRun,
}: {
  commands: PaletteCommand[];
  onRun: (command: PaletteCommand) => void;
}) {
  const ids = useId();
  const input = useRef<HTMLInputElement>(null);
  const [query, setQuery] = useState('');
  // The dialog moves focus to itself as it opens, after this mounts; take it back once it has.
  useEffect(() => {
    const timer = setTimeout(() => input.current?.focus());
    return () => clearTimeout(timer);
  }, []);
  const groups = useMemo(() => searchCommands(commands, query), [commands, query]);
  const found = groups.flatMap((group) => group.commands);
  const cursor = usePaletteCursor(found.length);
  const keys = (event: KeyboardEvent) => {
    const step = { ArrowDown: 1, ArrowUp: -1 }[event.key];
    if (step && found.length) {
      event.preventDefault();
      cursor.move(step);
    }
    if (event.key === 'Enter' && found[cursor.current]) onRun(found[cursor.current]);
  };
  return (
    <>
      <div className={styles.searchRow}>
        <FontAwesomeIcon icon={faMagnifyingGlass} className={styles.searchIcon} />
        <input
          ref={input}
          role="combobox"
          aria-label="Search commands"
          aria-expanded
          aria-controls={`${ids}-results`}
          aria-autocomplete="list"
          aria-activedescendant={cursor.current >= 0 ? `${ids}-${cursor.current}` : undefined}
          className={styles.search}
          placeholder="Modules, classes, recommendations, pages…"
          value={query}
          onChange={(event) => {
            setQuery(event.target.value);
            cursor.setActive(0);
          }}
          onKeyDown={keys}
        />
        <kbd className={styles.kbd}>esc</kbd>
      </div>
      <Results
        ids={ids}
        groups={groups}
        current={cursor.current}
        onHover={cursor.setActive}
        onRun={onRun}
      />
      {found.length === 0 && <p className={styles.nothing}>No command matches “{query}”.</p>}
    </>
  );
}

interface ResultsProps {
  ids: string;
  groups: CommandGroup[];
  current: number;
  onHover: (index: number) => void;
  onRun: (command: PaletteCommand) => void;
}

function Results({ ids, groups, current, onHover, onRun }: ResultsProps) {
  let index = -1;
  return (
    <div role="listbox" id={`${ids}-results`} aria-label="Results" className={styles.results}>
      {groups.map((group) => (
        <div key={group.name} role="group" aria-labelledby={`${ids}-${group.name}`}>
          <span id={`${ids}-${group.name}`} className={styles.groupName}>
            {group.name}
          </span>
          {group.commands.map((command) => {
            index += 1;
            const position = index;
            return (
              <div
                key={command.id}
                id={`${ids}-${position}`}
                role="option"
                aria-selected={position === current}
                className={position === current ? styles.optionActive : styles.option}
                onMouseMove={() => onHover(position)}
                onClick={() => onRun(command)}
              >
                <span className={styles.optionText}>
                  <span className={styles.optionLabel}>{command.label}</span>
                  {command.detail && <span className={styles.optionDetail}>{command.detail}</span>}
                </span>
                {position === current && <span className={styles.enter}>↵</span>}
              </div>
            );
          })}
        </div>
      ))}
    </div>
  );
}
