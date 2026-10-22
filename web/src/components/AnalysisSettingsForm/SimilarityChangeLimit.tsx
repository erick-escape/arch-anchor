import { Checkbox } from 'antd';
import SettingNumber from './SettingNumber';

interface SimilarityChangeLimitProps {
  // Also the input's accessible name.
  label: string;
  explanation: string;
  value: number | null;
  onChange: (value: number | null) => void;
}

// The lowest similarity change of a recommended merge (#41), move or split (#73): a change
// lies in [-1, 1], and null means no limit. Unticking "No limit" starts from 0, which keeps
// out every change that lowers similarity.
const SimilarityChangeLimit = ({
  label,
  explanation,
  value,
  onChange,
}: SimilarityChangeLimitProps) => (
  <SettingNumber
    label={label}
    explanation={explanation}
    value={value}
    min={-1}
    max={1}
    step={0.05}
    precision={2}
    disabled={value === null}
    onChange={onChange}
  >
    <Checkbox
      checked={value === null}
      onChange={(event) => onChange(event.target.checked ? null : 0)}
    >
      No limit
    </Checkbox>
  </SettingNumber>
);

export default SimilarityChangeLimit;
