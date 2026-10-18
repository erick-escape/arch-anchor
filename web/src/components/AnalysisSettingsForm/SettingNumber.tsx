import { InputNumber, Typography } from 'antd';
import { ReactNode } from 'react';
import styles from '../SettingsModal/Settings.module.css';

interface SettingNumberProps {
  // Also the input's accessible name.
  label: string;
  explanation: string;
  value: number | null;
  min: number;
  max?: number;
  step?: number;
  // Decimals kept; 0 for a count of classes or dependencies.
  precision: number;
  disabled?: boolean;
  onChange: (value: number) => void;
  // Shown after the input, e.g. a "No limit" switch.
  children?: ReactNode;
}

// One labelled number per setting. Clearing the input keeps the last value: every setting
// needs one, and an empty one would only fail on save.
const SettingNumber = ({
  label,
  explanation,
  value,
  min,
  max,
  step = 1,
  precision,
  disabled = false,
  onChange,
  children,
}: SettingNumberProps) => (
  <section aria-label={label} className={styles.setting}>
    <Typography.Text strong>{label}</Typography.Text>
    <Typography.Paragraph type="secondary" className={styles.explanation}>
      {explanation}
    </Typography.Paragraph>
    <div className={styles.inputRow}>
      <InputNumber
        aria-label={label}
        min={min}
        max={max}
        step={step}
        precision={precision}
        value={value}
        disabled={disabled}
        onChange={(changed) => {
          if (changed !== null) onChange(changed);
        }}
      />
      {children}
    </div>
  </section>
);

export default SettingNumber;
